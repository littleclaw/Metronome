package com.lttclaw.metronome.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import com.azhon.appupdate.manager.DownloadManager
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.drake.brv.utils.models
import com.drake.brv.utils.setDifferModels
import com.drake.brv.utils.setup
import com.lttclaw.metronome.R
import com.lttclaw.metronome.databinding.ActivityPlayBinding
import com.lttclaw.metronome.model.Section
import com.lttclaw.metronome.viewmodel.PlanCreationResult
import com.lttclaw.metronome.viewmodel.PlayViewModel
import com.permissionx.guolindev.PermissionX
import me.hgj.jetpackmvvm.base.activity.BaseVmDbActivity
import me.hgj.jetpackmvvm.ext.parseState
import me.hgj.jetpackmvvm.ext.view.clickNoRepeat

class PlayActivity : BaseVmDbActivity<PlayViewModel, ActivityPlayBinding>() {
    companion object {
        const val BACK_TIME_THRESHOLD = 3000L
    }

    private var lastBackTime: Long = 0L
    private var suppressPlanSelection = false
    private lateinit var planAdapter: ArrayAdapter<String>

    private val requestDataLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                refreshSelectedPlanUi()
            }
        }

    private val requestPickMusicLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode == RESULT_OK) {
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
        mViewModel.curSectionIndex.observe(this) { position ->
            if (position < 0) {
                return@observe
            }

            val adapter = mDatabind.rvPlay.adapter ?: return@observe
            val listData = mDatabind.rvPlay.models as? MutableList<Section> ?: return@observe
            if (position !in listData.indices) {
                return@observe
            }

            val oldPlayingIndex = listData.indexOfFirst { it.playing }
            if (oldPlayingIndex != -1 && oldPlayingIndex != position) {
                listData[oldPlayingIndex].playing = false
                adapter.notifyItemChanged(oldPlayingIndex)
            }

            listData[position].playing = true
            adapter.notifyItemChanged(position)
        }

        mViewModel.versionResult.observe(this) { resultState ->
            parseState(resultState, { version ->
                LogUtils.d(GsonUtils.toJson(version))
                val manager = DownloadManager.Builder(this).run {
                    apkUrl(version.apkUrl)
                    apkName("节拍器.apk")
                    smallIcon(R.mipmap.ic_launcher)
                    apkVersionCode(version.versionCode)
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

    @SuppressLint("CheckResult")
    override fun initView(savedInstanceState: Bundle?) {
        mDatabind.vm = mViewModel
        setupRecyclerView(mViewModel.initSectionList())
        setupPlanSpinner()
        refreshPlanSelector()

        mViewModel.initBgMusic(this)
        mViewModel.initText2Speech(baseContext)

        mDatabind.btnStart.setOnClickListener {
            val models = mDatabind.rvPlay.models
            if (models is List<*>) {
                @Suppress("UNCHECKED_CAST")
                mViewModel.play(models as List<Section>)
            }
        }
        mDatabind.btnStop.setOnClickListener {
            mViewModel.stop()
        }
        mDatabind.btnPause.setOnClickListener {
            mViewModel.pauseResume()
        }
        mDatabind.btnAddPlan.clickNoRepeat {
            showAddPlanDialog()
        }
        mDatabind.btnGoSetting.setOnClickListener {
            openPlanEditor(mViewModel.getSelectedPlanId())
        }
        mDatabind.btnBgSetting.setOnClickListener {
            openMusicSelector()
        }

        mViewModel.checkVersion()

        onBackPressedDispatcher.addCallback {
            val now = System.currentTimeMillis()
            if (now - lastBackTime < BACK_TIME_THRESHOLD) {
                finish()
            } else {
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

    private fun setupRecyclerView(initialData: List<Section>) {
        mDatabind.rvPlay.setup {
            addType<Section>(R.layout.item_play_section)
        }.models = initialData
    }

    private fun setupPlanSpinner() {
        planAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        planAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mDatabind.spinnerPlan.adapter = planAdapter
        mDatabind.spinnerPlan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (suppressPlanSelection || mViewModel.playing.get()) {
                    return
                }
                if (position == mViewModel.getSelectedPlanPosition()) {
                    return
                }
                val newDataList = mViewModel.selectPlan(position)
                refreshPlayList(newDataList)
                refreshPlanSelector()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun refreshSelectedPlanUi() {
        val newDataList = mViewModel.reloadSavedList()
        refreshPlayList(newDataList)
        refreshPlanSelector()
    }

    private fun refreshPlayList(sections: List<Section>) {
        mDatabind.rvPlay.setDifferModels(sections)
    }

    private fun refreshPlanSelector() {
        suppressPlanSelection = true
        planAdapter.clear()
        planAdapter.addAll(mViewModel.getPlanNames())
        planAdapter.notifyDataSetChanged()
        mDatabind.spinnerPlan.setSelection(mViewModel.getSelectedPlanPosition(), false)
        suppressPlanSelection = false
    }

    private fun showAddPlanDialog() {
        val inputView = AppCompatEditText(this).apply {
            hint = getString(R.string.plan_name_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine(true)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.add_plan_title)
            .setView(inputView)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val result = mViewModel.createPlan(inputView.text?.toString().orEmpty())
                handlePlanCreationResult(result, dialog)
            }
        }
        dialog.show()
    }

    private fun handlePlanCreationResult(result: PlanCreationResult, dialog: AlertDialog) {
        val errorMessageRes = result.errorMessageRes
        if (!result.success || errorMessageRes != null || result.planId == null) {
            ToastUtils.showShort(getString(errorMessageRes ?: R.string.plan_name_required))
            return
        }

        refreshPlayList(result.sections)
        refreshPlanSelector()
        dialog.dismiss()
        openPlanEditor(result.planId)
    }

    private fun openPlanEditor(planId: String) {
        val intent = Intent(this, SectionListActivity::class.java).apply {
            putExtra(SectionListActivity.EXTRA_PLAN_ID, planId)
        }
        requestDataLauncher.launch(intent)
    }

    private fun openMusicSelector() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
        } else {
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
}
