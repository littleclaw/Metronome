package com.lttclaw.metronome.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.azhon.appupdate.manager.DownloadManager
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.drake.brv.utils.divider
import com.drake.brv.utils.models
import com.drake.brv.utils.setDifferModels
import com.drake.brv.utils.setup
import com.lttclaw.metronome.R
import com.lttclaw.metronome.databinding.ActivityPlayBinding
import com.lttclaw.metronome.model.Section
import com.lttclaw.metronome.viewmodel.PlayViewModel
import com.permissionx.guolindev.PermissionX
import me.hgj.jetpackmvvm.base.activity.BaseVmDbActivity
import me.hgj.jetpackmvvm.ext.parseState

class PlayActivity : BaseVmDbActivity<PlayViewModel, ActivityPlayBinding>() {
    companion object{
        const val BACK_TIME_THRESHOLD = 3000L
    }
    private var lastBackTime:Long = 0L
    private val requestDataLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val newDataList = mViewModel.reloadSavedList()
                mDatabind.rvPlay.setDifferModels(newDataList)
            }
        }

    private val requestPickMusicLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ activityResult ->
        if(activityResult.resultCode == RESULT_OK){
            activityResult.data?.extras?.let {
                val name = it.getString("name")
                val duration = it.getInt("duration")
                mViewModel.curMusicName.value = name
                LogUtils.d(name, duration)
            }
            activityResult.data?.data?.let {
                mViewModel.setBgUri(baseContext, it)
                LogUtils.d("bg music set", it.toString())
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
        mViewModel.versionResult.observe(this){resultState->
            parseState(resultState,{version->
                LogUtils.d(GsonUtils.toJson(version))
                val manager = DownloadManager.Builder(this).run {
                    apkUrl(version.apkUrl)
                    apkName("节拍器.apk")
                    smallIcon(R.mipmap.ic_launcher)
                    //设置了此参数，那么内部会自动判断是否需要显示更新对话框，否则需要自己判断是否需要更新
                    apkVersionCode(version.versionCode)
                    //同时下面三个参数也必须要设置
                    apkVersionName(version.versionName)
                    apkSize(version.apkSize)
                    apkDescription(version.description)
                    build()
                }
                manager.download()
            })
        }
    }

    override fun dismissLoading() {
    }

    override fun initView(savedInstanceState: Bundle?) {
        val rvData = mViewModel.initSectionList()
        mViewModel.initBgMusic(this)
        mDatabind.rvPlay.divider(R.drawable.divider_horizontal)
            .setup {
                addType<Section>(R.layout.item_play_section)
            }.models = rvData

        mViewModel.initText2Speech(baseContext)

        mDatabind.btnStart.setOnClickListener {
            val models = mDatabind.rvPlay.models
            if(models is List<*>){
                val listData = models as List<*>
                mViewModel.play(listData as List<Section>)
            }
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
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                PermissionX.init(this)
                    .permissions(Manifest.permission.READ_MEDIA_AUDIO)
                    .onExplainRequestReason { scope, deniedList ->
                        scope.showRequestReasonDialog(deniedList, "需要读取手机储存的音乐文件", "确定", "取消")
                    }
                    .onForwardToSettings { scope, deniedList ->
                        scope.showForwardToSettingsDialog(deniedList, "需要手动授权，点确认跳转设置页", "确认", "取消")
                    }
                    .request { allGranted, _, _ ->
                        if (allGranted) {
                            val intent = Intent(this, SelectMusicActivity::class.java)
                            requestPickMusicLauncher.launch(intent)
                        } else {
                            ToastUtils.showLong("未授权读储存权限，无法读取手机音乐")
                        }
                    }
            }else {
                PermissionX.init(this)
                    .permissions(Manifest.permission.READ_EXTERNAL_STORAGE)
                    .onExplainRequestReason { scope, deniedList ->
                        scope.showRequestReasonDialog(deniedList, "需要读取手机储存的音乐文件", "确定", "取消")
                    }
                    .onForwardToSettings { scope, deniedList ->
                        scope.showForwardToSettingsDialog(deniedList, "需要手动授权，点确认跳转设置页", "确认", "取消")
                    }
                    .request { _, _, _ ->
                        val intent = Intent(this, SelectMusicActivity::class.java)
                        requestPickMusicLauncher.launch(intent)
                    }
            }
        }
        mViewModel.checkVersion()
        mDatabind.vm = mViewModel

        onBackPressedDispatcher.addCallback {
            val now = System.currentTimeMillis()
            if (now - lastBackTime < BACK_TIME_THRESHOLD){
                finish()
            }else{
                lastBackTime = now
                ToastUtils.showShort("再按一次退出")
            }
            isEnabled = true
        }
    }

    override fun showLoading(message: String) {
    }

    override fun onDestroy() {
        super.onDestroy()
        mViewModel.destroy()
    }
}