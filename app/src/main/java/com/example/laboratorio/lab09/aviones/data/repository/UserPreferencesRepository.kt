package com.example.laboratorio.lab09.aviones.data.repository

import com.example.laboratorio.lab09.aviones.model.CatalogOrder
import com.example.laboratorio.lab09.aviones.ui.data.StorePreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val catalogOrderStream: Flow<CatalogOrder>
    suspend fun saveCatalogOrder(order: CatalogOrder)
}

class DataStoreUserPreferencesRepository(
    private val storePreferences: StorePreferences
) : UserPreferencesRepository {

    override val catalogOrderStream: Flow<CatalogOrder> =
        storePreferences.catalogOrderFlow

    override suspend fun saveCatalogOrder(order: CatalogOrder) {
        storePreferences.saveCatalogOrder(order.name)
    }
}
