package com.example.laboratorio.lab09.aviones.data.repository

import com.example.laboratorio.lab09.aviones.model.Book
import com.example.laboratorio.lab09.aviones.model.OutOfStockException
import com.example.laboratorio.lab09.aviones.model.Profile
import com.example.laboratorio.lab09.aviones.model.StockUnavailableException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

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
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.documents.map { it.toBook() })
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getProfiles(): List<Profile> = authorProfiles

    // FieldValue.increment lo calcula el servidor sobre el valor guardado; las reglas
    // rechazan cualquier resultado menor que cero, así que dos reservas nunca se pisan.
    override suspend fun reserveUnit(bookId: String): Result<Unit> = changeStock(bookId, -1L)

    override suspend fun releaseUnits(bookId: String, units: Int): Result<Unit> =
        changeStock(bookId, units.toLong())

    private suspend fun changeStock(bookId: String, delta: Long): Result<Unit> {
        authRepository.ensureSignedIn().onFailure { return Result.failure(StockUnavailableException(it)) }
        return try {
            firestore.collection(PRODUCTS)
                .document(bookId)
                .update(STOCK, FieldValue.increment(delta))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED && delta < 0) {
                Result.failure(OutOfStockException())
            } else {
                Result.failure(StockUnavailableException(e))
            }
        } catch (e: Exception) {
            Result.failure(StockUnavailableException(e))
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