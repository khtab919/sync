package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.example.security.SuperAdminSecurityManager
import com.example.ui.auth.AdultWelcomeScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.MainFeatureScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                if (BuildConfig.LAYALI_API_URL.isNotBlank()) {
                    com.example.ui.screens.ServerAppScreen(BuildConfig.LAYALI_API_URL)
                } else {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    var adultPolicyAccepted by rememberSaveable { mutableStateOf(false) }
    if (!adultPolicyAccepted) {
        AdultWelcomeScreen(onContinue = { adultPolicyAccepted = true })
        return
    }
    val context = LocalContext.current
    val authFlow = remember(auth) { auth.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var showAuthScreen by rememberSaveable { mutableStateOf(true) }
    var guestModeBrowsing by rememberSaveable { mutableStateOf(false) }
    var traditionalUserDisplayName by rememberSaveable { mutableStateOf("") }
    var adminSessionToken by rememberSaveable { mutableStateOf<String?>(null) }
    var superAdminEmail by rememberSaveable { mutableStateOf<String?>(null) }
    val user = currentUser
    val isSuperAdmin = !adminSessionToken.isNullOrBlank()
    val hasAuthenticatedUser = user != null || traditionalUserDisplayName.isNotBlank() || isSuperAdmin

    if (showAuthScreen && !hasAuthenticatedUser && !guestModeBrowsing) {
        AuthScreen(
            onAuthSuccess = {
                showAuthScreen = false
            },
            onTraditionalLoginSuccess = { enteredUsername ->
                adminSessionToken = null
                superAdminEmail = null
                traditionalUserDisplayName = enteredUsername
                guestModeBrowsing = false
                showAuthScreen = false
            },
            onSuperAdminLoginSuccess = { email, sessionToken ->
                superAdminEmail = email
                adminSessionToken = sessionToken
                traditionalUserDisplayName = "Super Admin"
                guestModeBrowsing = false
                showAuthScreen = false
            },
            onDismiss = {
                guestModeBrowsing = true
                showAuthScreen = false
            }
        )
    } else if (showAuthScreen) {
        AuthScreen(
            onAuthSuccess = {
                showAuthScreen = false
            },
            onTraditionalLoginSuccess = { enteredUsername ->
                adminSessionToken = null
                superAdminEmail = null
                traditionalUserDisplayName = enteredUsername
                guestModeBrowsing = false
                showAuthScreen = false
            },
            onSuperAdminLoginSuccess = { email, sessionToken ->
                superAdminEmail = email
                adminSessionToken = sessionToken
                traditionalUserDisplayName = "Super Admin"
                guestModeBrowsing = false
                showAuthScreen = false
            },
            onDismiss = { showAuthScreen = false }
        )
    } else {
        val activeDisplayName = when {
            isSuperAdmin -> "Super Admin"
            user != null -> user.displayName ?: user.email?.substringBefore("@").orEmpty()
            traditionalUserDisplayName.isNotBlank() -> traditionalUserDisplayName
            else -> ""
        }
        MainFeatureScreen(
            currentUserId = user?.uid ?: if (traditionalUserDisplayName.isNotBlank()) "user_${traditionalUserDisplayName.hashCode()}" else "guest_session",
            userDisplayName = activeDisplayName,
            isGoogleSignedIn = user != null || traditionalUserDisplayName.isNotBlank(),
            userEmail = superAdminEmail
                ?: user?.email
                ?: if (traditionalUserDisplayName.isNotBlank()) "$traditionalUserDisplayName@layali.vip" else null,
            adminSessionToken = adminSessionToken,
            isSuperAdmin = isSuperAdmin,
            onRequestGoogleSignIn = { showAuthScreen = true },
            onSuperAdminSignOut = {
                SuperAdminSecurityManager.revokeSuperAdminSession(context)
                adminSessionToken = null
                superAdminEmail = null
                traditionalUserDisplayName = ""
                guestModeBrowsing = false
                showAuthScreen = true
            },
            onSignedOut = {
                SuperAdminSecurityManager.revokeSuperAdminSession(context)
                adminSessionToken = null
                superAdminEmail = null
                traditionalUserDisplayName = ""
                guestModeBrowsing = false
                showAuthScreen = true
            }
        )
    }
}

