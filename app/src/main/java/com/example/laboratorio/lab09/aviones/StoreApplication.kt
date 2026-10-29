package com.example.laboratorio.lab09.aviones

import android.app.Application
import com.example.laboratorio.lab09.aviones.data.AppContainer
import com.example.laboratorio.lab09.aviones.data.DefaultAppContainer

class StoreApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        container.authRepository.signInAnonymously()
    }
}