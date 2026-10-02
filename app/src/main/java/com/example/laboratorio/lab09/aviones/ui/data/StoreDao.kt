package com.example.laboratorio.lab09.aviones.ui.data

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Query("SELECT * FROM favorites")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM order_lines")
    fun observeOrderLines(): Flow<List<OrderLineEntity>>

    @Query("SELECT * FROM favorites WHERE bookId = :bookId")
    fun observeFavorite(bookId: String): Flow<FavoriteEntity?>

    @Query("SELECT * FROM order_lines WHERE bookId = :bookId")
    fun observeOrderLine(bookId: String): Flow<OrderLineEntity?>

    @Upsert
    suspend fun upsertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE bookId = :bookId")
    suspend fun deleteFavorite(bookId: String)

    @Upsert
    suspend fun upsertOrderLine(line: OrderLineEntity)

    @Query("DELETE FROM order_lines WHERE bookId = :bookId")
    suspend fun deleteOrderLine(bookId: String)

    @Query("DELETE FROM order_lines")
    suspend fun clearOrder()

    @Query("DELETE FROM favorites")
    suspend fun clearFavorites()
}