package com.example.laboratorio.lab09.aviones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.laboratorio.lab09.aviones.ui.navigation.StoreNavigation
import com.example.laboratorio.lab09.aviones.ui.theme.Lab09AvionesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab09AvionesTheme {
                StoreNavigation()
            }
        }
    }
}
