package com.ndev.android.ui.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// The package name of the application under test.
private const val TARGET_PACKAGE = "com.ndev.android.ui.sample"
// The total duration to run the animation for during the measurement phase.
private const val ANIMATION_DURATION_MS = 35_000L
// Default timeout for UI operations, like waiting for a view to appear.
private const val DEFAULT_TIMEOUT_MS = 5_000L
// A specific, shorter timeout for navigation drawer operations.
private const val DRAWER_TIMEOUT_MS = 3_000L
// The number of times each benchmark will be repeated to ensure stable results.
private const val BENCHMARK_ITERATIONS = 3

/**
 * A Macrobenchmark test for measuring animation performance within the target application.
 *
 * This class follows best practices for writing reliable Macrobenchmarks:
 * - **`StartupMode.WARM`**: Used to achieve more stable and repeatable results for UI tests by
 *   ensuring the app's process is already in memory.
 * - **Explicit Compilation Mode**: Using a specific `CompilationMode` (like `Partial` or `Full`)
 *   helps simulate real-world performance scenarios.
 * - **Sufficient Iterations**: Running the benchmark multiple times (`BENCHMARK_ITERATIONS`)
 *   provides statistically significant data.
 * - **Separation of Concerns**: The `setupBlock` handles navigation and state preparation, while
 *   the measurement block focuses solely on the actions being benchmarked.
 * - **Idempotent Setup**: The `setupBlock` is designed to be idempotent, meaning it can be
 *   run multiple times without creating side effects, ensuring a clean state for each iteration.
 */
@RunWith(AndroidJUnit4::class)
class AnimationBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * Measures the performance of the basic, unoptimized shimmer animation.
     */
    @Test
    fun shimmerAnimationBasic() {
        measureAnimation(
            testName = "shimmer_basic",
            stringResName = "basic_shimmer",
            viewToWaitForId = "basic_shimmer_view"
        )
    }

    /**
     * Measures the performance of the optimized shimmer animation.
     */
    @Test
    fun shimmerAnimationOpt() {
        measureAnimation(
            testName = "shimmer_opt",
            stringResName = "basic_shimmer_opt",
            viewToWaitForId = "basic_shimmer_view"
        )
    }

    /**
     * A generic method to measure the performance of a specific animation screen.
     * It navigates to the screen, starts the animation, waits for a fixed duration,
     * and then stops it, while collecting `FrameTimingMetric` data.
     *
     * @param testName A unique identifier for the test, used in benchmark reports.
     * @param stringResName The name of the string resource used as the menu item text for navigation.
     * @param viewToWaitForId The resource ID of a view on the target screen, used to confirm navigation was successful.
     */
    private fun measureAnimation(
        testName: String,
        stringResName: String,
        viewToWaitForId: String
    ) {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            iterations = BENCHMARK_ITERATIONS,
            // WARM startup ensures the app is already in memory, but the Activity is recreated.
            // This provides more stable results for UI-centric benchmarks.
            startupMode = StartupMode.WARM,
            // Partial compilation compiles critical paths, simulating a realistic
            // post-install performance scenario. It's commented out but can be enabled for analysis.
            // compilationMode = CompilationMode.Partial(),
            setupBlock = {
                // The setup block runs before each measurement iteration but is not measured itself.
                startActivityAndWait()
                navigateToScreen(stringResName, viewToWaitForId)
                // Ensure any previously running animations are stopped before we start.
                ensureAnimationStopped()
            }
        ) {
            // This is the measurement block, where performance is actually recorded.
            startAnimation()
            waitForAnimationDuration()
            stopAnimation()

            // A short pause to allow the UI to stabilize before the next iteration.
            Thread.sleep(500)
        }
    }

    /**
     * Navigates to a specific screen using the Navigation Drawer.
     *
     * @param stringResName The name of the string resource for the menu item text.
     * @param viewToWaitForId The resource ID of a view to wait for after clicking the menu item.
     * @throws AssertionError if a required UI element (drawer, menu item, or target view) is not found.
     */
    private fun MacrobenchmarkScope.navigateToScreen(
        stringResName: String,
        viewToWaitForId: String
    ) {
        openNavigationDrawer()

        val menuText = getStringFromTargetApp(stringResName)

        // Wait for the menu item to appear by its text.
        val menuItem = device.wait(
            Until.findObject(By.text(menuText)),
            DRAWER_TIMEOUT_MS
        ) ?: throw AssertionError(
            "Menu item with text '$menuText' (string resource: '$stringResName') not found. " +
                    "Check that the navigation drawer is open and contains this item."
        )

        menuItem.click()

        // Wait for the target screen to load by checking for a specific view.
        val targetViewLoaded = device.wait(
            Until.hasObject(By.res(TARGET_PACKAGE, viewToWaitForId)),
            DEFAULT_TIMEOUT_MS
        )

        if (!targetViewLoaded) {
            throw AssertionError(
                "Target view '$viewToWaitForId' not found after navigation. " +
                        "Check that the fragment is loaded correctly."
            )
        }

        // Allow time for screen transition animations to complete.
        device.waitForIdle()

        // After navigation, the drawer might remain open. Ensure it's closed.
        closeDrawerIfOpen()
    }

    /**
     * Checks if the Navigation Drawer is open and closes it if necessary.
     * It first attempts to use the back press, which is the standard way to close a drawer.
     * If that fails, it uses a fallback mechanism of clicking on the right side of the screen.
     */
    private fun MacrobenchmarkScope.closeDrawerIfOpen() {
        val navViewSelector = By.res(TARGET_PACKAGE, "nav_view")

        // Check if the drawer's main view is present.
        val isNavViewPresent = device.wait(Until.findObject(navViewSelector), 200) != null

        if (isNavViewPresent) {
            // First attempt: use the back button, which should close the drawer without navigating back.
            device.pressBack()

            // Wait for the drawer view to disappear.
            val closed = device.wait(Until.gone(navViewSelector), DRAWER_TIMEOUT_MS)

            if (!closed) {
                // Fallback: if the back press didn't work, click on the right 85% of the screen
                // to dismiss the left-hand drawer.
                try {
                    val screenWidth = device.displayWidth
                    val screenHeight = device.displayHeight
                    // Click in the vertical center, towards the right edge.
                    device.click((screenWidth * 0.85).toInt(), (screenHeight * 0.5).toInt())
                    device.waitForIdle()
                    // Add a small delay to ensure the drawer has time to animate closed.
                    Thread.sleep(150)
                } catch (t: Throwable) {
                    // This is a non-critical fallback. If it fails, log it for debugging but don't crash the test.
                    // Log.w("AnimationBenchmark", "Failed to close drawer with fallback click.", t)
                }
            }
        }
    }


    /**
     * Opens the Navigation Drawer by clicking the navigation button in the Toolbar.
     * @throws AssertionError if the Toolbar or navigation button cannot be found.
     */
    private fun MacrobenchmarkScope.openNavigationDrawer() {
        val toolbar = device.findObject(By.res(TARGET_PACKAGE, "toolbar"))
            ?: throw AssertionError("Toolbar not found")

        // Find the navigation button using common accessibility descriptions or class names.
        val navButton = toolbar.findObject(By.desc("Open navigation drawer"))
            ?: toolbar.findObject(By.clazz("android.widget.ImageButton"))
            ?: throw AssertionError("Navigation drawer button not found in toolbar")

        navButton.click()

        // Confirm that the drawer has opened by waiting for its primary view.
        val drawerOpened = device.wait(
            Until.hasObject(By.res(TARGET_PACKAGE, "nav_view")),
            DRAWER_TIMEOUT_MS
        )

        if (!drawerOpened) {
            throw AssertionError("Navigation drawer did not open within $DRAWER_TIMEOUT_MS ms")
        }

        // Wait for the drawer's open-animation to finish.
        device.waitForIdle()
    }

    /**
     * Retrieves a string resource from the target application's context.
     *
     * @param stringResName The name of the string resource (e.g., "basic_shimmer").
     * @return The localized string from the target application.
     * @throws IllegalStateException if the string resource does not exist in the target app.
     */
    private fun getStringFromTargetApp(stringResName: String): String {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageManager = instrumentation.context.packageManager
        val targetResources = packageManager.getResourcesForApplication(TARGET_PACKAGE)

        val stringResId = targetResources.getIdentifier(
            stringResName,
            "string",
            TARGET_PACKAGE
        )

        if (stringResId == 0) {
            throw IllegalStateException(
                "String resource '$stringResName' not found in package '$TARGET_PACKAGE'. " +
                        "Check that the resource exists in strings.xml of the target app."
            )
        }

        return targetResources.getString(stringResId)
    }

    /**
     * Ensures that the animation is stopped before starting a measurement.
     * This is a defensive measure to guarantee a consistent starting state.
     */
    private fun MacrobenchmarkScope.ensureAnimationStopped() {
        val stopButton = device.findObject(By.res(TARGET_PACKAGE, "btn_stop"))
        if (stopButton?.isEnabled == true) {
            stopButton.click()
            device.waitForIdle()
            // A brief pause to ensure the UI has settled after stopping.
            Thread.sleep(300)
        }
    }

    /**
     * Starts the animation by clicking the "Start" button.
     * @throws AssertionError if the start button is not found or is not enabled.
     */
    private fun MacrobenchmarkScope.startAnimation() {
        val startButton = device.findObject(By.res(TARGET_PACKAGE, "btn_start"))
            ?: throw AssertionError("Start button (btn_start) not found on the screen")

        if (!startButton.isEnabled) {
            throw AssertionError("Start button is not enabled. Check UI state.")
        }

        startButton.click()

        // Give the animation a moment to begin.
        Thread.sleep(100)
    }

    /**
     * Stops the animation by clicking the "Stop" button.
     * @throws AssertionError if the stop button is not found or is not enabled.
     */
    private fun MacrobenchmarkScope.stopAnimation() {
        val stopButton = device.findObject(By.res(TARGET_PACKAGE, "btn_stop"))
            ?: throw AssertionError("Stop button (btn_stop) not found on the screen")

        if (!stopButton.isEnabled) {
            // This might indicate the animation wasn't running as expected.
            throw AssertionError("Stop button is not enabled. Animation might not be running.")
        }

        stopButton.click()
        device.waitForIdle()
    }

    /**
     * Pauses execution to allow the animation to run for its specified duration.
     */
    private fun waitForAnimationDuration() {
        Thread.sleep(ANIMATION_DURATION_MS)
    }
}

///**
// * An extension property for `MacrobenchmarkScope` to provide convenient access to the `UiDevice` instance.
// */
//private val MacrobenchmarkScope.device: UiDevice
//    get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())