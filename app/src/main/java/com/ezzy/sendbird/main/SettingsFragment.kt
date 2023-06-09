package com.ezzy.sendbird.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.viewbinding.ViewBinding
import com.ezzy.sendbird.R
import com.ezzy.sendbird.databinding.FragmentSettingsBinding
import com.ezzy.sendbird.utils.ActivityUtils
import com.ezzy.sendbird.utils.AuthenticationUtils
import com.ezzy.sendbird.utils.BindingFragment
import com.ezzy.sendbird.utils.UserInfoUtils
import com.sendbird.calls.SendBirdCall.currentUser


class SettingsFragment : BindingFragment<FragmentSettingsBinding>() {

    override val bindingInflater: (LayoutInflater) -> ViewBinding
        get() = FragmentSettingsBinding::inflate

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentUser = currentUser
        if (currentUser != null) {
            UserInfoUtils.setProfileImage(
                context,
                currentUser,
                view.findViewById(R.id.image_view_profile)
            )
            UserInfoUtils.setNickname(
                requireContext(),
                currentUser,
                view.findViewById(R.id.text_view_nickname)
            )
            (view.findViewById<View>(R.id.text_view_user_id) as TextView).text =
                currentUser.userId
        }

        binding.linearLayoutApplicationInformation
            .setOnClickListener { view1: View? ->
                if (activity != null) {
                    ActivityUtils.startApplicationInformationActivity(requireActivity())
                }
            }

       binding.linearLayoutSignOut.setOnClickListener { view1: View? ->
            AuthenticationUtils.deauthenticate(activity, object :
                AuthenticationUtils.DeauthenticateHandler {
                override fun onResult(isSuccess: Boolean) {
                    if (activity != null) {
                        ActivityUtils.startAuthenticateActivityAndFinish(requireActivity())
                    }
                }
            })
        }
    }
}