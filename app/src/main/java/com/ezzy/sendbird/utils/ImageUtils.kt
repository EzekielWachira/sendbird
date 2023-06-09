package com.ezzy.sendbird.utils

import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.BitmapImageViewTarget

object ImageUtils {
    fun displayCircularImageFromUrl(context: Context?, imageUrl: String?, imageView: ImageView?) {
        if (context == null || TextUtils.isEmpty(imageUrl) || imageView == null) {
            return
        }
        Glide.with(context)
            .asBitmap()
            .apply(RequestOptions().centerCrop().dontAnimate())
            .load(imageUrl)
            .into(object : BitmapImageViewTarget(imageView) {
                override fun setResource(resource: Bitmap?) {
                    val circularBitmapDrawable =
                        RoundedBitmapDrawableFactory.create(context.resources, resource)
                    circularBitmapDrawable.isCircular = true
                    imageView.setImageDrawable(circularBitmapDrawable)
                }
            })
    }
}
