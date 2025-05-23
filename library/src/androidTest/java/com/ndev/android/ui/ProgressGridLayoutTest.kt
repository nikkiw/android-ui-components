package com.ndev.android.ui


import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.ndev.android.ui.components.test.R
import com.ndev.android.ui.progress_grid.ProgressGridLayout
import com.ndev.android.ui.uitests.common.TestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class ProgressGridLayoutTest {

    @Test
    fun testStartAndStopAnimation_invokesInvalidate() {
        val intent = TestActivity.createWithLayout(
            ApplicationProvider.getApplicationContext(),
            R.layout.progress_grid_layout
        )
        ActivityScenario.launch<TestActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val progressGrid = activity.findViewById<ProgressGridLayout>(R.id.progress_grid)
                assertNotNull(progressGrid)
                // Checking that it does not crash on start/stop
                progressGrid.start()
                progressGrid.stop()
                // UI test cannot check private fields, but there should be no exceptions
            }
        }
    }

    @Test
    fun testOverlayColorIsSetFromAttributes() {
        val intent = TestActivity.createWithLayout(
            ApplicationProvider.getApplicationContext(),
            R.layout.progress_grid_layout
        )
        ActivityScenario.launch<TestActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val progressGrid = activity.findViewById<ProgressGridLayout>(R.id.progress_grid)
                assertEquals(0xAAFFFFFF.toInt(), progressGrid.overlayColor)
            }
        }
    }

    @Test
    fun testNoCrashOnLifecycleDetach() {
        val intent = TestActivity.createWithLayout(
            ApplicationProvider.getApplicationContext(),
            R.layout.progress_grid_layout
        )
        val scenario = ActivityScenario.launch<TestActivity>(intent)
        scenario.use {
            it.onActivity { activity ->
                val progressGrid = activity.findViewById<ProgressGridLayout>(R.id.progress_grid)
                progressGrid.start()
            }
            // Set Activity to DESTROYED state (simulates removing View from the window)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
            // If there were no exceptions - everything is ok, onDetachedFromWindow is called by the system
        }
    }

    @Test
    fun testSetAnimationDurationViaAttribute() {
        val intent = TestActivity.createWithLayout(
            ApplicationProvider.getApplicationContext(),
            R.layout.progress_grid_layout
        )
        ActivityScenario.launch<TestActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val progressGrid = activity.findViewById<ProgressGridLayout>(R.id.progress_grid)
                // You can check the public field
                assertTrue(progressGrid.animationDuration > 0)
            }
        }
    }

    @Test
    fun testStartUpdatesAnimatorAndProgress() {
        val intent = TestActivity.createWithLayout(
            ApplicationProvider.getApplicationContext(),
            R.layout.progress_grid_layout
        )
        ActivityScenario.launch<TestActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val progressGrid = activity.findViewById<ProgressGridLayout>(R.id.progress_grid)
                assertNotNull(progressGrid)
                // Before the start
                assertNull(progressGrid.animatorForTest)
                assertEquals(0f, progressGrid.progressForTest, 0.001f)
                // Start
                progressGrid.start()
                assertNotNull(progressGrid.animatorForTest)
                // Progress should reset to 0
                assertEquals(0f, progressGrid.progressForTest, 0.001f)
            }
        }
    }

    @Test
    fun testStopClearsAnimatorsAndWaveProgress() {
        val intent = TestActivity.createWithLayout(
            ApplicationProvider.getApplicationContext(),
            R.layout.progress_grid_layout
        )
        ActivityScenario.launch<TestActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val progressGrid = activity.findViewById<ProgressGridLayout>(R.id.progress_grid)
                progressGrid.start()
                progressGrid.stop()
                assertNull(progressGrid.animatorForTest)
                assertNull(progressGrid.waveAnimatorForTest)
                assertEquals(-1f, progressGrid.waveProgressForTest, 0.001f)
            }
        }
    }

//    @Test
//    fun testWaveAnimatorStartsAfterMainAnimationFinishes() {
//        val intent = TestActivity.createWithLayout(
//            ApplicationProvider.getApplicationContext(),
//            R.layout.progress_grid_layout
//        )
//
//        ActivityScenario.launch<TestActivity>(intent).use { scenario ->
//            scenario.onActivity { activity ->
//                val progressGrid = activity.findViewById<ProgressGridLayout>(R.id.progress_grid)
//                progressGrid.animationDuration = 1
//                progressGrid.start()
//            }
//
//            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
//
//            // Now re-enter to assert:
//            scenario.onActivity { activity ->
//                val progressGrid = activity.findViewById<ProgressGridLayout>(R.id.progress_grid)
//                assertNotNull(progressGrid.waveAnimatorForTest)
//                assertTrue(progressGrid.waveProgressForTest >= 0f)
//            }
//        }
//    }


    @Test
    fun testMaskAndOverlayBitmapsCreatedOnSizeChanged() {
        val intent = TestActivity.createWithLayout(
            ApplicationProvider.getApplicationContext(),
            R.layout.progress_grid_layout
        )
        ActivityScenario.launch<TestActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val progressGrid = activity.findViewById<ProgressGridLayout>(R.id.progress_grid)
                // Trigger onSizeChanged manually (e.g. layout)
                progressGrid.layout(0, 0, 100, 100)
                assertNotNull(progressGrid.overlayBitmapForTest)
                assertNotNull(progressGrid.overlayCanvasForTest)
                assertNotNull(progressGrid.maskBitmapForTest)
                assertNotNull(progressGrid.maskCanvasForTest)
            }
        }
    }
}