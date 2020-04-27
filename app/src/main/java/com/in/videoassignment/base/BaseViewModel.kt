package com.`in`.videoassignment.base

import androidx.lifecycle.ViewModel
import com.`in`.videoassignment.data.VideoRepo

open class BaseViewModel  : ViewModel {

    private var videoRepo:VideoRepo

    constructor(videoRepo: VideoRepo)
    {
        this.videoRepo = videoRepo
    }

    fun getVideoRepo() = videoRepo
}