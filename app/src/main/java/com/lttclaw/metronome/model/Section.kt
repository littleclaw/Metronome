package com.lttclaw.metronome.model

import com.drake.brv.annotaion.ItemOrientation
import com.drake.brv.item.ItemDrag
import com.drake.brv.item.ItemSwipe

data class Section(val repeatNum: Int, val length:Long, val delay: Long, var playing:Boolean = false):ItemSwipe,ItemDrag {
    override var itemOrientationSwipe: Int
        get() = ItemOrientation.HORIZONTAL
        set(value) {}
    override var itemOrientationDrag: Int
        get() = ItemOrientation.VERTICAL
        set(value) {}
}