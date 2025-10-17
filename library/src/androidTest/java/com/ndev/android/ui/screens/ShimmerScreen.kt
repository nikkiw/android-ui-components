package com.ndev.android.ui.screens


import com.kaspersky.kaspresso.screens.KScreen
import com.ndev.android.ui.components.test.R
import io.github.kakaocup.kakao.common.views.KView

/**
 * Describes the screen containing the ShimmerView.
 */
object ShimmerScreen : KScreen<ShimmerScreen>() {

    /**
     * The layout ID of the activity or fragment.
     * It is not used in this case because the TestActivity is dynamic.
     */
    override val layoutId: Int? = null

    /**
     * The class of the activity or fragment.
     * It is not used in this case. e.g. // TestActivity::class.java
     */
    override val viewClass: Class<*>? = null

    /**
     * Represents the ShimmerView on the screen.
     * It is crucial to assign an ID to the ShimmerView in the layout file.
     */
    val shimmerView = KView {
        withId(R.id.shimmer_view)
    }
}