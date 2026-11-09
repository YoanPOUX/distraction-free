package app.spotlightguard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spotlightguard.R
import app.spotlightguard.core.Feature
import app.spotlightguard.core.FeatureConfig
import app.spotlightguard.core.Mode
import app.spotlightguard.core.Usage
import app.spotlightguard.data.SettingsStore
import app.spotlightguard.data.UsageStore

@Composable
fun FeatureScreen(feature: Feature, onBack: () -> Unit) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore.get(context) }
    val usageStore = remember { UsageStore.get(context) }
    val s by settingsStore.state.collectAsState()
    val usages by usageStore.state.collectAsState()
    LaunchedEffect(Unit) { usageStore.refresh() }

    val colors = MaterialTheme.colorScheme
    val config = s.of(feature)
    val update: ((FeatureConfig) -> FeatureConfig) -> Unit = { transform ->
        settingsStore.edit(feature, transform)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = "←",
            fontSize = 24.sp,
            color = colors.onBackground,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .padding(horizontal = 4.dp, vertical = 8.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = featureTitle(feature),
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            color = colors.onBackground,
        )
        if (feature == Feature.DMS || feature == Feature.CHATS) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.feature_dms_detail),
                fontSize = 14.sp,
                color = colors.onSurfaceVariant,
            )
        }
        if (s.linked) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.linked_hint),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = colors.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))
        Divider()
        ToggleRow(
            title = stringResource(R.string.enabled),
            detail = null,
            checked = config.enabled,
        ) { v -> settingsStore.setEnabled(feature, v) }
        Divider()

        if (feature == Feature.DMS || feature == Feature.CHATS) {
            DmSettings(config, update)
        } else {
            BlockingSettings(config, usages[feature] ?: Usage(), update)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BlockingSettings(
    config: FeatureConfig,
    usage: Usage,
    update: ((FeatureConfig) -> FeatureConfig) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Spacer(Modifier.height(24.dp))
    SectionLabel(stringResource(R.string.section_mode))
    Segmented(
        options = listOf(stringResource(R.string.mode_total), stringResource(R.string.mode_rules)),
        selected = config.mode.ordinal,
    ) { i -> update { it.copy(mode = Mode.entries[i]) } }

    if (config.mode == Mode.RULES) {
        Spacer(Modifier.height(24.dp))
        SectionLabel(stringResource(R.string.section_rules))

        RuleRow(
            title = stringResource(R.string.rule_daily),
            detail = stringResource(R.string.rule_daily_detail),
            enabled = config.dailyBudgetEnabled,
            onEnabled = { v -> update { it.copy(dailyBudgetEnabled = v) } },
        ) {
            Stepper(config.dailyBudgetMinutes, min = 5, max = 480, step = 5) { v ->
                update { it.copy(dailyBudgetMinutes = v) }
            }
        }
        Divider()

        RuleRow(
            title = stringResource(R.string.rule_session),
            detail = stringResource(R.string.rule_session_detail),
            enabled = config.sessionLimitEnabled,
            onEnabled = { v -> update { it.copy(sessionLimitEnabled = v) } },
        ) {
            Stepper(config.sessionLimitMinutes, min = 1, max = 120, step = 1) { v ->
                update { it.copy(sessionLimitMinutes = v) }
            }
        }
        Divider()

        RuleRow(
            title = stringResource(R.string.rule_cooldown),
            detail = stringResource(R.string.rule_cooldown_detail),
            enabled = config.cooldownEnabled,
            onEnabled = { v -> update { it.copy(cooldownEnabled = v) } },
        ) {
            Stepper(config.cooldownMinutes, min = 5, max = 720, step = 5) { v ->
                update { it.copy(cooldownMinutes = v) }
            }
        }
        Divider()

        WindowRule(
            config = config,
            detail = stringResource(R.string.rule_window_detail),
            update = update,
        )
        Divider()
    }

    Spacer(Modifier.height(32.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.today),
            fontSize = 15.sp,
            color = colors.onSurfaceVariant,
        )
        val used = stringResource(R.string.minutes_fmt, (usage.dailyMs / 60_000L).toInt())
        val budget = stringResource(R.string.minutes_fmt, config.dailyBudgetMinutes)
        val label = if (config.mode == Mode.RULES && config.dailyBudgetEnabled) "$used / $budget" else used
        Text(text = label, fontSize = 15.sp, color = colors.onBackground)
    }
}

@Composable
private fun DmSettings(
    config: FeatureConfig,
    update: ((FeatureConfig) -> FeatureConfig) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Spacer(Modifier.height(24.dp))
    SectionLabel(stringResource(R.string.section_timing))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.reopen_title),
                fontSize = 15.sp,
                color = colors.onBackground,
            )
            Text(
                text = stringResource(R.string.reopen_detail),
                fontSize = 12.sp,
                color = colors.onSurfaceVariant,
            )
        }
        Stepper(config.reopenAfterMinutes, min = 0, max = 120, step = 5) { v ->
            update { it.copy(reopenAfterMinutes = v) }
        }
    }

    Spacer(Modifier.height(16.dp))
    Divider()
    WindowRule(
        config = config,
        detail = stringResource(R.string.rule_window_dms_detail),
        update = update,
    )
    Divider()
}

/** Règle « plage horaire » : deux heures à choisir avec le sélecteur du système. */
@Composable
private fun WindowRule(
    config: FeatureConfig,
    detail: String,
    update: ((FeatureConfig) -> FeatureConfig) -> Unit,
) {
    val context = LocalContext.current
    RuleRow(
        title = stringResource(R.string.rule_window),
        detail = detail,
        enabled = config.timeWindowEnabled,
        onEnabled = { v -> update { it.copy(timeWindowEnabled = v) } },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeChip(config.windowStartMinute) {
                pickTime(context, config.windowStartMinute) { m ->
                    update { it.copy(windowStartMinute = m) }
                }
            }
            Text(
                text = "→",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            TimeChip(config.windowEndMinute) {
                pickTime(context, config.windowEndMinute) { m ->
                    update { it.copy(windowEndMinute = m) }
                }
            }
        }
    }
}
