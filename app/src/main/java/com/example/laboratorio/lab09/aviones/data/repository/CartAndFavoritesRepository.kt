package com.example.laboratorio.lab09.aviones.data.repository

import com.example.laboratorio.lab09.aviones.model.OrderLine
import com.example.laboratorio.lab09.aviones.ui.data.FavoriteEntity
import com.example.laboratorio.lab09.aviones.ui.data.OrderLineEntity
import com.example.laboratorio.lab09.aviones.ui.data.StoreDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface CartAndFavoritesRepository {
    val favoritesStream: Flow<Set<String>>
    val orderLinesStream: Flow<List<OrderLine>>
    suspend fun toggleFavorite(bookId: String, isCurrentlyFavorite: Boolean)
    suspend fun saveOrderLine(bookId: String, quantity: Int)
    suspend fun removeOrderLine(bookId: String)
    suspend fun clearOrder()
}

class RoomCartAndFavoritesRepository(
    private val dao: StoreDao
) : CartAndFavoritesRepository {

    override val favoritesStream: Flow<Set<String>> =
        dao.observeFavorites().map { list ->
            list.map { it.bookId }.toSet()
        }

    override val orderLinesStream: Flow<List<OrderLine>> =
        dao.observeOrderLines().map { list ->
            list.map { OrderLine(bookId = it.bookId, quantity = it.quantity) }
        }

    override suspend fun toggleFavorite(bookId: String, isCurrentlyFavorite: Boolean) {
        if (isCurrentlyFavorite) {
            dao.deleteFavorite(bookId)
        } else {
            dao.upsertFavorite(FavoriteEntity(bookId = bookId))
        }
    }

    override suspend fun saveOrderLine(bookId: String, quantity: Int) {
        if (quantity <= 0) {
            dao.deleteOrderLine(bookId)
        } else {
            dao.upsertOrderLine(OrderLineEntity(bookId = bookId, quantity = quantity))
        }
    }

    override suspend fun removeOrderLine(bookId: String) {
        dao.deleteOrderLine(bookId)
    }

    override suspend fun clearOrder() {
        dao.clearOrder()
    }
}
