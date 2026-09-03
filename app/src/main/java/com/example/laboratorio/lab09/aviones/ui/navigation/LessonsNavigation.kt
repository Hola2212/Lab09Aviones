package com.example.laboratorio.lab09.aviones.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
//import com.example.laboratorio.lab09.aviones.ui.screens.
//import com.example.laboratorio.lab09.aviones.ui.screens.
import kotlinx.serialization.Serializable

@Serializable
data object LessonsRoute : NavKey

@Serializable
data object LessonDetailRoute : NavKey

@Composable
fun LessonsNavigation(
    modifier: Modifier = Modifier
) {
    val backStack = rememberNavBackStack(LessonsRoute)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<LessonsRoute> {
                LessonsScreen(
                    onOpenDetail = {
                        backStack.add(LessonDetailRoute)
                    }
                )
            }
            entry<LessonDetailRoute> {
                LessonDetailScreen(
                    onBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }
        }
    )
}