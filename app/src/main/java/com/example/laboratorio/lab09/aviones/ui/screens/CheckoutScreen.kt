package com.example.laboratorio.lab09.aviones.ui.screens
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.laboratorio.lab09.aviones.model.formatQuetzales
import com.example.laboratorio.lab09.aviones.ui.components.ScreenScaffold
import com.example.laboratorio.lab09.aviones.ui.state.BillingType
import com.example.laboratorio.lab09.aviones.ui.state.CheckoutUiState
import com.example.laboratorio.lab09.aviones.ui.state.PayMethod

@Composable
fun CheckoutScreen(
    uiState: CheckoutUiState,
    orderUnits: Int,
    totalCents: Int,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onBillingTypeChange: (BillingType) -> Unit,
    onNitChange: (String) -> Unit,
    onFiscalNameChange: (String) -> Unit,
    onPayMethodChange: (PayMethod) -> Unit,
    confirmEnabled: Boolean,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val nitFocusRequester = remember { FocusRequester() }
    val fiscalNameFocusRequester = remember { FocusRequester() }

    fun releaseFocusAndKeyboard() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    ScreenScaffold(
        title = "Checkout",
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Resumen del pedido", style = MaterialTheme.typography.titleSmall)
                Text("$orderUnits unidades", style = MaterialTheme.typography.titleSmall)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", style = MaterialTheme.typography.titleMedium)
                Text(
                    formatQuetzales(totalCents),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = { Text("Nombre completo *") },
                placeholder = { Text("Ej. Dulce Sandoval") },
                singleLine = true,
                isError = uiState.nameTouched && uiState.nameError != null,
                supportingText = { if (uiState.nameTouched) uiState.nameError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.phone,
                onValueChange = onPhoneChange,
                label = { Text("Teléfono / WhatsApp *") },
                placeholder = { Text("Ej. 55123456") },
                singleLine = true,
                isError = uiState.phoneTouched && uiState.phoneError != null,
                supportingText = { if (uiState.phoneTouched) uiState.phoneError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = if (uiState.billingType == BillingType.NIT) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onNext = { nitFocusRequester.requestFocus() },
                    onDone = { releaseFocusAndKeyboard() }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Facturación *", style = MaterialTheme.typography.titleSmall)
            Column(Modifier.selectableGroup()) {
                BillingType.entries.forEach { type ->
                    RadioRow(
                        text = if (type == BillingType.CF) "Consumidor Final (CF)" else "Factura con NIT",
                        selected = uiState.billingType == type,
                        onClick = {
                            releaseFocusAndKeyboard()
                            onBillingTypeChange(type)
                        }
                    )
                }
            }

            AnimatedVisibility(visible = uiState.billingType == BillingType.NIT) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "DATOS DE FACTURACIÓN FISCAL",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    OutlinedTextField(
                        value = uiState.nit,
                        onValueChange = onNitChange,
                        label = { Text("NIT *") },
                        placeholder = { Text("Ej. 45123") },
                        singleLine = true,
                        isError = uiState.nitTouched && uiState.nitError != null,
                        supportingText = { if (uiState.nitTouched) uiState.nitError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { fiscalNameFocusRequester.requestFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(nitFocusRequester)
                    )
                    OutlinedTextField(
                        value = uiState.fiscalName,
                        onValueChange = onFiscalNameChange,
                        label = { Text("Razón Social / Nombre fiscal *") },
                        placeholder = { Text("Ej. Guzmán Inversiones S.A.") },
                        singleLine = true,
                        isError = uiState.fiscalNameTouched && uiState.fiscalNameError != null,
                        supportingText = { if (uiState.fiscalNameTouched) uiState.fiscalNameError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { releaseFocusAndKeyboard() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(fiscalNameFocusRequester)
                    )
                }
            }

            Text("Método de Pago *", style = MaterialTheme.typography.titleSmall)
            Column(Modifier.selectableGroup()) {
                PayMethod.entries.forEach { method ->
                    RadioRow(
                        text = if (method == PayMethod.CASH) "Efectivo contra entrega" else "Transferencia bancaria",
                        selected = uiState.payMethod == method,
                        onClick = {
                            releaseFocusAndKeyboard()
                            onPayMethodChange(method)
                        }
                    )
                }
            }

            Button(
                onClick = onConfirmClick,
                enabled = confirmEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirmar pedido (Total ${formatQuetzales(totalCents)})")
            }

            if (!confirmEnabled) {
                Text(
                    text = "Completa los campos obligatorios para continuar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text)
    }
}