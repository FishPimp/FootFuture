package se.storleksprognosen.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.storleksprognosen.app.R
import se.storleksprognosen.app.ui.theme.PillShape
import se.storleksprognosen.app.ui.theme.extendedColors
import se.storleksprognosen.domain.model.Child
import se.storleksprognosen.domain.model.Gender
import se.storleksprognosen.domain.model.Season
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

enum class BadgeKind { MATCH, MISMATCH, GAP, NEUTRAL }

/** Rund statusbricka (🟢 match, 🔴 missmatch, ⚪ lucka) som studsar in när den visas. */
@Composable
fun StatusBadge(
    kind: BadgeKind,
    text: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
) {
    val colors = MaterialTheme.extendedColors
    val (container, content) = when (kind) {
        BadgeKind.MATCH -> colors.match to colors.onMatch
        BadgeKind.MISMATCH -> colors.mismatch to colors.onMismatch
        BadgeKind.GAP -> colors.gap to colors.onGap
        BadgeKind.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val dot = icon ?: when (kind) {
        BadgeKind.MATCH -> "🟢"
        BadgeKind.MISMATCH -> "🔴"
        BadgeKind.GAP -> "⚪"
        BadgeKind.NEUTRAL -> "•"
    }
    var appeared by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "badgeScale",
    )
    LaunchedEffect(Unit) { appeared = true }

    Surface(
        modifier = modifier.scale(scale),
        shape = PillShape,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(dot, fontSize = 12.sp)
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Säsongsbricka med säsongens egen färg. */
@Composable
fun SeasonChip(season: Season, modifier: Modifier = Modifier, label: String = seasonName(season)) {
    val colors = MaterialTheme.extendedColors
    val (container, content) = when (season) {
        Season.WINTER -> colors.winter to colors.onWinter
        Season.SUMMER -> colors.summer to colors.onSummer
        Season.SPRING_FALL -> colors.springFall to colors.onSpringFall
        Season.ALL_YEAR -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(modifier = modifier, shape = PillShape, color = container, contentColor = content) {
        Text(
            text = "${seasonEmoji(season)} $label",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Rund avatar med barnets emoji. */
@Composable
fun ChildAvatar(child: Child, modifier: Modifier = Modifier, size: Int = 40, selected: Boolean = false) {
    val target = when {
        selected -> MaterialTheme.colorScheme.primary
        child.gender == Gender.GIRL -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val background by animateColorAsState(target, spring(stiffness = Spring.StiffnessMediumLow), label = "avatar")
    Box(
        modifier = modifier
            .size(size.dp)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(genderEmoji(child.gender), fontSize = (size * 0.5f).sp)
    }
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 52.sp)
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        actions()
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** Ett datumfält som öppnar Material 3:s datumväljare. */
@Composable
fun DatePickerField(
    label: String,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    maxDate: LocalDate? = null,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    // Hela fältet öppnar datumväljaren, inte bara ikonen.
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { if (it is PressInteraction.Release) open = true }
    }
    OutlinedTextField(
        value = fullDate(date),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        trailingIcon = {
            IconButton(onClick = { open = true }) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = stringResource(R.string.date_pick))
            }
        },
        interactionSource = interactionSource,
        modifier = modifier.fillMaxWidth(),
    )
    if (open) {
        val maxMillis = maxDate?.atStartOfDay()?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    maxMillis == null || utcTimeMillis <= maxMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    open = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = message?.let { { Text(it) } },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Standardutfyllnad för listor ovanför bottennavigeringen och FAB:en. */
val ListContentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
