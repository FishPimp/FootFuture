package se.storleksprognosen.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import se.storleksprognosen.app.R
import se.storleksprognosen.app.ui.foot.CalibrationScreen
import se.storleksprognosen.app.ui.foot.FootScreen
import se.storleksprognosen.app.ui.foot.MeasureScreen
import se.storleksprognosen.app.ui.forecast.ForecastScreen
import se.storleksprognosen.app.ui.inventory.InventoryScreen

private enum class TopLevel(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    FORECAST("forecast", R.string.nav_forecast, Icons.Filled.Insights),
    INVENTORY("inventory", R.string.nav_inventory, Icons.Filled.Inventory2),
    FOOT("foot", R.string.nav_foot, Icons.Filled.Straighten),
}

/** Helskärmsvyer utan bottennavigering. */
private const val CALIBRATE_ROUTE = "foot/calibrate"
private const val MEASURE_ROUTE = "foot/measure"

@Composable
fun StorleksprognosenNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == null || TopLevel.entries.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevel.entries.forEach { destination ->
                        val selected = currentRoute == destination.route
                        val iconScale by animateFloatAsState(
                            targetValue = if (selected) 1.15f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "navIcon",
                        )
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null, modifier = Modifier.scale(iconScale)) },
                            label = { Text(stringResource(destination.label)) },
                        )
                    }
                }
            }
        },
    ) { outerPadding ->
        // Skärmarna har egna toppfält; här tar vi bara hänsyn till bottennavigeringen.
        val bottomPadding = if (showBottomBar) outerPadding.calculateBottomPadding() else 0.dp
        NavHost(
            navController = navController,
            startDestination = TopLevel.FORECAST.route,
            modifier = Modifier
                .padding(bottom = bottomPadding)
                .consumeWindowInsets(PaddingValues(bottom = bottomPadding)),
        ) {
            composable(TopLevel.FORECAST.route) {
                ForecastScreen()
            }
            composable(TopLevel.INVENTORY.route) {
                InventoryScreen()
            }
            composable(TopLevel.FOOT.route) {
                FootScreen(
                    onCalibrate = { navController.navigate(CALIBRATE_ROUTE) },
                    onMeasure = { navController.navigate(MEASURE_ROUTE) },
                )
            }
            composable(CALIBRATE_ROUTE) {
                CalibrationScreen(onBack = { navController.popBackStack() })
            }
            composable(MEASURE_ROUTE) {
                MeasureScreen(onClose = { navController.popBackStack() })
            }
        }
    }
}
