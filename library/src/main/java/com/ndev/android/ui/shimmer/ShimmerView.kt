package com.ndev.android.ui.shimmer

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.graphics.withClip
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

/**
 * A custom FrameLayout that applies a shimmering gradient animation over its visible children.
 *
 * This view creates a horizontal LinearGradient shader with transparent edges and a white center,
 * and animates it from left to right repeatedly. The shimmer effect is clipped to the bounds of
 * the visible child views, creating a highlight sweep across the content.
 *
 * The animation is automatically paused when the view is not visible or when the host
 * Activity/Fragment is paused.
 *
 * @constructor Creates a ShimmerView using the provided context, attribute set, and style.
 * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr An attribute in the current theme that contains a reference to a style resource
 *                     that supplies default values for the view. Can be 0 to not look for defaults.
 */
class ShimmerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shaderMatrix = Matrix()

    private var shimmerAnimator: ValueAnimator? = null
    private var shimmerTranslate = 0f
    private var shimmerJob: Job? = null

    // A lifecycle observer to pause/resume the animation.
    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            resumeShimmer()
        }

        override fun onPause(owner: LifecycleOwner) {
            pauseShimmer()
        }
    }

    /**
     * Called when the size of this view changes. Initializes or updates the shader used for the shimmer effect.
     *
     * @param w Current width of the view.
     * @param h Current height of the view.
     * @param oldw Previous width of the view.
     * @param oldh Previous height of the view.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            shimmerPaint.shader = LinearGradient(
                0f, 0f, w.toFloat(), 0f,
                intArrayOf(Color.TRANSPARENT, Color.WHITE, Color.TRANSPARENT),
                floatArrayOf(0.3f, 0.5f, 0.7f),
                Shader.TileMode.CLAMP
            )
        }
    }

    /**
     * Draws the shimmer effect on top of the child views. Clips the shimmer to the bounds of visible children.
     *
     * @param canvas The Canvas on which the background will be drawn.
     */
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        // Only draw the shimmer if the animator is running.
        if (shimmerAnimator?.isRunning != true) return

        val maskPath = Path().apply {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child.isVisible) {
                    addRect(
                        child.left.toFloat(), child.top.toFloat(),
                        child.right.toFloat(), child.bottom.toFloat(),
                        Path.Direction.CW
                    )
                }
            }
        }

        canvas.withClip(maskPath) {
            shaderMatrix.setTranslate(shimmerTranslate, 0f)
            shimmerPaint.shader?.setLocalMatrix(shaderMatrix)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shimmerPaint)
        }
    }

    /**
     * Starts a coroutine that waits for the view to be laid out, then creates and starts the animator.
     */
    fun startShimmer() {
        shimmerJob?.cancel()
        shimmerJob = findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
            // Wait until the view has valid dimensions.
            while (width == 0 || height == 0) {
                awaitFrame()
            }
            // Create and start the animator if it doesn't exist yet.
            if (shimmerAnimator == null) {
                shimmerAnimator = ValueAnimator.ofFloat(-width.toFloat(), width.toFloat()).apply {
                    duration = 1500
                    repeatCount = ValueAnimator.INFINITE
                    addUpdateListener { animation ->
                        shimmerTranslate = animation.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            }
        }
    }

    /**
     * Pauses the animation.
     */
    fun pauseShimmer() {
        shimmerAnimator?.pause()
    }

    /**
     * Resumes the animation if it was paused.
     */
    fun resumeShimmer() {
        // Only resume if the view is visible.
        if (visibility == View.VISIBLE) {
            shimmerAnimator?.resume()
        }
    }

    /**
     * Completely stops and cancels the animation.
     */
    fun stopShimmer() {
        shimmerJob?.cancel()
        shimmerJob = null
        shimmerAnimator?.cancel()
        shimmerAnimator = null
        invalidate()
    }

    /**
     * Called when the view is attached to a window.
     * Starts the animation and registers the lifecycle observer.
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        findViewTreeLifecycleOwner()?.lifecycle?.addObserver(lifecycleObserver)
        startShimmer()
    }

    /**
     * Called when the view is detached from a window.
     * Stops the animation and unregisters the observer to prevent leaks.
     */
    override fun onDetachedFromWindow() {
        stopShimmer()
        findViewTreeLifecycleOwner()?.lifecycle?.removeObserver(lifecycleObserver)
        super.onDetachedFromWindow()
    }

    /**
     * Called when the visibility of the view changes.
     * Pauses or resumes the animation based on the new visibility state.
     */
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.VISIBLE) {
            resumeShimmer()
        } else {
            pauseShimmer()
        }
    }
}