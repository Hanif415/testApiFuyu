package com.example.testapi.data.retrofit

import com.example.testapi.data.response.MyData
import com.example.testapi.data.response.ResultResponse
import com.example.testapi.data.response.TestResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path


interface ApiService {

    @Headers(
        "Authorization: Token r8_e9RVRVMET9WHqdeWiPa0iea7elNnWxz1Wg721",
        "Content-Type: application/json"
    )
    @POST("predictions")
    fun postTheImage(@Body data: MyData): Call<TestResponse>

    @Headers(
        "Authorization: Token r8_e9RVRVMET9WHqdeWiPa0iea7elNnWxz1Wg721",
        "Content-Type: application/json"
    )
    @GET("predictions/{id}")
    fun getResult(
        @Path("id") id: String
    ): Call<ResultResponse>

    @Headers(
        "Authorization: Token r8_e9RVRVMET9WHqdeWiPa0iea7elNnWxz1Wg721",
        "Content-Type: application/json"
    )
    @GET("predictions/{id}/cancel")
    fun resultCancel(
        @Path("id") id: String
    ): Call<ResultResponse>
}