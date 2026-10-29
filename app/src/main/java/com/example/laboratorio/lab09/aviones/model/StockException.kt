package com.example.laboratorio.lab09.aviones.model

class OutOfStockException : Exception("Este producto está agotado.")

class StockUnavailableException(cause: Throwable? = null) :
    Exception("No pudimos actualizar las existencias. Revisa tu conexión e intenta de nuevo.", cause)