package com.ndev.android.ui.uitests.common

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.ndev.android.ui.components.test.R
import com.ndev.android.ui.shimmer.ShimmerView

class ShimmerFragment : Fragment(R.layout.fragment_shimmer) {

    private var shimmer: ShimmerView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        shimmer = view.findViewById(R.id.shimmer_view)
        shimmer?.startShimmer()
    }

    override fun onDestroyView() {
        // Явно останавливаем анимацию, чтобы не полагаться только на onDetachedFromWindow()
        shimmer?.stopShimmer()
        shimmer = null
        super.onDestroyView()
    }
}
