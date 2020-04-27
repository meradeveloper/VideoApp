package com.`in`.videoassignment.di

import android.app.Application
import android.content.Context
import com.`in`.videoassignment.MyApp
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector

@Component(modules = arrayOf(AndroidInjectionModule::class,AppModule::class,ActivityBuilder::class))
interface AppComponent:AndroidInjector<MyApp> {

    override fun inject(instance: MyApp)

    @Component.Builder
    interface Builder
    {
        @BindsInstance
        fun app(application: Application):Builder
        fun build():AppComponent
    }
}