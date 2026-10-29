package com.example.laboratorio.lab09.aviones.data

import android.content.Context
import com.example.laboratorio.lab09.aviones.data.repository.AuthRepository
import com.example.laboratorio.lab09.aviones.data.repository.BooksRepository
import com.example.laboratorio.lab09.aviones.data.repository.CartAndFavoritesRepository
import com.example.laboratorio.lab09.aviones.data.repository.DataStoreUserPreferencesRepository
import com.example.laboratorio.lab09.aviones.data.repository.FirebaseAuthRepository
import com.example.laboratorio.lab09.aviones.data.repository.FirestoreBooksRepository
import com.example.laboratorio.lab09.aviones.data.repository.OrdersRepository
import com.example.laboratorio.lab09.aviones.data.repository.RetrofitOrdersRepository
import com.example.laboratorio.lab09.aviones.data.repository.RoomCartAndFavoritesRepository
import com.example.laboratorio.lab09.aviones.data.repository.UserPreferencesRepository
import com.example.laboratorio.lab09.aviones.ui.data.StoreDatabase
import com.example.laboratorio.lab09.aviones.ui.data.StorePreferences
import com.example.laboratorio.lab09.aviones.ui.network.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

interface AppContainer {
    val authRepository: AuthRepository
    val booksRepository: BooksRepository
    val cartAndFavoritesRepository: CartAndFavoritesRepository
    val userPreferencesRepository: UserPreferencesRepository
    val ordersRepository: OrdersRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(FirebaseAuth.getInstance())
    }

    override val booksRepository: BooksRepository by lazy {
        FirestoreBooksRepository(FirebaseFirestore.getInstance(), authRepository)
    }

    override val cartAndFavoritesRepository: CartAndFavoritesRepository by lazy {
        RoomCartAndFavoritesRepository(StoreDatabase.get(context).dao())
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        DataStoreUserPreferencesRepository(StorePreferences(context))
    }

    override val ordersRepository: OrdersRepository by lazy {
        RetrofitOrdersRepository(RetrofitClient.apiService)
    }
}
