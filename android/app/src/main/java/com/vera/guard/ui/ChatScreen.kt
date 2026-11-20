package com.vera.guard.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vera.guard.R
import com.vera.guard.data.FirstAid
import com.vera.guard.util.Locales

@Composable
fun ChatScreen(vm: AppViewModel, onNavigate: (Int) -> Unit) {
    val ctx = LocalContext.current
    val msgs by vm.chat.collectAsStateWithLifecycle()
    val busy by vm.chatBusy.collectAsStateWithLifecycle()
    val s by vm.settings.settings.collectAsStateWithLifecycle()
    val contacts by vm.profiles.contacts.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    var personalise by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val locale = Locales.forCode(s.language)
    val tts = remember { arrayOfNulls<TextToSpeech>(1) }
    DisposableEffect(Unit) { tts[0] = TextToSpeech(ctx) { st -> if (st == TextToSpeech.SUCCESS) tts[0]?.language = locale }; onDispose { tts[0]?.shutdown() } }
    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size - 1) }
    val mic = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { input = it }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text(stringResource(R.string.chat_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.chat_disclaimer), style = MaterialTheme.typography.bodySmall)
        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            androidx.compose.material3.Switch(checked = personalise, onCheckedChange = { personalise = it })
            Text(stringResource(R.string.chat_personalise), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
        }
        ChipRow(
            listOf(0 to stringResource(R.string.chip_fell), 1 to stringResource(R.string.chip_bleeding), 2 to stringResource(R.string.chip_cpr), 3 to stringResource(R.string.chip_hospital), 4 to stringResource(R.string.chip_journey), 5 to stringResource(R.string.chip_call)), -1,
        ) { i ->
            when (i) {
                0 -> vm.sendChat(ctx.getString(R.string.chip_fell), personalise)
                1 -> vm.sendChat(ctx.getString(R.string.chip_bleeding), personalise)
                2 -> vm.sendChat(ctx.getString(R.string.chip_cpr), personalise)
                3 -> onNavigate(1)
                4 -> onNavigate(2)
                5 -> contacts.firstOrNull { it.primary }?.let { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + it.phone))) } ?: onNavigate(4)
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (msgs.isEmpty()) item {
                Text(stringResource(R.string.chat_empty), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.offline_cards, FirstAid.cards.joinToString(", ") { it.title[s.language] ?: it.title.getValue("en") }), style = MaterialTheme.typography.bodySmall)
            }
            items(msgs) { m ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = if (m.fromUser) androidx.compose.ui.Alignment.End else androidx.compose.ui.Alignment.Start) {
                    Text(
                        m.text, Modifier.background(if (m.fromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(10.dp),
                        color = if (m.fromUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!m.fromUser) {
                        Row {
                            if (m.offline) Text(stringResource(R.string.offline_answer), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 8.dp))
                            OutlinedButton(onClick = { tts[0]?.speak(m.text, TextToSpeech.QUEUE_FLUSH, null, "chat") }) { Text(stringResource(R.string.speak)) }
                        }
                    }
                }
            }
            if (busy) item { Text("…") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            androidx.compose.material3.OutlinedTextField(value = input, onValueChange = { input = it.take(600) }, modifier = Modifier.weight(1f), placeholder = { Text(stringResource(R.string.chat_hint)) }, maxLines = 3)
            OutlinedButton(onClick = {
                try { mic.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())) } catch (e: Exception) { }
            }) { Text("🎤") }
            Button(onClick = { vm.sendChat(input, personalise); input = "" }, enabled = input.isNotBlank() && !busy) { Text(stringResource(R.string.send)) }
        }
    }
}
