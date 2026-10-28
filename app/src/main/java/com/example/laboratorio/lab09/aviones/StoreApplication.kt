package com.example.laboratorio.lab09.aviones

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

class StoreApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        signInAnonymously()
    }
    private fun signInAnonymously() {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            Log.d(
                TAG,
                "Sesión existente: UID=${currentUser.uid}, " +
                        "anónimo=${currentUser.isAnonymous}"
            )
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val user = result.user

                if (user != null) {
                    Log.d(
                        TAG,
                        "Sesión iniciada: UID=${user.uid}, " +
                                "anónimo=${user.isAnonymous}"
                    )
                } else {
                    Log.e(TAG, "La autenticación terminó sin un usuario.")
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "No se pudo iniciar la sesión anónima.", exception)
            }
    }
    private companion object {
        const val TAG = "FirebaseAuth"
    }
}