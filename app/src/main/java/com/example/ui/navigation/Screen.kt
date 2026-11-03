package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Documents : Screen("documents")
    data object Tools : Screen("tools")
    data object Settings : Screen("settings")
    data object Camera : Screen("camera")
    data object Crop : Screen("crop/{docId}/{pageIndex}") {
        fun createRoute(docId: Long, pageIndex: Int) = "crop/$docId/$pageIndex"
    }
    data object Edit : Screen("edit/{docId}/{pageIndex}") {
        fun createRoute(docId: Long, pageIndex: Int) = "edit/$docId/$pageIndex"
    }
    data object Viewer : Screen("viewer/{docId}") {
        fun createRoute(docId: Long) = "viewer/$docId"
    }
    data object ToolAction : Screen("tool_action/{toolKey}") {
        fun createRoute(toolKey: String) = "tool_action/$toolKey"
    }
}

data class BottomNavItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

val bottomNavItems = listOf(
    BottomNavItem(
        title = "Home",
        route = Screen.Home.route,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_home"
    ),
    BottomNavItem(
        title = "Documents",
        route = Screen.Documents.route,
        selectedIcon = Icons.Filled.Description,
        unselectedIcon = Icons.Outlined.Description,
        testTag = "nav_documents"
    ),
    BottomNavItem(
        title = "Tools",
        route = Screen.Tools.route,
        selectedIcon = Icons.Filled.Build,
        unselectedIcon = Icons.Outlined.Build,
        testTag = "nav_tools"
    ),
    BottomNavItem(
        title = "Settings",
        route = Screen.Settings.route,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings"
    )
)
