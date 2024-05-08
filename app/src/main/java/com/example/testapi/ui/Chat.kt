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
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.testapi.R
import com.example.testapi.data.response.Inputs
import com.example.testapi.data.response.MyData
import com.example.testapi.data.response.RapidData
import com.example.testapi.data.response.RapidResponse
import com.example.testapi.data.response.ResultResponse
import com.example.testapi.data.response.TestResponse
import com.example.testapi.data.retrofit.ApiConfig
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

@OptIn(DelicateCoroutinesApi::class)
class Chat : AppCompatActivity() {

    companion object {
        private const val TAG = "ChatActivity"
    }

    var first = true
    var n: Int = 1;

    val RecordAudioRequestCode = 1
    private var speechRecognizer: SpeechRecognizer? = null
    private var editText: TextView? = null
    private var micButton: ImageView? = null
    private var imageUrl: String = ""

    var mp: MediaPlayer? = null
    var mp2: MediaPlayer? = null

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkPermission()
        }

        mp = MediaPlayer.create(this, R.raw.button1)
        mp2 = MediaPlayer.create(this, R.raw.button2)
        editText = findViewById(R.id.tv_speech_to_text);
        micButton = findViewById(R.id.iv_mic);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        // Retrieve the string extra from the Intent
        imageUrl = intent.getStringExtra("EXTRA_URL").toString()

        generate(imageUrl, "jelaskan gambar ini")
        editText?.text = imageUrl

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

        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale("id", "ID").toString()
        )

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(bundle: Bundle) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(bytes: ByteArray) {}
            override fun onEndOfSpeech() {}
            override fun onError(i: Int) {}
            override fun onResults(bundle: Bundle) {
                val data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                editText?.text = data?.get(0)
                micButton?.setImageResource(R.drawable.baseline_mic_none_24)
                mp2?.start()
                generate(imageUrl, data?.get(0) ?: "deskripsikan gambar itu")
            }

            override fun onPartialResults(bundle: Bundle) {}
            override fun onEvent(i: Int, bundle: Bundle) {}
        })

        micButton?.setOnTouchListener({ view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    speechRecognizer?.stopListening()
                    editText?.hint = "click and hold"
                    mp2?.start()
                }

                MotionEvent.ACTION_DOWN -> {
                    speechRecognizer?.startListening(speechRecognizerIntent)
                    editText?.setText("")
                    micButton?.setImageResource(R.drawable.baseline_mic_24)
                    editText?.hint = "Listening..."
                    mp?.start()
                }
            }
            false
        })
    }

    private fun generate(imageUrl: String, prompt: String) {

        val inputs = Inputs(
            imageUrl, prompt, 1024, 0.2, 1
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
                    if (responseBody.status == "starting") {
                        setData("status: ${responseBody.status} \nattempt: ${n++}")
                        loud(responseBody.status)
                        // Use coroutine to delay execution
//                        GlobalScope.launch(Dispatchers.Main) {
//                            delay(10000) // Delay for 2 seconds
//                            getResult(id)
//                        }
                        runBlocking {
                            launch {
                                delay(10000L) // Delay for 2 seconds
                                getResult(id)
                            }
                        }
                    } else if (responseBody.status == "processing") {
                        setData("status: ${responseBody.status} \nattempt: ${n++}")
                        loud(responseBody.status)
                        runBlocking {
                            launch {
                                delay(5000L) // Delay for 2 seconds
                                getResult(id)
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

    private fun translate(sentence: String) {
        val rapidData = RapidData(
            sentence, "en", "id"
        )

        val client = ApiConfig.getApiService2().translate(rapidData)
        client.enqueue(object : Callback<RapidResponse> {
            override fun onResponse(
                call: Call<RapidResponse>, response: Response<RapidResponse>
            ) {
                val responseBody = response.body()
                if (responseBody != null) {
                    responseBody.data?.translations?.translatedText?.let { loud(it) }
                }
            }

            override fun onFailure(call: Call<RapidResponse>, t: Throwable) {
                Log.e(TAG, "onFailure: ${t.message}")
            }

        })
    }

    private fun setData(result: String) {
//        binding.text.text = result
    }

    private fun loud(text: String) {
        tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer!!.destroy()
        mp?.release()
        mp2?.release()
    }

    private fun checkPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf<String>(Manifest.permission.RECORD_AUDIO),
            RecordAudioRequestCode
        )
    }
}