package com.lttclaw.metronome.viewmodel

import android.content.Context
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
import com.blankj.utilcode.util.ToastUtils
import com.lttclaw.metronome.model.Section
import kotlinx.coroutines.launch
import me.hgj.jetpackmvvm.base.viewmodel.BaseViewModel
import java.util.Locale

const val SP_KEY = "listData"
class PlayViewModel: BaseViewModel() {
    private val playList = mutableListOf<Section>()
    private var timer: CountDownTimer? = null
    private var delayTimer: CountDownTimer ? =null
    val playing = ObservableBoolean(false)
    val curPlayingIndex = MutableLiveData(0)
    val curSectionIndex = MutableLiveData(0)
    private var text2Speech: TextToSpeech? = null
    fun initSectionList(): List<Section>{
        val spInstance = SPUtils.getInstance()
        val listData = spInstance.getString(SP_KEY, "")
        return if(listData.isEmpty()){
            val list = mutableListOf<Section>()
            val s1 = Section(11, 4000L, 7000L)
            val s2 = Section(12, 5000L, 10000L)
            val s3 = Section(12, 3000L, 0)
            list.add(s1)
            list.add(s2)
            list.add(s3)
            spInstance.put(SP_KEY, GsonUtils.toJson(list))
            list
        }else {
            GsonUtils.fromJson(listData, GsonUtils.getListType(Section::class.java))
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
        playList.addAll(rvList.slice(start..rvList.size))
        if (playList.size > 0){
            val current = playList[0]
            curSectionIndex.value = 0
            curPlayingIndex.value = 1
            playing.set(true)
            playSection(current)
        }
    }

    private fun playSection(current: Section) {
        timer = object : CountDownTimer(current.repeatNum * current.length, current.length) {
            override fun onTick(millisUntilFinished: Long) {
                speak(curPlayingIndex.value.toString())
                curPlayingIndex.value = curPlayingIndex.value!! + 1
            }

            override fun onFinish() {
                curPlayingIndex.value = 0
                speak("好棒棒，你完成第${curSectionIndex.value!!+1}节")
                delayTimer = object : CountDownTimer(current.delay, current.delay / 2) {
                    override fun onTick(p0: Long) {
                    }
                    override fun onFinish() {
                        if (curSectionIndex.value == playList.size) {
                            playing.set(false)
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
        timer!!.start()
    }

    fun stop(){
        playing.set(false)
        timer?.cancel()
        delayTimer?.cancel()
        curPlayingIndex.value = 1
        curSectionIndex.value = 0
    }

    fun pause(){
        ToastUtils.showShort("暂未实现")
    }

    fun destroy(){
        text2Speech?.also {
            it.stop()
            it.shutdown()
        }
    }
    private fun speak(s:String){
        text2Speech?.also {
            val bundle = bundleOf(TextToSpeech.Engine.KEY_PARAM_VOLUME to 1f)
            it.speak(s, TextToSpeech.QUEUE_ADD, bundle, "noId")
        }
    }

    fun <T> launch(block: suspend () -> T) {
        viewModelScope.launch {
            runCatching {
                block()
            }.onFailure {
                it.message?.let {
                    LogUtils.d(it)
                }
            }
        }
    }
}