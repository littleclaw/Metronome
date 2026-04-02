package com.lttclaw.metronome.viewmodel

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.lttclaw.metronome.model.Section
import com.lttclaw.metronome.model.Version
import com.lttclaw.metronome.network.apiService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import me.hgj.jetpackmvvm.base.viewmodel.BaseViewModel
import me.hgj.jetpackmvvm.ext.request
import me.hgj.jetpackmvvm.state.ResultState
import java.util.Locale
import kotlin.coroutines.resume

const val SP_KEY = "listData"
const val SP_BG_URI = "bgUri"
const val SP_BG_NAME = "bgName"
const val PAUSE = "暂停"
const val RESUME = "继续"

class PlayViewModel : BaseViewModel() {
    private val playList = mutableListOf<Section>()
    private var timer: CountDownTimer? = null
    private var delayTimer: CountDownTimer? = null
    private var prepJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    val playing = ObservableBoolean(false)
    val curPlayingIndex = MutableLiveData(0)
    val curSectionIndex = MutableLiveData(0)
    val pauseBtnText = MutableLiveData(PAUSE)
    val curMusicName = MutableLiveData("无")
    private var text2Speech: TextToSpeech? = null

    private var inDelay = false

    val versionResult = MutableLiveData<ResultState<Version>>()

    fun initSectionList(): List<Section> {
        val spInstance = SPUtils.getInstance()
        val listData = spInstance.getString(SP_KEY, "")
        return if (listData.isEmpty()) {
            val list = mutableListOf<Section>()
            val s1 = Section(11, 4000L, 7000L)
            val s2 = Section(12, 5000L, 8000L)
            val s3 = Section(12, 3000L, 1000)
            list.add(s1)
            list.add(s2)
            list.add(s3)
            spInstance.put(SP_KEY, GsonUtils.toJson(list))
            list
        } else {
            GsonUtils.fromJson(listData, GsonUtils.getListType(Section::class.java))
        }
    }

    fun initBgMusic(context: Context) {
        val spInstance = SPUtils.getInstance()
        val bgUriStr = spInstance.getString(SP_BG_URI)
        if (bgUriStr.isNotEmpty()) {
            setBgUri(context, bgUriStr.toUri())
        }
        val bgName = spInstance.getString(SP_BG_NAME)
        if (bgName.isNotEmpty()) {
            curMusicName.value = bgName
        }
    }

    fun initText2Speech(context: Context) {
        text2Speech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val languageCode = text2Speech?.setLanguage(Locale.CHINESE)
                if (languageCode == null || languageCode == TextToSpeech.LANG_NOT_SUPPORTED
                    || languageCode == TextToSpeech.LANG_MISSING_DATA) {
                    Toast.makeText(context, "不支持中文", Toast.LENGTH_SHORT).show()
                } else {
                    text2Speech!!.language = Locale.CHINA
                }
                text2Speech?.let {
                    it.setPitch(1f)
                    it.setSpeechRate(1f)
                }
            }
        }
    }

    fun setBgUri(context: Context, uri: Uri) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    val audioAttribute = AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                    setAudioAttributes(audioAttribute)
                    setDataSource(context, uri)
                    isLooping = true
                    setOnCompletionListener {
                        LogUtils.d("play complete!!")
                    }
                    prepare()
                    setVolume(0.5f, 0.5f)
                }
            } else {
                mediaPlayer?.apply {
                    reset()
                    isLooping = true
                    setDataSource(context, uri)
                    prepare()
                    setVolume(0.5f, 0.5f)
                }
            }
            saveBgSetting(uri)
        } catch (e: Exception) {
            LogUtils.e("Error setting background music: ${e.message}")
        }
    }

    private fun saveBgSetting(uri: Uri) {
        val spInstance = SPUtils.getInstance()
        spInstance.put(SP_BG_URI, uri.toString())
        spInstance.put(SP_BG_NAME, curMusicName.value)
    }

    fun reloadSavedList(): List<Section> {
        val spInstance = SPUtils.getInstance()
        val listData = spInstance.getString(SP_KEY, "")
        return if (listData.isEmpty()) {
            emptyList()
        } else {
            GsonUtils.fromJson(listData, GsonUtils.getListType(Section::class.java))
        }
    }

    fun play(rvList: List<Section>, start: Int = 0) {
        playList.clear()
        playList.addAll(rvList.slice(start..<rvList.size))
        LogUtils.d("playList size:" + playList.size)
        if (playList.isNotEmpty()) {
            curSectionIndex.value = 0
            curPlayingIndex.value = 0
            inDelay = false
            playing.set(true)
            pauseBtnText.value = PAUSE
            playSection(playList[0], true)
            mediaPlayer?.start()
        }
    }

    private fun playSection(current: Section, showPrepare: Boolean = false) {
        LogUtils.d("repeat:" + current.repeatNum, "length:" + current.length, "delay:" + current.delay)
        timer?.cancel()
        timer = object : CountDownTimer(current.repeatNum * current.length, current.length) {
            override fun onTick(millisUntilFinished: Long) {
                curPlayingIndex.value = curPlayingIndex.value!! + 1
                launch { speak(curPlayingIndex.value.toString()) }
            }

            override fun onFinish() {
                val finishedIndex = curSectionIndex.value!!
                curPlayingIndex.value = 0
                launch {
                    speak("好棒哦，你完成了第${finishedIndex + 1}节")

                    delayTimer?.cancel()
                    val delayMs = current.delay
                    delayTimer = object : CountDownTimer(delayMs, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                        }
                        override fun onFinish() {
                            inDelay = false
                            if (finishedIndex >= playList.size - 1) {
                                playing.set(false)
                                curSectionIndex.value = 0
                            } else {
                                curSectionIndex.value = finishedIndex + 1
                                val next = playList[curSectionIndex.value!!]
                                launch {
                                    if (delayMs <= 5000) {
                                        speak("做好准备，下一小节即将开始")
                                    }
                                    playSection(next, false)
                                }
                            }
                        }
                    }
                    delayTimer!!.start()
                    inDelay = true
                }
            }
        }
        
        prepJob?.cancel()
        prepJob = launch {
            if (showPrepare) {
                speak("做好准备，下一小节就要开始了")
                delay(5000)
            }
            timer!!.start()
        }
    }

    fun stop() {
        playing.set(false)
        inDelay = false
        timer?.cancel()
        delayTimer?.cancel()
        prepJob?.cancel()
        curPlayingIndex.value = 0
        curSectionIndex.value = 0
        mediaPlayer?.let {
            if (it.isPlaying) it.pause()
        }
    }

    fun pauseResume() {
        if (pauseBtnText.value == PAUSE) {
            timer?.cancel()
            delayTimer?.cancel()
            prepJob?.cancel()
            pauseBtnText.value = RESUME
            mediaPlayer?.pause()
        } else {
            pauseBtnText.value = PAUSE
            mediaPlayer?.start()

            val sectionIndex = curSectionIndex.value!!

            if (inDelay) {
                // Paused during inter-section delay; current section is already complete
                inDelay = false
                val nextIndex = sectionIndex + 1
                if (nextIndex < playList.size) {
                    curSectionIndex.value = nextIndex
                    playSection(playList[nextIndex], false)
                } else {
                    launch {
                        speak("好棒哦，你完成了全部小节")
                        playing.set(false)
                    }
                }
            } else {
                val playIndex = curPlayingIndex.value!!
                val curSection = playList[sectionIndex]
                val remainingNum = curSection.repeatNum - playIndex

                if (remainingNum > 0) {
                    val partialSection = Section(remainingNum, curSection.length, curSection.delay)
                    playSection(partialSection, false)
                } else {
                    curPlayingIndex.value = 0
                    val nextIndex = sectionIndex + 1
                    if (nextIndex < playList.size) {
                        curSectionIndex.value = nextIndex
                        playSection(playList[nextIndex], false)
                    } else {
                        launch {
                            speak("好棒哦，你完成了全部小节")
                            playing.set(false)
                        }
                    }
                }
            }
        }
    }

    fun destroy() {
        prepJob?.cancel()
        timer?.cancel()
        delayTimer?.cancel()
        text2Speech?.also {
            it.stop()
            it.shutdown()
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun checkVersion() {
        request({ apiService.getVersion() }, versionResult)
    }

    private suspend fun speak(s: String) = suspendCancellableCoroutine<Unit> { continuation ->
        val tts = text2Speech
        if (tts == null) {
            continuation.resume(Unit)
            return@suspendCancellableCoroutine
        }

        val utteranceId = System.nanoTime().toString()

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    continuation.resume(Unit)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == utteranceId) {
                    continuation.resume(Unit)
                }
            }

            override fun onError(id: String?, errorCode: Int) {
                if (id == utteranceId) {
                    continuation.resume(Unit)
                }
            }
        })

        val bundle = bundleOf(TextToSpeech.Engine.KEY_PARAM_VOLUME to 1f)
        val result = tts.speak(s, TextToSpeech.QUEUE_ADD, bundle, utteranceId)

        if (result != TextToSpeech.SUCCESS) {
            continuation.resume(Unit)
        }
    }

    private fun <T> launch(block: suspend () -> T): Job {
        return viewModelScope.launch {
            runCatching {
                block()
            }.onFailure { throwable ->
                throwable.message?.let {
                    LogUtils.d(it)
                }
            }
        }
    }
}
