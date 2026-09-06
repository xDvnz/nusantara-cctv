package id.nusantara.cctv.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import id.nusantara.cctv.BuildConfig
import id.nusantara.cctv.CctvApp
import id.nusantara.cctv.R
import id.nusantara.cctv.data.update.UpdateInfo
import id.nusantara.cctv.ui.about.AboutScreen
import id.nusantara.cctv.ui.components.OfflineBanner
import id.nusantara.cctv.ui.detail.CameraDetailScreen
import id.nusantara.cctv.ui.detail.FullscreenPlayerScreen
import id.nusantara.cctv.ui.favorites.FavoritesScreen
import id.nusantara.cctv.ui.home.HomeScreen
import id.nusantara.cctv.ui.map.MapScreen
import id.nusantara.cctv.ui.search.SearchScreen

object Routes {
    const val HOME = "home"
    const val MAP = "map"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val ABOUT = "about"
    const val CAMERA = "camera/{id}"
    const val PLAYER = "player/{id}"
    fun camera(id: String) = "camera/$id"
    fun player(id: String) = "player/$id"
}

private data class Tab(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, R.string.tab_home, Icons.Filled.Home),
    Tab(Routes.MAP, R.string.tab_map, Icons.Filled.Map),
    Tab(Routes.SEARCH, R.string.tab_search, Icons.Filled.Search),
    Tab(Routes.FAVORITES, R.string.tab_favorites, Icons.Filled.Favorite),
    Tab(Routes.ABOUT, R.string.tab_about, Icons.Filled.Info),
)

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val context = LocalContext.current
    val app = context.applicationContext as CctvApp
    val online by app.container.networkMonitor.isOnline.collectAsState()

    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        // beri jaringan waktu siap setelah launch (device/Wi-Fi lambat)
        kotlinx.coroutines.delay(4000)
        updateInfo = app.container.updateChecker.check(BuildConfig.VERSION_NAME)
    }
    updateInfo?.let { info ->
        UpdateDialog(info = info, currentVersion = BuildConfig.VERSION_NAME, onDismiss = { updateInfo = null })
    }

    val showBottomBar = currentRoute in tabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val label = stringResource(tab.labelRes)
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = label) },
                            label = { Text(label) },
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
            OfflineBanner(!online)
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    androidx.compose.animation.fadeIn(
                        androidx.compose.animation.core.tween(220),
                    ) + androidx.compose.animation.slideInVertically(
                        androidx.compose.animation.core.tween(220),
                    ) { it / 24 }
                },
                exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)) },
                popEnterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) },
                popExitTransition = {
                    androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)) +
                        androidx.compose.animation.slideOutVertically(
                            androidx.compose.animation.core.tween(220),
                        ) { it / 24 }
                },
            ) {
                composable(Routes.HOME) {
                    HomeScreen(onCameraClick = { navController.navigate(Routes.camera(it.id)) })
                }
                composable(Routes.MAP) {
                    MapScreen(onCameraClick = { navController.navigate(Routes.camera(it.id)) })
                }
                composable(Routes.SEARCH) {
                    SearchScreen(onCameraClick = { navController.navigate(Routes.camera(it.id)) })
                }
                composable(Routes.FAVORITES) {
                    FavoritesScreen(onCameraClick = { navController.navigate(Routes.camera(it.id)) })
                }
                composable(Routes.ABOUT) {
                    AboutScreen()
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

@Composable
private fun UpdateDialog(info: UpdateInfo, currentVersion: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_dialog_title)) },
        text = {
            Text(stringResource(R.string.update_dialog_message, info.version, currentVersion))
        },
        confirmButton = {
            TextButton(onClick = {
                runCatching {
                    context.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(info.url))
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
                onDismiss()
            }) { Text(stringResource(R.string.update_dialog_open)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_dialog_later)) }
        },
    )
}
