package ro.optometrist57.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.room.Room
import kotlinx.coroutines.launch
import ro.optometrist57.app.data.*
import ro.optometrist57.app.print.ReceiptRenderer
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { OptometristApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptometristApp() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("optometrist57", 0) }

    var patientName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf(prefs.getString("location", "") ?: "") }
    var odSph by remember { mutableStateOf("") }; var odCyl by remember { mutableStateOf("") }; var odAxis by remember { mutableStateOf("") }
    var osSph by remember { mutableStateOf("") }; var osCyl by remember { mutableStateOf("") }; var osAxis by remember { mutableStateOf("") }
    var dp by remember { mutableStateOf("") }; var add by remember { mutableStateOf("") }
    var vd by remember { mutableStateOf("") }; var va by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var photoLabel by remember { mutableStateOf("Nicio fotografie selectată") }
    var history by remember { mutableStateOf<List<ConsultationWithPatient>>(emptyList()) }
    var status by remember { mutableStateOf("Datele rămân pe acest telefon.") }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val dir = File(context.filesDir, "photos").apply { mkdirs() }
                val target = File(dir, "fisa-${System.currentTimeMillis()}.image")
                context.contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(target).use { output -> input.copyTo(output) } }
                target.absolutePath
            }.onSuccess { path -> photoPath = path; photoLabel = "Fotografie păstrată în arhiva locală" }
                .onFailure { status = "Nu am putut copia fotografia: ${it.localizedMessage}" }
        }
    }

    fun refresh() { scope.launch { history = db.consultations().allWithPatients() } }
    LaunchedEffect(Unit) { refresh() }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = { Text("OPTOMETRIST 57", fontWeight = FontWeight.Bold) })
    }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
            item {
                Text("CONSULTAȚIE NOUĂ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Introducere manuală – modulul AI offline va fi integrat în etapa următoare.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { OutlinedTextField(patientName, { patientName = it }, label = { Text("Nume pacient *") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item { OutlinedTextField(phone, { phone = it }, label = { Text("Telefon") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)) }
            item { OutlinedTextField(location, { location = it; prefs.edit().putString("location", it).apply() }, label = { Text("Locație (rămâne memorată)") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item { Text("OCHI DREPT – OD", fontWeight = FontWeight.Bold) }
            item { TripleFields("SPH", odSph, { odSph = it }, "CYL", odCyl, { odCyl = it }, "AX", odAxis, { odAxis = it }) }
            item { Text("OCHI STÂNG – OS", fontWeight = FontWeight.Bold) }
            item { TripleFields("SPH", osSph, { osSph = it }, "CYL", osCyl, { osCyl = it }, "AX", osAxis, { osAxis = it }) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(dp, { dp = it }, label = { Text("DP") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(add, { add = it }, label = { Text("ADD") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(vd, { vd = it }, label = { Text("VD") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(va, { va = it }, label = { Text("VA") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
            item { OutlinedTextField(notes, { notes = it }, label = { Text("Observații") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
            item {
                OutlinedButton(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text("Alege fotografia originală a fișei") }
                Text(photoLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Button(onClick = {
                    if (patientName.isBlank()) { status = "Completează numele pacientului."; return@Button }
                    scope.launch {
                        runCatching {
                            val existing = db.patients().find(patientName.trim(), phone.trim())
                            val patientId = existing?.id ?: db.patients().insert(PatientEntity(name = patientName.trim(), phone = phone.trim()))
                            val number = db.consultations().nextReceiptNumber()
                            val id = db.consultations().insert(ConsultationEntity(
                                patientId = patientId, receiptNumber = number, dateTime = System.currentTimeMillis(), location = location.trim(),
                                odSph = odSph.trim(), odCyl = odCyl.trim(), odAxis = odAxis.trim(), osSph = osSph.trim(), osCyl = osCyl.trim(), osAxis = osAxis.trim(),
                                dp = dp.trim(), add = add.trim(), vd = vd.trim(), va = va.trim(), notes = notes.trim(), originalPhotoPath = photoPath
                            ))
                            db.consultations().getWithPatient(id) ?: error("Consultația nu a putut fi recitită")
                        }.onSuccess { saved ->
                            status = "Consultația salvată. Bon nr. ${saved.consultation.receiptNumber}."
                            refresh()
                            val png = ReceiptRenderer.createPng(context, saved)
                            ReceiptRenderer.createPdf(context, saved)
                            ReceiptRenderer.sharePng(context, png)
                        }.onFailure { status = "Eroare la salvare: ${it.localizedMessage}" }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("SALVEAZĂ ȘI PREGĂTEȘTE BONUL") }
            }
            item { Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            item { HorizontalDivider(); Text("ISTORIC CONSULTAȚII", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(history, key = { it.consultation.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Bon ${item.consultation.receiptNumber} · ${item.patient.name}", fontWeight = FontWeight.Bold)
                        Text(SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ro", "RO")).format(Date(item.consultation.dateTime)), style = MaterialTheme.typography.bodySmall)
                        Text("OD ${item.consultation.odSph} / ${item.consultation.odCyl} × ${item.consultation.odAxis}    OS ${item.consultation.osSph} / ${item.consultation.osCyl} × ${item.consultation.osAxis}", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { val f = ReceiptRenderer.createPng(context, item); ReceiptRenderer.sharePng(context, f) }) { Text("RawBT") }
                            OutlinedButton(onClick = { val f = ReceiptRenderer.createPdf(context, item); ReceiptRenderer.sharePdf(context, f) }) { Text("PDF") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripleFields(l1: String, v1: String, c1: (String) -> Unit, l2: String, v2: String, c2: (String) -> Unit, l3: String, v3: String, c3: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        OutlinedTextField(v1, c1, label = { Text(l1) }, modifier = Modifier.weight(1f), singleLine = true)
        OutlinedTextField(v2, c2, label = { Text(l2) }, modifier = Modifier.weight(1f), singleLine = true)
        OutlinedTextField(v3, c3, label = { Text(l3) }, modifier = Modifier.weight(1f), singleLine = true)
    }
}
