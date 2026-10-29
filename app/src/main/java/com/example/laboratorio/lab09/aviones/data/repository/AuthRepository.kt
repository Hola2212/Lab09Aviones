package com.example.laboratorio.lab09.aviones.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth

interface AuthRepository {
    val currentUserId: String?
    fun signInAnonymously(onComplete: ((Boolean) -> Unit)? = null)
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override fun signInAnonymously(onComplete: ((Boolean) -> Unit)?) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(
                TAG,
                "Sesión existente: UID=${currentUser.uid}, anónimo=${currentUser.isAnonymous}"
            )
            onComplete?.invoke(true)
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    Log.d(
                        TAG,
                        "Sesión iniciada: UID=${user.uid}, anónimo=${user.isAnonymous}"
                    )
                    onComplete?.invoke(true)
                } else {
                    Log.e(TAG, "La autenticación terminó sin un usuario.")
                    onComplete?.invoke(false)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "No se pudo iniciar la sesión anónima.", exception)
                onComplete?.invoke(false)
            }
    }

    private companion object {
        const val TAG = "FirebaseAuthRepository"
    }
}
