package com.idp.smartgait.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.idp.smartgait.data.UserPreferences
import com.idp.smartgait.ui.components.AppLogo
import com.idp.smartgait.ui.components.PrimaryButton
import com.idp.smartgait.ui.theme.ElectricBlue
import com.idp.smartgait.ui.theme.NavyDark
import com.idp.smartgait.ui.theme.NavyLight
import com.idp.smartgait.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    preferences: UserPreferences,
    onSignUpSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = NavyLight,
        focusedBorderColor = ElectricBlue,
        unfocusedContainerColor = NavyLight.copy(alpha = .4f),
        focusedContainerColor = NavyLight.copy(alpha = .4f)
    )

    Column(
        Modifier.fillMaxSize().background(NavyDark).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(36.dp))
        AppLogo()
        Spacer(Modifier.height(24.dp))

        Text("Create Account", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(4.dp))
        Text("Join Smart Gait to start monitoring", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(24.dp))

        // Name
        OutlinedTextField(
            value = name, onValueChange = { name = it; nameError = null },
            label = { Text("Full Name") }, leadingIcon = { Icon(Icons.Rounded.Person, "Name") },
            isError = nameError != null, supportingText = nameError?.let { { Text(it) } },
            singleLine = true, shape = RoundedCornerShape(14.dp), colors = fieldColors,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        // Email
        OutlinedTextField(
            value = email, onValueChange = { email = it; emailError = null },
            label = { Text("Email") }, leadingIcon = { Icon(Icons.Rounded.Email, "Email") },
            isError = emailError != null, supportingText = emailError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            singleLine = true, shape = RoundedCornerShape(14.dp), colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        // Password
        OutlinedTextField(
            value = password, onValueChange = { password = it; passwordError = null },
            label = { Text("Password") }, leadingIcon = { Icon(Icons.Rounded.Lock, "Password") },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Toggle")
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            isError = passwordError != null, supportingText = passwordError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            singleLine = true, shape = RoundedCornerShape(14.dp), colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        // Confirm
        OutlinedTextField(
            value = confirmPassword, onValueChange = { confirmPassword = it; confirmError = null },
            label = { Text("Confirm Password") }, leadingIcon = { Icon(Icons.Rounded.Lock, "Confirm") },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            isError = confirmError != null, supportingText = confirmError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            singleLine = true, shape = RoundedCornerShape(14.dp), colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        if (loading) {
            CircularProgressIndicator(color = ElectricBlue, modifier = Modifier.padding(12.dp))
        } else {
            PrimaryButton("Create Account", modifier = Modifier.fillMaxWidth(), onClick = {
                nameError = if (name.isBlank()) "Name is required" else null
                emailError = when {
                    email.isBlank() -> "Email is required"
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email format"
                    else -> null
                }
                passwordError = when {
                    password.isBlank() -> "Password is required"
                    password.length < 6 -> "Password must be at least 6 characters"
                    else -> null
                }
                confirmError = when {
                    confirmPassword.isBlank() -> "Please confirm your password"
                    confirmPassword != password -> "Passwords do not match"
                    else -> null
                }
                if (nameError == null && emailError == null && passwordError == null && confirmError == null) {
                    loading = true
                    scope.launch {
                        preferences.signup(name.trim(), email.trim(), password)
                        loading = false
                        onSignUpSuccess()
                    }
                }
            })
        }

        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account?", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            TextButton(onClick = onBackToLogin) { Text("Login", color = ElectricBlue) }
        }
    }
}

