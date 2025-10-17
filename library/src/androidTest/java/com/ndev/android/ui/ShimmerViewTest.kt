package com.ndev.android.ui

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.ndev.android.ui.components.test.R
import com.ndev.android.ui.screens.ShimmerScreen
import com.ndev.android.ui.shimmer.ShimmerView
import com.ndev.android.ui.uitests.common.TestActivity
import io.github.kakaocup.kakao.common.assertions.BaseAssertions
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * An instrumented test class for the [ShimmerView] component.
 * It verifies the shimmer animation's behavior across different lifecycle events,
 * visibility changes, and configuration changes like screen rotation.
 * The tests use the Kaspresso framework for enhanced readability and stability.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ShimmerViewTest : TestCase() {

    /**
     * A JUnit rule that provides functional testing of a single activity.
     * It launches a [TestActivity] with a specific layout for the ShimmerView.
     */
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule<TestActivity>(
        TestActivity.createWithLayout(
            ApplicationProvider.getApplicationContext(),
            R.layout.shimmer_layout
        )
    )

    /**
     * Provides access to the device's UI, allowing for actions like screen rotation.
     */
    private val uiDevice: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    /**
     * A rule to grant necessary permissions at runtime. This can be useful for
     * tests that interact with device storage or other protected features.
     */
    @get:Rule
    val runtimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    /**
     * Tests that the shimmer animation is visible and running immediately after the activity launches.
     */
    @Test
    fun shimmerIsVisible_onActivityLaunch() {
        run {
            step("Check that ShimmerView is visible on launch") {
                ShimmerScreen {
                    shimmerView {
                        isVisible()
                        assertAnimatorRunning()
                    }
                }
            }
        }
    }

    /**
     * Verifies that the shimmer animation survives and continues to run
     * after the screen is rotated to landscape and back to portrait.
     */
    @Test
    fun shimmerSurvives_onScreenRotation() {
        run {
            step("Check initial visibility") {
                ShimmerScreen {
                    shimmerView {
                        isVisible()
                        assertAnimatorRunning()
                    }
                }
            }

            step("Rotate screen to landscape") {
                device.activities.isCurrent(TestActivity::class.java)
                uiDevice.setOrientationLandscape()
                device.uiDevice.waitForIdle()
            }

            step("Check visibility after landscape rotation") {
                ShimmerScreen {
                    shimmerView {
                        isVisible()
                        assertAnimatorRunning()
                    }
                }
            }

            step("Rotate screen back to portrait") {
                uiDevice.setOrientationPortrait()
                device.uiDevice.waitForIdle()
            }

            step("Check visibility after portrait rotation") {
                ShimmerScreen {
                    shimmerView {
                        isVisible()
                        assertAnimatorRunning()
                    }
                }
            }
        }
    }

    /**
     * Tests that the shimmer animation correctly pauses when the activity is paused (e.g., goes to the background)
     * and resumes when the activity is brought back to the foreground.
     */
    @Test
    fun shimmerPausesAndResumes_onLifecycleChange() {
        run {
            step("Verify shimmer is visible and animator is running on activity start") {
                ShimmerScreen {
                    shimmerView {
                        isVisible()
                        assertAnimatorRunning()
                        assertAnimatorNotPaused()
                    }
                }
            }

            step("Pause the activity and verify animator is paused") {
                moveToState(Lifecycle.State.CREATED)

                // Access the view directly because it's no longer visible on the screen
                // after the activity state change.
                withShimmerView { shimmerView ->
                    assertTrue(
                        "Animator should be paused when activity is not resumed",
                        shimmerView.isAnimatorPaused()
                    )
                }
            }

            step("Resume the activity and verify animator is running again") {
                moveToState(Lifecycle.State.RESUMED)
                device.uiDevice.waitForIdle()

                ShimmerScreen {
                    shimmerView {
                        isVisible()
                        assertAnimatorRunning()
                        assertAnimatorNotPaused()
                    }
                }
            }
        }
    }

    /**
     * Verifies that the shimmer animation pauses when the [ShimmerView]'s visibility is set to GONE
     * and resumes when it is set back to VISIBLE.
     */
    @Test
    fun shimmerPausesAndResumes_onVisibilityChange() {
        run {
            step("Initially shimmer is visible and animator is running") {
                ShimmerScreen {
                    shimmerView {
                        isVisible()
                        assertAnimatorRunning()
                    }
                }
            }

            step("Make ShimmerView GONE and verify animator is paused") {
                withShimmerView { shimmerView ->
                    shimmerView.visibility = View.GONE
                }

                ShimmerScreen {
                    shimmerView {
                        isNotDisplayed()
                        assertAnimatorPaused()
                    }
                }
            }

            step("Make ShimmerView VISIBLE again and verify animator resumed") {
                withShimmerView { shimmerView ->
                    shimmerView.visibility = View.VISIBLE
                }

                ShimmerScreen {
                    shimmerView {
                        isVisible()
                        assertAnimatorRunning()
                        assertAnimatorNotPaused()
                    }
                }
            }
        }
    }

    /**
     * Checks that the shimmer animation stops when the view is detached from its parent
     * and starts again when it is re-attached.
     */
    @Test
    fun shimmerStopsAndStarts_onDetachAttach() {
        run {
            var detachedShimmerView: ShimmerView? = null
            var parent: ViewGroup? = null

            step("Verify animator is running initially") {
                ShimmerScreen {
                    shimmerView {
                        isVisible()
                        assertAnimatorRunning()
                    }
                }
            }

            step("Detach ShimmerView from parent") {
                activityScenarioRule.scenario.onActivity { activity ->
                    val shimmerView = activity.findViewById<ShimmerView>(R.id.shimmer_view)
                    parent = shimmerView.parent as ViewGroup

                    activity.runOnUiThread {
                        parent?.removeView(shimmerView)
                        detachedShimmerView = shimmerView
                    }
                }

                device.uiDevice.waitForIdle()

                assertFalse(
                    "Animator should be stopped when view is detached",
                    detachedShimmerView?.isAnimatorRunning() ?: false
                )
            }

            step("Re-attach ShimmerView to parent") {
                activityScenarioRule.scenario.onActivity { activity ->
                    activity.runOnUiThread {
                        parent?.addView(detachedShimmerView)
                    }
                }

                device.uiDevice.waitForIdle()

                assertTrue(
                    "Animator should be running when view is re-attached",
                    detachedShimmerView?.isAnimatorRunning() ?: false
                )
            }
        }
    }

    // region Helper Functions

    /**
     * A helper function to move the current activity to a specific lifecycle state.
     * @param state The target [Lifecycle.State].
     */
    private fun moveToState(state: Lifecycle.State) {
        activityScenarioRule.scenario.moveToState(state)
        device.uiDevice.waitForIdle()
    }

    /**
     * A helper function to perform an action directly on the [ShimmerView] instance
     * on the main thread.
     * @param action A lambda function to execute with the ShimmerView.
     */
    private fun withShimmerView(action: (ShimmerView) -> Unit) {
        activityScenarioRule.scenario.onActivity { activity ->
            val shimmerView = activity.findViewById<ShimmerView>(R.id.shimmer_view)
            activity.runOnUiThread {
                action(shimmerView)
            }
        }
        device.uiDevice.waitForIdle()
    }

    // endregion

    // region Custom Kaspresso/Kakao Assertions

    /**
     * A custom Kaspresso assertion to check if the [ShimmerView]'s animator is currently running.
     */
    private fun BaseAssertions.assertAnimatorRunning() {
        view.check { view, _ ->
            val shimmer = view as ShimmerView
            assertTrue(
                "Animator should be running",
                shimmer.isAnimatorRunning()
            )
        }
    }

    /**
     * A custom Kaspresso assertion to check if the [ShimmerView]'s animator is paused.
     */
    private fun BaseAssertions.assertAnimatorPaused() {
        view.check { view, _ ->
            val shimmer = view as ShimmerView
            assertTrue(
                "Animator should be paused",
                shimmer.isAnimatorPaused()
            )
        }
    }

    /**
     * A custom Kaspresso assertion to check if the [ShimmerView]'s animator is not paused.
     */
    private fun BaseAssertions.assertAnimatorNotPaused() {
        view.check { view, _ ->
            val shimmer = view as ShimmerView
            assertFalse(
                "Animator should not be paused",
                shimmer.isAnimatorPaused()
            )
        }
    }

    // endregion
}