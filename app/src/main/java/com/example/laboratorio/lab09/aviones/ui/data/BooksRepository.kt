package com.example.laboratorio.lab09.aviones.ui.data

import com.example.laboratorio.lab09.aviones.model.Book
import com.example.laboratorio.lab09.aviones.model.Profile

object BooksRepository {
    val books: List<Book> = listOf(
        Book(
            id = "book1",
            name = "Cien años de soledad",
            description = "La historia de la familia Buendía a través de varias generaciones en el pueblo ficticio de Macondo.",
            price = 149.99,
            profileId = "author1"
        ),
        Book(
            id = "book2",
            name = "El principito",
            description = "Un relato sobre la amistad, el amor y la importancia de observar más allá de las apariencias.",
            price = 89.99,
            profileId = "author2"
        ),
        Book(
            id = "book3",
            name = "Orgullo y prejuicio",
            description = "Una novela sobre relaciones, primeras impresiones y diferencias sociales en la Inglaterra del siglo XIX.",
            price = 124.99,
            profileId = "author3"
        )
    )
    val profiles: List<Profile> = listOf(
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