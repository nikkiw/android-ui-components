package com.ndev.android.ui.library


import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.ndev.android.ui.progress_grid.ProgressGridLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPorterDuffColorFilter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], shadows = [ShadowPorterDuffColorFilter::class])
class ProgressGridLayoutTest {
    private lateinit var context: Context
    private lateinit var view: ProgressGridLayout

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        view = ProgressGridLayout(context)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }

    @Test
    fun testInitialState() {
        assertNull(view.animatorForTest)
        assertNull(view.waveAnimatorForTest)
        assertEquals(0f, view.progressForTest)
        assertEquals(-1f, view.waveProgressForTest)
        assertNotNull(view.overlayBitmapForTest)
        assertNotNull(view.maskBitmapForTest)
    }

    @Test
    fun testStartCreatesAnimator() {
        view.animationDuration = 1L
        view.start()

        val animator = view.animatorForTest
        assertNotNull("Animator should be initialized", animator)

        // End animation to trigger wave start
        animator?.end()

        val waveAnimator = view.waveAnimatorForTest
        assertNotNull("Wave animator should start after progress animation ends", waveAnimator)
        assertEquals(0f, view.waveProgressForTest)
    }

    @Test
    fun testStopCancelsAnimators() {
        view.animationDuration = 1L
        view.start()
        assertNotNull(view.animatorForTest)

        view.stop()

        assertNull("Animator should be null after stop", view.animatorForTest)
        assertNull("Wave animator should be null after stop", view.waveAnimatorForTest)
        assertEquals(-1f, view.waveProgressForTest)
    }

    @Test
    fun testOnSizeChangedUpdatesBitmaps() {
        val oldOverlay = view.overlayBitmapForTest
        val oldMask = view.maskBitmapForTest

        // Simulate a real size change by re-layout
        view.layout(0, 0, 300, 150)

        val newOverlay = view.overlayBitmapForTest
        val newMask = view.maskBitmapForTest

        assertNotSame("Overlay bitmap should be recreated on size change", oldOverlay, newOverlay)
        assertNotSame("Mask bitmap should be recreated on size change", oldMask, newMask)
        assertEquals(300, newOverlay?.width)
        assertEquals(150, newOverlay?.height)
        assertEquals(300, newMask?.width)
        assertEquals(150, newMask?.height)
    }

    @Test
    fun testDispatchDrawWithWaveAndWithout() {
        // Prepare view: immediately finish progress animation and start wave
        view.animationDuration = 0L
        var waveStarted = false
        view.onWaveStartListener = { waveStarted = true }
        view.start()
        // End progress animator to start wave
        view.animatorForTest?.end()
        assertTrue("Wave should have started", waveStarted)

        // Perform dispatchDraw while wave is active
        val activeBitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        val activeCanvas = Canvas(activeBitmap)
        println("activePixelAlpha=${activeBitmap.getPixel(100, 50).ushr(24)}")

        val dispatchMethod = view.javaClass.getDeclaredMethod("dispatchDraw", Canvas::class.java)
        dispatchMethod.isAccessible = true
        dispatchMethod.invoke(view, activeCanvas)
        val activePixelAlpha = activeBitmap.getPixel(100, 50).ushr(24)
        assertTrue(
            "Overlay should not be fully transparent while wave is active",
            activePixelAlpha > 0
        )

        // Stop wave and redraw
        view.stop()
        // Ensure full progress
        view.progress = 1f
        val stoppedBitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        val stoppedCanvas = Canvas(stoppedBitmap)
        dispatchMethod.invoke(view, stoppedCanvas)
        val stoppedPixelAlpha = stoppedBitmap.getPixel(100, 50).ushr(24)
        assertEquals("Overlay should be fully transparent after stop", 0, stoppedPixelAlpha)
    }

    @Test
    fun testWaveStartListenerCalled() {
        var called = false
        view.onWaveStartListener = { called = true }
        view.animationDuration = 0L
        view.start()

        view.animatorForTest?.end()

        assertTrue("Wave start listener should be invoked on wave animation start", called)
    }
}
