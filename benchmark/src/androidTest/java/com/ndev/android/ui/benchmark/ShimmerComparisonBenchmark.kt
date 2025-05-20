package com.ndev.android.ui.benchmark


import android.content.Context
import android.os.SystemClock
import android.view.LayoutInflater
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ndev.android.ui.shimmer.ShimmerView
import com.ndev.android.ui.shimmer.ShimmerViewGPU
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Microbenchmark for comparing CPU-based ShimmerView and GPU-based ShimmerViewGPU.
 * Measures drawing performance after at least one full animation cycle (1.5 seconds).
 * Uses instrumentation to run animations on the main (Looper) thread and then measures draw().
 * Requires dependency in build.gradle:
 *
 * androidTestImplementation "androidx.benchmark:benchmark-junit4:1.1.0"
 *
 * And set testInstrumentationRunner to:
 * "androidx.benchmark.junit4.AndroidBenchmarkRunner"
 */
@RunWith(AndroidJUnit4::class)
class ShimmerComparisonBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var shimmerCpu: ShimmerView
    private lateinit var shimmerGpu: ShimmerViewGPU

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before
    fun setUp() {
        instrumentation.runOnMainSync {
            // CPU-shimmer
            shimmerCpu = ShimmerView(context).apply {
                layout(0, 0, 600, 800)
                addView(
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_placeholder, this, false)
                )
                startShimmer()
            }
            // GPU-shimmer
            shimmerGpu = ShimmerViewGPU(context).apply {
                layout(0, 0, 600, 800)
                addView(
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_placeholder, this, false)
                )
                startShimmer()
            }
        }
        // Wait for full animation cycle (1500ms)
        SystemClock.sleep(1500)
    }

    @Test
    fun measureCpuShimmerDraw() = benchmarkRule.measureRepeated {
        instrumentation.runOnMainSync {
            // invalidate starts rebuild+draw inside RenderThread
            shimmerCpu.invalidate()
        }
    }

    @Test
    fun measureGpuShimmerDraw() = benchmarkRule.measureRepeated {
        instrumentation.runOnMainSync {
            // invalidate starts rebuild+draw inside RenderThread
            shimmerGpu.invalidate()
        }
    }
}
