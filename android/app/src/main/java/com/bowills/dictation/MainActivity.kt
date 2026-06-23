package com.bowills.dictation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Debug harness. The top section is the in-app record -> send -> show loop
 * (slices 1-3); the bottom section launches the system-wide floating mic
 * overlay (slice 4).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DictationScreen()
                }
            }
        }
    }
}

@Composable
private fun DictationScreen(vm: DictationViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()

    val micPermission = rememberMicPermissionLauncher(onGranted = vm::startRecording)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("VoiceText — debug loop", style = MaterialTheme.typography.titleLarge)
        Text(
            "Hold to record, release to transcribe. Backend: ${Config.BACKEND_BASE_URL}",
            style = MaterialTheme.typography.bodySmall,
        )

        when (state) {
            DictationUiState.Idle, is DictationUiState.Result, is DictationUiState.Error -> {
                Button(onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) vm.startRecording() else micPermission()
                }) { Text("Start recording") }
            }

            DictationUiState.Recording -> {
                Button(onClick = vm::stopAndSend) { Text("Stop & transcribe") }
            }

            DictationUiState.Sending -> {
                CircularProgressIndicator()
                Text("Transcribing…")
            }
        }

        when (val s = state) {
            is DictationUiState.Result -> {
                Text("Cleaned text", style = MaterialTheme.typography.titleMedium)
                Text(s.response.text)
                Text("Raw transcript", style = MaterialTheme.typography.titleMedium)
                Text(s.response.rawTranscript, style = MaterialTheme.typography.bodySmall)
                Text(
                    "asr=${s.response.asrModel}  cleanup=${s.response.cleanupModel}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            is DictationUiState.Error -> {
                Text("Error", style = MaterialTheme.typography.titleMedium)
                Text(s.message, color = MaterialTheme.colorScheme.error)
            }

            else -> Unit
        }

        HorizontalDivider()

        OverlaySection()
    }
}

/** Slice 4: start/stop the floating mic overlay service. */
@Composable
private fun OverlaySection() {
    val context = LocalContext.current
    val notifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* best effort; service runs regardless */ }
    val overlaySettings = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { /* user returns from the settings screen; they tap Start again */ }

    Text("Floating mic (system-wide)", style = MaterialTheme.typography.titleMedium)
    Text(
        "Shows a draggable mic over other apps. For now the cleaned text is copied " +
            "to the clipboard (text injection comes in the next slice).",
        style = MaterialTheme.typography.bodySmall,
    )

    Button(onClick = {
        // 1. Overlay permission must be granted in system settings.
        if (!Settings.canDrawOverlays(context)) {
            Toast.makeText(
                context,
                "Grant \"display over other apps\", then tap Start again.",
                Toast.LENGTH_LONG,
            ).show()
            overlaySettings.launch(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}"),
                ),
            )
            return@Button
        }
        // 2. Mic permission.
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Grant the mic permission, then tap Start again.", Toast.LENGTH_LONG).show()
            // Reuse the activity-result API via a one-shot request.
            (context as? ComponentActivity)?.let {
                ActivityCompatRequestMic(it)
            }
            return@Button
        }
        // 3. Notifications (API 33+) — best effort, foreground service needs it visible.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        OverlayDictationService.start(context)
    }) { Text("Start floating mic") }

    OutlinedButton(onClick = { OverlayDictationService.stop(context) }) {
        Text("Stop floating mic")
    }
}

/** Requests RECORD_AUDIO via the platform API (used from the overlay flow). */
private fun ActivityCompatRequestMic(activity: ComponentActivity) {
    androidx.core.app.ActivityCompat.requestPermissions(
        activity, arrayOf(Manifest.permission.RECORD_AUDIO), 0,
    )
}

/** Returns a callback that requests RECORD_AUDIO and starts recording when granted. */
@Composable
private fun rememberMicPermissionLauncher(onGranted: () -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) onGranted() }
    return { launcher.launch(Manifest.permission.RECORD_AUDIO) }
}
