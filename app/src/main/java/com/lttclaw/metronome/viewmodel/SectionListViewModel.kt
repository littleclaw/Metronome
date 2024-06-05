package com.lttclaw.metronome.viewmodel

import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.SPUtils
import com.lttclaw.metronome.model.Section
import me.hgj.jetpackmvvm.base.viewmodel.BaseViewModel

class SectionListViewModel: BaseViewModel() {
    fun loadListData():List<Section>{
        val spInstance = SPUtils.getInstance()
        val listData = spInstance.getString(SP_KEY, "")
        return if(listData.isEmpty()){
            emptyList()
        }else{
            GsonUtils.fromJson(listData, GsonUtils.getListType(Section::class.java))
        }
    }

    fun saveListData(listData: List<Any?>?){
        val spInstance = SPUtils.getInstance()
        spInstance.put(SP_KEY, GsonUtils.toJson(listData))
    }
}