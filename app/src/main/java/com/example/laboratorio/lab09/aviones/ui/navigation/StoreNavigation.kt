package com.example.laboratorio.lab09.aviones.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.laboratorio.lab09.aviones.ui.screens.BookDetailScreen
import com.example.laboratorio.lab09.aviones.ui.screens.CatalogScreen
import com.example.laboratorio.lab09.aviones.ui.screens.ProfileScreen
import com.example.laboratorio.lab09.aviones.ui.viewmodel.StoreViewModel
import kotlinx.serialization.Serializable
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.example.laboratorio.lab09.aviones.ui.screens.OrderScreen
import com.example.laboratorio.lab09.aviones.ui.screens.OrderLine

@Serializable
sealed interface StoreNavKey: NavKey {

    @Serializable
    data object Order : StoreNavKey
    @Serializable
    data object Catalog : StoreNavKey
    @Serializable
    data class Detail(val bookId: String) : StoreNavKey
    @Serializable
    data class Profile(val profileId: String) : StoreNavKey
}

@Composable
fun StoreNavigation(
    modifier: Modifier = Modifier,
    viewModel: StoreViewModel = viewModel()
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(StoreNavKey.Catalog)
    val catalogGridState = rememberLazyGridState()

    BackHandler(enabled = backStack.size > 1) { backStack.removeLastOrNull() }

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
                    favoriteBookIds = uiState.favoriteBookIds,
                    searchQuery = uiState.searchQuery,
                    gridState = catalogGridState,
                    onQueryChange = viewModel::onQueryChange,
                    onBookClick = { bookId -> backStack.add(StoreNavKey.Detail(bookId)) },
                    onToggleFavorite = viewModel::changeFavorite,
                    onOpenOrder = { backStack.add(StoreNavKey.Order) }
                )
            }

            entry<StoreNavKey.Detail> { key ->
                val book = uiState.books.firstOrNull { it.id == key.bookId }
                val profile = book?.let { current ->
                    uiState.profiles.firstOrNull { it.id == current.profileId}
                }
                val currentUnits = key.bookId.let { id -> uiState.orderLines[id] } ?: 0

                BookDetailScreen(
                    book = book,
                    profile = profile,
                    isFavorite = (book != null && uiState.favoriteBookIds.contains(book.id)),
                    currentUnitsInOrder = currentUnits,
                    onToggleFavorite = { book?.let {viewModel.changeFavorite(it.id)}},
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
                val linesList = uiState.orderLines.mapNotNull { (bookId, quantity) ->
                    val book = uiState.books.firstOrNull { it.id == bookId }
                    if (book != null && quantity > 0) {
                        OrderLine(book = book, quantity = quantity)
                    } else null
                }

                val calculatedTotal = linesList.sumOf { line ->
                    (line.book.priceCents / 100.0) * line.quantity
                }

                OrderScreen(
                    orderLines = linesList,
                    totalAmount = calculatedTotal,
                    orderMessage = null,
                    onIncreaseQuantity = { book -> viewModel.addBookToOrder(book.id) },
                    onDecreaseQuantity = { book -> viewModel.decreaseOrderQuantity(book.id) }, // <-- CONECTADO
                    onRemoveLine = { book -> viewModel.removeBookFromOrder(book.id) },       // <-- CONECTADO
                    onBackClick = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}