package com.lttclaw.metronome

import android.app.Application
import com.drake.brv.utils.BRV

class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        BRV.modelId = BR.m
    }
}