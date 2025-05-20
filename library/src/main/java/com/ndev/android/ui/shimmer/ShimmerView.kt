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

class ShimmerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shaderMatrix = Matrix()
    private var shimmerAnimator: ValueAnimator? = null
    private var shimmerTranslate = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            // Инициализируем горизонтальный градиент с прозрачными краями и белым центром
            shimmerPaint.shader = LinearGradient(
                0f, 0f, w.toFloat(), 0f,
                intArrayOf(Color.TRANSPARENT, Color.WHITE, Color.TRANSPARENT),
                floatArrayOf(0.3f, 0.5f, 0.7f),
                Shader.TileMode.CLAMP
            )
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        // Сначала отрисовываем дочерние view
        super.dispatchDraw(canvas)

        // Формируем Path, который объединяет области всех видимых дочерних элементов
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

        // Сохраняем состояние canvas и ограничиваем область отрисовки маской
        canvas.withClip(maskPath) {
            // Обновляем матрицу градиента согласно текущему значению shimmerTranslate
            shaderMatrix.setTranslate(shimmerTranslate, 0f)
            shimmerPaint.shader?.setLocalMatrix(shaderMatrix)

            // Рисуем градиент по всей области контейнера — он появится только внутри области maskPath
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shimmerPaint)
        }
    }

    fun startShimmer() {
        // Если размеры еще не установлены, отложим запуск анимации
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
                // Обеспечиваем плавную перерисовку
                postInvalidateOnAnimation()
            }
            start()
        }
    }


    fun stopShimmer() {
        shimmerAnimator?.apply {
            removeAllUpdateListeners() // Удаляем все слушатели обновления анимации
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
