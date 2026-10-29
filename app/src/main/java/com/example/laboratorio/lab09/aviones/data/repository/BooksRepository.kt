package com.example.laboratorio.lab09.aviones.data.repository

import com.example.laboratorio.lab09.aviones.model.Book
import com.example.laboratorio.lab09.aviones.model.Profile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface BooksRepository {
    fun getBooksStream(): Flow<List<Book>>
    fun getProfiles(): List<Profile>
}

class FirestoreBooksRepository(
    private val firestore: FirebaseFirestore
) : BooksRepository {

    override fun getBooksStream(): Flow<List<Book>> = callbackFlow {
        val listener = firestore.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val books = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val name = doc.getString("name") ?: ""
                            val description = doc.getString("description") ?: ""
                            val priceCents = (doc.getLong("priceCents")
                                ?: doc.getLong("price")
                                ?: doc.getDouble("priceCents")?.toLong()
                                ?: 0L).toInt()
                            val stock = (doc.getLong("stock")
                                ?: doc.getDouble("stock")?.toLong()
                                ?: 0L).toInt()
                            val imageUrl = doc.getString("imageUrl") ?: ""
                            val profileId = doc.getString("profileId") ?: ""

                            Book(
                                id = id,
                                name = name,
                                description = description,
                                priceCents = priceCents,
                                stock = stock,
                                imageUrl = imageUrl,
                                profileId = profileId
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(books)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override fun getProfiles(): List<Profile> = authorProfiles

    companion object {
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
