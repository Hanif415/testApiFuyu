package com.example.testapi.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.testapi.R
import com.example.testapi.data.response.Inputs
import com.example.testapi.data.response.MyData
import com.example.testapi.data.response.ResultResponse
import com.example.testapi.data.response.TestResponse
import com.example.testapi.data.retrofit.ApiConfig
import com.example.testapi.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var capture: ImageButton
    lateinit var toggleFlash: ImageButton
    lateinit var flipCamera: ImageButton
    lateinit var previewView: PreviewView
    private var cameraFacing = CameraSelector.LENS_FACING_BACK
    var first = true

    var storageReference: StorageReference? = null

    private var tts: TextToSpeech? = null

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                startCamera(cameraFacing)
            }
        }
    var n: Int = 1;

    companion object {
        private const val TAG = "MainActivity"
    }

    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        previewView = findViewById(R.id.cameraPreview);
        capture = findViewById(R.id.capture);
        toggleFlash = findViewById(R.id.toggleFlash);
        flipCamera = findViewById(R.id.flipCamera);

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activityResultLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startCamera(cameraFacing)
        }

        flipCamera.setOnClickListener(View.OnClickListener {
            cameraFacing = if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            startCamera(cameraFacing)
        })

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts!!.setLanguage(Locale("id", "ID"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "The Language specified is not supported!")
                }
            } else {
                Log.e("TTS", "Initialization Failed!")
            }
        }



    }

    private fun startCamera(cameraFacing: Int) {
        val aspectRatio: Int = aspectRatio(previewView.width, previewView.height)
        val listenableFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(this)

        listenableFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider =
                    listenableFuture.get() as ProcessCameraProvider

                val preview =
                    Preview.Builder().setTargetAspectRatio(aspectRatio).build()

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(windowManager.defaultDisplay.rotation).build()

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(cameraFacing).build()
                cameraProvider.unbindAll()

                val camera: Camera =
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

                if (camera.cameraInfo.hasFlashUnit()) {
                    camera.cameraControl.enableTorch(true)
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Flash is not available currently",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                capture.setOnClickListener {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    takePicture(imageCapture)
                }

                preview.setSurfaceProvider(previewView.surfaceProvider)

            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    fun takePicture(imageCapture: ImageCapture) {
        val file = File(getExternalFilesDir(null), System.currentTimeMillis().toString() + ".jpg")
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            outputFileOptions,
            Executors.newCachedThreadPool(),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    runOnUiThread {
                        loud("Foto telah ditangkap")

                        uploadImage(file)
                    }
                    startCamera(cameraFacing)
                }

                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to save: " + exception.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    startCamera(cameraFacing)
                }
            })
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = Math.max(width, height).toDouble() / Math.min(width, height)
        if (Math.abs(previewRatio - 4.0 / 3.0) <= Math.abs(previewRatio - 16.0 / 9.0)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    private fun uploadImage(file: File) {
        val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.CANADA)
        val now = Date()
        val fileName = formatter.format(now)
        storageReference =
            FirebaseStorage.getInstance().getReference("images/$fileName")

        val bitmap = BitmapFactory.decodeFile(file.toString())
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        val compressedImageData = baos.toByteArray()

        storageReference!!.putBytes(compressedImageData)
            .addOnSuccessListener {
                loud("Harap tunggu sebentar")
                storageReference!!.getDownloadUrl()
                    .addOnSuccessListener { uri ->
                        // This is the download URL for your image
                        val imageUrl = uri.toString()
                        generate(imageUrl)
                        // You can now use this URL to view the image in a browser
                    }
                    .addOnFailureListener {
                        // Handle any errors
                    }
            }.addOnFailureListener {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to Upload",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun generate(imageUrl: String) {

        val inputs = Inputs(
            imageUrl, "apa itu dan ada tulisan apa?", 1024, 0.2, 1
        )

        val myData = MyData(
            inputs, "01359160a4cff57c6b7d4dc625d0019d390c7c46f553714069f114b392f4a726"
        )

        val client = ApiConfig.getApiService().postTheImage(myData)
        client.enqueue(object : Callback<TestResponse> {
            override fun onResponse(call: Call<TestResponse>, response: Response<TestResponse>) {
                val responseBody = response.body()
                if (responseBody != null) {
                    getResult(responseBody.id)
                }
            }

            override fun onFailure(call: Call<TestResponse>, t: Throwable) {
                Log.e(TAG, "onFailure: ${t.message}")
            }

        })
    }

    private fun getResult(id: String) {
        val client = ApiConfig.getApiService().getResult(id)
        client.enqueue(object : Callback<ResultResponse> {
            override fun onResponse(
                call: Call<ResultResponse>, response: Response<ResultResponse>
            ) {
                val responseBody = response.body()
                if (responseBody != null) {
                    if (responseBody.status == "processing") {
                        setData("status: ${responseBody.status} \nattempt: ${n++}")
                        loud(responseBody.status)
                        runBlocking {
                            launch {
                                delay(3000L) // Delay for 2 seconds
                                getResult(id)
                            }
                        }
                    } else if (responseBody.status == "starting") {
                        setData("status: ${responseBody.status} \nattempt: ${n++}")
                        loud(responseBody.status)
                        if (first) {
                            runBlocking {
                                launch {
                                    delay(5000L) // Delay for 2 seconds
                                    getResult(id)
                                }
                            }
                        }
                    } else {
                        val sentence = responseBody.output?.joinToString(" ")
                        setData("result: $sentence")
                        if (sentence != null) {
                            loud(sentence)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<ResultResponse>, t: Throwable) {
                Log.e(TAG, "onFailure: ${t.message}")
            }

        })
    }

//    private fun cancel(id: String) {
//        val client = ApiConfig.getApiService().resultCancel(id)
//        client.enqueue(object : Callback<ResultResponse> {
//            override fun onResponse(
//                call: Call<ResultResponse>, response: Response<ResultResponse>
//            ) {
//                getResult(id)
//            }
//
//            override fun onFailure(call: Call<ResultResponse>, t: Throwable) {
//                Log.e(TAG, "onFailure: ${t.message}")
//            }
//
//        })
//    }

    private fun setData(result: String) {
        binding.text.text = result
    }

    private fun loud(text: String) {
        tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }
}