package com.ezzy.sendbird

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.ezzy.sendbird.main.HistoryFragment
import com.ezzy.sendbird.main.MainPagerAdapter
import com.ezzy.sendbird.utils.BroadcastUtils
import com.ezzy.sendbird.utils.ToastUtils
import com.ezzy.sendbird.utils.UserInfoUtils
import com.google.android.material.tabs.TabLayout
import com.sendbird.calls.DirectCallLog
import com.sendbird.calls.SendBirdCall.currentUser
import java.util.Arrays

class MainActivity : AppCompatActivity() {

    private val MANDATORY_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,  // for VoiceCall and VideoCall
        Manifest.permission.CAMERA,  // for VideoCall
        Manifest.permission.BLUETOOTH // for VoiceCall and VideoCall
    )
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1

    private var mToolbar: Toolbar? = null
    private var mTabLayout: TabLayout? = null
    private var mViewPager: ViewPager? = null

    private var mLinearLayoutToolbar: LinearLayout? = null
    private lateinit var mImageViewProfile: ImageView
    private lateinit var mTextViewNickname: TextView
    private lateinit var mTextViewUserId: TextView

    private lateinit var mMainPagerAdapter: MainPagerAdapter
    private var mReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        setUI()
        registerReceiver()
        checkPermissions()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.i(BaseApplication.TAG, "[MainActivity] onNewIntent()")
        setUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver()
    }

    private fun initViews() {
        mToolbar = findViewById(R.id.toolbar)
        mTabLayout = findViewById(R.id.tab_layout)
        mViewPager = findViewById(R.id.view_pager)
        mLinearLayoutToolbar = findViewById(R.id.linear_layout_toolbar)
        mImageViewProfile = findViewById(R.id.image_view_profile)
        mTextViewNickname = findViewById(R.id.text_view_nickname)
        mTextViewUserId = findViewById(R.id.text_view_user_id)
    }

    private fun setUI() {
        setSupportActionBar(mToolbar)
        mLinearLayoutToolbar!!.visibility = View.VISIBLE
        UserInfoUtils.setProfileImage(this, currentUser, mImageViewProfile)
        UserInfoUtils.setNickname(this, currentUser, mTextViewNickname)
        UserInfoUtils.setUserId(this, currentUser, mTextViewUserId)
        mMainPagerAdapter =
            MainPagerAdapter(this, supportFragmentManager, mTabLayout!!.tabCount)
        mViewPager!!.adapter = mMainPagerAdapter
        mTabLayout!!.setupWithViewPager(mViewPager)
        var tabIndex = 0
        mTabLayout!!.getTabAt(tabIndex)!!.setIcon(R.drawable.ic_call_filled).text = null
        tabIndex++
        mTabLayout!!.getTabAt(tabIndex)!!.setIcon(R.drawable.ic_layout_default).text = null
        tabIndex++
        mTabLayout!!.getTabAt(tabIndex)!!.setIcon(R.drawable.ic_settings).text = null
        if (supportActionBar != null) {
            supportActionBar?.title = mMainPagerAdapter.getPageTitle(mViewPager!!.currentItem)
        }
        mViewPager!!.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                mToolbar?.title = mMainPagerAdapter.getPageTitle(position)
                when (position) {
                    0 -> {
                        mLinearLayoutToolbar!!.visibility = View.VISIBLE
                        mTabLayout!!.getTabAt(0)!!.setIcon(R.drawable.ic_call_filled)
                        mTabLayout!!.getTabAt(1)!!.setIcon(R.drawable.ic_layout_default)
                        mTabLayout!!.getTabAt(2)!!.setIcon(R.drawable.ic_settings)
                    }

                    1 -> {
                        mLinearLayoutToolbar!!.visibility = View.GONE
                        mTabLayout!!.getTabAt(0)!!.setIcon(R.drawable.ic_call)
                        mTabLayout!!.getTabAt(1)!!.setIcon(R.drawable.icon_call_history)
                        mTabLayout!!.getTabAt(2)!!.setIcon(R.drawable.ic_settings)
                    }

                    2 -> {
                        mLinearLayoutToolbar!!.visibility = View.GONE
                        mTabLayout!!.getTabAt(0)!!.setIcon(R.drawable.ic_call)
                        mTabLayout!!.getTabAt(1)!!.setIcon(R.drawable.ic_layout_default)
                        mTabLayout!!.getTabAt(2)!!.setIcon(R.drawable.icon_settings_filled)
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    private fun registerReceiver() {
        Log.i(BaseApplication.TAG, "[MainActivity] registerReceiver()")
        if (mReceiver != null) {
            return
        }
        mReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(BaseApplication.TAG, "[MainActivity] onReceive()")
                val callLog =
                    intent.getSerializableExtra(BroadcastUtils.INTENT_EXTRA_CALL_LOG) as DirectCallLog?
                if (callLog != null) {
                    val historyFragment: HistoryFragment =
                        mMainPagerAdapter.getItem(1) as HistoryFragment
                    historyFragment.addLatestCallLog(callLog)
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(BroadcastUtils.INTENT_ACTION_ADD_CALL_LOG)
        registerReceiver(mReceiver, intentFilter)
    }

    private fun unregisterReceiver() {
        Log.i(BaseApplication.TAG, "[MainActivity] unregisterReceiver()")
        if (mReceiver != null) {
            unregisterReceiver(mReceiver)
            mReceiver = null
        }
    }

    private fun checkPermissions() {
        val permissions = ArrayList(Arrays.asList(*MANDATORY_PERMISSIONS))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val deniedPermissions = ArrayList<String>()
        for (permission in permissions) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permission)
            }
        }
        if (deniedPermissions.size > 0) {
            requestPermissions(
                deniedPermissions.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            var allowed = true
            for (result in grantResults) {
                allowed = allowed && result == PackageManager.PERMISSION_GRANTED
            }
            if (!allowed) {
                ToastUtils.showToast(this, "Permission denied.")
            }
        }
    }

}