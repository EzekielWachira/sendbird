package com.ezzy.sendbird

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ezzy.sendbird.utils.ActivityUtils
import com.ezzy.sendbird.utils.QRCodeUtils
import com.sendbird.calls.SendBirdCall.getSdkVersion

class AuthenticateActivity : AppCompatActivity() {

    private lateinit var mRelativeLayoutSignInWithQRCode: RelativeLayout
    private lateinit var mRelativeLayoutSignInManually: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authenticate)
        initViews()
    }

    private fun initViews() {
        //+ [QRCode]
        mRelativeLayoutSignInWithQRCode = findViewById(R.id.relative_layout_sign_in_with_qrcode)
        mRelativeLayoutSignInWithQRCode.setOnClickListener { view: View? ->
            mRelativeLayoutSignInWithQRCode.isEnabled = false
            mRelativeLayoutSignInManually!!.isEnabled = false
            QRCodeUtils.scanQRCode(this@AuthenticateActivity)
        }
        //- [QRCode]
        mRelativeLayoutSignInManually = findViewById(R.id.relative_layout_sign_in_manually)
        mRelativeLayoutSignInManually.setOnClickListener(View.OnClickListener { view: View? ->
            ActivityUtils.startSignInManuallyActivityForResult(
                this@AuthenticateActivity
            )
        })
        (findViewById<View>(R.id.text_view_quickstart_version) as TextView).text =
            getString(R.string.calls_quickstart_version, BaseApplication.VERSION)
        (findViewById<View>(R.id.text_view_sdk_version) as TextView).text =
            getString(R.string.calls_sdk_version, getSdkVersion())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ActivityUtils.START_SIGN_IN_MANUALLY_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                finish()
            }
            return
        }

        //+ [QRCode]
        if (QRCodeUtils.onActivityResult(
                this@AuthenticateActivity,
                requestCode,
                resultCode,
                data,
                object : QRCodeUtils.CompletionHandler {
                    override fun onCompletion(isSuccess: Boolean) {
                        if (isSuccess) {
                            ActivityUtils.startMainActivityAndFinish(this@AuthenticateActivity)
                        } else {
                            mRelativeLayoutSignInWithQRCode!!.isEnabled = true
                            mRelativeLayoutSignInManually!!.isEnabled = true
                        }
                    }

                }
            )
        ) {
            return
        }
        //- [QRCode]
    }
}