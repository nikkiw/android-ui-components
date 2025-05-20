package com.ndev.android.ui


import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.ndev.android.ui.components.test.R
import com.ndev.android.ui.shimmer.ShimmerView
import com.ndev.android.ui.uitests.common.TestActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ShimmerViewTest {
    /**
     * Test: the startShimmer() and stopShimmer() methods change the state of the animator via Reflection.
     */
    @Test
    fun shimmerView_startAndStopAnimator_stateChanges() {
        val intent = TestActivity.createWithLayout(
            ApplicationProvider.getApplicationContext(),
            R.layout.shimmer_layout
        )
        ActivityScenario.launch<TestActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                val shimmer = root.getChildAt(0) as ShimmerView

                val field = ShimmerView::class.java.getDeclaredField("shimmerAnimator").apply {
                    isAccessible = true
                }

                val animatorAfterAttach = field.get(shimmer)
                assertNotNull(
                    "Animator must be initialized after onAttachedToWindow",
                    animatorAfterAttach
                )

                shimmer.stopShimmer()
                val animatorAfterStop = field.get(shimmer)
                assertNull("Animator should become null after stopShimmer()", animatorAfterStop)

                shimmer.startShimmer()
                val animatorAfterStart = field.get(shimmer)
                assertNotNull(
                    "Animator must be non null after startShimmer()",
                    animatorAfterStart
                )
            }
        }
    }
}
