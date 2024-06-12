package com.lttclaw.metronome.viewmodel

import androidx.lifecycle.MutableLiveData
import com.lttclaw.metronome.model.MusicItem
import me.hgj.jetpackmvvm.base.viewmodel.BaseViewModel

class BgMusicSelectVm : BaseViewModel() {
    val curSelectedName = MutableLiveData("")
    var curSelectedItem: MusicItem? = null
}