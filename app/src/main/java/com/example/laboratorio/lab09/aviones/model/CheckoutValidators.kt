package com.example.laboratorio.lab09.aviones.model

fun validateName(value: String): String?{
    val trimmed = value.trim()
    val hasDigit= trimmed.any{it.isDigit()}
    val letterCount = trimmed.count{ it.isLetter()}
    return when{
        hasDigit -> "El nombre no debe contener números"
        letterCount < 3 -> "Ingresa al menos 3 letras"
        else -> null
    }
}
fun validatePhoneNumber(value: String): String?{
    val trimmed = value.trim()
    //isDigit
    return if (trimmed.length == 8 && trimmed.all{it.isDigit()}){
        null
    } else{
        "El telefono debe contener exactamente 8 dígitos"
    }
}
fun validateNit(value: String): String?{
    val trimmed = value.trim()
    return if (trimmed.length >= 5 && trimmed.all{it.isDigit()}){
        null
    } else{
        "Ingrese al menos 5 dígitos (pueden colocarse más)"
    }
}
fun validateFiscalName(value: String): String?{
    val trimmed = value.trim()
    return if (trimmed.length >= 3){
        null
    } else{
        "Ingrese la razón social/nombre fiscal (mínimo 3 letras)"
    }
}