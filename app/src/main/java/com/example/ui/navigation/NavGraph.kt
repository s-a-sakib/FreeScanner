package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.ui.components.ScannerBottomBar
import com.example.ui.screens.camera.CameraScreen
import com.example.ui.screens.crop.CropScreen
import com.example.ui.screens.edit.EditScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.tools.ToolDetailScreen
import com.example.ui.screens.tools.ToolsScreen
import com.example.ui.screens.viewer.DocumentViewerScreen
import com.example.ui.viewmodel.ScannerViewModel

@Composable
fun FreeScannerNavGraph(
    navController: NavHostController,
    viewModel: ScannerViewModel,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    val statusMessage by viewModel.statusMessage.collectAsState()

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    // Determine if bottom navigation bar should be visible
    val isBottomBarVisible = currentRoute in listOf(
        Screen.Home.route,
        Screen.Documents.route,
        Screen.Tools.route,
        Screen.Settings.route
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (isBottomBarVisible) {
                ScannerBottomBar(
                    currentRoute = currentRoute,
                    onNavigateToRoute = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Home Screen
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                    onNavigateToDocument = { docId -> navController.navigate(Screen.Viewer.createRoute(docId)) },
                    onNavigateToDocuments = { navController.navigate(Screen.Documents.route) },
                    onNavigateToTools = { navController.navigate(Screen.Tools.route) },
                    onNavigateToToolAction = { toolKey -> navController.navigate(Screen.ToolAction.createRoute(toolKey)) }
                )
            }

            // Documents / Library Screen
            composable(Screen.Documents.route) {
                LibraryScreen(
                    viewModel = viewModel,
                    onNavigateToDocument = { docId -> navController.navigate(Screen.Viewer.createRoute(docId)) }
                )
            }

            // Tools Screen
            composable(Screen.Tools.route) {
                ToolsScreen(
                    viewModel = viewModel,
                    onNavigateToToolAction = { toolKey -> navController.navigate(Screen.ToolAction.createRoute(toolKey)) }
                )
            }

            // Settings Screen
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }

            // Camera Screen
            composable(Screen.Camera.route) {
                CameraScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onFinishedScanning = { docId ->
                        navController.navigate(Screen.Viewer.createRoute(docId)) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            // Crop Screen
            composable(
                route = Screen.Crop.route,
                arguments = listOf(
                    navArgument("docId") { type = NavType.LongType },
                    navArgument("pageIndex") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
                val pageIndex = backStackEntry.arguments?.getInt("pageIndex") ?: 0
                CropScreen(
                    docId = docId,
                    pageIndex = pageIndex,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onDoneCrop = { dId, pIdx ->
                        navController.navigate(Screen.Edit.createRoute(dId, pIdx)) {
                            popUpTo(Screen.Viewer.createRoute(dId))
                        }
                    }
                )
            }

            // Edit Screen
            composable(
                route = Screen.Edit.route,
                arguments = listOf(
                    navArgument("docId") { type = NavType.LongType },
                    navArgument("pageIndex") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
                val pageIndex = backStackEntry.arguments?.getInt("pageIndex") ?: 0
                EditScreen(
                    docId = docId,
                    pageIndex = pageIndex,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onAddAnotherPage = {
                        navController.navigate(Screen.Camera.route)
                    },
                    onSaveDone = { dId ->
                        navController.navigate(Screen.Viewer.createRoute(dId)) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            // Document Viewer Screen
            composable(
                route = Screen.Viewer.route,
                arguments = listOf(
                    navArgument("docId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
                DocumentViewerScreen(
                    docId = docId,
                    viewModel = viewModel,
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Home.route)
                        }
                    },
                    onNavigateToCrop = { dId, pageIdx ->
                        navController.navigate(Screen.Crop.createRoute(dId, pageIdx))
                    },
                    onNavigateToEdit = { dId, pageIdx ->
                        navController.navigate(Screen.Edit.createRoute(dId, pageIdx))
                    },
                    onAddPage = {
                        navController.navigate(Screen.Camera.route)
                    }
                )
            }

            // Tool Action Detail Screen
            composable(
                route = Screen.ToolAction.route,
                arguments = listOf(
                    navArgument("toolKey") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val toolKey = backStackEntry.arguments?.getString("toolKey") ?: "merge"
                ToolDetailScreen(
                    toolKey = toolKey,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDocument = { docId ->
                        navController.navigate(Screen.Viewer.createRoute(docId))
                    }
                )
            }
        }
    }
}
