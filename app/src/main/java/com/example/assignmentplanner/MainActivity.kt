package com.example.assignmentplanner

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.assignmentplanner.ui.theme.AppTheme
import com.example.assignmentplanner.ui.theme.StudySquaredTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity()
{
    var createdCount = 0

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        createdCount = createdCount + 1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        {
            val windowInsetsController = window.decorView.windowInsetsController
            if (windowInsetsController != null)
            {
                windowInsetsController.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                windowInsetsController.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val savedThemeName by produceState(initialValue = AppTheme.Light.name) {
                context.dataStore.data.map { it[KEY_THEME] ?: AppTheme.Light.name }.collect { value = it }
            }
            val currentTheme = AppTheme.valueOf(savedThemeName)
            StudySquaredTheme(appTheme = currentTheme) {
                AppRoot(currentTheme = currentTheme, onThemeChange = { newTheme ->
                    scope.launch { context.dataStore.edit { it[KEY_THEME] = newTheme.name } }
                })
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppRoot(authVm: AuthViewModel = viewModel(), currentTheme: AppTheme, onThemeChange: (AppTheme) -> Unit)
{
    val authState by authVm.authState.collectAsState()
    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        authVm.handleSignInResult(it.data)
    }
    if (authState is UserAuthState.Loading || authState is UserAuthState.SignedOut || authState is UserAuthState.Error)
    {
        SignInScreen(authState = authState, onSignIn = { signInLauncher.launch(authVm.buildSignInIntent()) }, onDismissError = { authVm.clearError() })
    }
    else if (authState is UserAuthState.SignedIn)
    {
        MainContainer(user = (authState as UserAuthState.SignedIn).user, onSignOut = { authVm.signOut() }, currentTheme = currentTheme, onThemeChange = onThemeChange)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(
    vm: AssignmentViewModel = viewModel(),
    classroomVm: ClassroomViewModel = viewModel(),
    notesVm: NotesViewModel = viewModel(),
    user: UserInfo,
    onSignOut: () -> Unit,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit
)
{
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = config.screenWidthDp >= 600
    var currentScreen by remember { mutableStateOf("home") }

    ModalNavigationDrawer(drawerState = drawerState, gesturesEnabled = true,
        drawerContent = {
            val drawerWidth = if (isTablet) 0.4f else 0.75f
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(drawerWidth)) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Menu", modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                NavigationDrawerItem(label = { Text("Dashboard") }, selected = currentScreen == "home",
                    onClick = { scope.launch { currentScreen = "home"; drawerState.close() } },
                    icon = { Icon(Icons.Default.Dashboard, null) }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
                NavigationDrawerItem(
                    label = { Text("Project Planner") },
                    selected = currentScreen == "todo" || currentScreen == "scan",
                    onClick = { scope.launch { currentScreen = "todo"; drawerState.close() } },
                    icon = { Icon(Icons.Default.Assignment, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(label = { Text("Notes Library") }, selected = currentScreen == "notes" || currentScreen == "study",
                    onClick = { scope.launch { currentScreen = "notes"; drawerState.close() } },
                    icon = { Icon(Icons.Default.LibraryBooks, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
                NavigationDrawerItem(
                    label = { Text("Grades") },
                    selected = currentScreen == "grades",
                    onClick = { scope.launch { currentScreen = "grades"; drawerState.close() } },
                    icon = { Icon(Icons.Default.Grade, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(label = { Text("Calendar") }, selected = currentScreen == "calendar", onClick = { scope.launch { currentScreen = "calendar"; drawerState.close() } },
                    icon = { Icon(Icons.Default.CalendarMonth, null) }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                NavigationDrawerItem(label = { Text("Settings") }, selected = currentScreen == "settings", onClick = { scope.launch { currentScreen = "settings"; drawerState.close() } }, icon = { Icon(Icons.Default.Settings, null) }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
                NavigationDrawerItem(label = { Text("Account") }, selected = currentScreen == "user", onClick = { scope.launch { currentScreen = "user"; drawerState.close() } }, icon = { Icon(Icons.Default.Person, null) }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        var title = "App"
                        if (currentScreen == "home") title = "Dashboard"
                        else if (currentScreen == "todo") title = "Project Planner"
                        else if (currentScreen == "scan") title = "Project Planner"
                        else if (currentScreen == "calendar") title = "Calendar"
                        else if (currentScreen == "notes") title = "Notes Library"
                        else if (currentScreen == "study") title = "Study Guide"
                        else if (currentScreen == "grades") title = "Grades"
                        else if (currentScreen == "settings") title = "Settings"
                        else if (currentScreen == "user") title = "Account"
                        Text(title)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, "Menu") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White, navigationIconContentColor = Color.White)
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                if (currentScreen == "home") HomeScreen(user, vm, classroomVm, onNavigate = { currentScreen = it }, onNavigateToTodo = { currentScreen = "todo" }, onNavigateToGrades = { currentScreen = "grades" })
                else if (currentScreen == "todo") TodoListScreen(vm, classroomVm, user, onNavigateToScan = { currentScreen = "scan" })
                else if (currentScreen == "scan") AssignmentScreen(vm, onNavigateBack = { currentScreen = "todo" })
                else if (currentScreen == "calendar") CalendarScreen(vm, classroomVm, user)
                else if (currentScreen == "notes") NotesScreen(notesVm, classroomVm, user, onNavigateToStudyGuide = { currentScreen = "study" })
                else if (currentScreen == "study") StudyScheduleScreen(onNavigateBack = { currentScreen = "notes" })
                else if (currentScreen == "grades") GradesScreen(classroomVm)
                else if (currentScreen == "settings") SettingsScreen(currentTheme, onThemeChange)
                else if (currentScreen == "user") UserScreen(user, onSignOut)

                if (drawerState.isOpen)
                {
                    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { scope.launch { drawerState.close() } } })
                }
            }
        }
    }
}
