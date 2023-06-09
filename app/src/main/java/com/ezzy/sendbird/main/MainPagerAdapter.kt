package com.ezzy.sendbird.main

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.ezzy.sendbird.R

internal class MainPagerAdapter(context: Context, fm: FragmentManager, behavior: Int) :
    FragmentPagerAdapter(fm, behavior) {
    private val mFragmentList: MutableList<FragmentInfo> = ArrayList()

    private class FragmentInfo internal constructor(val mTitle: String, val mFragment: Fragment)

    init {
        mFragmentList.add(FragmentInfo("", DialFragment()))
        mFragmentList.add(
            FragmentInfo(
                context.getString(R.string.calls_history),
                HistoryFragment()
            )
        )
        mFragmentList.add(
            FragmentInfo(
                context.getString(R.string.calls_settings),
                SettingsFragment()
            )
        )
    }

    override fun getItem(position: Int): Fragment {
        return mFragmentList[position].mFragment
    }

    override fun getCount(): Int {
        return mFragmentList.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return mFragmentList[position].mTitle
    }
}