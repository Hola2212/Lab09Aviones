package com.example.laboratorio.lab09.aviones.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.laboratorio.lab09.aviones.model.calculateOrderTotalCents
import com.example.laboratorio.lab09.aviones.ui.screens.BookDetailScreen
import com.example.laboratorio.lab09.aviones.ui.screens.CatalogScreen
import com.example.laboratorio.lab09.aviones.ui.screens.CheckoutScreen
import com.example.laboratorio.lab09.aviones.ui.screens.ConfirmationScreen
import com.example.laboratorio.lab09.aviones.ui.screens.MyOrdersScreen
import com.example.laboratorio.lab09.aviones.ui.screens.OrderDetailScreen
import com.example.laboratorio.lab09.aviones.ui.screens.OrderLineDisplay
import com.example.laboratorio.lab09.aviones.ui.screens.OrderScreen
import com.example.laboratorio.lab09.aviones.ui.screens.ProfileScreen
import com.example.laboratorio.lab09.aviones.ui.state.SubmitOrderState
import com.example.laboratorio.lab09.aviones.ui.viewmodel.OrdersViewModel
import com.example.laboratorio.lab09.aviones.ui.viewmodel.StoreViewModel
import kotlinx.serialization.Serializable


@Serializable
sealed interface StoreNavKey : NavKey {

    @Serializable
    data object Checkout : StoreNavKey
    @Serializable
    data object Confirmation : StoreNavKey
    @Serializable
    data object Order : StoreNavKey
    @Serializable
    data object Catalog : StoreNavKey
    @Serializable
    data object MyOrders : StoreNavKey
    @Serializable
    data class OrderDetail(val orderId: String) : StoreNavKey
    @Serializable
    data class Detail(val bookId: String) : StoreNavKey
    @Serializable
    data class Profile(val profileId: String) : StoreNavKey
}

@Composable
fun StoreNavigation(
    modifier: Modifier = Modifier,
    viewModel: StoreViewModel = viewModel(),
    ordersViewModel: OrdersViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastReceipt by viewModel.lastReceipt.collectAsStateWithLifecycle()
    val checkoutStateFlow = viewModel.checkoutUiState.collectAsStateWithLifecycle()
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(StoreNavKey.Catalog)
    val catalogGridState = rememberLazyGridState()

    BackHandler(enabled = backStack.size > 1) { backStack.removeLastOrNull() }

    // El ViewModel solo publica el estado; la navegación la decide la raíz.
    LaunchedEffect(submitState) {
        if (submitState is SubmitOrderState.Sent) {
            backStack.add(StoreNavKey.Confirmation)
            viewModel.onOrderSentHandled()
        }
    }
    if (!uiState.isLoaded){
        Box(modifier = modifier.fillMaxSize())
        return
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        transitionSpec = {
            // Detalle/Perfil entran deslizándose desde la derecha.
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { -it })
        },
        popTransitionSpec = {
            // Al regresar, salen hacia la derecha y el catálogo reaparece desde la izquierda.
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
        entryProvider = entryProvider {
            entry<StoreNavKey.Catalog> {
                CatalogScreen(
                    books = uiState.filteredBooks,
                    totalCount = uiState.books.size,
                    orderUnits = uiState.totalOrderUnits,
                    favoriteBookIds = uiState.favoriteBookIds,
                    searchQuery = uiState.searchQuery,
                    catalogOrder = uiState.catalogOrder,
                    onCatalogOrderChange = viewModel::setCatalogOrder,
                    gridState = catalogGridState,
                    onQueryChange = viewModel::onQueryChange,
                    onBookClick = { bookId ->
                        backStack.add(StoreNavKey.Detail(bookId))
                    },
                    onToggleFavorite = viewModel::changeFavorite,
                    onOpenOrder = {
                        backStack.add(StoreNavKey.Order)
                    },
                    onOpenMyOrders = {
                        ordersViewModel.loadOrders()
                        backStack.add(StoreNavKey.MyOrders)
                    }
                )
            }

            entry<StoreNavKey.Detail> { key ->
                LaunchedEffect(key.bookId) { viewModel.clearOrderFeedback() }

                val book = uiState.books.firstOrNull { it.id == key.bookId }
                val profile = book?.let { current ->
                    uiState.profiles.firstOrNull { it.id == current.profileId }
                }
                val currentUnits = uiState.unitsInOrder(key.bookId)

                BookDetailScreen(
                    book = book,
                    profile = profile,
                    isFavorite = (book != null && uiState.favoriteBookIds.contains(book.id)),
                    currentUnitsInOrder = currentUnits,
                    orderConfirmation = uiState.orderConfirmation,
                    orderError = uiState.orderError,
                    onToggleFavorite = { book?.let { viewModel.changeFavorite(it.id) } },
                    onOpenProfile = { profile?.let { backStack.add(StoreNavKey.Profile(it.id)) } },
                    onAddToOrder = { bookId -> viewModel.addBookToOrder(bookId) },
                    onBack = { backStack.removeLastOrNull() }
                )
            }

            entry<StoreNavKey.Profile> { key ->
                val profile = uiState.profiles.firstOrNull { it.id == key.profileId }
                ProfileScreen(
                    profile = profile,
                    onBack = { backStack.removeLastOrNull() }
                )
            }

            entry<StoreNavKey.Order> {
                LaunchedEffect(Unit) { viewModel.clearOrderFeedback() }

                val linesList = uiState.orderLines.mapNotNull { orderLine ->
                    val book = uiState.books.firstOrNull { it.id == orderLine.bookId }
                    if (book != null) {
                        OrderLineDisplay(book = book, quantity = orderLine.quantity)
                    } else null
                }

                val totalCents = calculateOrderTotalCents(uiState.books, uiState.orderLines)

                OrderScreen(
                    orderLines = linesList,
                    totalCents = totalCents,
                    orderError = uiState.orderError,
                    onIncreaseQuantity = { book -> viewModel.addBookToOrder(book.id) },
                    onDecreaseQuantity = { book -> viewModel.decreaseOrderQuantity(book.id) },
                    onRemoveLine = { book -> viewModel.removeBookFromOrder(book.id) },
                    onCheckoutClick = { backStack.add(StoreNavKey.Checkout) },
                    onBackClick = { backStack.removeLastOrNull() }
                )
            }

            // PASO 3: Checkout
            entry<StoreNavKey.Checkout> {
                val totalUnitsState = remember(uiState.orderLines) {
                    derivedStateOf { uiState.orderLines.sumOf { it.quantity } }
                }
                val totalCents = remember(uiState.orderLines, uiState.books) {
                    calculateOrderTotalCents(uiState.books, uiState.orderLines)
                }
                val confirmEnabled by remember {
                    derivedStateOf {
                        checkoutStateFlow.value.isFormCorrect && totalUnitsState.value > 0
                    }
                }

                CheckoutScreen(
                    uiState = checkoutStateFlow.value,
                    orderUnits = totalUnitsState.value,
                    totalCents = totalCents,
                    onNameChange = viewModel::onNameChange,
                    onPhoneChange = viewModel::onPhoneChange,
                    onBillingTypeChange = viewModel::onBillingTypeChange,
                    onNitChange = viewModel::onNitChange,
                    onFiscalNameChange = viewModel::onFiscalNameChange,
                    onPayMethodChange = viewModel::onPayMethodChange,
                    confirmEnabled = confirmEnabled,
                    isSubmitting = submitState is SubmitOrderState.Submitting,
                    submitError = (submitState as? SubmitOrderState.Failed)?.message,
                    onConfirmClick = viewModel::confirmOrder,
                    onBackClick = {
                        viewModel.dismissSubmitError()
                        backStack.removeLastOrNull()
                    }
                )
            }

            // PASO 4: Confirmacion y Recibo
            entry<StoreNavKey.Confirmation> {
                ConfirmationScreen(
                    receipt = lastReceipt,
                    onReturnToCatalog = {
                        while (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        }
                    }
                )
            }

            entry<StoreNavKey.MyOrders> {
                val ordersState by ordersViewModel.ordersState.collectAsStateWithLifecycle()
                MyOrdersScreen(
                    uiState = ordersState,
                    onRetry = ordersViewModel::loadOrders,
                    onOrderClick = { id ->
                        ordersViewModel.loadOrder(id)
                        backStack.add(StoreNavKey.OrderDetail(id))
                    },
                    onGoToCatalog = {
                        while (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        }
                    },
                    onBack = { backStack.removeLastOrNull() }
                )
            }

            entry<StoreNavKey.OrderDetail> { key ->
                val detailState by ordersViewModel.detailState.collectAsStateWithLifecycle()
                OrderDetailScreen(
                    uiState = detailState,
                    onRetry = { ordersViewModel.loadOrder(key.orderId) },
                    onBack = { backStack.removeLastOrNull() },
                    onBackToOrders = {
                        ordersViewModel.loadOrders()   // refresca: el pedido borrado desaparece
                        backStack.removeLastOrNull()
                    }
                )
            }

        }
    )
}