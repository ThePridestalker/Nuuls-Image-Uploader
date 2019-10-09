package com.nuuls.axel.nuulsimageuploader

import android.net.Uri
import android.widget.Button
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter

@BindingAdapter("preview")
fun ImageView.setPreview(uri: Uri?) {
    if (uri == null) {
        setImageResource(R.drawable.nuulslogo)
    } else {
        setImageURI(uri)
    }
}

@BindingAdapter("state")
fun Button.setState(uploading: Boolean) {
    text = if (uploading) {
        setBackgroundColor(ContextCompat.getColor(context, R.color.upload_progress_background))
        context.getString(R.string.uploading)
    } else {
        setBackgroundColor(ContextCompat.getColor(context, R.color.upload_background))
        context.getString(R.string.upload)
    }
}