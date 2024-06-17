package com.lttclaw.metronome.network

import me.hgj.jetpackmvvm.network.BaseResponse

data class ApiResponse<T>(var errorCode: Int, var errorMsg: String, var data: T) : BaseResponse<T>() {
    override fun getResponseCode(): Int {
        return errorCode
    }

    override fun getResponseData(): T {
        return data
    }

    override fun getResponseMsg(): String {
        return errorMsg
    }

    override fun isSucces(): Boolean {
        return errorCode == 0
    }
}