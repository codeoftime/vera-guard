package com.vera.guard.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SectionCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit, error: String? = null, keyboard: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = singleLine, isError = error != null,
        supportingText = { if (error != null) Text(error) }, keyboardOptions = KeyboardOptions(keyboardType = keyboard), modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, hint: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label)
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun <T> ChipRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (v, label) -> FilterChip(selected = v == selected, onClick = { onSelect(v) }, label = { Text(label) }) }
    }
}

@Composable
fun Gap(h: Int = 8) = Spacer(Modifier.height(h.dp))

fun fmtAge(ms: Long): String {
    val m = ms / 60000
    return if (m < 1) "<1 min" else if (m < 60) "$m min" else "${m / 60} h ${m % 60} min"
}
