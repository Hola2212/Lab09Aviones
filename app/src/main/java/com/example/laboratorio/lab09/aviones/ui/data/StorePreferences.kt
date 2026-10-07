package com.example.laboratorio.lab09.aviones.ui.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.laboratorio.lab09.aviones.model.CatalogOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// En este archivo (StorePreferences) se implementó la persistencia de preferencias del usuario mediante DataStore.
// Autores: Raquel Vega
// IA utilizada: Gemini (versión del 6 de octubre de 2026)
// Fecha de modificación: 6/10/2026

private val Context.dataStore by preferencesDataStore(name = "store_preferences")

class StorePreferences(private val context: Context) {

    private object PreferencesKeys {
        val CATALOG_ORDER = stringPreferencesKey("catalog_order")
    }

    // Lectura como Flow con valor por defecto "NAME"
    val catalogOrderFlow: Flow<CatalogOrder> = context.dataStore.data.map { preferences ->
        CatalogOrder.fromStored(preferences[PreferencesKeys.CATALOG_ORDER])
    }

    // Guardar la preferencia
    suspend fun saveCatalogOrder(order: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CATALOG_ORDER] = order
        }
    }
}

