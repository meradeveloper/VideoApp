package com.`in`.videoassignment.ui.upload

import android.content.Intent
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.`in`.videoassignment.R
import com.`in`.videoassignment.ui.video.AllVideos
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_video_upload.*
import java.io.File
import javax.inject.Inject


class VideoUpload : AppCompatActivity(),RecordingFragment.CameraListener {

    @Inject
    lateinit var mviewModel:VideoUploadViewModel
    val REQUEST_VIDEO_CAPTURE = 1
    var VideoPath:Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidInjection.inject(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_video_upload)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        dispatchTakeVideoFragment()
    }

    private fun dispatchTakeVideoFragment() {
        fragmentManager.beginTransaction()
            .add(R.id.camera_container,RecordingFragment.newInstance()).commit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            VideoPath = data!!.data
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this,AllVideos::class.java))
        finish()
    }

    override fun onUploadVideo(path: String?):LiveData<Boolean> {
        return mviewModel.upload(Uri.fromFile(File(path)),this)
    }
}
