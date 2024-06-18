package com.lttclaw.metronome.viewmodel

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.widget.Toast
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.hgj.jetpackmvvm.base.viewmodel.BaseViewModel
import me.hgj.jetpackmvvm.ext.request
import me.hgj.jetpackmvvm.state.ResultState
import java.util.Locale

const val SP_KEY = "listData"
const val SP_BG_URI = "bgUri"
const val SP_BG_NAME = "bgName"
const val PAUSE = "暂停"
const val RESUME = "继续"
class PlayViewModel: BaseViewModel() {
    private val playList = mutableListOf<Section>()
    private var timer: CountDownTimer? = null
    private var delayTimer: CountDownTimer ? =null
    private var mediaPlayer: MediaPlayer? = null
    val playing = ObservableBoolean(false)
    val curPlayingIndex = MutableLiveData(0)
    val curSectionIndex = MutableLiveData(0)
    val pauseBtnText = MutableLiveData("暂停")
    val curMusicName = MutableLiveData("无")
    private var text2Speech: TextToSpeech? = null

    val versionResult = MutableLiveData<ResultState<Version>>()
    fun initSectionList(): List<Section>{
        val spInstance = SPUtils.getInstance()
        val listData = spInstance.getString(SP_KEY, "")
        return if(listData.isEmpty()){
            val list = mutableListOf<Section>()
            val s1 = Section(11, 4000L, 7000L)
            val s2 = Section(12, 5000L, 8000L)
            val s3 = Section(12, 3000L, 1000)
            list.add(s1)
            list.add(s2)
            list.add(s3)
            spInstance.put(SP_KEY, GsonUtils.toJson(list))
            list
        }else {
            GsonUtils.fromJson(listData, GsonUtils.getListType(Section::class.java))
        }
    }

    fun initBgMusic(context: Context){
        val spInstance = SPUtils.getInstance()
        val bgUriStr = spInstance.getString(SP_BG_URI)
        if(bgUriStr.isNotEmpty()){
            setBgUri(context, Uri.parse(bgUriStr))
        }
        val bgName = spInstance.getString(SP_BG_NAME)
        if (bgName.isNotEmpty()){
            curMusicName.value = bgName
        }
    }

    fun initText2Speech(context: Context) {
        text2Speech = TextToSpeech(context){status->
            if(status == TextToSpeech.SUCCESS){
                val languageCode = text2Speech?.setLanguage(Locale.CHINESE)
                if(languageCode==null || languageCode == TextToSpeech.LANG_NOT_SUPPORTED){
                    Toast.makeText(context, "不支持中文", Toast.LENGTH_SHORT).show()
                }else {
                    text2Speech!!.language = Locale.CHINA
                }
                text2Speech?.let {
                    it.setPitch(1f)
                    it.setSpeechRate(1f)
                }
            }
        }
    }

    fun setBgUri(context: Context, uri: Uri){
        if(mediaPlayer == null){
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
        }else{
            mediaPlayer?.apply {
                setDataSource(context, uri)
                prepare()
                setVolume(0.5f, 0.5f)
            }
        }
        saveBgSetting(uri)
    }

    private fun saveBgSetting(uri: Uri) {
        val spInstance = SPUtils.getInstance()
        spInstance.put(SP_BG_URI, uri.toString())
        spInstance.put(SP_BG_NAME, curMusicName.value)
    }

    fun reloadSavedList():List<Section> {
        val spInstance = SPUtils.getInstance()
        val listData = spInstance.getString(SP_KEY, "")
        return if(listData.isEmpty()){
            emptyList()
        }else{
            GsonUtils.fromJson(listData, GsonUtils.getListType(Section::class.java))
        }
    }

    fun play(rvList: List<Section>, start:Int = 0){
        playList.clear()
        playList.addAll(rvList.slice(start..<rvList.size))
        LogUtils.d("playList size:"+playList.size)
        if (playList.size > 0){
            val current = playList[0]
            curSectionIndex.value = 0
            curPlayingIndex.value = 0
            playing.set(true)
            playSection(current)
            mediaPlayer?.start()
        }
    }

    private fun playSection(current: Section) {
        LogUtils.d("repeat:"+current.repeatNum, "length:"+current.length, "delay:"+current.delay)
        timer = object : CountDownTimer(current.repeatNum * current.length, current.length) {

            override fun onTick(millisUntilFinished: Long) {
                curPlayingIndex.value = curPlayingIndex.value!! + 1
                speak(curPlayingIndex.value.toString())
            }

            override fun onFinish() {
                curPlayingIndex.value = 0
                speak("好棒哦，你完成了第${curSectionIndex.value!!+1}节")
                delayTimer = object : CountDownTimer(current.delay, current.delay / 2) {
                    override fun onTick(p0: Long) {
                    }
                    override fun onFinish() {
                        if (curSectionIndex.value!! >= playList.size) {
                            playing.set(false)
                            curPlayingIndex.value = 0
                            curSectionIndex.value = 0
                        } else {
                            curSectionIndex.value = curSectionIndex.value!! + 1
                            val next = playList[curSectionIndex.value!!]
                            playSection(next)
                        }
                    }
                }
                delayTimer!!.start()
            }
        }
        launch {
            speak("做好准备，下一小节就要开始了")
            delay(5000)
            timer!!.start()
        }
    }

    fun stop(){
        playing.set(false)
        timer?.cancel()
        delayTimer?.cancel()
        curPlayingIndex.value = 0
        curSectionIndex.value = 0
        mediaPlayer?.stop()
        mediaPlayer = null
    }

    fun pauseResume(){
        if (pauseBtnText.value == PAUSE){
            timer?.cancel()
            delayTimer?.cancel()
            pauseBtnText.value = RESUME
            mediaPlayer?.pause()
        }else{
            pauseBtnText.value = PAUSE
            mediaPlayer?.start()
            val playIndex = curPlayingIndex.value!!
            val sectionIndex = curSectionIndex.value!!
            val curSection = playList[sectionIndex]
            val remainingNum = curSection.repeatNum - playIndex
            if (remainingNum > 0){
                val partialSection = Section(remainingNum, curSection.length, curSection.delay)
                playSection(partialSection)
            }else{
                curPlayingIndex.value = 0
                curSectionIndex.value = playIndex + 1
                val nextIndex = curSectionIndex.value!!
                if(nextIndex < playList.size){
                    val next = playList[nextIndex]
                    playSection(next)
                }else{
                    speak("好棒哦，你完成了第${curSectionIndex.value!!+1}节")
                    playing.set(false)
                }
            }
        }
    }

    fun destroy(){
        text2Speech?.also {
            it.stop()
            it.shutdown()
        }
        mediaPlayer?.release()
    }

    fun checkVersion(){
        request({ apiService.getVersion()}, versionResult)
    }
    private fun speak(s:String){
        text2Speech?.also {
            val bundle = bundleOf(TextToSpeech.Engine.KEY_PARAM_VOLUME to 1f)
            it.speak(s, TextToSpeech.QUEUE_ADD, bundle, "noId")
        }
    }

    private fun <T> launch(block: suspend () -> T) {
        viewModelScope.launch {
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