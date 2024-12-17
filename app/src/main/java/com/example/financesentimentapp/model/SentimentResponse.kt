package com.example.financesentimentapp.model

data class SentimentResponse(
    val sentiment: String,
    val results: Map<String, Float>
)
