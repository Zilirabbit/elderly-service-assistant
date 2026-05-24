package com.example.eldercareapp.api

import com.example.eldercareapp.model.ChatPolicyRequest
import com.example.eldercareapp.model.ChatPolicyResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AssistantApi {
    @GET("health")
    suspend fun health(): Map<String, String>

    @POST("api/v1/chat-policy")
    suspend fun chatPolicy(@Body request: ChatPolicyRequest): ChatPolicyResponse
}
