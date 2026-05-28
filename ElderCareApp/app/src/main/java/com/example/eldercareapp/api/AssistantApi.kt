package com.example.eldercareapp.api

import com.example.eldercareapp.model.ChatPolicyRequest
import com.example.eldercareapp.model.ChatPolicyResponse
import com.example.eldercareapp.model.MaterialChecklist
import com.example.eldercareapp.model.MaterialItem
import com.example.eldercareapp.model.SaveMaterialRequest
import com.example.eldercareapp.model.SaveMaterialResponse
import com.example.eldercareapp.model.TtsSynthesizeRequest
import com.example.eldercareapp.model.TtsSynthesizeResponse
import com.example.eldercareapp.model.VoiceTranscribeResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.POST

interface AssistantApi {
    @GET("health")
    suspend fun health(): Map<String, String>

    @POST("api/v1/chat-policy")
    suspend fun chatPolicy(@Body request: ChatPolicyRequest): ChatPolicyResponse

    @POST("api/v1/tts/synthesize")
    suspend fun synthesizeTts(@Body request: TtsSynthesizeRequest): TtsSynthesizeResponse

    @Multipart
    @POST("api/v1/asr/transcribe")
    suspend fun transcribeVoice(
        @Part file: MultipartBody.Part,
        @Part("language") language: RequestBody,
        @Part("user_id") userId: RequestBody,
    ): VoiceTranscribeResponse

    @GET("api/v1/materials/items")
    suspend fun materialItems(): List<MaterialItem>

    @GET("api/v1/materials/{itemCode}")
    suspend fun materialChecklist(@Path("itemCode") itemCode: String): MaterialChecklist

    @POST("api/v1/materials/save")
    suspend fun saveMaterialChecklist(@Body request: SaveMaterialRequest): SaveMaterialResponse
}
