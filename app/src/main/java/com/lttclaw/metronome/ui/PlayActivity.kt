package com.lttclaw.metronome.ui

import android.os.Bundle
import com.drake.brv.utils.setup
import com.lttclaw.metronome.R
import com.lttclaw.metronome.databinding.ActivityPlayBinding
import com.lttclaw.metronome.model.Section
import com.lttclaw.metronome.viewmodel.PlayViewModel
import me.hgj.jetpackmvvm.base.activity.BaseVmDbActivity

class PlayActivity: BaseVmDbActivity<PlayViewModel, ActivityPlayBinding>() {
    override fun createObserver() {
        //
    }

    override fun dismissLoading() {
    }

    override fun initView(savedInstanceState: Bundle?) {
        val rvData = mViewModel.initSectionList()
        mDatabind.rvPlay.setup {
            addType<Section>(R.layout.item_play_section)
        }.models = rvData

        mViewModel.initText2Speech(baseContext)

        mDatabind.btnStart.setOnClickListener {
            mViewModel.play(rvData)
        }
        mDatabind.btnStop.setOnClickListener {
            mViewModel.stop()
        }
    }

    override fun showLoading(message: String) {
    }
}