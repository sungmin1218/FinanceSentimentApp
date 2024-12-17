package com.example.financesentimentapp.network

import com.example.financesentimentapp.model.SentimentResponse
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @GET("/analyze")
    fun analyzeSentiment(@Query("text") text: String): Call<SentimentResponse>

    @Multipart
    @POST("/analyze_file")
    fun analyzeFile(@Part("file") file: okhttp3.MultipartBody.Part): Call<SentimentResponse>
}
