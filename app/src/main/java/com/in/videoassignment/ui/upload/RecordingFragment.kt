package com.`in`.videoassignment.ui.upload

import android.app.Activity
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.Visibility
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.MediaController
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.`in`.videoassignment.Camera.CameraVideoFragment
import com.`in`.videoassignment.R
import kotlinx.android.synthetic.main.fragment_recording.*
import java.io.File
import java.io.IOException
import java.lang.RuntimeException


private const val ARG_PARAM1 = "param1"

class RecordingFragment : CameraVideoFragment(),View.OnClickListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var mlistener: CameraListener? = null
    lateinit private var countDownTimer:CountDownTimer
    private var mOutputFilePath: String? = null

    override val getTextureResource: Int
        get() = R.id.mTextureView

    override fun setUp(view: View?) {
         //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            //param1 = it.getString(ARG_PARAM1)
        }
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        if(activity is CameraListener)
            mlistener = activity
        else
            throw RuntimeException("implement Camera Listener is Required")
    }

    override fun onDetach() {
        super.onDetach()
        mlistener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = inflater.inflate(R.layout.fragment_recording, container, false)
        view.findViewById<ImageView>(R.id.mRecordVideo).setOnClickListener(this)
        view.findViewById<ImageView>(R.id.mPlayVideo).setOnClickListener(this)
        view.findViewById<ImageView>(R.id.mSwitchCamera).setOnClickListener(this)
        view.findViewById<Button>(R.id.uploadbtn).setOnClickListener(this)
        countDownTimer =
            object : CountDownTimer(DURATION, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    tvtimer.setText("" + millisUntilFinished / 1000)
                }

                override fun onFinish() {
                    try {
                        countDownTimer!!.cancel()
                        mIsRecordingVideo = false
                        stopAutoRecording()
                        prepareVideoViews()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        return view
    }

    private fun prepareVideoViews() {
        if (mVideoView.visibility == View.GONE) {
            mVideoView.visibility = View.VISIBLE
            mPlayVideo.visibility = View.VISIBLE
            mTextureView.visibility = View.GONE
            uploadbtn.visibility = View.VISIBLE
            try {
                setMediaForRecordedVideo()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @Throws(IOException::class)
    private fun setMediaForRecordedVideo() {
        //mOutputFilePath = parseVideo(mOutputFilePath);
        // Set media controller
        mVideoView.setMediaController(MediaController(activity))
        mVideoView.requestFocus()
        //mVideoView.setVideoPath(mOutputFilePath);
        mVideoView.setVideoURI(Uri.parse(mOutputFilePath))
        mVideoView.seekTo(100)
        mVideoView.setOnCompletionListener { mp: MediaPlayer? ->
            // Reset player
            mVideoView.visibility = View.GONE
            mTextureView.visibility = View.VISIBLE
            mPlayVideo.visibility = View.GONE
            mRecordVideo.setImageResource(R.drawable.ic_record)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            RecordingFragment().apply {
                arguments = Bundle().apply {
                    //putString(ARG_PARAM1, param1)
                }
            }
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.mRecordVideo ->

                if (mIsRecordingVideo) {
                    try {
                        countDownTimer.cancel()
                        tvtimer.visibility = View.GONE
                        stopRecordingVideo()
                        prepareVideoViews()
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                } else {
                    startRecordingVideo()
                    tvtimer.visibility = View.VISIBLE
                    countDownTimer.start()
                    mRecordVideo.setImageResource(R.drawable.ic_stop)
                    //Receive out put file here
                    mOutputFilePath = currentFile?.getAbsolutePath()
                }
            R.id.mPlayVideo -> {
                mVideoView.start()
                mPlayVideo.visibility = View.GONE
            }
            R.id.mSwitchCamera -> switchCamera()
            R.id.uploadbtn ->
            {
                mlistener?.onUploadVideo(mOutputFilePath)?.observe(activity as VideoUpload, Observer {
                    if(it)
                        prepareCameraViews()
                    else
                        Toast.makeText(activity,"Error in Uplaoding",Toast.LENGTH_LONG).show()
                })
            }
        }
    }

    private fun prepareCameraViews() {
        if (mVideoView.visibility == View.VISIBLE) {
            mVideoView.visibility = View.GONE
            mPlayVideo.visibility = View.GONE
            mTextureView.visibility = View.VISIBLE
            uploadbtn.visibility = View.GONE
            try {
                reopenCamera()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    interface CameraListener {fun onUploadVideo(path:String?):LiveData<Boolean>}
}
