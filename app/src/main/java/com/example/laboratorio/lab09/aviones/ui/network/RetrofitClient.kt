package com.example.laboratorio.lab09.aviones.ui.network

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

// En este código usamos IA para entender la instancia única de Retrofit
// Autor: Raquel Vega
// IA utilizada: Gemini versión del 10 de octubre de 2026

object RetrofitClient {
    private const val BASE_URL = "https://6ac7d90b75a4ce3fe7224cd6.mockapi.io/" //  MockAPI URL

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val apiService: OrderApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OrderApiService::class.java)
    }
}