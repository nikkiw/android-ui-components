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
import android.widget.FrameLayout
import androidx.core.graphics.withClip
import androidx.core.view.isVisible

/**
 * A custom FrameLayout that renders a GPU-accelerated shimmer effect over its visible children.
 *
 * This view uses a hardware layer and a linear gradient shader to create a moving "shine" animation.
 * The shimmer automatically starts when the view is attached to a window and stops when detached.
 *
 * @constructor Creates a new [ShimmerViewGPU].
 * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
 * @param attrs An optional set of XML attributes to initialize the view.
 * @param defStyleAttr An optional default style attribute to apply to this view.
 */
class ShimmerViewGPU @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /** Paint used to draw the shimmer gradient. */
    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** Matrix to translate the shader, creating the moving shimmer effect. */
    private val shaderMatrix = Matrix()

    /** Animator driving the horizontal translation of the shimmer. */
    private var shimmerAnimator: ValueAnimator? = null

    /** Current horizontal offset of the shimmer shader. */
    private var shimmerTranslate = 0f

    /** Path defining the union of child view bounds to mask the shimmer effect. */
    private var maskPath: Path? = null

    private val startShimmerRunnable = Runnable { startShimmer() }

    init {
        // Enable hardware layer for better performance of the shader
        setLayerType(LAYER_TYPE_HARDWARE, shimmerPaint)
    }

    /**
     * Called when the size of this view changes.
     *
     * Sets up or updates the LinearGradient shader to span the full width.
     * Invalidates the mask path so it will be rebuilt for the new dimensions.
     *
     * @param w Current width of this view.
     * @param h Current height of this view.
     * @param oldw Previous width of this view.
     * @param oldh Previous height of this view.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w > 0 && h > 0) {
            shimmerPaint.shader = LinearGradient(
                0f, 0f,                   // start coordinates
                w.toFloat(), 0f,          // end coordinates
                intArrayOf(
                    Color.TRANSPARENT,    // fully transparent at 30% of gradient
                    Color.WHITE,          // opaque white at center
                    Color.TRANSPARENT     // fully transparent at 70% of gradient
                ),
                floatArrayOf(0.3f, 0.5f, 0.7f),
                Shader.TileMode.CLAMP
            )
            // Reset mask so it will be rebuilt on next draw
            maskPath = null
        }
    }

    /**
     * Builds a mask path encompassing all visible child views.
     *
     * This path is used to clip the shimmer effect so it only appears over children.
     */
    private fun buildMaskPath() {
        maskPath = Path().apply {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child.isVisible) {
                    addRect(
                        child.left.toFloat(),
                        child.top.toFloat(),
                        child.right.toFloat(),
                        child.bottom.toFloat(),
                        Path.Direction.CW
                    )
                }
            }
        }
    }

    /**
     * Draws children and then overlays the shimmer gradient clipped to the mask path.
     *
     * @param canvas The Canvas on which the background will be drawn.
     */
    override fun dispatchDraw(canvas: Canvas) {
        // Build mask if not present
        if (maskPath == null) buildMaskPath()

        // Save layer to apply shimmer paint separately
        val saveCount = canvas.saveLayer(
            0f, 0f,
            width.toFloat(), height.toFloat(),
            null
        )

        // Draw child views normally
        super.dispatchDraw(canvas)

        // Clip to mask and draw the moving shader
        maskPath?.let { path ->
            canvas.withClip(path) {
                // Move shader according to current animation value
                shaderMatrix.setTranslate(shimmerTranslate, 0f)
                shimmerPaint.shader?.setLocalMatrix(shaderMatrix)

                // Draw a full-coverage rectangle with the shimmer shader
                drawRect(0f, 0f, width.toFloat(), height.toFloat(), shimmerPaint)
            }
        }

        // Restore canvas
        canvas.restoreToCount(saveCount)
    }

    /**
     * Starts the shimmer animation.
     *
     * If the view has not been measured yet, retries after a short delay.
     */
    fun startShimmer() {
        removeCallbacks(startShimmerRunnable)

        if (width == 0 || height == 0) {
            // Wait until layout is complete
            postDelayed(startShimmerRunnable, 32)
            return
        }

        // Cancel any existing animator
        shimmerAnimator?.cancel()

        // Animate translation from left to right and repeat infinitely
        shimmerAnimator = ValueAnimator.ofFloat(-width.toFloat(), width.toFloat()).apply {
            duration = 1500L
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { anim ->
                shimmerTranslate = anim.animatedValue as Float
                postInvalidateOnAnimation()
            }
            start()
        }
    }

    /**
     * Stops the shimmer animation and invalidates the view.
     */
    fun stopShimmer() {
        removeCallbacks(startShimmerRunnable)

        shimmerAnimator?.run {
            removeAllUpdateListeners()
            cancel()
        }
        shimmerAnimator = null
        postInvalidate()
    }

    /**
     * Automatically start shimmer when the view is attached to a window.
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startShimmer()
    }

    /**
     * Stop shimmer when the view is detached from a window to avoid resource leaks.
     */
    override fun onDetachedFromWindow() {
        stopShimmer()
        super.onDetachedFromWindow()
    }
}
