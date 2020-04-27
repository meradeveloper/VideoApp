package com.`in`.videoassignment.di

import com.`in`.videoassignment.ui.upload.VideoUpload
import com.`in`.videoassignment.ui.upload.VideoUploadModule
import com.`in`.videoassignment.ui.video.AllVideos
import com.`in`.videoassignment.ui.video.VideoModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivityBuilder {

    @ContributesAndroidInjector(modules = arrayOf(VideoModule::class))
    abstract fun contributeAllVideos(): AllVideos;

    @ContributesAndroidInjector(modules = arrayOf(VideoUploadModule::class))
    abstract fun contributeVideoUpload(): VideoUpload;
}