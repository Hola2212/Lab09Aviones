package com.example.laboratorio.lab09.aviones.data.repository

import com.example.laboratorio.lab09.aviones.model.Book
import com.example.laboratorio.lab09.aviones.model.OutOfStockException
import com.example.laboratorio.lab09.aviones.model.Profile
import com.example.laboratorio.lab09.aviones.model.StockUnavailableException
import com.google.firebase.firestore.DocumentSnapshot
import android.util.Log
import com.example.laboratorio.lab09.aviones.model.StockPermissionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException
import com.google.firebase.firestore.MetadataChanges

interface BooksRepository {
    fun getBooksStream(): Flow<List<Book>>
    fun getProfiles(): List<Profile>
    suspend fun reserveUnit(bookId: String): Result<Unit>
    suspend fun releaseUnits(bookId: String, units: Int): Result<Unit>
}

class FirestoreBooksRepository(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : BooksRepository {

    override fun getBooksStream(): Flow<List<Book>> = flow {
        authRepository.ensureSignedIn().getOrThrow()
        emitAll(observeProducts())
    }

    private fun observeProducts(): Flow<List<Book>> = callbackFlow {
        val listener = firestore.collection(PRODUCTS)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (snapshot.isEmpty && snapshot.metadata.isFromCache) {
                        return@addSnapshotListener
                    }
                    try {
                        val books = snapshot.documents.map { it.toBook() }
                        trySend(books)
                    } catch (exception: Exception) {
                        close(exception)
                    }
                }
            }
        awaitClose {
            listener.remove()
        }
    }

    override fun getProfiles(): List<Profile> = authorProfiles

    override suspend fun reserveUnit(bookId: String): Result<Unit> = changeStock(bookId, -1L)

    override suspend fun releaseUnits(
        bookId: String,
        units: Int
    ): Result<Unit> {
        if (units <= 0) {
            return Result.failure(
                IllegalArgumentException(
                    "La cantidad a devolver debe ser mayor que cero."
                )
            )
        }

        return changeStock(bookId, units.toLong())
    }

    private suspend fun changeStock(
        bookId: String,
        delta: Long
    ): Result<Unit> {
        authRepository.ensureSignedIn().onFailure {
            return Result.failure(StockUnavailableException(it))
        }
        return try {
            val document = firestore.collection(PRODUCTS).document(bookId)
            firestore.runTransaction<Unit> { transaction ->
                val snapshot = transaction.get(document)

                check(snapshot.exists()) {
                    "El producto solicitado no existe."
                }
                val currentStock = snapshot.getLong(STOCK)
                    ?: error("El producto no tiene existencias válidas.")
                check(currentStock in 0L..Int.MAX_VALUE.toLong()) {
                    "Las existencias están fuera del rango permitido."
                }
                if (delta < 0 && currentStock < -delta) {
                    throw OutOfStockException()
                }
                val updatedStock = currentStock + delta
                check(updatedStock in 0L..Int.MAX_VALUE.toLong()) {
                    "Las existencias resultantes no son válidas."
                }
                transaction.update(document, STOCK, updatedStock)
                Unit
            }.await()

            Result.success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            val causes = generateSequence<Throwable>(exception) {
                it.cause
            }.toList()
            val outOfStock = causes
                .filterIsInstance<OutOfStockException>()
                .firstOrNull()
            val firestoreError = causes
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            val failure = when {
                outOfStock != null -> outOfStock
                firestoreError?.code ==
                        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    StockPermissionException(exception)

                else -> StockUnavailableException(exception)
            }
            Log.e(
                "FirestoreBooksRepository",
                "Falló la actualización: bookId=$bookId, delta=$delta",
                exception
            )
            Result.failure(failure)
        }
    }

    private fun DocumentSnapshot.toBook() = Book(
        id = id,
        name = getString("name").orEmpty(),
        description = getString("description").orEmpty(),
        priceCents = (getLong("priceCents") ?: getLong("price") ?: 0L).toInt(),
        stock = (getLong(STOCK) ?: 0L).toInt(),
        imageUrl = getString("imageUrl").orEmpty(),
        profileId = getString("profileId").orEmpty()
    )

    companion object {
        private const val PRODUCTS = "products"
        private const val STOCK = "stock"

        val authorProfiles: List<Profile> = listOf(
            Profile(
                id = "author1",
                name = "Gabriel García Márquez",
                role = "Autor",
                location = "Colombia",
                description = "Escritor colombiano y ganador del Premio Nobel de Literatura, reconocido como una figura destacada del realismo mágico."
            ),
            Profile(
                id = "author2",
                name = "Antoine de Saint-Exupéry",
                role = "Autor y aviador",
                location = "Francia",
                description = "Escritor y aviador francés conocido mundialmente por sus obras inspiradas en la amistad, la humanidad y la aviación."
            ),
            Profile(
                id = "author3",
                name = "Jane Austen",
                role = "Autora",
                location = "Inglaterra",
                description = "Escritora inglesa reconocida por sus novelas sobre las relaciones humanas y la sociedad de su época."
            )
        )
    }
}