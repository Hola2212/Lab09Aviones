package com.example.laboratorio.lab09.aviones

import android.app.Application
import com.example.laboratorio.lab09.aviones.data.AppContainer
import com.example.laboratorio.lab09.aviones.data.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StoreApplication : Application() {

    lateinit var container: AppContainer

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        applicationScope.launch {
            container.authRepository.ensureSignedIn()
        }
    }
}