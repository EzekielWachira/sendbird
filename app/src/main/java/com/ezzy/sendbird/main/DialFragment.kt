package com.ezzy.sendbird.main

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.viewbinding.ViewBinding
import com.ezzy.sendbird.call.CallService
import com.ezzy.sendbird.databinding.FragmentDialBinding
import com.ezzy.sendbird.utils.BindingFragment
import com.ezzy.sendbird.utils.PrefUtils


class DialFragment : BindingFragment<FragmentDialBinding>() {


    override val bindingInflater: (LayoutInflater) -> ViewBinding
        get() = FragmentDialBinding::inflate

    private lateinit var mInputMethodManager: InputMethodManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mInputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        setUpUi()
    }

    fun setUpUi() {
        with(binding) {
            imageViewVideoCall.isEnabled = false
            imageViewVoiceCall.isEnabled = false

            val savedCalleeId: String? = PrefUtils.getCalleeId(requireContext())
            if (!TextUtils.isEmpty(savedCalleeId)) {
                textInputEditTextUserId.setText(savedCalleeId)
                savedCalleeId?.length?.let { textInputEditTextUserId.setSelection(it) }
                imageViewVideoCall.isEnabled = true
                imageViewVoiceCall.isEnabled = true
            }

            textInputEditTextUserId.setOnEditorActionListener(OnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    textInputEditTextUserId.clearFocus()
                    mInputMethodManager.hideSoftInputFromWindow(
                        textInputEditTextUserId.windowToken,
                        0
                    )
                    return@OnEditorActionListener true
                }
                false
            })
            textInputEditTextUserId.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(editable: Editable) {
                    imageViewVideoCall.isEnabled = editable != null && editable.isNotEmpty()
                    imageViewVoiceCall.isEnabled = editable != null && editable.isNotEmpty()
                }
            })

            imageViewVideoCall.setOnClickListener { _: View? ->
                val calleeId =
                    if (textInputEditTextUserId.text != null) textInputEditTextUserId.text
                        .toString() else ""
                if (!TextUtils.isEmpty(calleeId)) {
                    CallService.dial(requireContext(), calleeId, true)
                    PrefUtils.setCalleeId(requireContext(), calleeId)
                }
            }

            imageViewVoiceCall.setOnClickListener {
                val calleeId =
                    if (textInputEditTextUserId.text != null) textInputEditTextUserId.text
                        .toString() else ""
                if (!TextUtils.isEmpty(calleeId)) {
                    CallService.dial(requireContext(), calleeId, false)
                    PrefUtils.setCalleeId(requireContext(), calleeId)
                }
            }
        }
    }
}
