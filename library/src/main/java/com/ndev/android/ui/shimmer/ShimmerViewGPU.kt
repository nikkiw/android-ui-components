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


class ShimmerViewGPU @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shaderMatrix = Matrix()
    private var shimmerAnimator: ValueAnimator? = null
    private var shimmerTranslate = 0f
    private var maskPath: Path? = null

    init {
        // включаем аппаратный слой на View
        setLayerType(LAYER_TYPE_HARDWARE, shimmerPaint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            shimmerPaint.shader = LinearGradient(
                0f, 0f, w.toFloat(), 0f,
                intArrayOf(Color.TRANSPARENT, Color.WHITE, Color.TRANSPARENT),
                floatArrayOf(0.3f, 0.5f, 0.7f),
                Shader.TileMode.CLAMP
            )
            // сбрасываем маску, чтобы она перестроилась при следующем dispatchDraw
            maskPath = null
        }
    }

    private fun buildMaskPath() {
        maskPath = Path().apply {
            for (i in 0 until childCount) {
                val c = getChildAt(i)
                if (c.isVisible) {
                    addRect(
                        c.left.toFloat(), c.top.toFloat(),
                        c.right.toFloat(), c.bottom.toFloat(),
                        Path.Direction.CW
                    )
                }
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (maskPath == null) buildMaskPath()
        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        super.dispatchDraw(canvas)
        maskPath?.let { path ->
            canvas.withClip(path) {
                shaderMatrix.setTranslate(shimmerTranslate, 0f)
                shimmerPaint.shader?.setLocalMatrix(shaderMatrix)
                drawRect(0f, 0f, width.toFloat(), height.toFloat(), shimmerPaint)
            }
        }
        canvas.restoreToCount(saveCount)
    }

    fun startShimmer() {
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
                this@ShimmerViewGPU.postInvalidateOnAnimation()
            }
            start()
        }
    }

    fun stopShimmer() {
        shimmerAnimator?.run {
            removeAllUpdateListeners()
            cancel()
        }
        shimmerAnimator = null
        postInvalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startShimmer()
    }

    override fun onDetachedFromWindow() {
        stopShimmer()
        super.onDetachedFromWindow()
    }
}
