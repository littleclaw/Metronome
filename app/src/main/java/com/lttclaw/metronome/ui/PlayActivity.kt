package com.lttclaw.metronome.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.drake.brv.utils.divider
import com.drake.brv.utils.setDifferModels
import com.drake.brv.utils.setup
import com.lttclaw.metronome.R
import com.lttclaw.metronome.databinding.ActivityPlayBinding
import com.lttclaw.metronome.model.Section
import com.lttclaw.metronome.viewmodel.PlayViewModel
import me.hgj.jetpackmvvm.base.activity.BaseVmDbActivity

class PlayActivity : BaseVmDbActivity<PlayViewModel, ActivityPlayBinding>() {
    private val requestDataLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val newDataList = mViewModel.reloadSavedList()
                mDatabind.rvPlay.setDifferModels(newDataList)
            }
        }

    override fun createObserver() {
        //
    }

    override fun dismissLoading() {
    }

    override fun initView(savedInstanceState: Bundle?) {
        val rvData = mViewModel.initSectionList()
        mDatabind.rvPlay.divider(R.drawable.divider_horizontal)
            .setup {
                addType<Section>(R.layout.item_play_section)
            }.models = rvData

        mViewModel.initText2Speech(baseContext)

        mDatabind.btnStart.setOnClickListener {
            mViewModel.play(rvData)
        }
        mDatabind.btnStop.setOnClickListener {
            mViewModel.stop()
        }
        mDatabind.btnPause.setOnClickListener {
            mViewModel.pause()
        }
        mDatabind.btnGoSetting.setOnClickListener {
            val intent = Intent(this, SectionListActivity::class.java)
            requestDataLauncher.launch(intent)
        }
    }

    override fun showLoading(message: String) {
    }

    override fun onDestroy() {
        super.onDestroy()
        mViewModel.destroy()
    }
}