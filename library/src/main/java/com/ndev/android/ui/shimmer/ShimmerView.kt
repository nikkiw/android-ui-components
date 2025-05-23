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
 * A custom FrameLayout that applies a shimmering gradient animation over its visible children.
 *
 * This view creates a horizontal LinearGradient shader with transparent edges and a white center,
 * and animates it from left to right repeatedly. The shimmer effect is clipped to the bounds of
 * the visible child views, creating a highlight sweep across the content.
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

    /** Paint used to draw the shimmer gradient. */
    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shaderMatrix = Matrix()
    private var shimmerAnimator: ValueAnimator? = null
    private var shimmerTranslate = 0f

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
            // Initialize a horizontal gradient with transparent edges and a white center
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
        // First, draw child views normally
        super.dispatchDraw(canvas)

        // Create a Path combining the bounds of all visible children for clipping
        val maskPath = Path().apply {
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

        // Clip the drawing area to the combined mask of children
        canvas.withClip(maskPath) {
            // Translate the gradient shader according to the current shimmerTranslate value
            shaderMatrix.setTranslate(shimmerTranslate, 0f)
            shimmerPaint.shader?.setLocalMatrix(shaderMatrix)

            // Draw the gradient across the entire container; only visible through the mask
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shimmerPaint)
        }
    }

    /**
     * Starts the shimmer animation. If the view has not been laid out yet, delays start until dimensions are known.
     */
    fun startShimmer() {
        // If size is not yet established, retry after a frame
        if (width == 0 || height == 0) {
            postDelayed({ startShimmer() }, 32)
            return
        }
        shimmerAnimator?.cancel()
        shimmerAnimator = ValueAnimator.ofFloat(-width.toFloat(), width.toFloat()).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                shimmerTranslate = animation.animatedValue as Float
                // Trigger a redraw on each animation frame
                postInvalidateOnAnimation()
            }
            start()
        }
    }

    /**
     * Stops the shimmer animation and resets state. Removes any attached listeners.
     */
    fun stopShimmer() {
        shimmerAnimator?.apply {
            removeAllUpdateListeners()
            cancel()
        }
        shimmerAnimator = null
        // Invalidate to clear any remaining shimmer frame
        postInvalidate()
    }

    /**
     * Called when the view is attached to a window. Automatically starts the shimmer effect.
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startShimmer()
    }

    /**
     * Called when the view is detached from a window. Stops the shimmer animation to avoid leaks.
     */
    override fun onDetachedFromWindow() {
        stopShimmer()
        super.onDetachedFromWindow()
    }
}
