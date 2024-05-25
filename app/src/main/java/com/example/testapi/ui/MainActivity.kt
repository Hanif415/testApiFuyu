package com.example.testapi.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
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
import androidx.camera.core.impl.utils.MatrixExt.postRotate
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.testapi.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var cameraFacing = CameraSelector.LENS_FACING_BACK

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                startCamera(cameraFacing)
            }
        }

    private var storageReference: StorageReference? = null

    private var tts: TextToSpeech? = null

    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activityResultLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startCamera(cameraFacing)
        }

        initTTS();
    }

    private fun startCamera(cameraFacing: Int) {
        val aspectRatio: Int =
            aspectRatio(binding.cameraPreview.width, binding.cameraPreview.height)
        val listenableFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(this)

        listenableFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider =
                    listenableFuture.get() as ProcessCameraProvider

                val preview = Preview.Builder().setTargetAspectRatio(aspectRatio).build()

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(windowManager.defaultDisplay.rotation).build()

                val cameraSelector =
                    CameraSelector.Builder().requireLensFacing(cameraFacing).build()
                cameraProvider.unbindAll()

                val camera: Camera =
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

                if (camera.cameraInfo.hasFlashUnit()) {
                    camera.cameraControl.enableTorch(true)
                } else {
                    runOnUiThread {
                        loud("Flash kamera tidak tersedia")
                    }
                }

                binding.cameraPreview.setOnClickListener {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    takePicture(imageCapture)
                }

                preview.setSurfaceProvider(binding.cameraPreview.surfaceProvider)

            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - 4.0 / 3.0) <= abs(previewRatio - 16.0 / 9.0)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun initTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts!!.setLanguage(Locale("id", "ID"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "The Language specified is not supported!")
                }
                loud("silahkan ketuk layar untuk mengambil foto")
            } else {
                Log.e("TTS", "Initialization Failed!")
            }
        }
    }

    private fun takePicture(imageCapture: ImageCapture) {
        val file = File(getExternalFilesDir(null), System.currentTimeMillis().toString() + ".jpg")
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(outputFileOptions,
            Executors.newCachedThreadPool(),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    runOnUiThread {
                        loud("Foto telah ditangkap, harap tunggu sebentar")

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

    private fun uploadImage(file: File) {
        val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.CANADA)
        val now = Date()
        val fileName = formatter.format(now)
        storageReference = FirebaseStorage.getInstance().getReference("images/$fileName")

        val compressedImageData = adjustImageOrientation(file)

//        val uri = Uri.fromFile(file)

        if (compressedImageData != null) {
            storageReference!!.putBytes(compressedImageData).addOnSuccessListener {
                storageReference!!.getDownloadUrl().addOnSuccessListener { uri ->
                    // This is the download URL for your image
                    val imageUrl = uri.toString()
                    val intent = Intent(this, ChatActivity::class.java).apply {
                        putExtra("EXTRA_URL", imageUrl)
                    }
                    startActivity(intent)
                }.addOnFailureListener {
                    // Handle any errors
                }
            }.addOnFailureListener {
                loud("Terjadi kesalahan, tolong tangkap foto kembali")
            }
        }
    }

    private fun adjustImageOrientation(file: File): ByteArray? {
        val exifInterface = ExifInterface(file.absolutePath)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val rotationAngle = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        if (bitmap == null) {
            println("Failed to decode Bitmap from file")
            return null
        }

        val matrix = Matrix().apply { postRotate(rotationAngle.toFloat()) }
        val rotatedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        val baos = ByteArrayOutputStream()
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        return baos.toByteArray()
    }


    private fun loud(text: String) {
        tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    override fun onRestart() {
        super.onRestart()
        startCamera(cameraFacing)
        loud("silahkan ketuk layar untuk mengambil foto")
    }
}