package com.example.laboratorio.lab09.aviones.ui.network

import com.example.laboratorio.lab09.aviones.model.MyOrders
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// En este código usamos IA para estructurar y entender las funciones suspend y las anotaciones de Retrofit (@POST, @GET, @Path, @Query).
// Autor: Raquel Vega
// IA utilizada: Gemini versión del 10 de octubre de 2026

interface OrderApiService {
    @POST("orders")
    suspend fun createOrder(@Body order: MyOrders): MyOrders

    @GET("orders")
    suspend fun getOrders(
        @Query("sortBy") sortBy: String = "id",
        @Query("order") order: String = "desc"
    ): List<MyOrders>

    @GET("orders/{id}")
    suspend fun getOrderById(@Path("id") id: String): MyOrders
}