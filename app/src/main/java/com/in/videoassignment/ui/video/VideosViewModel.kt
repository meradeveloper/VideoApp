package com.`in`.videoassignment.ui.video

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.`in`.videoassignment.base.BaseViewModel
import com.`in`.videoassignment.data.Response
import com.`in`.videoassignment.data.VideoRepo
import javax.inject.Inject

class VideosViewModel : BaseViewModel {

    @Inject
    constructor(videoRepo: VideoRepo) : super(videoRepo)

    var videos : LiveData<Response>?=null
    fun getVideos()
    {
        videos = getVideoRepo().getAllVideosFromStorage()
    }

    fun cancelJob()
    {
        getVideoRepo().cancelJob()
    }
}