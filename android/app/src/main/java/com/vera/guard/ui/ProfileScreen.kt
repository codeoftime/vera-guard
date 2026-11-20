package com.vera.guard.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vera.guard.R
import com.vera.guard.data.Consent
import com.vera.guard.data.Contact
import com.vera.guard.data.Profile
import com.vera.guard.data.Validators

@Composable
fun ProfileScreen(vm: AppViewModel) {
    var section by rememberSaveable { mutableStateOf("profile") }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            ChipRow(
                listOf("profile" to stringResource(R.string.sec_profile), "contacts" to stringResource(R.string.sec_contacts), "sharing" to stringResource(R.string.sec_sharing),
                    "security" to stringResource(R.string.sec_security), "settings" to stringResource(R.string.sec_settings)), section,
            ) { section = it }
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp)) {
            when (section) {
                "profile" -> ProfileForm(vm)
                "contacts" -> ContactsSection(vm)
                "sharing" -> SharingSection(vm)
                "security" -> SecuritySection(vm)
                else -> SettingsSection(vm)
            }
            Gap(24)
        }
    }
}

@Composable
private fun ProfileForm(vm: AppViewModel) {
    val ctx = LocalContext.current
    val saved by vm.profiles.profile.collectAsStateWithLifecycle()
    var p by remember(saved) { mutableStateOf(saved) }
    var h by remember(saved) { mutableStateOf(saved.heightCm?.let { if (saved.imperial) { val (f, i) = Validators.cmToFtIn(it); "$f.$i" } else "%.0f".format(it) } ?: "") }
    var w by remember(saved) { mutableStateOf(saved.weightKg?.let { if (saved.imperial) "%.0f".format(Validators.kgToLb(it)) else "%.1f".format(it) } ?: "") }
    var showErrors by remember { mutableStateOf(false) }
    var savedMsg by remember { mutableStateOf(false) }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) { }
            p = p.copy(photoUri = uri.toString())
        }
    }
    fun parseH(): Float? = h.toFloatOrNull()?.let { if (p.imperial) { val ft = it.toInt(); val inch = Math.round((it - ft) * 10f); Validators.ftInToCm(ft, inch) } else it }
    fun parseW(): Float? = w.toFloatOrNull()?.let { if (p.imperial) Validators.lbToKg(it) else it }
    val nameErr = if (showErrors && p.fullName.isNotBlank() && !Validators.name(p.fullName)) stringResource(R.string.err_name) else null
    val dobErr = if (!Validators.dob(p.dob)) stringResource(R.string.err_dob) else null
    val hErr = if (h.isNotBlank() && !Validators.height(parseH())) stringResource(R.string.err_height) else null
    val wErr = if (w.isNotBlank() && !Validators.weight(parseW())) stringResource(R.string.err_weight) else null
    val docErr = if (p.doctorPhone.isNotBlank() && !Validators.phone(p.doctorPhone)) stringResource(R.string.err_phone) else null

    SectionCard(stringResource(R.string.sec_profile)) {
        val bmp = remember(p.photoUri) {
            try { if (p.photoUri.isBlank()) null else ctx.contentResolver.openInputStream(Uri.parse(p.photoUri))?.use { android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }) } } catch (e: Exception) { null }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.size(64.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            OutlinedButton(onClick = { photo.launch(arrayOf("image/*")) }) { Text(stringResource(R.string.pick_photo)) }
            if (p.photoUri.isNotBlank()) TextButton(onClick = { p = p.copy(photoUri = "") }) { Text(stringResource(R.string.remove)) }
        }
        Field(stringResource(R.string.full_name), p.fullName, { p = p.copy(fullName = it.take(60)) }, nameErr)
        Field(stringResource(R.string.dob), p.dob, { p = p.copy(dob = it.take(10)) }, dobErr)
        Text(stringResource(R.string.gender), style = MaterialTheme.typography.labelMedium)
        ChipRow(listOf("" to "–", "female" to stringResource(R.string.g_female), "male" to stringResource(R.string.g_male), "other" to stringResource(R.string.g_other)), p.gender) { p = p.copy(gender = it) }
        SwitchRow(stringResource(R.string.imperial_units), p.imperial, { v ->
            val cm = parseH(); val kg = parseW()
            p = p.copy(imperial = v)
            h = cm?.let { if (v) { val (f, i) = Validators.cmToFtIn(it); "$f.$i" } else "%.0f".format(it) } ?: ""
            w = kg?.let { if (v) "%.0f".format(Validators.kgToLb(it)) else "%.1f".format(it) } ?: ""
        }, hint = stringResource(R.string.imperial_hint))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(stringResource(if (p.imperial) R.string.height_ftin else R.string.height_cm), h, { h = it.take(5) }, hErr, KeyboardType.Decimal, modifier = Modifier.weight(1f))
            Field(stringResource(if (p.imperial) R.string.weight_lb else R.string.weight_kg), w, { w = it.take(5) }, wErr, KeyboardType.Decimal, modifier = Modifier.weight(1f))
        }
        Text(stringResource(R.string.blood_group), style = MaterialTheme.typography.labelMedium)
        ChipRow(Validators.bloodGroups.map { it to it.ifEmpty { "–" } }, p.bloodGroup) { p = p.copy(bloodGroup = it) }
        Field(stringResource(R.string.allergies), p.allergies, { p = p.copy(allergies = it.take(300)) }, singleLine = false)
        Field(stringResource(R.string.conditions), p.conditions, { p = p.copy(conditions = it.take(300)) }, singleLine = false)
        Field(stringResource(R.string.medications), p.medications, { p = p.copy(medications = it.take(300)) }, singleLine = false)
        Field(stringResource(R.string.doctor_name), p.doctorName, { p = p.copy(doctorName = it.take(60)) })
        Field(stringResource(R.string.doctor_phone), p.doctorPhone, { p = p.copy(doctorPhone = it.take(20)) }, docErr, KeyboardType.Phone)
        Field(stringResource(R.string.home_address), p.homeAddress, { p = p.copy(homeAddress = it.take(200)) }, singleLine = false)
        Field(stringResource(R.string.ice_note), p.iceNote, { p = p.copy(iceNote = it.take(200)) }, singleLine = false)
        Text(stringResource(R.string.profile_hint), style = MaterialTheme.typography.bodySmall)
        Button(onClick = {
            showErrors = true
            if (nameErr == null && dobErr == null && hErr == null && wErr == null && docErr == null && (p.fullName.isBlank() || Validators.name(p.fullName))) {
                vm.profiles.saveProfile(p.copy(heightCm = parseH(), weightKg = parseW(), doctorPhone = Validators.normalizePhone(p.doctorPhone)))
                savedMsg = true
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
        if (savedMsg) Text(stringResource(R.string.saved), color = VeraGreen)
    }
}

@Composable
private fun ContactsSection(vm: AppViewModel) {
    val contacts by vm.profiles.contacts.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Contact?>(null) }
    var full by remember { mutableStateOf(false) }
    SectionCard(stringResource(R.string.sec_contacts)) {
        Text(stringResource(R.string.contacts_hint), style = MaterialTheme.typography.bodySmall)
        if (contacts.isEmpty()) Text(stringResource(R.string.no_contacts), color = VeraAmber)
        contacts.forEach { c ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text((if (c.primary) "★ " else "") + c.name, style = MaterialTheme.typography.titleSmall)
                    Text("${c.phone} · ${c.relationship} · ${c.lang.uppercase()}", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { editing = c }) { Text(stringResource(R.string.edit)) }
                TextButton(onClick = { vm.profiles.deleteContact(c.id) }) { Text(stringResource(R.string.remove)) }
            }
        }
        Button(onClick = { if (contacts.size >= 5) full = true else editing = Contact(vm.profiles.newId(), "", "", "", "en", contacts.isEmpty()) }) { Text(stringResource(R.string.add_contact)) }
        if (full) Text(stringResource(R.string.max_contacts), color = VeraAmber)
    }
    editing?.let { c -> ContactDialog(c, onDismiss = { editing = null }, onSave = { vm.profiles.saveContact(it); editing = null }) }
}

@Composable
private fun ContactDialog(initial: Contact, onDismiss: () -> Unit, onSave: (Contact) -> Unit) {
    var c by remember { mutableStateOf(initial) }
    var tried by remember { mutableStateOf(false) }
    val nameErr = if (tried && !Validators.name(c.name)) stringResource(R.string.err_name) else null
    val phoneErr = if (tried && !Validators.phone(c.phone)) stringResource(R.string.err_phone) else null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.contact)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Field(stringResource(R.string.full_name), c.name, { c = c.copy(name = it.take(60)) }, nameErr)
                Field(stringResource(R.string.phone), c.phone, { c = c.copy(phone = it.take(20)) }, phoneErr, KeyboardType.Phone)
                Field(stringResource(R.string.relationship), c.relationship, { c = c.copy(relationship = it.take(30)) })
                Text(stringResource(R.string.sms_language), style = MaterialTheme.typography.labelMedium)
                ChipRow(listOf("en" to "English", "hi" to "हिन्दी", "kn" to "ಕನ್ನಡ"), c.lang) { c = c.copy(lang = it) }
                SwitchRow(stringResource(R.string.primary_contact), c.primary, { c = c.copy(primary = it) })
            }
        },
        confirmButton = { TextButton(onClick = { tried = true; if (Validators.name(c.name) && Validators.phone(c.phone)) onSave(c.copy(phone = Validators.normalizePhone(c.phone), name = c.name.trim())) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun SharingSection(vm: AppViewModel) {
    val p by vm.profiles.profile.collectAsStateWithLifecycle()
    val c by vm.profiles.consent.collectAsStateWithLifecycle()
    fun set(f: (Consent) -> Consent) = vm.profiles.saveConsent(f(c))
    SectionCard(stringResource(R.string.sec_sharing)) {
        Text(stringResource(R.string.sharing_hint), style = MaterialTheme.typography.bodySmall)
        SwitchRow(stringResource(R.string.full_name), c.name, { v -> set { it.copy(name = v) } })
        SwitchRow(stringResource(R.string.age), c.age, { v -> set { it.copy(age = v) } })
        SwitchRow(stringResource(R.string.gender), c.gender, { v -> set { it.copy(gender = v) } })
        SwitchRow(stringResource(R.string.blood_group), c.bloodGroup, { v -> set { it.copy(bloodGroup = v) } })
        SwitchRow(stringResource(R.string.allergies), c.allergies, { v -> set { it.copy(allergies = v) } })
        SwitchRow(stringResource(R.string.conditions), c.conditions, { v -> set { it.copy(conditions = v) } })
        SwitchRow(stringResource(R.string.medications), c.medications, { v -> set { it.copy(medications = v) } })
        SwitchRow(stringResource(R.string.doctor_name), c.doctor, { v -> set { it.copy(doctor = v) } })
        SwitchRow(stringResource(R.string.home_address), c.address, { v -> set { it.copy(address = v) } })
        SwitchRow(stringResource(R.string.ice_note), c.note, { v -> set { it.copy(note = v) } })
    }
    SectionCard(stringResource(R.string.sharing_preview)) {
        val shared = vm.profiles.sharedFields(p, c)
        if (shared.isEmpty()) Text(stringResource(R.string.nothing_shared)) else shared.forEach { (k, v) -> Text("• $k: ${if (v is List<*>) v.joinToString(", ") else v}") }
        Text(stringResource(R.string.sharing_note), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SecuritySection(vm: AppViewModel) {
    val ctx = LocalContext.current
    val s by vm.settings.settings.collectAsStateWithLifecycle()
    var cancelPin by remember { mutableStateOf("") }
    var duressPin by remember { mutableStateOf("") }
    var cancelSet by remember { mutableStateOf(vm.store.hasPin("cancel")) }
    var duressSet by remember { mutableStateOf(vm.store.hasPin("duress")) }
    var pinErr by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    SectionCard(stringResource(R.string.sec_security)) {
        Text(stringResource(R.string.pin_hint), style = MaterialTheme.typography.bodySmall)
        Field(stringResource(if (cancelSet) R.string.cancel_pin_set else R.string.cancel_pin), cancelPin, { cancelPin = it.filter { c -> c.isDigit() }.take(8) }, keyboard = KeyboardType.NumberPassword)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = cancelPin.length >= 4, onClick = {
                if (cancelPin == duressPin || (duressSet && vm.store.checkPin("duress", cancelPin))) pinErr = R.string.err_pin_same
                else { vm.store.setPin("cancel", cancelPin); cancelPin = ""; cancelSet = true; pinErr = null }
            }) { Text(stringResource(R.string.save)) }
            if (cancelSet) OutlinedButton(onClick = { vm.store.setPin("cancel", null); cancelSet = false }) { Text(stringResource(R.string.remove)) }
        }
        Field(stringResource(if (duressSet) R.string.duress_pin_set else R.string.duress_pin), duressPin, { duressPin = it.filter { c -> c.isDigit() }.take(8) }, keyboard = KeyboardType.NumberPassword)
        Text(stringResource(R.string.duress_hint), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = duressPin.length >= 4, onClick = {
                if (duressPin == cancelPin || (cancelSet && vm.store.checkPin("cancel", duressPin))) pinErr = R.string.err_pin_same
                else { vm.store.setPin("duress", duressPin); duressPin = ""; duressSet = true; pinErr = null }
            }) { Text(stringResource(R.string.save)) }
            if (duressSet) OutlinedButton(onClick = { vm.store.setPin("duress", null); duressSet = false }) { Text(stringResource(R.string.remove)) }
        }
        pinErr?.let { Text(stringResource(it), color = VeraRed) }
        SwitchRow(stringResource(R.string.app_lock), s.appLock, { v -> vm.settings.update { it.copy(appLock = v) } }, hint = stringResource(R.string.app_lock_hint))
    }
    SectionCard(stringResource(R.string.your_data)) {
        Text(stringResource(R.string.data_hint), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = {
            vm.refreshEvents()
            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_TEXT, vm.exportData()), null))
        }) { Text(stringResource(R.string.export_data)) }
        Button(onClick = { confirmDelete = true }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = VeraRed)) { Text(stringResource(R.string.delete_all)) }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.delete_all)) }, text = { Text(stringResource(R.string.delete_confirm)) },
        confirmButton = { TextButton(onClick = { confirmDelete = false; vm.deleteAll { (ctx as? Activity)?.recreate() } }) { Text(stringResource(R.string.delete_all), color = VeraRed) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
}
