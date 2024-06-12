package com.lttclaw.metronome.model

import android.net.Uri
import androidx.databinding.BaseObservable

data class MusicItem(val uri: Uri,
                     val name: String,
                     val duration: Int,
                     val size: Int, var selected:Boolean=false): BaseObservable()