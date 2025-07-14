package com.yourcompany.worklisten

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.yourcompany.worklisten.data.repository.WordRepository
import com.yourcompany.worklisten.data.repository.SettingsRepository
import com.yourcompany.worklisten.ui.screens.GroupScreen
import com.yourcompany.worklisten.ui.screens.LibraryScreen
import com.yourcompany.worklisten.ui.screens.PlayerScreen
import com.yourcompany.worklisten.ui.screens.ReviewScreen
import com.yourcompany.worklisten.ui.viewmodel.GroupViewModel
import com.yourcompany.worklisten.ui.viewmodel.LibraryViewModel
import com.yourcompany.worklisten.ui.viewmodel.PlayerViewModel
import com.yourcompany.worklisten.ui.viewmodel.ReviewViewModel
import com.yourcompany.worklisten.utils.FileImporter
import com.yourcompany.worklisten.utils.NavItem
import com.yourcompany.worklisten.utils.BottomNavigationBar
import com.yourcompany.worklisten.utils.TtsHelper
import com.yourcompany.worklisten.utils.openAppSettings

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WorkListenApp(
    repository: WordRepository,
    settingsRepository: SettingsRepository,
    fileImporter: FileImporter,
    ttsHelper: TtsHelper
) {
    val permissions = if (Build.VERSION.SDK_INT <= 32) {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    } else {
        // Android 13+ 使用SAF，原则上不需要存储权限，此处为空
        // 如果未来需要访问其他媒体文件，可添加 READ_MEDIA_IMAGES, READ_MEDIA_VIDEO 等
        emptyList()
    }

    val permissionState = rememberMultiplePermissionsState(permissions)

    if (permissionState.allPermissionsGranted || permissions.isEmpty()) {
        MainAppScreen(repository, settingsRepository, fileImporter, ttsHelper)
    } else {
        PermissionRequestScreen(
            onGrantPermission = { permissionState.launchMultiplePermissionRequest() }
        )
    }

    LaunchedEffect(Unit) {
        if (permissions.isNotEmpty() && !permissionState.allPermissionsGranted) {
            permissionState.launchMultiplePermissionRequest()
        }
    }
}

@Composable
private fun MainAppScreen(
    repository: WordRepository,
    settingsRepository: SettingsRepository,
    fileImporter: FileImporter,
    ttsHelper: TtsHelper
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                items = listOf(
                    NavItem.Player,
                    NavItem.Review,
                    NavItem.Library,
                    NavItem.Group
                )
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavItem.Player.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavItem.Player.route) {
                val viewModel = viewModel<PlayerViewModel>(
                    factory = ViewModelFactory(repository, settingsRepository, fileImporter, ttsHelper)
                )
                PlayerScreen(viewModel)
            }
            composable(NavItem.Review.route) {
                val viewModel = viewModel<ReviewViewModel>(
                    factory = ViewModelFactory(repository, settingsRepository, fileImporter, ttsHelper)
                )
                ReviewScreen(viewModel)
            }
            composable(NavItem.Library.route) {
                val viewModel = viewModel<LibraryViewModel>(
                    factory = ViewModelFactory(repository, settingsRepository, fileImporter, ttsHelper)
                )
                LibraryScreen(viewModel)
            }
            composable(NavItem.Group.route) {
                val viewModel = viewModel<GroupViewModel>(
                    factory = ViewModelFactory(repository, settingsRepository, fileImporter, ttsHelper)
                )
                GroupScreen(viewModel)
            }
        }
    }
}

@Composable
private fun PermissionRequestScreen(onGrantPermission: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "权限申请",
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "为了导入本地词库文件，本应用需要您授予读取设备存储空间的权限。",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onGrantPermission) {
            Text("授予权限")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { openAppSettings(context) }) {
            Text("打开应用设置")
        }
    }
}

class ViewModelFactory(
    private val repository: WordRepository,
    private val settingsRepository: SettingsRepository,
    private val fileImporter: FileImporter,
    private val ttsHelper: TtsHelper
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(PlayerViewModel::class.java) -> {
                PlayerViewModel(repository, settingsRepository, ttsHelper) as T
            }
            modelClass.isAssignableFrom(ReviewViewModel::class.java) -> {
                ReviewViewModel(repository, ttsHelper) as T
            }
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> {
                LibraryViewModel(repository, fileImporter) as T
            }
            modelClass.isAssignableFrom(GroupViewModel::class.java) -> {
                GroupViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
} 