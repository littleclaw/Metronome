package com.lttclaw.metronome.model

data class Section(val repeatNum: Int, val length:Long, val delay: Long, val playing:Boolean = false)