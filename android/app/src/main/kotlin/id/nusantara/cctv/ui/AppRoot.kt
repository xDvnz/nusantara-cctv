package id.nusantara.cctv.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import id.nusantara.cctv.CctvApp
import id.nusantara.cctv.ui.components.OfflineBanner
import id.nusantara.cctv.ui.detail.CameraDetailScreen
import id.nusantara.cctv.ui.detail.FullscreenPlayerScreen
import id.nusantara.cctv.ui.favorites.FavoritesScreen
import id.nusantara.cctv.ui.home.HomeScreen
import id.nusantara.cctv.ui.map.MapScreen
import id.nusantara.cctv.ui.regions.RegionsScreen
import id.nusantara.cctv.ui.search.SearchScreen
import id.nusantara.cctv.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val MAP = "map"
    const val REGIONS = "regions"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val CAMERA = "camera/{id}"
    const val PLAYER = "player/{id}"
    fun camera(id: String) = "camera/$id"
    fun player(id: String) = "player/$id"
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Beranda", Icons.Filled.Home),
    Tab(Routes.MAP, "Peta", Icons.Filled.Map),
    Tab(Routes.REGIONS, "Wilayah", Icons.AutoMirrored.Filled.List),
    Tab(Routes.SEARCH, "Cari", Icons.Filled.Search),
    Tab(Routes.FAVORITES, "Favorit", Icons.Filled.Favorite),
)

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val context = LocalContext.current
    val app = context.applicationContext as CctvApp
    val online by app.container.networkMonitor.isOnline.collectAsState()

    val showBottomBar = currentRoute in tabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding(),
            )) {
            OfflineBanner(!online && currentRoute != null)
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onCameraClick = { navController.navigate(Routes.camera(it.id)) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    )
                }
                composable(Routes.MAP) {
                    MapScreen(onCameraClick = { navController.navigate(Routes.camera(it.id)) })
                }
                composable(Routes.REGIONS) {
                    RegionsScreen(onCameraClick = { navController.navigate(Routes.camera(it.id)) })
                }
                composable(Routes.SEARCH) {
                    SearchScreen(onCameraClick = { navController.navigate(Routes.camera(it.id)) })
                }
                composable(Routes.FAVORITES) {
                    FavoritesScreen(onCameraClick = { navController.navigate(Routes.camera(it.id)) })
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen()
                }
                composable(Routes.CAMERA) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    CameraDetailScreen(
                        cameraId = id,
                        onBack = { navController.popBackStack() },
                        onFullscreen = { navController.navigate(Routes.player(id)) },
                        onOpenMap = { navController.navigate(Routes.MAP) { launchSingleTop = true } },
                    )
                }
                composable(Routes.PLAYER) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    FullscreenPlayerScreen(cameraId = id, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
