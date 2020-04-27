package com.`in`.videoassignment

import android.app.Application
import com.`in`.videoassignment.di.DaggerAppComponent
import com.google.firebase.FirebaseApp
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

class MyApp : Application(),HasAndroidInjector{

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        DaggerAppComponent.builder().app(this).build().inject(this);
    }

    override fun androidInjector(): AndroidInjector<Any>? {
        return dispatchingAndroidInjector;
    }
}