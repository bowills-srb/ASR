package com.bowills.dictation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Slice 1-3 debug harness: record a turn, POST it to the backend, and show the
 * cleaned text. This proves the full audio -> ASR -> cleanup loop from a phone,
 * before the overlay + accessibility injection are built.
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
    val context = androidx.compose.ui.platform.LocalContext.current
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
    }
}

/** Returns a callback that requests RECORD_AUDIO and starts recording when granted. */
@Composable
private fun rememberMicPermissionLauncher(onGranted: () -> Unit): () -> Unit {
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) onGranted() }
    return { launcher.launch(Manifest.permission.RECORD_AUDIO) }
}
