package com.`in`.videoassignment.ui.video

import com.`in`.videoassignment.data.VideoRepo
import dagger.Module
import dagger.Provides

@Module
class VideoModule {

    @Provides
    fun VideoViewModel(videoRepo: VideoRepo) =
        VideosViewModel(videoRepo = videoRepo)
}