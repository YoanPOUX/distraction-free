package app.spotlightguard.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.spotlightguard.R
import app.spotlightguard.core.Feature
import app.spotlightguard.data.DiagnosticStore
import app.spotlightguard.data.SettingsStore

/** Deux niveaux : la liste des fonctions, puis les réglages d'une fonction. */
@Composable
fun MainScreen() {
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = Feature.entries.firstOrNull { it.name == selectedName }

    BackHandler(enabled = selected != null) { selectedName = null }

    if (selected == null) {
        HomeScreen(onOpen = { selectedName = it.name })
    } else {
        FeatureScreen(feature = selected, onBack = { selectedName = null })
    }
}

@Composable
private fun HomeScreen(onOpen: (Feature) -> Unit) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore.get(context) }
    val s by settingsStore.state.collectAsState()
    var serviceOn by remember { mutableStateOf(isServiceEnabled(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) serviceOn = isServiceEnabled(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            color = colors.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        ServiceStatus(on = serviceOn) {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        Spacer(Modifier.height(32.dp))
        SectionLabel(stringResource(R.string.section_features))
        Divider()
        Feature.entries.forEach { feature ->
            val config = s.of(feature)
            FeatureRow(
                title = featureTitle(feature),
                summary = featureSummary(feature, config),
                checked = config.enabled,
                onToggle = { v -> settingsStore.setEnabled(feature, v) },
                onOpen = { onOpen(feature) },
            )
            Divider()
        }

        Spacer(Modifier.height(16.dp))
        ToggleRow(
            title = stringResource(R.string.linked_title),
            detail = stringResource(R.string.linked_detail),
            checked = s.linked,
        ) { v -> settingsStore.update { it.copy(linked = v) } }
        Divider()

        Spacer(Modifier.height(24.dp))
        ToggleRow(
            title = stringResource(R.string.diagnostic),
            detail = stringResource(R.string.diagnostic_detail),
            checked = s.diagnosticEnabled,
        ) { v -> settingsStore.update { it.copy(diagnosticEnabled = v) } }

        if (s.diagnosticEnabled) {
            val dump by DiagnosticStore.dump.collectAsState()
            val emptyText = stringResource(R.string.diagnostic_empty)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceVariant)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                Text(
                    text = dump.ifEmpty { emptyText },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = colors.onSurfaceVariant,
                )
            }
            TextButton(onClick = { copyToClipboard(context, dump) }) {
                Text(stringResource(R.string.copy), color = colors.onBackground)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
