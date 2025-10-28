package com.ndev.android.ui.shimmer

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import com.ndev.android.ui.components.R
import androidx.core.graphics.toColorInt

/**
 * A flexible and optimized ShimmerView for displaying loading animations.
 *
 * This view creates a shimmering effect over its child views, which are typically placeholders
 * for content that is loading. It is highly customizable and lifecycle-aware.
 *
 * Key Features:
 * - Supports custom color arrays for the shimmer gradient via the `shimmerColors` attribute.
 * - Utilizes a default style (`Ndev.Shimmer.Default`) as a fallback for attributes.
 * - Allows for preview-only overrides in the layout editor using the `tools:*` namespace.
 * - Includes adaptive frame throttling to reduce rendering overhead and improve performance.
 * - Automatically handles starting, pausing, and resuming the animation based on view visibility and lifecycle state.
 *
 * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr An attribute in the current theme that contains a reference to a style resource
 *                     that supplies default values for the view. Can be 0 to not look for defaults.
 */
class ShimmerViewOpt @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // --- XML Attributes (with default values) ---

    private var baseColor: Int = "#EEEEEE".toColorInt()
    private var highlightColor: Int = "#FFFFFF".toColorInt()
    private var edgeColor: Int = Color.TRANSPARENT

    /** An array of colors to be used in the shimmer gradient. Overrides single color attributes. */
    private var colorsArray: IntArray? = null

    private var orientation: Int = ORIENTATION_VERTICAL
    private var direction: Int = DIRECTION_FORWARD

    /** The width of the shimmer highlight as a fraction of the view's total size (0.0 to 1.0). */
    private var highlightWidth: Float = 0.2f
    /** The position of the highlight's center within the gradient (0.0 to 1.0). */
    private var centerPos: Float = 0.5f
    /** The alpha (transparency) of the shimmer effect (0.0 to 1.0). */
    private var shimmerAlphaF: Float = 1.0f

    /** The duration of one shimmer animation cycle in milliseconds. */
    private var shimmerDurationAttr: Long = 1500L
    /** If true, the shimmer animation will start automatically when the view is attached. */
    private var shimmerAutoStart: Boolean = true
    /** If true, enables adaptive frame throttling to save CPU/GPU resources. */
    private var useAdaptiveThrottling: Boolean = true

    companion object {
        private const val ORIENTATION_VERTICAL = 0
        private const val ORIENTATION_HORIZONTAL = 1
        private const val ORIENTATION_DIAGONAL = 2

        private const val DIRECTION_FORWARD = 0
        private const val DIRECTION_REVERSE = 1
    }

    // --- Internal Properties ---

    /** The Paint object used to draw the shimmer gradient, with anti-aliasing enabled. */
    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isAntiAlias = true
    }

    /** A matrix used to translate the gradient shader, creating the animation effect. */
    private val shaderMatrix = Matrix()

    /** The animator responsible for driving the shimmer animation. */
    private var shimmerAnimator: ValueAnimator? = null
    /** A coroutine job that waits for the view to be measured before starting the animation. */
    private var shimmerStarterJob: Job? = null

    /** The current translation value of the shimmer, updated by the animator. */
    private var shimmerTranslate = 0f

    /** The LinearGradient shader that creates the shimmer effect. */
    private var gradientShader: LinearGradient? = null

    /** The actual duration of the animation, derived from XML attributes. */
    private var shimmerDuration = shimmerDurationAttr

    // --- Adaptive Frame Throttling Properties ---
    /** Exponential Moving Average of frame time in nanoseconds. Starts at a 60fps equivalent. */
    private var emaFrameNs: Double = 16_666_667.0
    private val emaAlpha = 0.12 // Smoothing factor for the EMA calculation.

    private var lastAnimatorUpdateNs: Long = 0L
    private var lastInvalidateNs: Long = 0L
    /** Hard limit for frame time, prevents stuttering on high-refresh-rate displays (e.g., 120Hz). */
    private val minFrameNsHardLimit = 8_333_333L
    /** The target frame time for a smooth 60fps animation. */
    private val targetFrameNs = 16_666_667L

    /**
     * A LifecycleObserver that automatically pauses and resumes the animation
     * in response to the host's lifecycle events (e.g., onPause, onResume).
     */
    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) { resumeShimmer() }
        override fun onPause(owner: LifecycleOwner) { pauseShimmer() }
    }

    init {
        // Use a hardware layer for performance.
        setLayerType(LAYER_TYPE_HARDWARE, null)
        // This view will draw the shimmer effect itself in dispatchDraw.
        setWillNotDraw(false)
        // Allow drawing outside of the view's bounds.
        clipChildren = false
        clipToPadding = false

        // Read attributes, using the library's default style as a fallback.
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.ShimmerView, defStyleAttr, R.style.Ndev_Shimmer_Default)
        try {
            baseColor = a.getColor(R.styleable.ShimmerView_shimmerBaseColor, baseColor)
            highlightColor = a.getColor(R.styleable.ShimmerView_shimmerHighlightColor, highlightColor)
            edgeColor = a.getColor(R.styleable.ShimmerView_shimmerEdgeColor, edgeColor)

            // If a color array resource is provided, load it.
            val colorsResId = a.getResourceId(R.styleable.ShimmerView_shimmerColors, 0)
            if (colorsResId != 0) {
                colorsArray = loadColorArray(colorsResId, highlightColor)
            }

            orientation = a.getInt(R.styleable.ShimmerView_shimmerOrientation, ORIENTATION_VERTICAL)
            direction = a.getInt(R.styleable.ShimmerView_shimmerDirection, DIRECTION_FORWARD)

            highlightWidth = a.getFloat(R.styleable.ShimmerView_shimmerHighlightWidth, highlightWidth).coerceIn(0.01f, 1f)
            centerPos = a.getFloat(R.styleable.ShimmerView_shimmerCenterPos, centerPos).coerceIn(0f, 1f)
            shimmerAlphaF = a.getFloat(R.styleable.ShimmerView_shimmerAlpha, shimmerAlphaF).coerceIn(0f, 1f)

            shimmerDuration = a.getInt(R.styleable.ShimmerView_shimmerDuration, shimmerDurationAttr.toInt()).toLong()
            shimmerAutoStart = a.getBoolean(R.styleable.ShimmerView_shimmerAutoStart, true)
            useAdaptiveThrottling = a.getBoolean(R.styleable.ShimmerView_shimmerUseAdaptiveThrottling, true)
        } finally {
            a.recycle()
        }

        shimmerPaint.alpha = (shimmerAlphaF * 255).toInt()

        if (shimmerAutoStart && isAttachedToWindow) startShimmer()
    }

    /**
     * Loads a color array from a resource ID.
     * @param resId The resource ID of the color array.
     * @param fallback A fallback color to use if a color in the array cannot be resolved.
     * @return An IntArray of colors.
     */
    private fun loadColorArray(resId: Int, fallback: Int): IntArray {
        val ta: TypedArray = resources.obtainTypedArray(resId)
        val result = IntArray(ta.length())
        for (i in 0 until ta.length()) {
            // This safely handles both direct hex values and color resource references.
            result[i] = try { ta.getColor(i, fallback) } catch (e: Exception) { fallback }
        }
        ta.recycle()
        return result
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Attach to the lifecycle of the owner (Activity/Fragment) to auto-pause/resume.
        findViewTreeLifecycleOwner()?.lifecycle?.addObserver(lifecycleObserver)
        if (shimmerAutoStart) startShimmer()
    }

    override fun onDetachedFromWindow() {
        // Clean up resources to prevent leaks.
        stopShimmer()
        findViewTreeLifecycleOwner()?.lifecycle?.removeObserver(lifecycleObserver)
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        // Recreate the gradient and animator when the view size changes.
        createGradient(w, h)
        shimmerPaint.shader = gradientShader
        recreateAnimatorIfNeeded()
    }

    /**
     * Creates the LinearGradient shader based on the current orientation and color settings.
     * @param w The width of the view.
     * @param h The height of the view.
     */
    private fun createGradient(w: Int, h: Int) {
        // Use the provided color array, or fall back to a 3-color gradient.
        val colors = colorsArray ?: intArrayOf(edgeColor, highlightColor, edgeColor)

        // If using a 3-color gradient, create specific positions for a centered highlight.
        if (colors.size == 3) {
            val half = highlightWidth.coerceIn(0.01f, 1f) / 2f
            val p0 = (centerPos - half).coerceIn(0f, 1f) // Start position
            val p1 = centerPos.coerceIn(0f, 1f)         // Center position
            val p2 = (centerPos + half).coerceIn(0f, 1f) // End position
            val positions = floatArrayOf(p0, p1, p2)

            gradientShader = when (orientation) {
                ORIENTATION_HORIZONTAL -> LinearGradient(0f, 0f, w.toFloat(), 0f, colors, positions, Shader.TileMode.CLAMP)
                ORIENTATION_DIAGONAL -> LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), colors, positions, Shader.TileMode.CLAMP)
                else -> LinearGradient(0f, 0f, 0f, h.toFloat(), colors, positions, Shader.TileMode.CLAMP) // Default to Vertical
            }
        } else {
            // For gradients with more or fewer than 3 colors, space them evenly.
            gradientShader = when (orientation) {
                ORIENTATION_HORIZONTAL -> LinearGradient(0f, 0f, w.toFloat(), 0f, colors, null, Shader.TileMode.CLAMP)
                ORIENTATION_DIAGONAL -> LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), colors, null, Shader.TileMode.CLAMP)
                else -> LinearGradient(0f, 0f, 0f, h.toFloat(), colors, null, Shader.TileMode.CLAMP) // Default to Vertical
            }
        }
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        // Ensure child views are also on a hardware layer for smooth rendering.
        child.setLayerType(LAYER_TYPE_HARDWARE, null)
        postInvalidateOnAnimation()
    }

    override fun onViewRemoved(child: View) {
        super.onViewRemoved(child)
        postInvalidateOnAnimation()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) postInvalidateOnAnimation()
    }

    /**
     * Overridden to draw the shimmer effect over the child views.
     */
    override fun dispatchDraw(canvas: Canvas) {
        val drawStart = System.nanoTime()

        // First, let the children draw themselves.
        super.dispatchDraw(canvas)

        val animator = shimmerAnimator
        val shader = gradientShader
        // Only draw the shimmer if the animation is running and the shader is ready.
        if (animator?.isRunning != true || shader == null) return

        // Translate the shader matrix based on the current animation value.
        shaderMatrix.reset()
        when (orientation) {
            ORIENTATION_HORIZONTAL -> shaderMatrix.setTranslate(shimmerTranslate, 0f)
            ORIENTATION_DIAGONAL -> shaderMatrix.setTranslate(shimmerTranslate, shimmerTranslate)
            else -> shaderMatrix.setTranslate(0f, shimmerTranslate)
        }
        shader.setLocalMatrix(shaderMatrix)
        shimmerPaint.shader = shader

        // Iterate through all visible child placeholders and draw the shimmer on top of them.
        for (container in children) {
            if (container !is ViewGroup || !container.isVisible) continue

            for (placeholder in container.children) {
                if (!placeholder.isVisible) continue

                // Calculate the absolute coordinates of the placeholder.
                val cLeft = (container.left + placeholder.left).toFloat()
                val cTop = (container.top + placeholder.top).toFloat()
                val cRight = (container.left + placeholder.right).toFloat()
                val cBottom = (container.top + placeholder.bottom).toFloat()

                if (cRight <= cLeft || cBottom <= cTop) continue

                canvas.drawRect(cLeft, cTop, cRight, cBottom, shimmerPaint)
            }
        }

        // Update the EMA of frame duration for adaptive throttling.
        val drawEnd = System.nanoTime()
        val drawDuration = (drawEnd - drawStart).toDouble()
        emaFrameNs = emaFrameNs * (1.0 - emaAlpha) + drawDuration * emaAlpha
    }

    /**
     * Starts the shimmer animation. If the view has not been measured yet,
     * it will wait until the size is available before starting.
     */
    fun startShimmer() {
        shimmerStarterJob?.cancel()

        // If view is already measured, start immediately.
        if (width > 0 && height > 0) {
            ensureAnimatorStarted()
            return
        }

        // Otherwise, launch a coroutine to wait for the next frame after layout.
        val lifecycleOwner = findViewTreeLifecycleOwner() ?: run {
            // Fallback if no lifecycle owner is found (e.g., in a preview).
            ensureAnimatorStarted()
            return
        }

        shimmerStarterJob = lifecycleOwner.lifecycleScope.launch {
            while (width == 0 || height == 0) {
                awaitFrame()
            }
            ensureAnimatorStarted()
        }
    }

    /**
     * Creates and starts the ValueAnimator if it doesn't already exist.
     */
    private fun ensureAnimatorStarted() {
        if (shimmerAnimator == null) {
            // The primary size depends on the orientation for translation calculation.
            val primarySize = if (orientation == ORIENTATION_HORIZONTAL) width.coerceAtLeast(1).toFloat() else height.coerceAtLeast(1).toFloat()

            // Define the animation range.
            val from: Float
            val to: Float
            if (direction == DIRECTION_REVERSE) {
                from = primarySize
                to = -primarySize
            } else {
                from = -primarySize
                to = primarySize
            }

            shimmerAnimator = ValueAnimator.ofFloat(from, to).apply {
                duration = shimmerDuration
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener { animation ->
                    shimmerTranslate = animation.animatedValue as Float

                    // --- Adaptive Frame Throttling Logic ---
                    // The goal of this logic is to prevent excessive redrawing and save CPU/GPU resources,
                    // especially on high-refresh-rate displays (e.g., 90Hz, 120Hz). The ValueAnimator may
                    // trigger updates very frequently (e.g., every 8ms on a 120Hz screen), but a smooth
                    // shimmer effect doesn't require such a high frame rate. Throttling redraws to a
                    // target (e.g., 60fps) is more efficient.
                    //
                    // The algorithm works as follows:
                    // 1.  **Measure:** It calculates the time (`delta`) between animator updates. This gives us
                    //     a raw measure of how fast the system is running the animation loop.
                    //
                    // 2.  **Smooth:** The raw `delta` can be noisy. To get a more stable value, we use an
                    //     Exponential Moving Average (EMA). This smooths out temporary fluctuations and gives
                    //     us a reliable average frame time (`emaFrameNs`).
                    //
                    // 3.  **Adapt and Throttle:**
                    //     a. **Throttling:** On a fast device, `emaFrameNs` might be low (e.g., 8ms). We don't want
                    //        to redraw that often. So, we take the larger value between our smoothed frame time
                    //        and a desired target (`targetFrameNs`, e.g., 16.6ms for 60fps). This effectively
                    //        caps the frame rate.
                    //     b. **Adapting:** If the system is under heavy load, `emaFrameNs` might become high
                    //        (e.g., 30ms). In this case, the algorithm adapts by using this higher value as the
                    //        target. This prevents us from queueing up redraw requests that the system can't handle,
                    //        which would otherwise lead to jank.
                    //
                    // 4.  **Decide:** Finally, it checks if enough time has passed since the *last actual redraw*
                    //     (`lastInvalidateNs`). If the elapsed time is greater than or equal to our calculated
                    //     `allowedFrameNs`, it requests a new frame via `postInvalidateOnAnimation()`.
                    //     Otherwise, it skips the redraw for this animator tick.
                    val now = System.nanoTime()
                    if (lastAnimatorUpdateNs != 0L) {
                        val delta = (now - lastAnimatorUpdateNs).toDouble()
                        emaFrameNs = emaFrameNs * (1.0 - emaAlpha) + delta * emaAlpha
                    }
                    lastAnimatorUpdateNs = now

                    // Calculate the target frame delay based on the EMA.
                    val adaptiveFrameNs = if (useAdaptiveThrottling) emaFrameNs.coerceAtLeast(targetFrameNs.toDouble()).toLong() else targetFrameNs
                    val allowedFrameNs = adaptiveFrameNs.coerceAtLeast(minFrameNsHardLimit)

                    // Invalidate only if enough time has passed since the last draw.
                    if (now - lastInvalidateNs >= allowedFrameNs) {
                        lastInvalidateNs = now
                        postInvalidateOnAnimation()
                    }
                }
            }
        }

        if (shimmerAnimator?.isStarted != true) {
            shimmerAnimator?.start()
        }
    }

    /**
     * Recreates the animator. This is necessary if properties like size or duration change while
     * the animation is running.
     */
    private fun recreateAnimatorIfNeeded() {
        val running = shimmerAnimator?.isRunning == true
        shimmerAnimator?.run {
            removeAllUpdateListeners()
            cancel()
        }
        shimmerAnimator = null
        if (running) {
            ensureAnimatorStarted()
        }
    }

    /** Pauses the shimmer animation if it is currently running. */
    fun pauseShimmer() {
        shimmerAnimator?.takeIf { it.isRunning }?.pause()
    }

    /** Resumes the shimmer animation if it was previously paused. */
    fun resumeShimmer() {
        shimmerAnimator?.takeIf { it.isPaused }?.resume()
    }

    /** Stops and cleans up the shimmer animation completely. */
    fun stopShimmer() {
        shimmerStarterJob?.cancel()
        shimmerStarterJob = null

        shimmerAnimator?.run {
            removeAllUpdateListeners()
            cancel()
        }
        shimmerAnimator = null
        // Request a final redraw to clear the last shimmer frame.
        postInvalidateOnAnimation()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        // Automatically pause/resume the animation when the view's visibility changes.
        if (visibility == VISIBLE) {
            resumeShimmer()
        } else {
            pauseShimmer()
        }
    }

    // --- Testing Methods ---
    @VisibleForTesting
    internal fun isAnimatorRunning(): Boolean = shimmerAnimator?.isRunning == true

    @VisibleForTesting
    internal fun isAnimatorPaused(): Boolean = shimmerAnimator?.isPaused == true

    @VisibleForTesting
    internal fun getAnimator(): ValueAnimator? = shimmerAnimator
}