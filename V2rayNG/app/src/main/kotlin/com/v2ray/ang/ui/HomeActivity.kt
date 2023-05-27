package com.v2ray.ang.ui


import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayoutMediator
import com.tbruyelle.rxpermissions.RxPermissions
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.HiddifyHomeBinding
import com.v2ray.ang.extension.getColorEx
import com.v2ray.ang.extension.toPersianDigit
import com.v2ray.ang.ui.bottomsheets.AddConfigBottomSheets
import com.v2ray.ang.ui.bottomsheets.BottomSheetPresenter
import com.v2ray.ang.util.SpeedtestUtil
import com.v2ray.ang.util.Utils
import com.v2ray.ang.viewmodel.HiddifyMainViewModel


class HomeActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: HiddifyHomeBinding
    val hiddifyMainViewModel: HiddifyMainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HiddifyHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        title = ""
//        val spannableString = SpannableString(binding.toolbar.title.toString()+" "+BuildConfig.VERSION_NAME.toPersianDigit(this))
//
//        spannableString.setSpan(RelativeSizeSpan(0.6f), binding.toolbar.title.length+1, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
////        spannableString.setSpan(BackgroundColorSpan(getColorEx(R.color.colorAccent)),binding.toolbar.title.length+1,spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//
//        binding.toolbar.title=spannableString

        setSupportActionBar(binding.toolbar)
//        val anim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
//        binding.bottomNavigationView.startAnimation(anim)
//    binding.bottomNavigationView.actionView.startAnimation(anim)
        setBottomNavigation()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RxPermissions(this)
                .request(Manifest.permission.POST_NOTIFICATIONS)
                .subscribe {
//                    if (!it)
//                        context.toast(R.string.toast_permission_denied)
                }
        }
        setDrawer()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    private val bottomSheetPresenter = BottomSheetPresenter()
    private fun setBottomNavigation() {
        val adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
        adapter.addFragment(HiddifyMainActivity(hiddifyMainViewModel))
        adapter.addFragment(MainActivity(hiddifyMainViewModel))
        adapter.addFragment(HiddifyAdvancedFragment())
        binding.viewPager.setAdapter(adapter)
        binding.viewPager.offscreenPageLimit = 2

//        binding.bottomNavigationView.setOnNavigationItemSelectedListener { item ->
//
//            val fragmentManager = supportFragmentManager
//            val fragmentTransaction = fragmentManager.beginTransaction()
//
//// Define animation resources
//            val enterAnimation = R.anim.enter_animation
//            val exitAnimation = R.anim.exit_animation
//            val popEnterAnimation = R.anim.pop_enter_animation
//            val popExitAnimation = R.anim.pop_exit_animation
//
//            when (item.itemId) {
//                R.id.action_home -> {
//                    val fragment1 = HiddifyMainActivity()
//                    fragmentTransaction.setCustomAnimations(enterAnimation, exitAnimation, popEnterAnimation, popExitAnimation)
//                    fragmentTransaction.replace(R.id.fragmentContainer, fragment1)
//                    fragmentTransaction.commit()
//                    true
//                }
//                R.id.action_configs -> {
//                val fragment2 = MainActivity()
//                fragmentTransaction.setCustomAnimations(enterAnimation, exitAnimation, popEnterAnimation, popExitAnimation)
//                fragmentTransaction.replace(R.id.fragmentContainer, fragment2)
//                fragmentTransaction.commit()
//                    true
//                }
//                R.id.action_advanced -> {
//                    val fragment3 = HiddifyAdvancedFragment()
//                    fragmentTransaction.setCustomAnimations(enterAnimation, exitAnimation, popEnterAnimation, popExitAnimation)
//                    fragmentTransaction.replace(R.id.fragmentContainer, fragment3)
//                    fragmentTransaction.commit()
//                    true
//                }
//                else -> false
//            }
//        }
        binding.bottomNavigationView.selectedItemId = R.id.action_home
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.setIcon(R.drawable.ic_hiddify)
                    tab.setText(R.string.home)
                }
                1 -> {
                    tab.setIcon(R.drawable.ic_vpn_lock_24)
                    tab.setText(R.string.configs)
                }
                2 -> {
                    tab.setIcon(R.drawable.baseline_flash_on_24)
                    tab.setText(R.string.advanced)
                }
            }

        }.attach()


    }

    fun setDrawer() {

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_hiddify) // Set your desired drawer indicator icon
        }
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener(this)
        binding.version.text = "v${BuildConfig.VERSION_NAME} (${SpeedtestUtil.getLibVersion()})"
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            //R.id.server_profile -> activityClass = MainActivity::class.java
            R.id.sub_setting -> {
                startActivity(Intent(this, SubSettingActivity::class.java))
            }

            R.id.settings -> {
                startActivity(
                    Intent(this, SettingsActivity::class.java)
                        .putExtra("isRunning", false)
                )
            }
            R.id.user_asset_setting -> {
                startActivity(Intent(this, UserAssetActivity::class.java))
            }
            R.id.feedback -> {
                Utils.openUri(this, AppConfig.v2rayNGIssues)
            }
            R.id.promotion -> {
//                Utils.openUri(requireActivity(), "${Utils.decode(AppConfig.promotionUrl)}?t=${System.currentTimeMillis()}")
                if (!Utils.openUri(this, "tg://resolve?domain=hiddify"))
                    Utils.openUri(this, "https://t.me/hiddify")
            }
            R.id.logcat -> {
                startActivity(Intent(this, LogcatActivity::class.java))
            }
        }

        return true
    }


    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }


    internal class ViewPagerAdapter(fragmentManager: androidx.fragment.app.FragmentManager, lifecycle: androidx.lifecycle.Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
        private val arrayList: ArrayList<Fragment> = ArrayList()
        fun addFragment(fragment: Fragment) {
            arrayList.add(fragment)
        }

        override fun getItemCount(): Int {
            return arrayList.size
        }

        override fun createFragment(position: Int): Fragment {
            return arrayList[position]
        }
    }
}


