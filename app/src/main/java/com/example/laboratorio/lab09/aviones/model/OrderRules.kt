package com.example.laboratorio.lab09.aviones.model

/**
 * Para este código se utilizó IA como guía, ya que las instrucciones no estaban tan claras y el desarrollo
 * fue más complejo de lo pensado.
 *
 * */
sealed interface OrderUpdateResult {
    data class Success(
        val updatedOrder: List<OrderLine>
    ) : OrderUpdateResult

    data class Rejected(
        val reason: String
    ) : OrderUpdateResult
}

fun addToOrder(
    books: List<Book>,
    currentOrder: List<OrderLine>,
    bookId: String,
    increment: Int
): OrderUpdateResult {
    if (increment <= 0) {
        return OrderUpdateResult.Rejected(
            reason = "La cantidad debe ser mayor que cero."
        )
    }

    val book = books.firstOrNull { it.id == bookId }
        ?: return OrderUpdateResult.Rejected(
            reason = "El producto solicitado no existe."
        )

    val currentQuantity = currentOrder
        .firstOrNull { it.bookId == bookId }
        ?.quantity
        ?: 0

    val requestedQuantity = currentQuantity + increment

    if (requestedQuantity > book.stock) {
        return OrderUpdateResult.Rejected(
            reason = if (book.stock == 0) {
                "Este producto está agotado."
            } else {
                "Solo hay ${book.stock} unidades disponibles."
            }
        )
    }

    val updatedOrder = if (currentQuantity == 0) {
        currentOrder + OrderLine(
            bookId = bookId,
            quantity = increment
        )
    } else {
        currentOrder.map { line ->
            if (line.bookId == bookId) {
                line.copy(quantity = requestedQuantity)
            } else {
                line
            }
        }
    }

    return OrderUpdateResult.Success(updatedOrder)
}

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