package com.ndev.android.ui.sample.fragments.shimmerview


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.ndev.android.ui.sample.R
import com.ndev.android.ui.shimmer.ShimmerView
import com.ndev.android.ui.shimmer.ShimmerViewGPU

class BasicShimmerFragment : Fragment() {

    companion object {
        private const val ARG_LAYOUT_ID = "layout_id"

        /**
         * Creates a BasicShimmerFragment instance with the passed layout id.
         * If layoutId is not specified, the default R.layout.fragment_basic_shimmer is used.
         */
        @JvmStatic
        fun newInstance(@LayoutRes layoutId: Int = R.layout.fragment_basic_shimmer): BasicShimmerFragment {
            val fragment = BasicShimmerFragment()
            fragment.arguments = Bundle().apply {
                putInt(ARG_LAYOUT_ID, layoutId)
            }
            return fragment
        }
    }


    private lateinit var shimmerView: FrameLayout
    private lateinit var controlPanel: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var buttonContent: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutRes = arguments?.getInt(ARG_LAYOUT_ID) ?: R.layout.fragment_basic_shimmer
        return inflater.inflate(layoutRes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initializing views
        shimmerView = view.findViewById(R.id.basic_shimmer_view)
        controlPanel = view.findViewById(R.id.control_panel)
        statusText = view.findViewById(R.id.status_text)

        // Add placeholder content to ShimmerView
        addPlaceholderContent()

        // Customizing the control buttons
        view.findViewById<Button>(R.id.btn_start).setOnClickListener {
            (shimmerView as? ShimmerView)?.startShimmer()
            (shimmerView as? ShimmerViewGPU)?.startShimmer()
            updateStatus(R.string.base_shimmer_status_shimmer_start)
        }

        view.findViewById<Button>(R.id.btn_stop).setOnClickListener {
            (shimmerView as? ShimmerView)?.stopShimmer()
            (shimmerView as? ShimmerViewGPU)?.stopShimmer()
            updateStatus(R.string.base_shimmer_status_shimmer_stop)
        }

        buttonContent = view.findViewById<Button>(R.id.btn_toggle_content).apply {
            setOnClickListener {
                toggleContent()
            }
        }
    }

    /*
     * Function to add placeholder elements inside a vertical LinearLayout
     */
    private fun addPlaceholderContent() {
        // Obtain LayoutInflater for inflating layouts
        val layoutInflater = LayoutInflater.from(context)

        // Create a vertical LinearLayout programmatically
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Add several placeholder views of varying sizes into the container
        repeat(3) {
            val placeholderView = layoutInflater
                .inflate(R.layout.item_placeholder, container, false)
            container.addView(placeholderView)
        }

        // Add the container to the shimmerView
        shimmerView.addView(container)
    }

    private fun toggleContent() {
        // Switching between real content and placeholder
        val isShowingPlaceholder = shimmerView.findViewById<ViewGroup>(R.id.real_content) == null

        if (isShowingPlaceholder) {
            // Remove placeholders and add real content
            shimmerView.removeAllViews()
            val realContent = layoutInflater.inflate(R.layout.item_real_content, shimmerView, false)
            shimmerView.addView(realContent)
            (shimmerView as? ShimmerView)?.stopShimmer()
            (shimmerView as? ShimmerViewGPU)?.stopShimmer()
            buttonContent.text = getString(R.string.base_shimmer_shimmer)
            updateStatus(R.string.base_shimmer_status_content)
        } else {
            // Return placeholders
            shimmerView.removeAllViews()
            addPlaceholderContent()
            (shimmerView as? ShimmerView)?.startShimmer()
            (shimmerView as? ShimmerViewGPU)?.startShimmer()
            buttonContent.text = getString(R.string.base_shimmer_content)
            updateStatus(R.string.base_shimmer_status_placeholder)
        }
    }

    private fun updateStatus(@StringRes resId: Int) {
        statusText.text = getString(resId)
    }

}