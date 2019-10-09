package com.nuuls.axel.nuulsimageuploader

sealed class UploadEvent {
    data class Error(val message: String) : UploadEvent()
    data class Result(val url: String) : UploadEvent()
}