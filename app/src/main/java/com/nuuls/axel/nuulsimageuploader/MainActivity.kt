package com.nuuls.axel.nuulsimageuploader

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.nuuls.axel.nuulsimageuploader.databinding.MainActivityBinding
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: MainActivityBinding
    private lateinit var viewModel: MainViewModel

    private var currentImagePath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        binding = DataBindingUtil.setContentView<MainActivityBinding>(this, R.layout.main_activity).apply {
            lifecycleOwner = this@MainActivity
            vm = viewModel
            buttonCamera.setOnClickListener {
                if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                } else {
                    requestPermission(PERMISSION_CAPTURE_REQUEST_CODE)
                }
            }
        }

        viewModel.event.observe(this) {
            when (it) {
                is UploadEvent.Result -> {
                    generateToast("Copied: ${it.url}")

                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("nuuls url", it.url))
                }
                is UploadEvent.Error -> generateToast(it.message)
            }
        }

        // Handling the intent coming from other app
        val type = intent.type
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // if im receiving an image from other app
            if (Intent.ACTION_SEND == intent.action && type != null) {
                handleSendImage(intent)
            }
        } else {
            requestPermission(PERMISSION_SHARE_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                PERMISSION_SHARE_REQUEST_CODE -> {
                    handleSendImage(intent)
                }
                PERMISSION_CAPTURE_REQUEST_CODE -> {
                    startCamera()
                }
            }
        }
    }

    private fun requestPermission(code: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), code)
    }

    private fun startCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { captureIntent ->
            captureIntent.resolveActivity(packageManager)?.also {
                try {
                    MediaUtils.createImageFile(this).apply { currentImagePath = absolutePath }
                } catch (e: IOException) {
                    null
                }?.also {
                    val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", it)
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                    startActivityForResult(captureIntent, CAMERA_REQUEST_CODE)
                }
            }
        }
    }

    private fun handleSendImage(intent: Intent) {
        if (intent.type?.startsWith("image/") == false) {
            return
        }

        // copy the shared image to a new file so we don't remove exif data form the original
        val uri = intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri ?: return
        val mimeType = contentResolver?.getType(uri)
        val mimeTypeMap = MimeTypeMap.getSingleton()
        val extension = mimeTypeMap.getExtensionFromMimeType(mimeType) ?: return
        val copy = MediaUtils.createImageFile(this, extension)

        try {
            contentResolver.openInputStream(uri)?.run { copy.outputStream().use { copyTo(it) } }
            if (copy.extension == "jpg" || copy.extension == "jpeg") {
                MediaUtils.removeExifAttributes(copy.absolutePath)
            }
            val copyUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", copy)
            viewModel.setPath(copy.absolutePath, copyUri)
        } catch (e: IOException) {
            Log.e(TAG, Log.getStackTraceString(e))
            copy.delete()
        }

    }

    // after getting the pic taken from the camera activity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == CAMERA_REQUEST_CODE) {
            val imageFile = File(currentImagePath)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", imageFile)
            try {
                MediaUtils.removeExifAttributes(currentImagePath)
                viewModel.setPath(currentImagePath, uri)
            } catch (e: IOException) {
                Log.e(TAG, Log.getStackTraceString(e))
                imageFile.delete()
            }
        }
    }

    private fun generateToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val PERMISSION_CAPTURE_REQUEST_CODE = 1
        private const val PERMISSION_SHARE_REQUEST_CODE = 2
        private const val CAMERA_REQUEST_CODE = 2
    }

}
