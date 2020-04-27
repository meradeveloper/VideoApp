package com.`in`.videoassignment.ui.video

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.`in`.videoassignment.R
import com.`in`.videoassignment.data.Status
import com.`in`.videoassignment.data.VideoRepo
import com.`in`.videoassignment.ui.upload.VideoUpload
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_all_videos.*
import javax.inject.Inject


class AllVideos : AppCompatActivity() {

    @Inject
    lateinit var viewmodel: VideosViewModel

    @Inject
    lateinit var videoRepo: VideoRepo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidInjection.inject(this)
        hideSystemUI()
        setContentView(R.layout.activity_all_videos)
        if(isInternetAvailable())
        {
            tvWarning.visibility = View.GONE
            viewmodel.getVideos()
            viewmodel.videos?.observe(this, Observer {

                if(it.status==Status.SUCCESS)
                {
                    viewmodel.cancelJob()
                    if((it?.Response as List<*>).size>0)
                    {
                        tvWarning.visibility = View.GONE
                        supportFragmentManager.beginTransaction().
                            add(R.id.container,VideoFragment.
                                newInstance(it)).commit()
                    }
                    else
                    {
                        tvWarning.visibility = View.VISIBLE
                        tvWarning.text = resources.getString(R.string.Add_Video)
                    }
                }
            })
        }
        else
        {
            tvWarning.visibility = View.VISIBLE
            tvWarning.text = resources.getString(R.string.Internet_Not_Available)
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)?.state == NetworkInfo.State.CONNECTED ||
            connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)?.state == NetworkInfo.State.CONNECTED
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    fun addYourOwn(view: View) {
        startActivity(Intent(this, VideoUpload::class.java))
        finish()
    }

    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}




