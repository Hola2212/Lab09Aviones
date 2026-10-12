package com.example.laboratorio.lab09.aviones.ui.network

import com.example.laboratorio.lab09.aviones.model.OrderDto
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
    suspend fun createOrder(@Body order: OrderDto): OrderDto

    @GET("orders")
    suspend fun getOrders(
        @Query("sortBy") sortBy: String = "date",
        @Query("order") order: String = "desc"
    ): List<OrderDto>

    @GET("orders/{id}")
    suspend fun getOrderById(@Path("id") id: String): OrderDto
}