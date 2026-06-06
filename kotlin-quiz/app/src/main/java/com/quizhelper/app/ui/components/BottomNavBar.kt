package com.quizhelper.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizhelper.app.ui.navigation.Screen
import com.quizhelper.app.ui.theme.*

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("首页", Screen.Home.route, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("错题", Screen.WrongQuestions.route, Icons.Filled.Book, Icons.Outlined.Book),
    BottomNavItem("历史", Screen.History.route, Icons.Filled.List, Icons.Outlined.List),
    BottomNavItem("设置", Screen.Settings.route, Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = White,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Blue600 else Gray500
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Blue600,
                    unselectedIconColor = Gray400,
                    selectedTextColor = Blue600,
                    unselectedTextColor = Gray500,
                    indicatorColor = Blue50
                )
            )
        }
    }
}
