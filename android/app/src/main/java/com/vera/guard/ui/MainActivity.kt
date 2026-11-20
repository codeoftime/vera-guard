package com.vera.guard.ui

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vera.guard.R
import com.vera.guard.data.SecureStore
import com.vera.guard.service.Actions
import com.vera.guard.service.ServiceStarter
import com.vera.guard.util.Locales
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Locales.wrap(newBase, Locales.code(SecureStore(newBase))))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AppViewModel = hiltViewModel()
            val s by vm.settings.settings.collectAsStateWithLifecycle()
            VeraTheme(s.theme) {
                var unlocked by rememberSaveable { mutableStateOf(false) }
                if (s.appLock && !unlocked) LockGate { unlocked = true } else AppRoot(vm)
            }
        }
    }

    fun authenticate(onOk: () -> Unit) {
        val allowed = if (Build.VERSION.SDK_INT >= 30) BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL else BiometricManager.Authenticators.BIOMETRIC_WEAK
        if (BiometricManager.from(this).canAuthenticate(allowed) != BiometricManager.BIOMETRIC_SUCCESS) { onOk(); return } // nothing enrolled: cannot lock
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onOk() }
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(getString(R.string.app_name)).setAllowedAuthenticators(allowed).build())
    }
}

@Composable
private fun LockGate(onUnlock: () -> Unit) {
    val act = LocalContext.current as MainActivity
    LaunchedEffect(Unit) { act.authenticate(onUnlock) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.app_locked), style = MaterialTheme.typography.headlineSmall)
        Button(onClick = { act.authenticate(onUnlock) }, modifier = Modifier.padding(top = 16.dp)) { Text(stringResource(R.string.unlock)) }
    }
}

@Composable
private fun AppRoot(vm: AppViewModel) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val s by vm.settings.settings.collectAsStateWithLifecycle()
    // Make sure the protection service is running whenever the app is opened.
    LaunchedEffect(Unit) { try { ServiceStarter.start(ctx, Actions.START) } catch (e: Exception) { } }
    val labels = listOf(R.string.tab_home, R.string.tab_nearby, R.string.tab_journey, R.string.tab_chat, R.string.tab_profile)
    val icons = listOf("🛡", "🏥", "🧭", "💬", "👤")
    Scaffold(
        bottomBar = {
            NavigationBar {
                labels.forEachIndexed { i, l ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Text(icons[i]) }, label = { Text(stringResource(l), maxLines = 1) })
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().statusBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                if (s.quickExit) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { (ctx as Activity).finishAndRemoveTask() }) { Text(stringResource(R.string.quick_exit)) }
                    }
                }
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        0 -> HomeScreen(vm)
                        1 -> NearbyScreen(vm)
                        2 -> JourneyScreen(vm)
                        3 -> ChatScreen(vm, onNavigate = { tab = it })
                        else -> ProfileScreen(vm)
                    }
                }
            }
        }
    }
}
