package com.example.dreamjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ihad.ui.theme.IHADTheme

sealed class Screen(val route: String) {
    object List : Screen("dream_list")
    object Detail : Screen("dream_detail/{dreamId}") {
        fun createRoute(dreamId: String) = "dream_detail/$dreamId"
    }
    object Form : Screen("dream_form?dreamId={dreamId}") {
        fun createRoute(dreamId: String? = null) = if (dreamId != null) "dream_form?dreamId=$dreamId" else "dream_form"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = DreamDatabase.getDatabase(applicationContext)
        val repository = DreamRepository(database.dreamDao())
        val factory = DreamViewModelFactory(repository)

        setContent {
            IHADTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: DreamViewModel = viewModel(factory = factory)
                    DreamJournalApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun DreamJournalApp(viewModel: DreamViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Screen.List.route
    ) {
        composable(Screen.List.route) {
            DreamListScreen(
                uiState = uiState,
                onDreamClick = { dreamId ->
                    viewModel.selectDream(dreamId)
                    navController.navigate(Screen.Detail.createRoute(dreamId))
                },
                onAddClick = {
                    viewModel.setupFormForCreate()
                    navController.navigate(Screen.Form.createRoute())
                },
                onUndoDelete = {
                    viewModel.undoDelete()
                },
                onClearUserMessage = {
                    viewModel.clearUserMessage()
                }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("dreamId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val dreamId = backStackEntry.arguments?.getString("dreamId") ?: ""
            LaunchedEffect(dreamId) {
                if (dreamId.isNotEmpty()) {
                    viewModel.selectDream(dreamId)
                }
            }

            DreamDetailScreen(
                uiState = uiState,
                onBack = {
                    navController.popBackStack()
                },
                onEditClick = { id ->
                    uiState.selectedDream?.let { dream ->
                        viewModel.setupFormForEdit(dream)
                        navController.navigate(Screen.Form.createRoute(id))
                    }
                },
                onStatusChange = { dream, newStatus ->
                    viewModel.updateDreamStatus(dream, newStatus)
                },
                onDeleteConfirm = { dream ->
                    viewModel.deleteDream(dream) {
                        navController.popBackStack()
                    }
                },
                onClearUserMessage = {
                    viewModel.clearUserMessage()
                }
            )
        }

        composable(
            route = Screen.Form.route,
            arguments = listOf(
                navArgument("dreamId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val dreamId = backStackEntry.arguments?.getString("dreamId")
            LaunchedEffect(dreamId) {
                if (dreamId != null) {
                    uiState.dreams.find { it.id == dreamId }?.let { dream ->
                        viewModel.setupFormForEdit(dream)
                    }
                }
            }

            DreamFormScreen(
                uiState = uiState,
                onTitleChange = viewModel::onTitleChanged,
                onDescriptionChange = viewModel::onDescriptionChanged,
                onInterpretationChange = viewModel::onInterpretationChanged,
                onStatusChange = viewModel::onStatusChanged,
                onSave = viewModel::saveDream,
                onSaveSuccess = {
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                },
                onClearUserMessage = {
                    viewModel.clearUserMessage()
                }
            )
        }
    }
}
