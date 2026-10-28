package com.example.laboratorio.lab09.aviones.ui.data

import android.util.Log
import com.example.laboratorio.lab09.aviones.model.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

object CatalogSeeder {

    suspend fun seed(books: List<Book>) {
        check(FirebaseAuth.getInstance().currentUser != null) {
            "Debes iniciar sesión antes de cargar el catálogo."
        }

        require(books.size == 500) {
            "El catálogo debe contener exactamente 500 productos."
        }

        require(books.map { it.id }.toSet().size == 500) {
            "El catálogo contiene identificadores duplicados."
        }

        require(books.all { it.id.isNotBlank() && '/' !in it.id }) {
            "El catálogo contiene identificadores inválidos."
        }

        val firestore = FirebaseFirestore.getInstance()
        val products = firestore.collection("products")

        val existing = products.limit(1).get(Source.SERVER).await()

        check(existing.isEmpty) {
            "La colección products ya contiene documentos. Revisa la carga antes de continuar."
        }

        books.chunked(100).forEachIndexed { index, batchBooks ->
            val batch = firestore.batch()

            batchBooks.forEach { book ->
                val document = products.document(book.id)

                val data = mapOf(
                    "id" to book.id,
                    "name" to book.name,
                    "description" to book.description,
                    "priceCents" to book.priceCents,
                    "stock" to book.stock,
                    "imageUrl" to book.imageUrl,
                    "profileId" to book.profileId
                )

                batch.set(document, data)
            }

            batch.commit().await()

            Log.d(
                TAG,
                "Lote ${index + 1}/5 confirmado: " +
                        "${(index + 1) * 100}/500 productos."
            )
        }

        Log.d(TAG, "Los cinco lotes fueron confirmados por Firestore.")
    }

    private const val TAG = "CatalogSeeder"
}