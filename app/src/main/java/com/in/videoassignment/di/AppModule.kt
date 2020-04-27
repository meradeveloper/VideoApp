package com.`in`.videoassignment.di

import com.`in`.videoassignment.data.VideoRepo
import dagger.Module
import dagger.Provides

@Module
class AppModule {

    @Provides
    fun provideVideoRepo() = VideoRepo()
}