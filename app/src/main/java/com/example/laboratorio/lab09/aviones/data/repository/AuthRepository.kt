package com.example.laboratorio.lab09.aviones.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

interface AuthRepository {
    val currentUserId: String?
    suspend fun ensureSignedIn(): Result<Unit>
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth
) : AuthRepository {

    private val signInMutex = Mutex()

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override suspend fun ensureSignedIn(): Result<Unit> = signInMutex.withLock {
        if (auth.currentUser != null) {
            return@withLock Result.success(Unit)
        }
        try {
            auth.signInAnonymously().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}