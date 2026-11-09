package app.spotlightguard.ui

import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spotlightguard.R
import app.spotlightguard.core.Feature
import app.spotlightguard.core.FeatureConfig
import app.spotlightguard.core.Mode
import app.spotlightguard.service.BlockerService

@Composable
internal fun featureTitle(feature: Feature): String = stringResource(
    when (feature) {
        Feature.SPOTLIGHT -> R.string.feature_spotlight
        Feature.REELS -> R.string.feature_reels
        Feature.DMS -> R.string.feature_dms
        Feature.STORIES -> R.string.feature_stories
    },
)

/** Une ligne résumant les réglages actifs d'une fonction. */
@Composable
internal fun featureSummary(feature: Feature, config: FeatureConfig): String {
    val off = stringResource(R.string.summary_off)
    val total = stringResource(R.string.summary_total)
    val noRules = stringResource(R.string.summary_no_rules)
    val daily = stringResource(R.string.summary_daily, config.dailyBudgetMinutes)
    val session = stringResource(R.string.summary_session, config.sessionLimitMinutes)
    val dmsAlways = stringResource(R.string.summary_dms_always)
    val dmsAfter = stringResource(R.string.summary_dms_after, config.reopenAfterMinutes)

    if (!config.enabled) return off
    if (feature == Feature.DMS) return if (config.reopenAfterMinutes == 0) dmsAlways else dmsAfter
    if (config.mode == Mode.TOTAL) return total

    val window = "%s–%s".format(formatMinute(config.windowStartMinute), formatMinute(config.windowEndMinute))
    val parts = listOfNotNull(
        daily.takeIf { config.dailyBudgetEnabled },
        session.takeIf { config.sessionLimitEnabled },
        window.takeIf { config.timeWindowEnabled },
    )
    return if (parts.isEmpty()) noRules else parts.joinToString(" · ")
}

internal fun formatMinute(minuteOfDay: Int): String = "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

@Composable
internal fun ServiceStatus(on: Boolean, onOpenSettings: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (on) colors.onBackground else colors.outline),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(if (on) R.string.service_on else R.string.service_off),
                fontSize = 14.sp,
                color = colors.onSurfaceVariant,
            )
        }
        if (!on) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpenSettings, shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.service_enable))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.service_hint),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
internal fun Divider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
internal fun ToggleRow(
    title: String,
    detail: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = colors.onBackground)
            if (detail != null) {
                Text(text = detail, fontSize = 13.sp, color = colors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Ligne de la liste d'accueil : toucher la ligne ouvre les réglages, l'interrupteur active. */
@Composable
internal fun FeatureRow(
    title: String,
    summary: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpen: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 17.sp, color = colors.onBackground)
            Spacer(Modifier.height(2.dp))
            Text(text = summary, fontSize = 13.sp, color = colors.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

/** Ligne de règle : titre + interrupteur, et le réglage dessous quand elle est active. */
@Composable
internal fun RuleRow(
    title: String,
    detail: String,
    enabled: Boolean,
    onEnabled: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ToggleRow(title = title, detail = detail, checked = enabled, onChange = onEnabled)
        if (enabled) {
            Box(modifier = Modifier.padding(bottom = 12.dp)) { content() }
        }
    }
}

@Composable
internal fun Stepper(
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    onChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton("−") { onChange((value - step).coerceAtLeast(min)) }
        Text(
            text = stringResource(R.string.minutes_fmt, value),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.widthIn(min = 72.dp),
        )
        StepButton("+") { onChange((value + step).coerceAtMost(max)) }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
internal fun TimeChip(minuteOfDay: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = formatMinute(minuteOfDay),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
internal fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant)
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) colors.background else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) colors.onBackground else colors.onSurfaceVariant,
                )
            }
        }
    }
}

internal fun pickTime(context: Context, minuteOfDay: Int, onPick: (Int) -> Unit) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onPick(hour * 60 + minute) },
        minuteOfDay / 60,
        minuteOfDay % 60,
        true,
    ).show()
}

internal fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("diagnostic", text))
}

internal fun isServiceEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    val component = ComponentName(context, BlockerService::class.java)
    val candidates = listOf(component.flattenToString(), component.flattenToShortString())
    return enabled.split(':').any { entry -> candidates.any { it.equals(entry, ignoreCase = true) } }
}
