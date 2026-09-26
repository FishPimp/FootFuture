package se.storleksprognosen.app.ui.inventory

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import se.storleksprognosen.app.R
import se.storleksprognosen.app.ui.components.ConfirmDeleteDialog
import se.storleksprognosen.app.ui.components.categoryEmoji
import se.storleksprognosen.app.ui.components.categoryName
import se.storleksprognosen.app.ui.components.genderEmoji
import se.storleksprognosen.app.ui.components.seasonEmoji
import se.storleksprognosen.app.ui.components.seasonName
import se.storleksprognosen.app.ui.theme.PillShape
import se.storleksprognosen.domain.model.Category
import se.storleksprognosen.domain.model.Child
import se.storleksprognosen.domain.model.InventoryItem
import se.storleksprognosen.domain.model.Season
import se.storleksprognosen.domain.sizing.ClothingSizes
import se.storleksprognosen.domain.sizing.ShoeSizes

private fun selectableSizes(category: Category): List<Int> = when (category) {
    Category.SHOES -> ShoeSizes.SELECTABLE.toList()
    Category.CLOTHES -> ClothingSizes.SELECTABLE
}

private fun defaultSize(category: Category): Int = when (category) {
    Category.SHOES -> 24
    Category.CLOTHES -> 98
}

@Composable
fun ItemDialog(
    existing: InventoryItem?,
    children: List<Child>,
    onSave: (InventoryItem) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(existing?.title.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: Category.SHOES) }
    var size by rememberSaveable { mutableIntStateOf(existing?.size ?: defaultSize(Category.SHOES)) }
    var season by rememberSaveable { mutableStateOf(existing?.season ?: Season.WINTER) }
    var ownerId by rememberSaveable { mutableStateOf(existing?.ownerChildId ?: children.firstOrNull()?.id) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val sizes = selectableSizes(category)
    val sizeIndex = sizes.indexOf(size).takeIf { it >= 0 } ?: sizes.indexOfFirst { it >= size }.coerceAtLeast(0)
    val titleError = title.isBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.item_dialog_new else R.string.item_dialog_edit)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.item_title)) },
                    placeholder = { Text(stringResource(R.string.item_title_placeholder)) },
                    singleLine = true,
                    isError = showErrors && titleError,
                    supportingText = if (showErrors && titleError) {
                        { Text(stringResource(R.string.error_title_required)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(stringResource(R.string.item_category), style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    Category.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = category == option,
                            onClick = {
                                if (category != option) {
                                    category = option
                                    size = defaultSize(option)
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, Category.entries.size),
                            label = { Text("${categoryEmoji(option)} ${categoryName(option)}") },
                        )
                    }
                }

                Text(stringResource(R.string.item_size), style = MaterialTheme.typography.titleSmall)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FilledTonalIconButton(
                        onClick = { size = sizes[(sizeIndex - 1).coerceAtLeast(0)] },
                        enabled = sizeIndex > 0,
                    ) { Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.action_decrease)) }
                    AnimatedContent(
                        targetState = sizes[sizeIndex],
                        transitionSpec = {
                            scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn() togetherWith fadeOut()
                        },
                        label = "itemSize",
                    ) { value ->
                        Text(
                            stringResource(R.string.size_label, value),
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(120.dp),
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { size = sizes[(sizeIndex + 1).coerceAtMost(sizes.lastIndex)] },
                        enabled = sizeIndex < sizes.lastIndex,
                    ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_increase)) }
                }

                Text(stringResource(R.string.item_season), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Season.entries.forEach { option ->
                        FilterChip(
                            selected = season == option,
                            onClick = { season = option },
                            label = { Text("${seasonEmoji(option)} ${seasonName(option)}") },
                            shape = PillShape,
                        )
                    }
                }

                if (children.isNotEmpty()) {
                    Text(stringResource(R.string.item_owner), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        children.forEach { child ->
                            FilterChip(
                                selected = ownerId == child.id,
                                onClick = { ownerId = child.id },
                                label = { Text("${genderEmoji(child.gender)} ${child.name}") },
                                shape = PillShape,
                            )
                        }
                    }
                }

                if (existing != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val owner = ownerId
                if (titleError || owner == null) {
                    showErrors = true
                } else {
                    onSave(
                        InventoryItem(
                            id = existing?.id ?: InventoryViewModel.newId(),
                            ownerChildId = owner,
                            title = title.trim(),
                            category = category,
                            size = sizes[sizeIndex],
                            season = season,
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
            title = stringResource(R.string.item_delete_title),
            message = null,
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
