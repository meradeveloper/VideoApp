package com.`in`.videoassignment.data

import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.lifecycle.LiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import java.util.*


class VideoRepo {
    private var dbreference = FirebaseFirestore.getInstance()
    private var storageReference = FirebaseStorage.getInstance().getReference()
    private var TAG = "VideoRepo"
    private var job:CompletableJob?=null

    fun getAllVideosFromStorage():LiveData<Response>
    {
        job = Job()
        return object:LiveData<Response>()
        {
            override fun onActive() {
                super.onActive()
                var Response = Response(Status.FAILED,"Not Fetching",null)
                job?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                    dbreference.collection("Videos")
                        .orderBy("id",Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e(TAG, "Listen failed.", e)
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            Response.status= Status.SUCCESS
                            Response.Response = snapshot.documents.map {
                                Video(it.getString("id"),it.getString("videoName"),it.getString("videoUrl"))
                            }.toList()
                            value = Response

                        } else {

                        }
                    }
                    }
                }
            }
        }
    }

    fun uploadVideo(filePath: Uri?,context: Context):LiveData<Boolean>
    {
        return object : LiveData<Boolean>()
        {
            override fun onActive() {
                super.onActive()
                if (filePath != null) { //displaying progress dialog while image is uploading
                    val progressDialog = ProgressDialog(context)
                    progressDialog.setTitle("Uploading")
                    progressDialog.setCancelable(false)
                    progressDialog.show()
                    var pathstring = "videos/" + filePath.lastPathSegment

                    //getting the storage reference
                    val sRef = storageReference.child(
                        pathstring
                    )
                    // task for uploading video

                    var uploadTask = sRef.putFile(filePath)

                    uploadTask.continueWithTask { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let {
                                throw it
                            }
                        }
                        sRef.downloadUrl
                    }.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val downloadUri = task.result
                            progressDialog.dismiss()
                            //displaying success toast
                            Toast.makeText(context, "File Uploaded ", Toast.LENGTH_LONG)
                                .show()
                            value = true
                            //creating the upload object to store uploaded image details

                            val upload = Video(
                                Date().time.toString(),
                                task.result?.lastPathSegment,
                                downloadUri.toString()

                            )
                            //adding an upload to firebase database
                            dbreference.collection("Videos").add(upload).addOnCompleteListener{

                            }
                        } else {
                            // Handle failures
                            // ...
                        }
                    }.addOnFailureListener{
                        value = false
                    }
                    uploadTask.addOnProgressListener { taskSnapshot ->
                        //displaying the upload progress
                        val progress =
                            100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                        progressDialog.setMessage("Uploaded " + progress.toInt() + "%...")
                    }

                } else { //display an error if no file is selected
                }

            }
        }
    }

    fun cancelJob()
    {
        job?.cancel()
    }
}