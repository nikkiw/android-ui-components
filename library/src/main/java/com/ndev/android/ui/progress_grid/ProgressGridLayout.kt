package com.ndev.android.ui.progress_grid

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import com.ndev.android.ui.components.R
import java.lang.reflect.Modifier.PROTECTED

/**
 * A custom {@link FrameLayout} that displays a grid-based progress reveal effect with an optional
 * light wave animation overlay.
 *
 * @constructor Creates a new ProgressGridLayout.
 * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view (if any).
 * @param defStyle The default style to apply to this view. If 0, no style will be applied.
 */
class ProgressGridLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    /** Duration of the reveal animation, in milliseconds. */
    var animationDuration: Long = 2000L

    /** Number of columns in the progress grid. */
    private var columns: Int = 8

    /** Number of rows in the progress grid. */
    private var rows: Int = 8

    /**
     * Color of the semi-transparent overlay drawn over the child views.
     * Default is white with alpha = 170.
     */
    var overlayColor: Int = Color.argb(170, 255, 255, 255)

    /**
     * Current progress of the reveal, from 0.0 (no cells revealed) to 1.0 (all cells revealed).
     */
    var progress: Float = 0f

    private var animator: ValueAnimator? = null

    // For animating the light wave
    private var waveAnimator: ValueAnimator? = null

    /**
     * Progress of the light wave effect, from 0.0 to 1.0, or -1.0 when inactive.
     */
    private var waveProgress: Float = -1f

    // Paint objects for drawing overlay, mask, and wave effects
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = overlayColor }
    private val maskPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL }
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val porterPaint =
        Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) }

    // Bitmap and canvas for mask and overlay drawing
    private var maskBitmap: Bitmap? = null
    private var maskCanvas: Canvas? = null
    private var overlayBitmap: Bitmap? = null
    private var overlayCanvas: Canvas? = null


    init {
        val ta = context.theme.obtainStyledAttributes(attrs, R.styleable.ProgressGridView, 0, 0)
        try {
            animationDuration =
                ta.getInt(R.styleable.ProgressGridView_animationDuration, animationDuration.toInt())
                    .toLong()
            rows = ta.getInt(R.styleable.ProgressGridView_gridRows, rows)
            columns = ta.getInt(R.styleable.ProgressGridView_gridCols, columns)
            overlayColor = ta.getColor(R.styleable.ProgressGridView_overlayColor, overlayColor)
        } finally {
            ta.recycle()
        }
        overlayPaint.color = overlayColor
        wavePaint.shader = null
    }


    fun start() {
        stop()
        progress = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = animationDuration * 1000
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Если stop не был вызван, запускаем световую волну
                    if (animator != null) {
                        startWave()
                    }
                }
            })
            start()
        }
    }

    fun stop() {
        animator?.cancel()
        animator = null
        waveAnimator?.cancel()
        waveAnimator = null
        waveProgress = -1f
        invalidate()
    }

    // Light wave animation (looped)
    private fun startWave() {
        waveAnimator?.cancel()
        waveProgress = 0f
        waveAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener {
                waveProgress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    onWaveStartListener?.invoke()
                }
            })
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            overlayBitmap?.recycle()
            overlayBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            overlayBitmap?.setHasAlpha(true)
            overlayCanvas = Canvas(overlayBitmap!!)
        }
        ensureMaskBitmap()
    }

    private fun ensureMaskBitmap() {
        if (maskBitmap == null || maskBitmap?.width != width || maskBitmap?.height != height) {
            if (width > 0 && height > 0) {
                maskBitmap?.recycle()
                maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                maskBitmap?.setHasAlpha(true)
                maskCanvas = Canvas(maskBitmap!!)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
        overlayBitmap?.recycle()
        overlayBitmap = null
        overlayCanvas = null
        maskCanvas = null
        maskBitmap?.recycle()
        maskBitmap = null
    }

    override fun dispatchDraw(canvas: Canvas) {
        // Если анимация неактивна, рисуем только детей (без overlay)
        if (animator == null && waveAnimator == null) {
            super.dispatchDraw(canvas)
            return
        }

        super.dispatchDraw(canvas)
        if (width == 0 || height == 0) return

        if (overlayBitmap == null || overlayCanvas == null) return

        // 1. Clear overlayBitmap to a transparent state
        overlayBitmap?.eraseColor(Color.TRANSPARENT)
        overlayCanvas?.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // 2. Prepare mask bitmap (black = "clear", transparent = "keep cover")
        val mask = maskBitmap ?: return
        val mCanvas = maskCanvas ?: return
        mask.eraseColor(Color.TRANSPARENT)
        val cellWidth = width / columns.toFloat()
        val cellHeight = height / rows.toFloat()
        val totalCells = columns * rows
        val revealedCells = (totalCells * progress).toInt()
        var i = 0
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                if (i < revealedCells) {
                    val left = col * cellWidth
                    val top = row * cellHeight
                    mCanvas.drawRect(left, top, left + cellWidth, top + cellHeight, maskPaint)
                }
                i++
            }
        }

        // 3. “Cut” windows in the overlay using DST_OUT
        overlayCanvas?.drawBitmap(mask, 0f, 0f, porterPaint)

        // 4. Light wave effect when active
        if (waveProgress >= 0f) {
            // The light wave travels horizontally from left to right
            val waveWidth = width / 4f
            val centerX = width * waveProgress
            val shader = LinearGradient(
                centerX - waveWidth, 0f, centerX + waveWidth, 0f,
                intArrayOf(0x00FFFFFF.toInt(), 0x80FFFFFF.toInt(), 0x00FFFFFF.toInt()),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            wavePaint.shader = shader
            overlayCanvas?.drawRect(0f, 0f, width.toFloat(), height.toFloat(), wavePaint)
            wavePaint.shader = null
        }

        // 5. Draw the final overlay on top of the content
        overlayBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
    }


    // for testing

    @get:VisibleForTesting(otherwise = PROTECTED)
    val animatorForTest: ValueAnimator?
        get() = animator

    @get:VisibleForTesting(otherwise = PROTECTED)
    val waveAnimatorForTest: ValueAnimator?
        get() = waveAnimator

    @get:VisibleForTesting(otherwise = PROTECTED)
    val progressForTest: Float
        get() = progress

    @get:VisibleForTesting(otherwise = PROTECTED)
    val waveProgressForTest: Float
        get() = waveProgress

    @get:VisibleForTesting(otherwise = PROTECTED)
    val overlayBitmapForTest: Bitmap?
        get() = overlayBitmap

    @get:VisibleForTesting(otherwise = PROTECTED)
    val overlayCanvasForTest: Canvas?
        get() = overlayCanvas

    @get:VisibleForTesting(otherwise = PROTECTED)
    val maskBitmapForTest: Bitmap?
        get() = maskBitmap

    @get:VisibleForTesting(otherwise = PROTECTED)
    val maskCanvasForTest: Canvas?
        get() = maskCanvas

    @get:VisibleForTesting(otherwise = PROTECTED)
    var onWaveStartListener: (() -> Unit)? = null
}