package com.yourcompany.worklisten.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.yourcompany.worklisten.data.local.model.Word
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import android.os.Handler
import android.os.Looper

private enum class RequestType {
    SPEAK,
    PAUSE
}

private data class SpeakRequest(
    val type: RequestType,
    val text: String = "",
    val locale: Locale = Locale.getDefault(),
    val speed: Float = 1.0f,
    val pauseDuration: Long = 0,
    val onDone: () -> Unit,
    val utteranceId: String
)

class TtsHelper(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech
    private val mainHandler = Handler(Looper.getMainLooper())
    private val requestQueue = ConcurrentLinkedQueue<SpeakRequest>()
    private var isProcessingQueue = false
    private val _isReady = MutableLiveData<Boolean>()
    val isReady: LiveData<Boolean> = _isReady

    private val punctuationRegex = Regex("[.,;；。!\"#$%&'()*+,-./:;<=>?@\\[\\]^_`{|}~]")

    init {
        _isReady.value = false
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            _isReady.postValue(true)
            processQueue() // Process any pending requests
        }
    }

    fun speak(word: Word?, speed: Float, onDone: () -> Unit = {}) {
        if (word == null) {
            onDone()
            return
        }
        val rawText = if (word.isJapanese) word.word else word.originalWord
        val textToSpeak = punctuationRegex.replace(rawText, " ") // Clean the word text
        val locale = if (word.isJapanese) Locale.JAPANESE else Locale.ENGLISH

        val utteranceId = UUID.randomUUID().toString()
        val request = SpeakRequest(
            type = RequestType.SPEAK,
            text = textToSpeak,
            locale = locale,
            speed = speed,
            onDone = onDone,
            utteranceId = utteranceId
        )
        requestQueue.offer(request)
        processQueue()
    }

    fun speakMeaning(word: Word?, speed: Float, onDone: () -> Unit = {}) {
        if (word == null) {
            onDone()
            return
        }

        // Split meanings and create a list of speak/pause requests
        val meaningParts = word.meaning.split(Regex("[;；]"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (meaningParts.isEmpty()) {
            onDone()
            return
        }
        
        meaningParts.forEachIndexed { index, part ->
            val textToSpeak = punctuationRegex.replace(part, " ")
            val utteranceId = UUID.randomUUID().toString()
            
            // The onDone callback is only attached to the very last request
            val partDoneCallback = if (index == meaningParts.size - 1) onDone else { {} }

            val speakRequest = SpeakRequest(
                type = RequestType.SPEAK,
                text = textToSpeak,
                locale = Locale.CHINESE,
                speed = speed,
                onDone = partDoneCallback,
                utteranceId = utteranceId
            )
            requestQueue.offer(speakRequest)

            // Add a pause request if it's not the last part
            if (index < meaningParts.size - 1) {
                val pauseRequest = SpeakRequest(
                    type = RequestType.PAUSE,
                    pauseDuration = 200L, // 200ms pause
                    onDone = {},
                    utteranceId = UUID.randomUUID().toString()
                )
                requestQueue.offer(pauseRequest)
            }
        }
        processQueue()
    }

    private fun processQueue() {
        if (isProcessingQueue || !_isReady.value!!) return

        val request = requestQueue.poll() ?: return
        isProcessingQueue = true

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                if (utteranceId == request.utteranceId) {
                    mainHandler.post {
                        request.onDone()
                        isProcessingQueue = false
                        processQueue() // Process next request
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                 if (utteranceId == request.utteranceId) {
                    mainHandler.post {
                        request.onDone() // Also continue on error
                        isProcessingQueue = false
                        processQueue()
                    }
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                if (utteranceId == request.utteranceId) {
                    mainHandler.post {
                        request.onDone() // Also continue on error
                        isProcessingQueue = false
                        processQueue()
                    }
                }
            }
        })

        when(request.type) {
            RequestType.SPEAK -> {
                tts.language = request.locale
                tts.setSpeechRate(request.speed)
                tts.speak(request.text, TextToSpeech.QUEUE_ADD, null, request.utteranceId)
            }
            RequestType.PAUSE -> {
                tts.playSilentUtterance(request.pauseDuration, TextToSpeech.QUEUE_ADD, request.utteranceId)
            }
        }
    }

    fun playSilence(durationInMillis: Long, onDone: () -> Unit = {}) {
        val request = SpeakRequest(
            type = RequestType.PAUSE,
            pauseDuration = durationInMillis,
            onDone = onDone,
            utteranceId = UUID.randomUUID().toString()
        )
        requestQueue.offer(request)
        processQueue()
    }

    fun stop() {
        requestQueue.clear()
        if (tts.isSpeaking) {
            tts.stop()
        }
        isProcessingQueue = false
    }

    fun warmUp() {
        // Play a very short silent utterance to warm up the engine
        playSilence(1, {})
    }
    
    fun shutdown() {
        tts.shutdown()
    }
} 