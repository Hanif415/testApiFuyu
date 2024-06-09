package com.example.testapi.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.testapi.R
import com.example.testapi.data.response.Inputs
import com.example.testapi.data.response.MyData
import com.example.testapi.data.response.ResultResponse
import com.example.testapi.data.response.PredictResponse
import com.example.testapi.data.retrofit.ApiConfig
import com.example.testapi.databinding.ActivityChatBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ChatActivity"
    }

    private lateinit var binding: ActivityChatBinding

    private val RecordAudioRequestCode = 1
    private lateinit var speechRecognizer: SpeechRecognizer

    private var mp: MediaPlayer? = null
    private var mp2: MediaPlayer? = null

    private var imageUrl: String = ""

    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkPermission()
        }

        // Retrieve the string extra from the Intent
        imageUrl = intent.getStringExtra("EXTRA_URL").toString()

        mp = MediaPlayer.create(this, R.raw.button1)
        mp2 = MediaPlayer.create(this, R.raw.button2)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        initTTS()

        initSpeechToText()

        Log.d(TAG, imageUrl)

        predict(imageUrl, "deskripsikan gambar ini dalam bahasa indonesia")
    }

    private fun initSpeechToText() {
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale("id", "ID").toString()
        )

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(bundle: Bundle) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(bytes: ByteArray) {}
            override fun onEndOfSpeech() {}
            override fun onError(i: Int) {
                loud("terjadi kesalahan, tolong ketuk kembali layar untuk bertanya")
            }

            override fun onResults(bundle: Bundle) {
                val data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                binding.ivMic.setImageResource(R.drawable.baseline_mic_none_24)
                mp2?.start()

                predict(
                    imageUrl,
                    data?.get(0) ?: "deskripsikan gambar itu dengan bahasa indonesia"
                )
            }

            override fun onPartialResults(bundle: Bundle) {}
            override fun onEvent(i: Int, bundle: Bundle) {}
        })

        binding.mainView.setOnClickListener {
            if (tts.isSpeaking) {
                tts.stop()
            }
            speechRecognizer.startListening(speechRecognizerIntent)
            binding.ivMic.setImageResource(R.drawable.baseline_mic_24)
            mp?.start()
        }
    }

    private fun initTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale("id", "ID"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "The Language specified is not supported!")
                }
            } else {
                Log.e("TTS", "Initialization Failed!")
            }
        }
    }

    private fun predict(imageUrl: String, prompt: String) {

        val inputs = Inputs(
            imageUrl, prompt, 1024, 0.2, 1
        )

        val myData = MyData(
            inputs, "01359160a4cff57c6b7d4dc625d0019d390c7c46f553714069f114b392f4a726"
        )

        val client = ApiConfig.getApiService().postTheImage(myData)
        client.enqueue(object : Callback<PredictResponse> {
            override fun onResponse(
                call: Call<PredictResponse>,
                response: Response<PredictResponse>
            ) {
                val responseBody = response.body()
                if (responseBody != null) {
                    getResult(responseBody.id)
                }
            }

            override fun onFailure(call: Call<PredictResponse>, t: Throwable) {
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
                    if (responseBody.status == "starting") {
                        loud("memulai, harap tunggu sebentar")
                        runBlocking {
                            launch {
                                delay(10000L) // Delay for 2 seconds
                                getResult(id)
                            }
                        }
                    } else if (responseBody.status == "processing") {
                        loud("memproses, harap tunggu sebentar lagi")
                        runBlocking {
                            launch {
                                delay(5000L) // Delay for 2 seconds
                                getResult(id)
                            }
                        }
                    } else if (responseBody.status == "succeeded") {
                        val sentence = responseBody.output?.joinToString(" ")
                        if (sentence != null) {
                            loud("$sentence. Silahkan ketuk layar untuk bertanya")
                        } else {
                            loud("Terjadi kesalahan, mohon untuk ketuk layar kembali untuk bertanya")
                        }
                    } else if (responseBody.status == "failed") {
                        loud("Terjadi kesalahan, mohon untuk ketuk layar kembali untuk bertanya")
                    } else {
                        loud("Terjadi kesalahan, mohon untuk ketuk layar kembali untuk bertanya")
                    }
                }
            }

            override fun onFailure(call: Call<ResultResponse>, t: Throwable) {
                Log.e(TAG, "onFailure: ${t.message}")
            }

        })
    }

    private fun loud(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private fun checkPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            RecordAudioRequestCode
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        mp?.release()
        mp2?.release()
    }
}
