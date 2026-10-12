package com.idp.smartgait.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.idp.smartgait.data.UserPreferences
import com.idp.smartgait.data.UserProfile
import com.idp.smartgait.ui.components.PrimaryButton
import com.idp.smartgait.ui.theme.ElectricBlue
import com.idp.smartgait.ui.theme.NavyDark
import com.idp.smartgait.ui.theme.NavyLight
import com.idp.smartgait.ui.theme.TextSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val activityOptions = listOf("Running", "Walking", "Sprinting", "Football", "Basketball", "Gym Training", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    preferences: UserPreferences,
    onComplete: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var activity by remember { mutableStateOf("") }
    var activityExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Pre-fill name from signup
    var loaded by remember { mutableStateOf(false) }
    if (!loaded) {
        loaded = true
        scope.launch {
            val profile = preferences.getUserProfile()
            if (profile.name.isNotBlank()) name = profile.name
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = NavyLight,
        focusedBorderColor = ElectricBlue,
        unfocusedContainerColor = NavyLight.copy(alpha = .4f),
        focusedContainerColor = NavyLight.copy(alpha = .4f)
    )

    Column(
        Modifier.fillMaxSize().background(NavyDark).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(36.dp))
        Text("Set Up Your Profile", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(4.dp))
        Text("Help us personalise your experience", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))

        // Name
        OutlinedTextField(
            value = name, onValueChange = { name = it; error = null },
            label = { Text("Full Name") },
            singleLine = true, shape = RoundedCornerShape(14.dp), colors = fieldColors,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        // Age
        OutlinedTextField(
            value = age, onValueChange = { age = it.filter { c -> c.isDigit() }; error = null },
            label = { Text("Age") },
            singleLine = true, shape = RoundedCornerShape(14.dp), colors = fieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        // Height
        OutlinedTextField(
            value = height, onValueChange = { height = it; error = null },
            label = { Text("Height (cm)") },
            singleLine = true, shape = RoundedCornerShape(14.dp), colors = fieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        // Weight
        OutlinedTextField(
            value = weight, onValueChange = { weight = it; error = null },
            label = { Text("Weight (kg)") },
            singleLine = true, shape = RoundedCornerShape(14.dp), colors = fieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        // Activity dropdown
        ExposedDropdownMenuBox(expanded = activityExpanded, onExpandedChange = { activityExpanded = it }) {
            OutlinedTextField(
                value = activity,
                onValueChange = {},
                readOnly = true,
                label = { Text("Preferred Activity") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(activityExpanded) },
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = activityExpanded, onDismissRequest = { activityExpanded = false }) {
                activityOptions.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { activity = option; activityExpanded = false })
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))

        PrimaryButton("Save & Continue", modifier = Modifier.fillMaxWidth(), onClick = {
            val ageVal = age.toIntOrNull()
            val heightVal = height.toFloatOrNull()
            val weightVal = weight.toFloatOrNull()
            error = when {
                name.isBlank() -> "Name is required"
                ageVal == null || ageVal < 10 || ageVal > 120 -> "Enter a valid age (10–120)"
                heightVal == null || heightVal < 50 || heightVal > 250 -> "Enter a valid height in cm (50–250)"
                weightVal == null || weightVal < 20 || weightVal > 300 -> "Enter a valid weight in kg (20–300)"
                activity.isBlank() -> "Select an activity"
                else -> null
            }
            if (error == null) {
                scope.launch {
                    val existing = preferences.getUserProfile()
                    preferences.saveProfile(
                        UserProfile(
                            name = name.trim(),
                            email = existing.email,
                            age = ageVal!!,
                            height = heightVal!!,
                            weight = weightVal!!,
                            activityType = activity
                        )
                    )
                    onComplete()
                }
            }
        })

        Spacer(Modifier.height(16.dp))
    }
}

