package com.lttclaw.metronome.viewmodel

import androidx.lifecycle.MutableLiveData
import com.lttclaw.metronome.model.Section
import me.hgj.jetpackmvvm.base.viewmodel.BaseViewModel

class SectionViewModel : BaseViewModel() {
    val repeatNum = MutableLiveData("")
    val length = MutableLiveData("")
    val delay = MutableLiveData("")

    fun setSection(section: Section){
        repeatNum.value = section.repeatNum.toString()
        length.value = section.length.toString()
        delay.value = section.delay.toString()
    }

    fun checkEmpty(): Boolean{
        return repeatNum.value.isNullOrEmpty() || length.value.isNullOrEmpty() || delay.value.isNullOrEmpty()
    }
    fun getSection(): Section{
        return Section(repeatNum.value!!.toInt(), length.value!!.toLong(), delay.value!!.toLong())
    }
}