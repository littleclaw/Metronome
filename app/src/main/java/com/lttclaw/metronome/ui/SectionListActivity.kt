package com.lttclaw.metronome.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.drake.brv.utils.addModels
import com.drake.brv.utils.divider
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.lttclaw.metronome.R
import com.lttclaw.metronome.databinding.ActivitySectionListBinding
import com.lttclaw.metronome.databinding.DialogSectionEditBinding
import com.lttclaw.metronome.model.Section
import com.lttclaw.metronome.viewmodel.SectionListViewModel
import com.lttclaw.metronome.viewmodel.SectionViewModel
import me.hgj.jetpackmvvm.base.activity.BaseVmDbActivity

class SectionListActivity : BaseVmDbActivity<SectionListViewModel, ActivitySectionListBinding>() {
    override fun createObserver() {

    }

    override fun dismissLoading() {
    }

    override fun initView(savedInstanceState: Bundle?) {
        mDatabind.rvList.divider(R.drawable.divider_horizontal).setup {
            addType<Section>(R.layout.item_section_edit)
            R.id.btn_edit.onClick {
                val pos = layoutPosition
                val dialog = EditFragment(this.getModel()){
                    val listData = mDatabind.rvList.models as MutableList<Section>
                    listData[pos] = it
                    LogUtils.d(it.delay, it.length)
                    notifyItemChanged(pos)
                }
                dialog.show(supportFragmentManager, "edit")
            }
        }.models = mViewModel.loadListData()

        mDatabind.btnSave.setOnClickListener {
            val dataList = mDatabind.rvList.models
            mViewModel.saveListData(dataList)
            setResult(RESULT_OK)
            finish()
        }
        mDatabind.btnAdd.setOnClickListener {
            val dialog = EditFragment(null){
                mDatabind.rvList.addModels(mutableListOf(it), true)
            }
            dialog.show(supportFragmentManager, "add")
        }
    }

    override fun showLoading(message: String) {
    }

    class EditFragment(val section: Section?, private val callback:(section:Section)->Unit) : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {
                val builder = AlertDialog.Builder(it)
                val dialogBinding = DataBindingUtil.inflate<DialogSectionEditBinding>(
                    layoutInflater,
                    R.layout.dialog_section_edit,
                    null,
                    false
                )
                val sectionVm: SectionViewModel by viewModels()
                if(section == null){
                    val default = Section(49, 7000L, 10000L)
                    sectionVm.setSection(default)
                }else {
                    sectionVm.setSection(section)
                }
                dialogBinding.m = sectionVm
                builder.setTitle("填写小节参数")
                    .setPositiveButton("确定") { dialog, _ ->
                        if(sectionVm.checkEmpty()){
                            ToastUtils.showShort("请检查输入，必须输入非空数字")
                        }else{
                            val inputSection = sectionVm.getSection()
                            callback.invoke(inputSection)
                        }
                    }
                    .setView(dialogBinding.root)
                    .setNegativeButton("取消") { _, _ ->
                    }
                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }
    }
}