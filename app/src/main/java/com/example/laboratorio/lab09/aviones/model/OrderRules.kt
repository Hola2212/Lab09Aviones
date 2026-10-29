package com.example.laboratorio.lab09.aviones.model

/**
 * Para este código se utilizó IA como guía, ya que las instrucciones no estaban tan claras y el desarrollo
 * fue más complejo de lo pensado.
 *
 * */

fun decreaseOrderQuantity(
    currentOrder: List<OrderLine>,
    bookId: String
): List<OrderLine> {
    val currentLine = currentOrder.firstOrNull { it.bookId == bookId }
        ?: return currentOrder

    if (currentLine.quantity <= 1) {
        return currentOrder.filterNot { it.bookId == bookId }
    }

    return currentOrder.map { line ->
        if (line.bookId == bookId) {
            line.copy(quantity = line.quantity - 1)
        } else {
            line
        }
    }
}

fun removeFromOrder(
    currentOrder: List<OrderLine>,
    bookId: String
): List<OrderLine> {
    return currentOrder.filterNot { it.bookId == bookId }
}

fun calculateLineSubtotalCents(
    book: Book,
    quantity: Int
): Int {
    return book.priceCents * quantity
}

fun calculateOrderTotalCents(
    books: List<Book>,
    order: List<OrderLine>
): Int {
    return order.sumOf { line ->
        val book = books.firstOrNull { it.id == line.bookId }
        if (book == null) {
            0
        } else {
            calculateLineSubtotalCents(book, line.quantity)
        }
    }
}