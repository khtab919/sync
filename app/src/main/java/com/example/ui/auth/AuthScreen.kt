package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.CharcoalButton
import com.example.ui.theme.CoralPinkPrimary
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.VioletBlueVip
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
    startInCreateMode: Boolean = false,
    isLightTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onTraditionalLoginSuccess: (usernameOrEmail: String) -> Unit = { _ -> onAuthSuccess() },
    onSuperAdminLoginSuccess: (adminEmail: String, sessionToken: String) -> Unit = { _, _ -> },
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var usernameOrEmail by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var fullNameInput by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isCreateAccountMode by rememberSaveable { mutableStateOf(startInCreateMode) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusNotice by remember { mutableStateOf<String?>(null) }
    var showChangePasswordDialog by rememberSaveable { mutableStateOf(false) }
    var savedAccountsRefreshTick by remember { mutableStateOf(0) }

    val savedAccounts = remember(savedAccountsRefreshTick) {
        SuperAdminSecurityManager.getSavedAccounts(context)
    }

    LaunchedEffect(Unit) {
        SuperAdminSecurityManager.ensureInitialized(context)
        savedAccountsRefreshTick += 1
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
            .background(MaterialTheme.colorScheme.background)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LAYALI",
                    letterSpacing = 2.sp,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.testTag("auth_theme_toggle")
                ) {
                    Icon(
                        imageVector = if (isLightTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = if (isLightTheme) "الوضع الليلي" else "الوضع النهاري",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            WingedHeartLogo(modifier = Modifier.size(72.dp))
            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (isCreateAccountMode) "إنشاء حساب جديد" else "تسجيل الدخول",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isCreateAccountMode) "أنشئ حسابك لتعرض الخدمات وتتواصل بأمان" else "مرحبًا بعودتك إلى عالم ليالي",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Clean High-Contrast Authentication Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp)
                ) {
                    if (isCreateAccountMode) {
                        Text(
                            text = "الاسم الكامل أو اللقب",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
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
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_fullname_input")
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Field 1: Username or Email
                    Text(
                        text = "اسم المستخدم أو البريد الإلكتروني",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
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
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_username_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Field 2: Password with show/hide eye icon
                    Text(
                        text = "كلمة المرور",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
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
                                tint = MaterialTheme.colorScheme.primary
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
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input")
                    )

                    if (isCreateAccountMode) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "تأكيد كلمة المرور",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; errorMessage = null },
                            placeholder = { Text("أعد إدخال كلمة المرور") },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("auth_confirm_password_input")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "استخدم 8 أحرف على الأقل لحماية حسابك",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }

                    // Recovery is available only while signing in.
                    if (!isCreateAccountMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    showChangePasswordDialog = true
                                    errorMessage = null
                                },
                                modifier = Modifier.testTag("forgot_password_button")
                            ) {
                                Text("نسيت كلمة المرور؟", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
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
                            color = EmeraldVerified.copy(alpha = 0.12f),
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

                    // Primary Capsule CTA Button
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
                            if (isCreateAccountMode && fullNameInput.trim().length < 2) {
                                errorMessage = "أدخل اسمًا واضحًا مكونًا من حرفين على الأقل"
                                return@Button
                            }
                            if (isCreateAccountMode && cleanUser.length < 3) {
                                errorMessage = "اسم المستخدم يجب أن يتكون من 3 أحرف على الأقل"
                                return@Button
                            }
                            if (cleanPass.length < if (isCreateAccountMode) 8 else 4) {
                                errorMessage = if (isCreateAccountMode) "استخدم كلمة مرور من 8 أحرف على الأقل" else "يرجى إدخال كلمة المرور"
                                return@Button
                            }
                            if (isCreateAccountMode && password != confirmPassword) {
                                errorMessage = "كلمتا المرور غير متطابقتين"
                                return@Button
                            }
                            val authResult = SuperAdminSecurityManager.authenticate(
                                context = context,
                                emailOrUsername = cleanUser,
                                passwordInput = cleanPass,
                                fullNameOverride = if (isCreateAccountMode) fullNameInput.trim() else "",
                                createAccount = isCreateAccountMode
                            )
                            savedAccountsRefreshTick += 1
                            when (authResult) {
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
                            .height(50.dp)
                            .testTag("primary_login_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralPinkPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (isCreateAccountMode) "إنشاء الحساب وتسجيل الدخول" else "تسجيل الدخول",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Fast Sign-In Section: "أو تسجيل الدخول عبر:"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "أو المتابعة باستخدام",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Horizontal Clear Social Sign-in Capsule Buttons (Google & Apple)
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
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
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
                                        color = CoralPinkPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4285F4)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "G",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Google",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }


                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Create New Account / Switch Mode Link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCreateAccountMode) "لديك حساب بالفعل؟" else "ليس لديك حساب؟",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCreateAccountMode) "تسجيل الدخول" else "إنشاء حساب جديد",
                            style = MaterialTheme.typography.titleMedium,
                            color = VioletBlueVip,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable {
                                    isCreateAccountMode = !isCreateAccountMode
                                    confirmPassword = ""
                                    errorMessage = null
                                    statusNotice = null
                                }
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                                .testTag("toggle_create_account_link")
                        )
                    }

                    // Saved Accounts in Room SQLite Database Strip
                    if (savedAccounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${savedAccounts.size} حساب محفوظ",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldVerified
                            )
                            Text(
                                text = "الحسابات المحفوظة في قاعدة البيانات (SQLite)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        savedAccounts.take(4).forEach { acc ->
                            Surface(
                                onClick = {
                                    usernameOrEmail = if (acc.role == "super_admin") acc.email else acc.username
                                    isCreateAccountMode = false
                                    statusNotice = "تم اختيار الحساب المحفوظ (${acc.username} • LEVEL ${acc.level}). أدخل كلمة المرور أو اضغط «تغيير كلمة المرور»."
                                    errorMessage = null
                                },
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .testTag("saved_account_row_${acc.accountId}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            usernameOrEmail = if (acc.role == "super_admin") acc.email else acc.username
                                            showChangePasswordDialog = true
                                        },
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text(
                                            text = "تغيير كلمة السر",
                                            fontSize = 10.sp,
                                            color = CoralPinkPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${acc.displayName} (${acc.username})",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "LEVEL ${acc.level} • رصيد $${acc.walletBalanceUsd} • مشفر PBKDF2",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
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
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = "تصفح الشاشة الرئيسية والأقسام مباشرة",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(76.dp))
        }

        // Floating Telegram @RamaNUR Support Button on Auth Screen
        ExtendedFloatingActionButton(
            onClick = { LayaliNotificationHelper.openTelegramSupport(context) },
            containerColor = CoralPinkPrimary,
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
                    fontWeight = FontWeight.SemiBold
                )
            }
        )

        // Interactive Change / Reset Password Dialog backed by Room SQLite + PBKDF2-HMAC-SHA256
        if (showChangePasswordDialog) {
            var targetIdentifier by rememberSaveable { mutableStateOf(usernameOrEmail) }
            var currentPasswordInput by rememberSaveable { mutableStateOf("") }
            var newPasswordInput by rememberSaveable { mutableStateOf("") }
            var confirmNewPasswordInput by rememberSaveable { mutableStateOf("") }
            var useDirectRecoveryMode by rememberSaveable { mutableStateOf(false) }
            var dialogError by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { showChangePasswordDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        text = "تغيير أو استعادة كلمة المرور",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "يتم تشفير كلمة المرور الجديدة بخوارزمية PBKDF2-HMAC-SHA256 وحفظها مباشرة في قاعدة بيانات الحسابات (Room SQLite).",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !useDirectRecoveryMode,
                                onClick = {
                                    useDirectRecoveryMode = false
                                    dialogError = null
                                },
                                label = { Text("بكلمة السر الحالية", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VioletBlueVip,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = useDirectRecoveryMode,
                                onClick = {
                                    useDirectRecoveryMode = true
                                    dialogError = null
                                },
                                label = { Text("استعادة فورية للحساب", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CoralPinkPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = targetIdentifier,
                            onValueChange = {
                                targetIdentifier = it
                                dialogError = null
                            },
                            label = { Text("اسم المستخدم أو البريد الإلكتروني", fontSize = 11.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("change_password_identifier_input")
                        )

                        if (!useDirectRecoveryMode) {
                            OutlinedTextField(
                                value = currentPasswordInput,
                                onValueChange = {
                                    currentPasswordInput = it
                                    dialogError = null
                                },
                                label = { Text("كلمة المرور الحالية", fontSize = 11.sp) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("change_password_current_input")
                            )
                        }

                        OutlinedTextField(
                            value = newPasswordInput,
                            onValueChange = {
                                newPasswordInput = it
                                dialogError = null
                            },
                            label = { Text("كلمة المرور الجديدة (6 خانات فأكثر)", fontSize = 11.sp) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("change_password_new_input")
                        )

                        OutlinedTextField(
                            value = confirmNewPasswordInput,
                            onValueChange = {
                                confirmNewPasswordInput = it
                                dialogError = null
                            },
                            label = { Text("تأكيد كلمة المرور الجديدة", fontSize = 11.sp) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("change_password_confirm_input")
                        )

                        if (dialogError != null) {
                            Text(
                                text = dialogError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPasswordInput != confirmNewPasswordInput) {
                                dialogError = "كلمتا المرور الجديدتان غير متطابقتين"
                                return@Button
                            }
                            val result = SuperAdminSecurityManager.changeAccountPassword(
                                context = context,
                                identifier = targetIdentifier,
                                currentPassword = currentPasswordInput,
                                newPassword = newPasswordInput,
                                requireCurrentPassword = !useDirectRecoveryMode
                            )
                            result.onSuccess { record ->
                                usernameOrEmail = if (record.email.equals(SuperAdminSecurityManager.INITIAL_SUPER_ADMIN_EMAIL, ignoreCase = true)) {
                                    record.email
                                } else {
                                    record.username
                                }
                                password = newPasswordInput
                                savedAccountsRefreshTick += 1
                                statusNotice = "تم تحديث وحفظ كلمة المرور في قاعدة البيانات بنجاح للحساب (${record.username}) ✓"
                                errorMessage = null
                                showChangePasswordDialog = false
                            }.onFailure { err ->
                                dialogError = err.message ?: "تعذر تغيير كلمة المرور"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VioletBlueVip,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("confirm_change_password_button")
                    ) {
                        Text("حفظ كلمة المرور الجديدة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showChangePasswordDialog = false }) {
                        Text("إلغاء", fontSize = 12.sp)
                    }
                }
            )
        }
    }
}
