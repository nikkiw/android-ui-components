package com.ndev.android.ui.uitests.common

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.ndev.android.ui.shimmer.ShimmerView

// TestActivity: can inflate layout or create views by class
class TestActivity : AppCompatActivity() {
    companion object {
        // If layout ID is passed - inflate it
        const val EXTRA_LAYOUT_ID = "layout_id"

        // If a list of View-classes is passed - create them dynamically
        const val EXTRA_VIEW_CLASSES = "view_classes"

        fun createWithLayout(context: Context, layoutRes: Int): Intent =
            Intent(context, TestActivity::class.java).apply {
                putExtra(EXTRA_LAYOUT_ID, layoutRes)
            }

        fun createWithClasses(context: Context, classes: List<String>): Intent =
            Intent(context, TestActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_VIEW_CLASSES, ArrayList(classes))
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layoutId = intent.getIntExtra(EXTRA_LAYOUT_ID, 0)
        if (layoutId != 0) {
            setContentView(layoutId)
            return
        }

        val scrollView = ScrollView(this)
        val container = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        scrollView.addView(container)
        setContentView(scrollView)

        val classNames = intent.getStringArrayListExtra(EXTRA_VIEW_CLASSES)
            ?: arrayListOf(ShimmerView::class.java.name)

        classNames.forEach { className ->
            try {
                val clazz = Class.forName(className).asSubclass(View::class.java)
                val constructor = clazz.getConstructor(Context::class.java)
                val view = constructor.newInstance(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        resources.displayMetrics.run { (200 * density).toInt() }
                    )
                }
                container.addView(view)
            } catch (e: Exception) {
                Log.e("TestActivity", "Failed to create view $className", e)
            }
        }
    }
}
