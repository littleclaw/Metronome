package com.lttclaw.metronome.network.service

import com.lttclaw.metronome.model.Version
import com.lttclaw.metronome.network.ApiResponse
import retrofit2.http.GET

interface VersionService {
    companion object{
        const val SERVER_URL = "http://www.smallfurrypaw.top/"
    }
    @GET("files/version.json")
    suspend fun getVersion(): ApiResponse<Version>
}