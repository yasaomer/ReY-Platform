package com.example.privateoml

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.privateoml.data.DatabaseHelper
import com.example.privateoml.ui.editor.LastMessageEditor
import com.example.privateoml.ui.lock.LockScreen
import com.example.privateoml.ui.main.MainScreen
import com.example.privateoml.ui.settings.SettingsScreen
import com.example.privateoml.ui.setup.SetupWizardScreen
import com.example.privateoml.ui.splash.SplashScreen

@Composable
fun MainNavigation() {
  val context = LocalContext.current
  val dbHelper = remember { DatabaseHelper(context) }
  
  val backStack = rememberNavBackStack(Splash)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Splash> {
          SplashScreen(
            onNavigateNext = {
              val isSetupDone = dbHelper.getConfig("is_setup_completed", "false") == "true"
              if (!isSetupDone) {
                backStack.add(Setup)
              } else {
                backStack.add(Lock)
              }
            },
            modifier = Modifier.safeDrawingPadding()
          )
        }

        entry<Setup> {
          SetupWizardScreen(
            onFinish = {
              backStack.add(Lock)
            },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        
        entry<Lock> {
          LockScreen(
            onUnlock = {
              backStack.add(Main)
            },
            modifier = Modifier.safeDrawingPadding()
          )
        }

        entry<Main> {
          MainScreen(
            onItemClick = { navKey -> backStack.add(navKey) },
            modifier = Modifier.safeDrawingPadding()
          )
        }

        entry<LastMessage> {
          LastMessageEditor(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }

        entry<Settings> {
          SettingsScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
      },
  )
}
