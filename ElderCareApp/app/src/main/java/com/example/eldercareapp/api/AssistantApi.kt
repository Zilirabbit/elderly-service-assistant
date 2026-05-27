package com.example.eldercareapp.api

import com.example.eldercareapp.model.ChatPolicyRequest
import com.example.eldercareapp.model.ChatPolicyResponse
import com.example.eldercareapp.model.MaterialChecklist
import com.example.eldercareapp.model.MaterialItem
import com.example.eldercareapp.model.SaveMaterialRequest
import com.example.eldercareapp.model.SaveMaterialResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST

interface AssistantApi {
    @GET("health")
    suspend fun health(): Map<String, String>

    @POST("api/v1/chat-policy")
    suspend fun chatPolicy(@Body request: ChatPolicyRequest): ChatPolicyResponse

    @GET("api/v1/materials/items")
    suspend fun materialItems(): List<MaterialItem>

    @GET("api/v1/materials/{itemCode}")
    suspend fun materialChecklist(@Path("itemCode") itemCode: String): MaterialChecklist

    @POST("api/v1/materials/save")
    suspend fun saveMaterialChecklist(@Body request: SaveMaterialRequest): SaveMaterialResponse
}
