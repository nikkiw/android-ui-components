package com.ndev.android.ui

import android.Manifest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.ndev.android.ui.components.test.R
import com.ndev.android.ui.uitests.common.ShimmerFragment
import com.ndev.android.ui.uitests.common.TestActivity
import leakcanary.LeakAssertions
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * This test class is designed to verify that the [ShimmerFragment] does not cause memory leaks.
 * It repeatedly replaces the fragment using `commitNow()` in a loop to simulate high-stress
 * scenarios and then uses LeakCanary to assert that no leaks are present.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ShimmerFragmentLeakTest : TestCase() {

    /**
     * A rule to launch and manage the lifecycle of [TestActivity] for this test.
     * The activity is created with a specific layout required for the test.
     */
    private val activityScenarioRule = ActivityScenarioRule<TestActivity>(
        TestActivity.createWithLayout(
            ApplicationProvider.getApplicationContext(),
            R.layout.activity_test
        )
    )

    /**
     * A [RuleChain] to ensure proper ordering of test rules.
     * It grants necessary storage permissions before launching the activity.
     */
    @get:Rule
    val rule: RuleChain = RuleChain
        .outerRule(
            GrantPermissionRule.grant(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        )
        .around(activityScenarioRule)

    /**
     * Test case that checks for memory leaks after multiple fragment replacements.
     */
    @Test
    fun fragmentReplaceLeakCheck() = run {
        step("Add initial fragment synchronously") {
            activityScenarioRule.scenario.onActivity { activity ->
                val fm = activity.supportFragmentManager
                fm.beginTransaction()
                    .replace(R.id.fragment_container, ShimmerFragment())
                    .commitNow()
            }
        }

        step("Do many replace operations (synchronously) to try to reveal leaks") {
            val iterations = 30
            for (i in 0 until iterations) {
                activityScenarioRule.scenario.onActivity { activity ->
                    val fm = activity.supportFragmentManager
                    fm.beginTransaction()
                        .replace(R.id.fragment_container, ShimmerFragment())
                        // Using commitNow() is convenient and deterministic in tests.
                        .commitNow()
                }
                // Synchronize with the UI thread to ensure the transaction is complete.
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            }
        }

        step("Force GC and check for leaks with LeakCanary") {
            assertNoLeaks()
        }
    }


    /**
     * A helper function that forces garbage collection and then asserts
     * that no memory leaks have been detected by LeakCanary.
     */
    private fun assertNoLeaks() {
        // Wait for the UI thread to be idle before proceeding.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        // Suggest that the system runs the garbage collector.
        System.gc()
        // Assert that LeakCanary has not detected any memory leaks.
        LeakAssertions.assertNoLeaks()
    }
}