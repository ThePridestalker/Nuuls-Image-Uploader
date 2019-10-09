package com.nuuls.axel.nuulsimageuploader

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel : ViewModel() {
    private var path = ""

    val event = SingleLiveEvent<UploadEvent>()
    val urlLiveData = MutableLiveData("")
    val uriLiveData = MutableLiveData<Uri>()
    val uploadLiveData = MutableLiveData(false)

    fun upload() {
        if (path.isEmpty() || uploadLiveData.value == true) {
            event.postValue(UploadEvent.Error("Take a picture first :D"))
            return
        }
        uploadLiveData.postValue(true)
        viewModelScope.launch {
            val result = NuulsUploader.upload(File(path))
            if (result != null) {
                urlLiveData.postValue(result)
                event.postValue(UploadEvent.Result(result))
            } else {
                event.postValue(UploadEvent.Error("ERROR"))
            }
            path = ""
            uriLiveData.postValue(null)
            uploadLiveData.postValue(false)
        }
    }

    fun setPath(path: String, uri: Uri) {
        this.path = path
        this.urlLiveData.value = ""
        this.uriLiveData.value = uri
    }
}