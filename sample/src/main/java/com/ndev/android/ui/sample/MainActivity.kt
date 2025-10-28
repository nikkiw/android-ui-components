package com.ndev.android.ui.sample


import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.ndev.android.ui.sample.fragments.progress_grid.ProgressGridFragment
import com.ndev.android.ui.sample.fragments.shimmerview.BasicShimmerFragment

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Initialize DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        // Setup ActionBar and DrawerToggle
        setSupportActionBar(findViewById(R.id.toolbar))
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, findViewById(R.id.toolbar),
            R.string.nav_drawer_open, R.string.nav_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // If this is the first launch, show the default fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, BasicShimmerFragment())
                .commit()
            navigationView.setCheckedItem(R.id.nav_basic_shimmer)
            supportActionBar?.title = getString(R.string.basic_shimmer)
        }

        // The new way to handle the back button press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // If the drawer is open, close it
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    // If the drawer is closed, perform the default action
                    // (close the Activity or navigate to the previous fragment)
                    // To do this, we temporarily disable our callback and invoke the system's back press event
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true // Re-enable it for future presses
                }
            }
        })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val fragment: Fragment
        val title: String

        when (item.itemId) {
            R.id.nav_basic_shimmer -> {
                fragment = BasicShimmerFragment.newInstance(R.layout.fragment_basic_shimmer)
                title = getString(R.string.basic_shimmer)
            }

            R.id.nav_basic_shimmer_opt -> {
                fragment = BasicShimmerFragment.newInstance(R.layout.fragment_basic_shimmer_opt)
                title = getString(R.string.basic_shimmer_opt)
            }

            R.id.nav_progress_grid -> {
                fragment = ProgressGridFragment.newInstance(R.layout.fragment_progress_grid)
                title = getString(R.string.progress_grid_title)
            }
            else -> {
                fragment = BasicShimmerFragment()
                title = getString(R.string.basic_shimmer)
            }
        }

        // Replace the current fragment with the new one
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        // Update the title and close the drawer
        supportActionBar?.title = title
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}