package com.balungpisah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable 
import androidx.compose.foundation.interaction.MutableInteractionSource 
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color 
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.balungpisah.data.repository.AuthRepository
import com.balungpisah.data.repository.ChatRepository
import com.balungpisah.ui.screens.*
import com.balungpisah.ui.theme.BalungPisahTheme
import com.balungpisah.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var authRepository: AuthRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        authRepository = AuthRepository.getInstance(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            var themeSelection by remember { mutableStateOf("System") }

            val useDarkTheme = when (themeSelection) {
                "Light" -> false
                "Dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            BalungPisahTheme(darkTheme = useDarkTheme) {
                AppNavigation(
                    authRepository = authRepository,
                    activity = this,
                    currentTheme = themeSelection,
                    onThemeChange = { themeSelection = it }
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    authRepository: AuthRepository,
    activity: ComponentActivity,
    currentTheme: String,
    onThemeChange: (String) -> Unit
) {
    var isAuthenticated by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        isAuthenticated = authRepository.isAuthenticated()
        isLoading = false
    }
    
    if (isLoading) {
        SplashScreen()
    } else if (isAuthenticated) {
        MainScreen(
            authRepository = authRepository,
            activity = activity,
            currentTheme = currentTheme,
            onThemeChange = onThemeChange,
            onSignOut = {
                isAuthenticated = false
            }
        )
    } else {
        BalungPisahTheme {
            val navController = rememberNavController()
            var showSplash by remember { mutableStateOf(true) }

            if (showSplash) {
                SplashScreen()
                LaunchedEffect(Unit) {
                    delay(2000)
                    showSplash = false
                }
            } else {
                AuthScreen(
                    authRepository = authRepository,
                    onLoginSuccess = {
                        isAuthenticated = true
                    }
                )
            }
        }
    }
}

class ChatViewModelFactory(
    private val repository: ChatRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    authRepository: AuthRepository,
    activity: ComponentActivity,
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val chatRepository = ChatRepository.getInstance(activity)
    val scope = rememberCoroutineScope()

    var hideBottomBar by remember { mutableStateOf(false) }

    // Create ChatViewModel here so it's shared
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(chatRepository)
    )
    
    var showHistory by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isKeyboardOpen = rememberKeyboardOpen()
    
    val items = listOf(
        BottomNavItem("chat", "Chat", R.drawable.icon_chat),
        BottomNavItem("dashboard", "Dashboard", R.drawable.dashboard),
        BottomNavItem("reports", "Laporan", R.drawable.report),
        BottomNavItem("settings", "Settings", R.drawable.settings)
    )
    
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (!isKeyboardOpen && !showHistory && !showSettings) {
                NavigationBar(
                    tonalElevation = 8.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            )) {
            NavHost(navController, startDestination = "chat") {
                composable("chat") {
                    ChatScreen(
                        viewModel = chatViewModel,
                        onNavigateToHistory = { showHistory = true },
                        onNavigateToSettings = { showSettings = true },
                        onInputFocusChange = { focused ->
                            hideBottomBar = focused
                        }
                    )
                }
                composable("dashboard") {
                    DashboardScreen(context)
                }
                composable("reports") {
                    ReportsScreen()
                }
                composable("settings") {
                    SettingsScreen(
                        currentTheme = currentTheme,
                        onThemeChange = onThemeChange,
                        onNavigateBack = { navController.popBackStack() },
                        onSignOut = {
                            scope.launch {
                                authRepository.logout()
                                onSignOut()
                            }
                        }
                    )
                }
            }
            
            // History drawer (slide from left)
            AnimatedHistoryDrawer(
                visible = showHistory,
                onDismiss = { showHistory = false },
                chatRepository = chatRepository,
                chatViewModel = chatViewModel
            )
            
            // Settings drawer (slide from right)
            AnimatedSettingsDrawer(
                visible = showSettings,
                onDismiss = { showSettings = false },
                currentTheme = currentTheme,
                onThemeChange = onThemeChange,
                onSignOut = {
                    scope.launch {
                        authRepository.logout()
                        onSignOut()
                    }
                }
            )
        }
    }
}

@Composable
fun AnimatedHistoryDrawer(
    visible: Boolean,
    onDismiss: () -> Unit,
    chatRepository: ChatRepository,
    chatViewModel: ChatViewModel
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = androidx.compose.animation.core.tween(300)
        ),
        exit = androidx.compose.animation.fadeOut(
            animationSpec = androidx.compose.animation.core.tween(300)
        )
    ) {
        // Scrim/backdrop
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            color = Color.Black.copy(alpha = 0.5f)
        ) {}
    }
    
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            )
        ),
        exit = androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = androidx.compose.animation.core.tween(300)
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 56.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 16.dp,
            shadowElevation = 16.dp
        ) {
            HistoryScreen(
                chatRepository = chatRepository,
                onNavigateBack = onDismiss,
                onThreadSelected = { threadId ->
                    chatViewModel.loadThread(threadId)
                    onDismiss()
                },
                onNewChat = {
                    chatViewModel.startNewChat()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun AnimatedSettingsDrawer(
    visible: Boolean,
    onDismiss: () -> Unit,
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    onSignOut: () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = androidx.compose.animation.core.tween(300)
        ),
        exit = androidx.compose.animation.fadeOut(
            animationSpec = androidx.compose.animation.core.tween(300)
        )
    ) {
        // Scrim/backdrop
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            color = Color.Black.copy(alpha = 0.5f)
        ) {}
    }
    
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            )
        ),
        exit = androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(300)
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 56.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 16.dp,
            shadowElevation = 16.dp
        ) {
            SettingsScreen(
                currentTheme = currentTheme,
                onThemeChange = onThemeChange,
                onNavigateBack = onDismiss,
                onSignOut = onSignOut
            )
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val iconRes: Int
)


@Composable
fun rememberKeyboardOpen(): Boolean {
    val density = LocalDensity.current
    return WindowInsets.ime.getBottom(density) > 0
}