package com.`in`.videoassignment.ui.upload

import android.content.Context
import android.net.Uri
import android.util.Log
import com.`in`.videoassignment.base.BaseViewModel
import com.`in`.videoassignment.data.VideoRepo
import javax.inject.Inject

class VideoUploadViewModel : BaseViewModel {

    @Inject
    constructor(videoRepo: VideoRepo):super(videoRepo)


    fun upload(path: Uri?, context: Context) = getVideoRepo().uploadVideo(path,context)

}