package com.example.laboratorio.lab09.aviones.ui.state

import androidx.lifecycle.ViewModel
import com.example.laboratorio.lab09.aviones.model.validateFiscalName
import com.example.laboratorio.lab09.aviones.model.validateName
import com.example.laboratorio.lab09.aviones.model.validateNit
import com.example.laboratorio.lab09.aviones.model.validatePhoneNumber


enum class BillingType{
    CF,
    NIT
}
enum class PayMethod{
    CASH,
    TRANSFER
}

data class CheckoutUiState(
    //chechout
    val name: String = "",
    val phone: String = "",
    val billingType: BillingType = BillingType.CF,
    val nit: String = "",
    val fiscalName: String = "",
    val payMethod: PayMethod= PayMethod.CASH,
    //foco
    val nameTouched: Boolean= false,
    val phoneTouched: Boolean = false,
    val nitTouched: Boolean= false,
    val fiscalNameTouched: Boolean = false,
    //errores
    val nameError: String?=null,
    val phoneError: String?=null,
    val nitError: String?=null,
    val fiscalNameError: String?=null
){
    val isFormCorrect: Boolean get(){
        val baseValid = validateName(name) == null && validatePhoneNumber(phone) == null
        val billingValid = when(billingType){
            BillingType.CF -> true
            BillingType.NIT -> validateNit(nit) == null && validateFiscalName(fiscalName) == null
        }
        return baseValid && billingValid
    }
}