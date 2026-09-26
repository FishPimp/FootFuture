package se.storleksprognosen.app.ui.forecast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import se.storleksprognosen.app.R
import se.storleksprognosen.app.ui.components.ConfirmDeleteDialog
import se.storleksprognosen.app.ui.components.DatePickerField
import se.storleksprognosen.app.ui.components.formatDecimal
import se.storleksprognosen.app.ui.components.genderEmoji
import se.storleksprognosen.app.ui.components.parseDecimal
import se.storleksprognosen.domain.model.Child
import se.storleksprognosen.domain.model.Gender
import se.storleksprognosen.domain.model.Measurement
import java.time.LocalDate

@Composable
fun ChildDialog(
    existing: Child?,
    today: LocalDate,
    onSave: (Child) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var gender by rememberSaveable { mutableStateOf(existing?.gender ?: Gender.GIRL) }
    var birthdate by rememberSaveable { mutableStateOf(existing?.birthdate ?: today.minusYears(1)) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val nameError = name.isBlank()
    val dateError = birthdate.isAfter(today)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.child_dialog_new else R.string.child_dialog_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.child_name)) },
                    singleLine = true,
                    isError = showErrors && nameError,
                    supportingText = if (showErrors && nameError) {
                        { Text(stringResource(R.string.error_name_required)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    Gender.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = gender == option,
                            onClick = { gender = option },
                            shape = SegmentedButtonDefaults.itemShape(index, Gender.entries.size),
                            label = {
                                Text("${genderEmoji(option)} ${stringResource(if (option == Gender.GIRL) R.string.gender_girl else R.string.gender_boy)}")
                            },
                        )
                    }
                }
                DatePickerField(
                    label = stringResource(R.string.child_birthdate),
                    date = birthdate,
                    onDateChange = { birthdate = it },
                    maxDate = today,
                    isError = dateError,
                    supportingText = if (dateError) stringResource(R.string.error_birthdate_future) else null,
                )
                if (existing != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nameError || dateError) {
                    showErrors = true
                } else {
                    onSave(
                        Child(
                            id = existing?.id ?: ForecastViewModel.newId(),
                            name = name.trim(),
                            gender = gender,
                            birthdate = birthdate,
                        )
                    )
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )

    if (confirmDelete && existing != null) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.child_delete_title, existing.name),
            message = stringResource(R.string.child_delete_message),
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
fun MeasurementDialog(
    child: Child,
    existing: Measurement?,
    today: LocalDate,
    onSave: (Measurement) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var date by rememberSaveable { mutableStateOf(existing?.date ?: today) }
    var heightText by rememberSaveable { mutableStateOf(existing?.heightCm?.let { formatDecimal(it, 1) }.orEmpty()) }
    var footText by rememberSaveable { mutableStateOf(existing?.footMm?.let { formatDecimal(it, 0) }.orEmpty()) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val height = parseDecimal(heightText)
    val foot = parseDecimal(footText)
    val heightError = heightText.isNotBlank() && (height == null || height !in 30f..150f)
    val footError = footText.isNotBlank() && (foot == null || foot !in 50f..250f)
    val bothEmpty = heightText.isBlank() && footText.isBlank()
    val dateError = when {
        date.isBefore(child.birthdate) -> R.string.error_date_before_birth
        date.isAfter(today) -> R.string.error_date_future
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.measurement_dialog_new else R.string.measurement_dialog_edit)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                DatePickerField(
                    label = stringResource(R.string.measurement_date),
                    date = date,
                    onDateChange = { date = it },
                    maxDate = today,
                    isError = dateError != null,
                    supportingText = dateError?.let { stringResource(it) },
                )
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it },
                    label = { Text(stringResource(R.string.measurement_height)) },
                    singleLine = true,
                    isError = showErrors && heightError,
                    supportingText = if (showErrors && heightError) {
                        { Text(stringResource(R.string.error_height_range)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = footText,
                    onValueChange = { footText = it },
                    label = { Text(stringResource(R.string.measurement_foot)) },
                    singleLine = true,
                    isError = showErrors && footError,
                    supportingText = if (showErrors && footError) {
                        { Text(stringResource(R.string.error_foot_range)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.measurement_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (showErrors && bothEmpty) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (existing != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (heightError || footError || bothEmpty || dateError != null) {
                    showErrors = true
                } else {
                    onSave(
                        Measurement(
                            id = existing?.id ?: ForecastViewModel.newId(),
                            childId = child.id,
                            date = date,
                            footMm = foot,
                            heightCm = height,
                        )
                    )
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )

    if (confirmDelete) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.measurement_delete_title),
            message = null,
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
