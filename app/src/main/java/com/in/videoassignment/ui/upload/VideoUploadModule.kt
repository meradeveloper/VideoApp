package com.`in`.videoassignment.ui.upload

import com.`in`.videoassignment.data.VideoRepo
import dagger.Module
import dagger.Provides

@Module
class VideoUploadModule {

    @Provides
    fun provideVideoUploadViewModel(videoRepo: VideoRepo)=VideoUploadViewModel(videoRepo = videoRepo)
}