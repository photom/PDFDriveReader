package com.hitsuji.pdfdrivereader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hitsuji.pdfdrivereader.domain.model.AppMode
import com.hitsuji.pdfdrivereader.presentation.library.LibraryScreen
import com.hitsuji.pdfdrivereader.presentation.library.LibraryViewModel
import com.hitsuji.pdfdrivereader.presentation.reader.ReaderScreen
import com.hitsuji.pdfdrivereader.presentation.reader.ReaderViewModel
import com.hitsuji.pdfdrivereader.presentation.theme.PdfDriveReaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Use edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            PdfDriveReaderTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(mainViewModel: MainViewModel = hiltViewModel()) {
    val session by mainViewModel.session.collectAsState()
    val navController = rememberNavController()

    if (session == null) {
        // Show splash/loading until session is restored
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // Determine start destination based on session
        val startDest = remember {
            if (session?.lastMode == AppMode.READER && session?.lastUri != null) {
                "reader/${java.net.URLEncoder.encode(session?.lastUri!!, "UTF-8")}"
            } else {
                "library"
            }
        }

        NavHost(navController = navController, startDestination = startDest) {
            composable("library") {
                val viewModel: LibraryViewModel = hiltViewModel()
                LibraryScreen(
                    viewModel = viewModel,
                    onDocumentClick = { uri ->
                        navController.navigate("reader/${java.net.URLEncoder.encode(uri, "UTF-8")}")
                    }
                )
            }
            composable(
                route = "reader/{uri}",
                arguments = listOf(navArgument("uri") { type = NavType.StringType })
            ) { backStackEntry ->
                val uri = backStackEntry.arguments?.getString("uri") ?: ""
                val decodedUri = java.net.URLDecoder.decode(uri, "UTF-8")
                val viewModel: ReaderViewModel = hiltViewModel()
                
                LaunchedEffect(decodedUri) {
                    viewModel.loadDocument(decodedUri)
                }

                ReaderScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        // If we launched directly into Reader, backstack will be empty.
                        // We must navigate to library instead of finishing the activity.
                        if (navController.previousBackStackEntry == null) {
                            navController.navigate("library") {
                                popUpTo("reader/{uri}") { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    }
}
