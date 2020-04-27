package com.`in`.videoassignment.Camera

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.`in`.videoassignment.R
import com.`in`.videoassignment.base.BaseFragment
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.DexterError
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit


abstract class CameraVideoFragment : BaseFragment() {
    private var cameraId = CAMERA_FRONT
    private var manager: CameraManager? = null

    companion object {
        private const val TAG = "CameraVideoFragment"
        private const val SENSOR_ORIENTATION_INVERSE_DEGREES = 270
        private const val SENSOR_ORIENTATION_DEFAULT_DEGREES = 90
        private val INVERSE_ORIENTATIONS = SparseIntArray()
        private val DEFAULT_ORIENTATIONS = SparseIntArray()
        const val CAMERA_FRONT = "1"
        const val CAMERA_BACK = "0"
        const val DURATION = 30*1000L
        private const val VIDEO_DIRECTORY_NAME = "VideoAssignment"
        /**
         * In this sample, we choose a video size with 3x4 for  aspect ratio. for more perfectness 720 as well Also, we don't use sizes
         * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
         *
         * @param choices The list of available sizes
         * @return The video size 1080p,720px
         */
        private fun chooseVideoSize(choices: Array<Size>): Size {
            for (size in choices) {
                if (1920 == size.width && 1080 == size.height) {
                    return size
                }
            }
            for (size in choices) {
                if (size.width == size.height * 4 / 3 && size.width <= 1080) {
                    return size
                }
            }
            Log.e(
                TAG,
                "Couldn't find any suitable video size"
            )
            return choices[choices.size - 1]
        }

        /**
         * Given `choices` of `Size`s supported by a camera, chooses the smallest one whose
         * width and height are at least as large as the respective requested values, and whose aspect
         * ratio matches with the specified value.
         *
         * @param choices     The list of sizes that the camera supports for the intended output class
         * @param width       The minimum desired width
         * @param height      The minimum desired height
         * @param aspectRatio The aspect ratio
         * @return The optimal `Size`, or an arbitrary one if none were big enough
         */
        private fun chooseOptimalSize(
            choices: Array<Size>,
            width: Int,
            height: Int,
            aspectRatio: Size?
        ): Size { // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough: MutableList<Size> =
                ArrayList()
            val w = aspectRatio!!.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.height == option.width * h / w && option.width >= width && option.height >= height
                ) {
                    bigEnough.add(option)
                }
            }
            // Pick the smallest of those, assuming we found any
            return if (bigEnough.size > 0) {
                Collections.min(bigEnough, CompareSizesByArea())
            } else {
                Log.e(
                    TAG,
                    "Couldn't find any suitable preview size"
                )
                choices[0]
            }
        }

        init {
            INVERSE_ORIENTATIONS.append(
                Surface.ROTATION_270,
                0
            )
            INVERSE_ORIENTATIONS.append(
                Surface.ROTATION_180,
                90
            )
            INVERSE_ORIENTATIONS.append(
                Surface.ROTATION_90,
                180
            )
            INVERSE_ORIENTATIONS.append(
                Surface.ROTATION_0,
                270
            )
        }

        init {
            DEFAULT_ORIENTATIONS.append(
                Surface.ROTATION_90,
                0
            )
            DEFAULT_ORIENTATIONS.append(
                Surface.ROTATION_0,
                90
            )
            DEFAULT_ORIENTATIONS.append(
                Surface.ROTATION_270,
                180
            )
            DEFAULT_ORIENTATIONS.append(
                Surface.ROTATION_180,
                270
            )
        }
    }

    protected var currentFile: File? = null
        private set
    /**
     * An [AutoFitTextureView] for camera preview.
     */
    private var mTextureView: AutoFitTextureView? = null
    /**
     * A reference to the opened [CameraDevice].
     */
    private var mCameraDevice: CameraDevice? = null
    /**
     * A reference to the current [CameraCaptureSession] for
     * preview.
     */
    private var mPreviewSession: CameraCaptureSession? = null
    /**
     * [TextureView.SurfaceTextureListener] handles several lifecycle events on a
     * [TextureView].
     */
    private val mSurfaceTextureListener: SurfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            surfaceTexture: SurfaceTexture,
            width: Int, height: Int
        ) {
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(
            surfaceTexture: SurfaceTexture,
            width: Int, height: Int
        ) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
    }
    /**
     * The [Size] of camera preview.
     */
    private var mPreviewSize: Size? = null
    /**
     * The [Size] of video recording.
     */
    private var mVideoSize: Size? = null
    /**
     * MediaRecorder
     */
    private var mMediaRecorder: MediaRecorder? = null
    /**
     * Whether the app is recording video now
     */
    var mIsRecordingVideo = false
    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private var mBackgroundThread: HandlerThread? = null
    /**
     * A [Handler] for running tasks in the background.
     */
    private var mBackgroundHandler: Handler? = null
    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val mCameraOpenCloseLock =
        Semaphore(1)
    /**
     * [CameraDevice.StateCallback] is called when [CameraDevice] changes its status.
     */
    private val mStateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            mCameraDevice = cameraDevice
            startPreview()
            mCameraOpenCloseLock.release()
            if (null != mTextureView) {
                configureTransform(mTextureView!!.width, mTextureView!!.height)
            }
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
            val activity: Activity = getActivity()
            activity?.finish()
        }
    }
    private var mSensorOrientation: Int? = null
    private var mPreviewBuilder: CaptureRequest.Builder? = null
    abstract val getTextureResource: Int
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        mTextureView = view.findViewById(getTextureResource)
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        requestPermission()
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    /**
     * Starts a background thread and its [Handler].
     */
    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    /**
     * Stops the background thread and its [Handler].
     */
    private fun stopBackgroundThread() {
        mBackgroundThread!!.quitSafely()
        try {
            mBackgroundThread!!.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    /**
     * Requesting permissions storage, audio and camera at once
     */
    fun requestPermission() {
        Dexter.withActivity(getActivity()).withPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) { // check if all permissions are granted or not
                    if (report.areAllPermissionsGranted()) {
                        if (mTextureView!!.isAvailable) {
                            openCamera(mTextureView!!.width, mTextureView!!.height)
                        } else {
                            mTextureView!!.surfaceTextureListener = mSurfaceTextureListener
                        }
                    }
                    // check for permanent denial of any permission show alert dialog
                    if (report.isAnyPermissionPermanentlyDenied) { // open Settings activity
                        showSettingsDialog()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).withErrorListener { error: DexterError? ->
                Toast.makeText(
                    getActivity().getApplicationContext(),
                    "Error occurred! ",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .onSameThread()
            .check()
    }

    /**
     * Showing Alert Dialog with Settings option in case of deny any permission
     */
    private fun showSettingsDialog() {
        val builder =
            AlertDialog.Builder(getActivity())
        builder.setTitle(getString(R.string.message_need_permission))
        builder.setMessage(getString(R.string.message_permission))
        builder.setPositiveButton(
            getString(R.string.title_go_to_setting),
            DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
                dialog.cancel()
                openSettings()
            }
        )
        builder.show()
    }

    fun switchCamera() {
        if (cameraId == CAMERA_FRONT) {
            cameraId = CAMERA_BACK
            closeCamera()
            reopenCamera()
        } else if (cameraId == CAMERA_BACK) {
            cameraId = CAMERA_FRONT
            closeCamera()
            reopenCamera()
        }
    }

    fun reopenCamera() {
        if (mTextureView!!.isAvailable) {
            openCamera(mTextureView!!.width, mTextureView!!.height)
        } else {
            mTextureView!!.surfaceTextureListener = mSurfaceTextureListener
        }
    }

    // navigating settings app
    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri =
            Uri.fromParts("package", getActivity().getPackageName(), null)
        intent.data = uri
        startActivityForResult(intent, 101)
    }

    /**
     * Tries to open a [CameraDevice]. The result is listened by `mStateCallback`.
     */
    private fun openCamera(width: Int, height: Int) {
        val activity: Activity = getActivity()
        if (null == activity || activity.isFinishing) {
            return
        }
        manager =
            activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            Log.d(TAG, "tryAcquire")
            if (!mCameraOpenCloseLock.tryAcquire(
                    3500,
                    TimeUnit.MILLISECONDS
                )
            ) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            /**
             * default front camera will activate
             */

            val characteristics = manager!!.getCameraCharacteristics(cameraId)
            val map = characteristics
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
            if (map == null) {
                throw RuntimeException("Cannot get available preview/video sizes")
            }
            mVideoSize = chooseVideoSize(
                map.getOutputSizes(
                    MediaRecorder::class.java
                )
            )
            mPreviewSize = chooseOptimalSize(
                map.getOutputSizes(
                    SurfaceTexture::class.java
                ),
                width, height, mVideoSize
            )
            val orientation: Int = getResources().getConfiguration().orientation
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView!!.setAspectRatio(mPreviewSize!!.width, mPreviewSize!!.height)
            } else {
                mTextureView!!.setAspectRatio(mPreviewSize!!.height, mPreviewSize!!.width)
            }
            configureTransform(width, height)
            mTextureView!!.requestFocus()
            mMediaRecorder = MediaRecorder()
            if (ActivityCompat.checkSelfPermission(
                    getActivity(),
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) { // TODO: Consider calling
                requestPermission()
                return
            }
            manager!!.openCamera(cameraId, mStateCallback, null)
        } catch (e: CameraAccessException) {
            Log.e(
                TAG,
                "openCamera: Cannot access the camera."
            )
        } catch (e: NullPointerException) {
            Log.e(
                TAG,
                "Camera2API is not supported on the device."
            )
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.")
        }
    }// External sdcard file location
    // Create storage directory if it does not exist

    /**
     * Create directory and return file
     * returning video file
     */
    private val outputMediaFile: File?
        private get() { // External sdcard file location
            val mediaStorageDir = File(
                Environment.getExternalStorageDirectory(),
                VIDEO_DIRECTORY_NAME
            )
            // Create storage directory if it does not exist
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d(
                        TAG, "Oops! Failed create "
                                + VIDEO_DIRECTORY_NAME + " directory"
                    )
                    return null
                }
            }
            val timeStamp = SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()
            ).format(Date())
            val mediaFile: File
            mediaFile = File(
                mediaStorageDir.path + File.separator
                        + "VID_" + timeStamp + ".mp4"
            )
            return mediaFile
        }

    /**
     * close camera and release object
     */
    private fun closeCamera() {
        try {
            mCameraOpenCloseLock.acquire()
            closePreviewSession()
            if (null != mCameraDevice) {
                mCameraDevice!!.close()
                mCameraDevice = null
            }
            if (null != mMediaRecorder) {
                mMediaRecorder!!.release()
                mMediaRecorder = null
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.")
        } finally {
            mCameraOpenCloseLock.release()
        }
    }

    /**
     * Start the camera preview.
     */
    private fun startPreview() {
        if (null == mCameraDevice || !mTextureView!!.isAvailable || null == mPreviewSize) {
            return
        }
        try {
            closePreviewSession()
            val texture = mTextureView!!.surfaceTexture!!
            texture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)
            mPreviewBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            val previewSurface = Surface(texture)
            mPreviewBuilder!!.addTarget(previewSurface)
            mCameraDevice!!.createCaptureSession(
                listOf(previewSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        mPreviewSession = session
                        updatePreview()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(
                            TAG,
                            "onConfigureFailed: Failed "
                        )
                    }
                }, mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Update the camera preview. [.startPreview] needs to be called in advance.
     */
    private fun updatePreview() {
        if (null == mCameraDevice) {
            return
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder)
            val thread = HandlerThread("CameraPreview")
            thread.start()
            mPreviewSession!!.setRepeatingRequest(
                mPreviewBuilder!!.build(),
                null,
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder?) {
        builder!!.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
    }

    /**
     * Configures the necessary [Matrix] transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val activity: Activity = getActivity()
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return
        }
        val rotation = activity.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0F, 0F, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect =
            RectF(0F, 0F, mPreviewSize!!.height.toFloat(), mPreviewSize!!.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                viewHeight.toFloat() / mPreviewSize!!.height,
                viewWidth.toFloat() / mPreviewSize!!.width
            )
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate(90 * (rotation - 2).toFloat(), centerX, centerY)
        }
        mTextureView!!.setTransform(matrix)
    }

    @Throws(IOException::class)
    private fun setUpMediaRecorder() {
        val activity: Activity = getActivity() ?: return
        mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        /**
         * create video output file
         */
        currentFile = outputMediaFile
        /**
         * set output file in media recorder
         */
        mMediaRecorder!!.setOutputFile(currentFile!!.absolutePath)
        val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P)
        mMediaRecorder!!.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight)
        mMediaRecorder!!.setVideoEncodingBitRate(profile.videoBitRate)
        mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mMediaRecorder!!.setVideoFrameRate(profile.videoFrameRate)
        mMediaRecorder!!.setMaxDuration(DURATION.toInt())
        mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mMediaRecorder!!.setAudioEncodingBitRate(profile.audioBitRate)
        mMediaRecorder!!.setAudioSamplingRate(profile.audioSampleRate)
        val rotation = activity.windowManager.defaultDisplay.rotation
        when (mSensorOrientation) {
            SENSOR_ORIENTATION_DEFAULT_DEGREES -> mMediaRecorder!!.setOrientationHint(
                DEFAULT_ORIENTATIONS[rotation]
            )
            SENSOR_ORIENTATION_INVERSE_DEGREES -> mMediaRecorder!!.setOrientationHint(
                INVERSE_ORIENTATIONS[rotation]
            )
        }
        mMediaRecorder!!.prepare()
    }

    fun startRecordingVideo() {
        if (null == mCameraDevice || !mTextureView!!.isAvailable || null == mPreviewSize) {
            return
        }
        try {
            closePreviewSession()
            setUpMediaRecorder()
            val texture = mTextureView!!.surfaceTexture!!
            texture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)
            mPreviewBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            val surfaces: MutableList<Surface> =
                ArrayList()
            /**
             * Surface for the camera preview set up
             */
            val previewSurface = Surface(texture)
            surfaces.add(previewSurface)
            mPreviewBuilder!!.addTarget(previewSurface)
            //MediaRecorder setup for surface
            val recorderSurface = mMediaRecorder!!.surface
            surfaces.add(recorderSurface)
            mPreviewBuilder!!.addTarget(recorderSurface)
            // Start a capture session
            mCameraDevice!!.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        mPreviewSession = cameraCaptureSession
                        updatePreview()
                        getActivity().runOnUiThread(Runnable {
                            mIsRecordingVideo = true
                            // Start recording
                            mMediaRecorder!!.start()
                        })
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        Log.e(
                            TAG,
                            "onConfigureFailed: Failed"
                        )
                    }
                },
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession!!.close()
            mPreviewSession = null
        }
    }

    fun stopAutoRecording() { // UI
        mIsRecordingVideo = false
        try {
            mPreviewSession!!.stopRepeating()
            mPreviewSession!!.abortCaptures()
            mMediaRecorder!!.reset()
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    @Throws(Exception::class)
    fun stopRecordingVideo() { // UI
        mIsRecordingVideo = false
        try {

            mPreviewSession!!.stopRepeating()
            mPreviewSession!!.abortCaptures()
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        // Stop recording
        mMediaRecorder!!.stop()
        mMediaRecorder!!.reset()
    }

    /**
     * Compares two `Size`s based on their areas.
     */
    internal class CompareSizesByArea : Comparator<Size> {
        override fun compare(
            lhs: Size,
            rhs: Size
        ): Int { // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(
                lhs.width.toLong() * lhs.height -
                        rhs.width.toLong() * rhs.height
            )
        }
    }
}