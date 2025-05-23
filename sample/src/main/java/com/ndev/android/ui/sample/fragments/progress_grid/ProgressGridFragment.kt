package com.ndev.android.ui.sample.fragments.progress_grid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.ndev.android.ui.progress_grid.ProgressGridLayout
import com.ndev.android.ui.sample.R

class ProgressGridFragment : Fragment() {

    companion object {
        private const val ARG_LAYOUT_ID = "layout_id"

        /**
         * Creates a BasicShimmerFragment instance with the passed layout id.
         * If layoutId is not specified, the default R.layout.fragment_basic_shimmer is used.
         */
        @JvmStatic
        fun newInstance(@LayoutRes layoutId: Int = R.layout.fragment_progress_grid): ProgressGridFragment {
            val fragment = ProgressGridFragment()
            fragment.arguments = Bundle().apply {
                putInt(ARG_LAYOUT_ID, layoutId)
            }
            return fragment
        }
    }


    private lateinit var progressGridLayout: ProgressGridLayout
    private lateinit var controlPanel: LinearLayout
    private lateinit var statusText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutRes = arguments?.getInt(ARG_LAYOUT_ID) ?: R.layout.fragment_progress_grid
        return inflater.inflate(layoutRes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initializing views
        progressGridLayout = view.findViewById(R.id.progress_grid)
        controlPanel = view.findViewById(R.id.control_panel)
        statusText = view.findViewById(R.id.status_text)

        // Customizing the control buttons
        view.findViewById<Button>(R.id.btn_start).setOnClickListener {
            progressGridLayout.start()
            updateStatus(R.string.progress_grid_status_shimmer_start)
        }

        view.findViewById<Button>(R.id.btn_stop).setOnClickListener {
            progressGridLayout.stop()
            updateStatus(R.string.progress_grid_status_shimmer_stop)
        }
    }

    private fun updateStatus(@StringRes resId: Int) {
        statusText.text = getString(resId)
    }

}