package com.lttclaw.metronome.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.blankj.utilcode.util.LogUtils
import com.drake.brv.utils.divider
import com.drake.brv.utils.models
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

    private val requestPickMusicLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ activityResult ->
        if(activityResult.resultCode == RESULT_OK){
            activityResult.data?.data?.let {
                mViewModel.setBgUri(baseContext, it)
                LogUtils.d("bg music set", it.toString())
            }
            activityResult.data?.extras?.let {
                val name = it.getString("name")
                val duration = it.getInt("duration")
                mViewModel.curMusicName.value = name
                LogUtils.d(name, duration)
            }
        }
    }
    override fun createObserver() {
        mViewModel.curSectionIndex.observe(this){ position->
            if(position >= 0){
                val adapter = mDatabind.rvPlay.adapter
                val listData = mDatabind.rvPlay.models as MutableList<Section>
                val oldPlaying = listData.find {
                    it.playing
                }
                val oldPlayingIndex = listData.indexOf(oldPlaying)
                if(oldPlayingIndex != -1 && oldPlayingIndex != position){
                    listData[oldPlayingIndex].playing = false
                    adapter!!.notifyItemChanged(oldPlayingIndex)
                }
                listData[position].playing = true
                adapter!!.notifyItemChanged(position)
            }
        }
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
            val listData = mDatabind.rvPlay.models as List<Section>
            mViewModel.play(listData)
        }
        mDatabind.btnStop.setOnClickListener {
            mViewModel.stop()
        }
        mDatabind.btnPause.setOnClickListener {
            mViewModel.pauseResume()
        }
        mDatabind.btnGoSetting.setOnClickListener {
            val intent = Intent(this, SectionListActivity::class.java)
            requestDataLauncher.launch(intent)
        }
        mDatabind.btnBgSetting.setOnClickListener {
            val intent = Intent(this, SelectMusicActivity::class.java)
            requestPickMusicLauncher.launch(intent)
        }
        mDatabind.vm = mViewModel
    }

    override fun showLoading(message: String) {
    }

    override fun onDestroy() {
        super.onDestroy()
        mViewModel.destroy()
    }
}