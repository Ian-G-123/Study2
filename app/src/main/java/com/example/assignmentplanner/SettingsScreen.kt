package com.example.assignmentplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assignmentplanner.ui.theme.AppTheme

@Composable
fun SettingsScreen(currentTheme: AppTheme, onThemeChange: (AppTheme) -> Unit)
{
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        Text(text = "Theme Settings", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp), color = MaterialTheme.colorScheme.onBackground)

        themeoption("Light Mode", currentTheme == AppTheme.Light) {
            onThemeChange(AppTheme.Light)
        }
        themeoption("Dark Mode", currentTheme == AppTheme.Dark) { onThemeChange(AppTheme.Dark) }
        themeoption("Pastel Theme", currentTheme == AppTheme.Pastel) {
            onThemeChange(AppTheme.Pastel)
        }
        themeoption("Ocean Breeze", currentTheme == AppTheme.Ocean) { onThemeChange(AppTheme.Ocean) }
        themeoption("Midnight", currentTheme == AppTheme.Midnight) {
            onThemeChange(AppTheme.Midnight)
        }
    }
}

@Composable
fun themeoption(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit)
{
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, color = if (enabled) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        RadioButton(selected = selected, onClick = onClick, enabled = enabled, colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary))
    }
}
