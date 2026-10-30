package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.security.AuthResult
import com.example.security.SuperAdminSecurityManager
import com.example.ui.components.WingedHeartLogo
import com.example.ui.theme.BrandChampagneGold
import com.example.ui.theme.BrandDeepViolet
import com.example.ui.theme.BrandDustyRose
import com.example.ui.theme.BrandRoyalPlum
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.SoftGold
import com.example.ui.theme.VelvetPlum
import com.example.util.LayaliNotificationHelper
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("إعداد تسجيل الدخول عبر Google غير مكتمل (default_web_client_id)")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("نوع بيانات الاعتماد غير متوقع")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "تعذر تسجيل الدخول عبر Google")
        }
    }
}

fun signOut(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onTraditionalLoginSuccess: (usernameOrEmail: String) -> Unit = { _ -> onAuthSuccess() },
    onSuperAdminLoginSuccess: (adminEmail: String, sessionToken: String) -> Unit = { _, _ -> },
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var usernameOrEmail by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var fullNameInput by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isCreateAccountMode by rememberSaveable { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = coroutineScope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF281136),
                        BrandDeepViolet,
                        ObsidianNight
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Luxury Brand Header
            WingedHeartLogo(modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isCreateAccountMode) "إنشاء حساب جديد" else "تسجيل الدخول",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "خدمات البنات • ليالي وسبا VIP في جميع المدن السورية والتركية",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandChampagneGold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Main Luxury Authentication Card (Screen 1 of UI/UX Case Study)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BrandRoyalPlum.copy(alpha = 0.72f)
                ),
                border = BorderStroke(1.2.dp, BrandChampagneGold.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp)
                ) {
                    if (isCreateAccountMode) {
                        Text(
                            text = "الاسم الكامل أو اللقب",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = fullNameInput,
                            onValueChange = {
                                fullNameInput = it
                                errorMessage = null
                            },
                            placeholder = { Text("أدخل اسمك أو لقبك الشخصي") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = BrandChampagneGold
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandChampagneGold,
                                unfocusedBorderColor = BrandChampagneGold.copy(alpha = 0.35f),
                                focusedContainerColor = ObsidianNight.copy(alpha = 0.55f),
                                unfocusedContainerColor = ObsidianNight.copy(alpha = 0.40f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_fullname_input")
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Field 1: Username or Email
                    Text(
                        text = "اسم المستخدم أو البريد الإلكتروني",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = usernameOrEmail,
                        onValueChange = {
                            usernameOrEmail = it
                            errorMessage = null
                        },
                        placeholder = { Text("اسم المستخدم أو البريد الإلكتروني") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = BrandChampagneGold
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandChampagneGold,
                            unfocusedBorderColor = BrandChampagneGold.copy(alpha = 0.35f),
                            focusedContainerColor = ObsidianNight.copy(alpha = 0.55f),
                            unfocusedContainerColor = ObsidianNight.copy(alpha = 0.40f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_username_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Field 2: Password with show/hide eye icon
                    Text(
                        text = "كلمة المرور",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        placeholder = { Text("كلمة المرور") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = BrandChampagneGold
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { isPasswordVisible = !isPasswordVisible },
                                modifier = Modifier.testTag("toggle_password_visibility_button")
                            ) {
                                Icon(
                                    imageVector = if (isPasswordVisible) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription = if (isPasswordVisible) {
                                        "إخفاء كلمة المرور"
                                    } else {
                                        "إظهار كلمة المرور"
                                    },
                                    tint = BrandChampagneGold
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandChampagneGold,
                            unfocusedBorderColor = BrandChampagneGold.copy(alpha = 0.35f),
                            focusedContainerColor = ObsidianNight.copy(alpha = 0.55f),
                            unfocusedContainerColor = ObsidianNight.copy(alpha = 0.40f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input")
                    )

                    // Forgot Password Link
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                statusNotice = "تم إرسال رابط استعادة كلمة المرور والتواصل الفوري عبر الدعم @RamaNUR"
                                errorMessage = null
                            },
                            modifier = Modifier.testTag("forgot_password_button")
                        ) {
                            Text(
                                text = "نسيت كلمة المرور؟",
                                style = MaterialTheme.typography.labelLarge,
                                color = SoftGold
                            )
                        }

                        Text(
                            text = "حماية مشفرة VIP",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldVerified
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (statusNotice != null) {
                        Surface(
                            color = EmeraldVerified.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EmeraldVerified),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = statusNotice ?: "",
                                color = EmeraldVerified,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Primary Metallic Gold Button: "تسجيل الدخول"
                    Button(
                        onClick = {
                            errorMessage = null
                            statusNotice = null
                            val cleanUser = usernameOrEmail.trim()
                            val cleanPass = password.trim()
                            if (cleanUser.isBlank()) {
                                errorMessage = "يرجى إدخال اسم المستخدم أو البريد الإلكتروني"
                                return@Button
                            }
                            if (cleanPass.length < 4) {
                                errorMessage = "يرجى إدخال كلمة مرور مكونة من 4 خانات على الأقل"
                                return@Button
                            }
                            when (val authResult = SuperAdminSecurityManager.authenticate(context, cleanUser, cleanPass)) {
                                is AuthResult.SuperAdminSuccess -> {
                                    onSuperAdminLoginSuccess(authResult.email, authResult.sessionToken)
                                }
                                is AuthResult.InvalidSuperAdminPassword -> {
                                    errorMessage = authResult.message
                                }
                                is AuthResult.CustomerSuccess -> {
                                    val resolvedName = if (isCreateAccountMode && fullNameInput.isNotBlank()) {
                                        fullNameInput.trim()
                                    } else {
                                        authResult.displayName
                                    }
                                    onTraditionalLoginSuccess(resolvedName)
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("primary_login_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandChampagneGold,
                            contentColor = ObsidianNight
                        )
                    ) {
                        Text(
                            text = if (isCreateAccountMode) "إنشاء الحساب وتسجيل الدخول" else "تسجيل الدخول",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianNight
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Fast Sign-In Section: "أو تسجيل الدخول عبر:"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = BrandChampagneGold.copy(alpha = 0.25f)
                        )
                        Text(
                            text = "أو تسجيل الدخول عبر:",
                            style = MaterialTheme.typography.labelLarge,
                            color = SoftGold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = BrandChampagneGold.copy(alpha = 0.25f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Horizontal Clear Social Sign-in Buttons (Google & Apple)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Google Sign-In Button
                        Surface(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = coroutineScope,
                                    onAuthCancelled = {
                                        isLoading = false
                                    }
                                )
                            },
                            enabled = !isLoading,
                            color = Color.White,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, BrandChampagneGold),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = ObsidianNight,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4285F4)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "G",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Google",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ObsidianNight,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Apple Sign-In Button
                        Surface(
                            onClick = {
                                errorMessage = null
                                onTraditionalLoginSuccess("عميل Apple VIP")
                            },
                            color = Color.White,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, BrandChampagneGold),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("apple_sign_in_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Apple",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ObsidianNight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Create New Account / Switch Mode Link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCreateAccountMode) "لديك حساب بالفعل؟" else "ليس لديك حساب؟",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SoftGold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCreateAccountMode) "تسجيل الدخول" else "إنشاء حساب جديد",
                            style = MaterialTheme.typography.titleMedium,
                            color = BrandChampagneGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    isCreateAccountMode = !isCreateAccountMode
                                    errorMessage = null
                                    statusNotice = null
                                }
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                                .testTag("toggle_create_account_link")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (onDismiss != null) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("dismiss_auth_screen_button"),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, BrandChampagneGold.copy(alpha = 0.7f))
                ) {
                    Text(
                        text = "تصفح الشاشة الرئيسية والأقسام مباشرة",
                        color = BrandChampagneGold,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(76.dp))
        }

        // Floating Telegram @RamaNUR Support Button on Auth Screen
        ExtendedFloatingActionButton(
            onClick = { LayaliNotificationHelper.openTelegramSupport(context) },
            containerColor = BrandDustyRose,
            contentColor = Color.White,
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("auth_telegram_support_fab"),
            icon = {
                Icon(
                    imageVector = Icons.Default.SupportAgent,
                    contentDescription = "دعم تلجرام"
                )
            },
            text = {
                Text(
                    text = "الدعم @RamaNUR",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        )
    }
}
