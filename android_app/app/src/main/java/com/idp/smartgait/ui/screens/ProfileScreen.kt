package com.idp.smartgait.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.idp.smartgait.data.UserPreferences
import com.idp.smartgait.data.UserProfile
import com.idp.smartgait.ui.components.PrimaryButton
import com.idp.smartgait.ui.components.SectionHeader
import com.idp.smartgait.ui.theme.ElectricBlue
import com.idp.smartgait.ui.theme.NavyCard
import com.idp.smartgait.ui.theme.NavyLight
import com.idp.smartgait.ui.theme.RiskLow
import com.idp.smartgait.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private val activities = listOf("Running", "Walking", "Sprinting", "Football", "Basketball", "Gym Training", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(preferences: UserPreferences, modifier: Modifier = Modifier) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var activity by remember { mutableStateOf("") }
    var activityExpanded by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Load profile
    LaunchedEffect(Unit) {
        val p = preferences.getUserProfile()
        name = p.name; email = p.email
        age = if (p.age > 0) p.age.toString() else ""
        height = if (p.height > 0) p.height.toString() else ""
        weight = if (p.weight > 0) p.weight.toString() else ""
        activity = p.activityType
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = NavyLight,
        focusedBorderColor = ElectricBlue,
        unfocusedContainerColor = NavyLight.copy(alpha = .4f),
        focusedContainerColor = NavyLight.copy(alpha = .4f)
    )

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Profile", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Manage your personal information", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("PERSONAL INFORMATION")

                    OutlinedTextField(value = name, onValueChange = { name = it; saved = false; error = null },
                        label = { Text("Full Name") }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = fieldColors,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth())

                    OutlinedTextField(value = email, onValueChange = { email = it; saved = false; error = null },
                        label = { Text("Email") }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = fieldColors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth())

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = age, onValueChange = { age = it.filter { c -> c.isDigit() }; saved = false },
                            label = { Text("Age") }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = fieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next), modifier = Modifier.weight(1f))

                        OutlinedTextField(value = height, onValueChange = { height = it; saved = false },
                            label = { Text("Height (cm)") }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = fieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next), modifier = Modifier.weight(1f))
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = weight, onValueChange = { weight = it; saved = false },
                            label = { Text("Weight (kg)") }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = fieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done), modifier = Modifier.weight(1f))

                        ExposedDropdownMenuBox(expanded = activityExpanded, onExpandedChange = { activityExpanded = it }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(value = activity, onValueChange = {}, readOnly = true,
                                label = { Text("Activity") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(activityExpanded) },
                                shape = RoundedCornerShape(14.dp), colors = fieldColors, modifier = Modifier.fillMaxWidth().menuAnchor())
                            ExposedDropdownMenu(expanded = activityExpanded, onDismissRequest = { activityExpanded = false }) {
                                activities.forEach { opt -> DropdownMenuItem(text = { Text(opt) }, onClick = { activity = opt; activityExpanded = false; saved = false }) }
                            }
                        }
                    }
                }
            }
        }

        item {
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
            }
            if (saved) {
                Text("✓ Profile saved", color = RiskLow, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
            }
            PrimaryButton("Save Profile", modifier = Modifier.fillMaxWidth(), onClick = {
                val ageVal = age.toIntOrNull()
                val heightVal = height.toFloatOrNull()
                val weightVal = weight.toFloatOrNull()
                error = when {
                    name.isBlank() -> "Name is required"
                    ageVal != null && (ageVal < 10 || ageVal > 120) -> "Enter a valid age (10–120)"
                    heightVal != null && (heightVal < 50 || heightVal > 250) -> "Enter a valid height"
                    weightVal != null && (weightVal < 20 || weightVal > 300) -> "Enter a valid weight"
                    else -> null
                }
                if (error == null) {
                    scope.launch {
                        preferences.saveProfile(
                            UserProfile(name.trim(), email.trim(), ageVal ?: 0, heightVal ?: 0f, weightVal ?: 0f, activity)
                        )
                        saved = true
                    }
                }
            })
        }
    }
}

