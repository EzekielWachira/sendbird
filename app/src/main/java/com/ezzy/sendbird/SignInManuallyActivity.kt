package com.ezzy.sendbird

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.ezzy.sendbird.utils.ActivityUtils
import com.ezzy.sendbird.utils.AuthenticationUtils
import com.ezzy.sendbird.utils.PrefUtils
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SignInManuallyActivity : AppCompatActivity() {

    private var mContext: Context? = null
    private var mInputMethodManager: InputMethodManager? = null

    private lateinit var mTextInputLayoutAppId: TextInputLayout
    private lateinit var mTextInputEditTextAppId: TextInputEditText
    private lateinit var mTextInputLayoutUserId: TextInputLayout
    private lateinit var mTextInputEditTextUserId: TextInputEditText
    private lateinit var mTextInputLayoutAccessToken: TextInputLayout
    private lateinit var mTextInputEditTextAccessToken: TextInputEditText
    private lateinit var mRelativeLayoutSignIn: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_manually)
        mContext = this
        mInputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        initViews()
    }

    private fun initViews() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.title = getString(R.string.calls_sign_in_manually_title)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
            supportActionBar!!.setHomeAsUpIndicator(R.drawable.icon_close)
        }
        mTextInputLayoutAppId = findViewById(R.id.text_input_layout_app_id)
        mTextInputEditTextAppId = findViewById(R.id.text_input_edit_text_app_id)
        mTextInputLayoutUserId = findViewById(R.id.text_input_layout_user_id)
        mTextInputEditTextUserId = findViewById(R.id.text_input_edit_text_user_id)
        mTextInputLayoutAccessToken = findViewById(R.id.text_input_layout_access_token)
        mTextInputEditTextAccessToken = findViewById(R.id.text_input_edit_text_access_token)
        mRelativeLayoutSignIn = findViewById(R.id.relative_layout_sign_in)
        val savedAppId: String? = PrefUtils.getAppId(this)
        if (!TextUtils.isEmpty(savedAppId)) {
            if (savedAppId != "YOUR_APPLICATION_ID") {
                mTextInputEditTextAppId.setText(savedAppId)
            }
        }
        val savedUserId: String? = PrefUtils.getUserId(this)
        if (!TextUtils.isEmpty(savedUserId)) {
            mTextInputEditTextUserId.setText(savedUserId)
        }
        val savedAccessToken: String? = PrefUtils.getAccessToken(this)
        if (!TextUtils.isEmpty(savedAccessToken)) {
            mTextInputEditTextAccessToken.setText(savedAccessToken)
        }
        checkSignInStatus()
        mTextInputEditTextAppId.setOnEditorActionListener(OnEditorActionListener { textView: TextView?, actionId: Int, keyEvent: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mTextInputEditTextAppId.clearFocus()
                mInputMethodManager!!.hideSoftInputFromWindow(
                    mTextInputEditTextAppId.windowToken,
                    0
                )
                return@OnEditorActionListener true
            }
            false
        })
        mTextInputEditTextAppId.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable) {
                checkSignInStatus()
            }
        })
        mTextInputEditTextUserId.setOnEditorActionListener(OnEditorActionListener { textView: TextView?, actionId: Int, keyEvent: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mTextInputEditTextUserId.clearFocus()
                mInputMethodManager!!.hideSoftInputFromWindow(
                    mTextInputEditTextUserId.windowToken,
                    0
                )
                return@OnEditorActionListener true
            }
            false
        })
        mTextInputEditTextUserId.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable) {
                checkSignInStatus()
            }
        })
        mTextInputEditTextAccessToken.setOnEditorActionListener(OnEditorActionListener { textView: TextView?, actionId: Int, keyEvent: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mTextInputEditTextAccessToken.clearFocus()
                mInputMethodManager!!.hideSoftInputFromWindow(
                    mTextInputEditTextAccessToken.windowToken,
                    0
                )
                return@OnEditorActionListener true
            }
            false
        })
        mRelativeLayoutSignIn.setOnClickListener(View.OnClickListener { view: View? ->
            var appId = ""
            var userId = ""
            var accessToken = ""
            appId =
                if (mTextInputEditTextAppId.text != null) mTextInputEditTextAppId.text
                    .toString() else ""
            userId =
                if (mTextInputEditTextUserId.text != null) mTextInputEditTextUserId.text
                    .toString() else ""
            accessToken =
                if (mTextInputEditTextAccessToken.text != null) mTextInputEditTextAccessToken.text
                    .toString() else ""
            if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(userId)
                && (application as BaseApplication).initSendBirdCall(appId)
            ) {
                AuthenticationUtils.authenticate(mContext, userId, accessToken) { isSuccess ->
                    if (isSuccess) {
                        setResult(RESULT_OK, null)
                        ActivityUtils.startMainActivityAndFinish(this@SignInManuallyActivity)
                    } else {
                        mTextInputLayoutAppId.isEnabled = true
                        mTextInputLayoutUserId.isEnabled = true
                        mTextInputLayoutAccessToken.isEnabled = true
                        mRelativeLayoutSignIn.isEnabled = true
                    }
                }
                mTextInputLayoutAppId.isEnabled = false
                mTextInputLayoutUserId.isEnabled = false
                mTextInputLayoutAccessToken.isEnabled = false
                mRelativeLayoutSignIn.isEnabled = false
            }
        })
    }

    private fun checkSignInStatus() {
        val appId =
            if (mTextInputEditTextAppId.text != null) mTextInputEditTextAppId.text.toString() else ""
        val userId =
            if (mTextInputEditTextUserId.text != null) mTextInputEditTextUserId.text.toString() else ""
        mRelativeLayoutSignIn.isEnabled = !TextUtils.isEmpty(appId) && !TextUtils.isEmpty(userId)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}