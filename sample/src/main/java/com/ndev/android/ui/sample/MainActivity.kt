package com.ndev.android.ui.sample


import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.ndev.android.ui.sample.fragments.shimmerview.BasicShimmerFragment

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Инициализация DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        // Настройка ActionBar и DrawerToggle
        setSupportActionBar(findViewById(R.id.toolbar))
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, findViewById(R.id.toolbar),
            R.string.nav_drawer_open, R.string.nav_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Если это первый запуск, показываем первый фрагмент по умолчанию
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, BasicShimmerFragment())
                .commit()
            navigationView.setCheckedItem(R.id.nav_basic_shimmer)
            supportActionBar?.title = getString(R.string.basic_shimmer)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Обработка выбора пункта меню в навигационном drawer
        val fragment: Fragment
        val title: String

        when (item.itemId) {
            R.id.nav_basic_shimmer -> {
                fragment = BasicShimmerFragment.newInstance(R.layout.fragment_basic_shimmer)
                title = getString(R.string.basic_shimmer)
            }

            R.id.nav_basic_shimmer_gpu -> {
                fragment = BasicShimmerFragment.newInstance(R.layout.fragment_basic_shimmer_gpu)
                title = getString(R.string.basic_shimmer_gpu)
            }
//            R.id.nav_list_shimmer -> {
//                fragment = ListShimmerFragment()
//                title = getString(R.string.list_shimmer)
//            }
//            R.id.nav_card_shimmer -> {
//                fragment = CardShimmerFragment()
//                title = getString(R.string.card_shimmer)
//            }
//            R.id.nav_control_shimmer -> {
//                fragment = ControlShimmerFragment()
//                title = getString(R.string.control_shimmer)
//            }
//            R.id.nav_custom_shimmer -> {
//                fragment = CustomShimmerFragment()
//                title = getString(R.string.custom_shimmer)
//            }
            else -> {
                fragment = BasicShimmerFragment()
                title = getString(R.string.basic_shimmer)
            }
        }

        // Заменяем текущий фрагмент выбранным
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        // Обновляем заголовок и закрываем drawer
        supportActionBar?.title = title
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        // Если drawer открыт, закрываем его при нажатии кнопки назад
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}