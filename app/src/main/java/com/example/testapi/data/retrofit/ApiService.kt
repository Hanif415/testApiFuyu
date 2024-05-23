package com.example.testapi.data.retrofit

import com.example.testapi.data.response.ApiConstants.AUTHORIZATION_HEADER
import com.example.testapi.data.response.ApiConstants.AUTHORIZATION_HEADER_RAPID
import com.example.testapi.data.response.ApiConstants.CONTENT_TYPE_HEADER
import com.example.testapi.data.response.MyData
import com.example.testapi.data.response.RapidData
import com.example.testapi.data.response.RapidResponse
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
        AUTHORIZATION_HEADER,
        CONTENT_TYPE_HEADER
    )
    @POST("predictions")
    fun postTheImage(@Body data: MyData): Call<TestResponse>

    @Headers(
        AUTHORIZATION_HEADER,
        CONTENT_TYPE_HEADER
    )
    @GET("predictions/{id}")
    fun getResult(
        @Path("id") id: String
    ): Call<ResultResponse>

    @Headers(
        AUTHORIZATION_HEADER_RAPID,
        CONTENT_TYPE_HEADER
    )
    @POST("language/translate/v2")
    fun translate(@Body data: RapidData): Call<RapidResponse>

//    @Headers(
//        "Authorization: Token r8_4v6YWRZTquzxCN7WLPcIrrU20kL4wdd1LHHtR",
//        "Content-Type: application/json"
//    )
//    @GET("predictions/{id}/cancel")
//    fun resultCancel(
//        @Path("id") id: String
//    ): Call<ResultResponse>
}