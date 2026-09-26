package se.storleksprognosen.app.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import se.storleksprognosen.app.R
import se.storleksprognosen.app.ui.components.BadgeKind
import se.storleksprognosen.app.ui.components.ChildAvatar
import se.storleksprognosen.app.ui.components.EmptyState
import se.storleksprognosen.app.ui.components.SeasonChip
import se.storleksprognosen.app.ui.components.SectionHeader
import se.storleksprognosen.app.ui.components.StatusBadge
import se.storleksprognosen.app.ui.components.categoryEmoji
import se.storleksprognosen.app.ui.components.categoryName
import se.storleksprognosen.app.ui.components.monthYear
import se.storleksprognosen.app.ui.components.periodInSentence
import se.storleksprognosen.app.ui.components.periodLabel
import se.storleksprognosen.app.ui.components.phaseEmoji
import se.storleksprognosen.app.ui.theme.CardShape
import se.storleksprognosen.app.ui.theme.PillShape
import se.storleksprognosen.app.ui.theme.extendedColors
import se.storleksprognosen.domain.matcher.ItemEvaluation
import se.storleksprognosen.domain.matcher.MatchStatus
import se.storleksprognosen.domain.matcher.SeasonNeed
import se.storleksprognosen.domain.model.Category
import se.storleksprognosen.domain.model.Child
import se.storleksprognosen.domain.model.InventoryItem
import se.storleksprognosen.domain.model.Season
import se.storleksprognosen.domain.season.SeasonCalendar
import se.storleksprognosen.domain.season.SeasonPeriod
import java.time.LocalDate

private enum class InventoryTab { ITEMS, MATRIX }

private sealed interface InventoryDialog {
    data class EditItem(val item: InventoryItem?) : InventoryDialog
    data class NeedDetails(val child: Child, val period: SeasonPeriod) : InventoryDialog
}

@Composable
fun InventoryScreen(viewModel: InventoryViewModel = viewModel(factory = InventoryViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(InventoryTab.ITEMS) }
    var filter by rememberSaveable { mutableStateOf<Category?>(null) }
    var dialog by remember { mutableStateOf<InventoryDialog?>(null) }
    val today = remember { LocalDate.now() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.nav_inventory), style = MaterialTheme.typography.headlineMedium)
                        Text(
                            stringResource(R.string.inventory_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.children.isNotEmpty() && tab == InventoryTab.ITEMS) {
                ExtendedFloatingActionButton(
                    onClick = { dialog = InventoryDialog.EditItem(null) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.action_add_item)) },
                    shape = PillShape,
                )
            }
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding))
            state.children.isEmpty() -> EmptyState(
                emoji = "📦",
                title = stringResource(R.string.inventory_no_children_title),
                message = stringResource(R.string.inventory_no_children_message),
                modifier = Modifier.padding(padding),
            )
            else -> Column(modifier = Modifier.padding(top = padding.calculateTopPadding())) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    InventoryTab.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = tab == option,
                            onClick = { tab = option },
                            shape = SegmentedButtonDefaults.itemShape(index, InventoryTab.entries.size),
                            label = {
                                Text(
                                    stringResource(
                                        if (option == InventoryTab.ITEMS) R.string.inventory_tab_items else R.string.inventory_tab_matrix
                                    )
                                )
                            },
                        )
                    }
                }
                when (tab) {
                    InventoryTab.ITEMS -> ItemsList(
                        state = state,
                        filter = filter,
                        onFilterChange = { filter = it },
                        today = today,
                        onItemClick = { dialog = InventoryDialog.EditItem(it) },
                    )
                    InventoryTab.MATRIX -> SeasonMatrix(
                        state = state,
                        onCellClick = { child, period -> dialog = InventoryDialog.NeedDetails(child, period) },
                    )
                }
            }
        }
    }

    when (val current = dialog) {
        null -> Unit
        is InventoryDialog.EditItem -> ItemDialog(
            existing = current.item,
            children = state.children,
            onSave = {
                viewModel.saveItem(it)
                dialog = null
            },
            onDelete = {
                current.item?.let(viewModel::deleteItem)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        is InventoryDialog.NeedDetails -> NeedDetailsDialog(
            child = current.child,
            period = current.period,
            needs = state.needs.filter { it.child.id == current.child.id && it.period == current.period },
            onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun ItemsList(
    state: InventoryUiState,
    filter: Category?,
    onFilterChange: (Category?) -> Unit,
    today: LocalDate,
    onItemClick: (InventoryItem) -> Unit,
) {
    val childrenById = state.children.associateBy { it.id }
    val visible = state.evaluations.filter { filter == null || it.item.category == filter }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "filters") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter == null,
                    onClick = { onFilterChange(null) },
                    label = { Text(stringResource(R.string.filter_all)) },
                    shape = PillShape,
                )
                Category.entries.forEach { category ->
                    FilterChip(
                        selected = filter == category,
                        onClick = { onFilterChange(category) },
                        label = { Text("${categoryEmoji(category)} ${categoryName(category)}") },
                        shape = PillShape,
                    )
                }
            }
        }
        if (state.evaluations.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    emoji = "🧺",
                    title = stringResource(R.string.inventory_empty_title),
                    message = stringResource(R.string.inventory_empty_message),
                )
            }
        }
        items(visible, key = { it.item.id }) { evaluation ->
            ItemCard(
                evaluation = evaluation,
                ownerName = childrenById[evaluation.item.ownerChildId]?.name.orEmpty(),
                today = today,
                onClick = { onItemClick(evaluation.item) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun ItemCard(
    evaluation: ItemEvaluation,
    ownerName: String,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = evaluation.item
    ElevatedCard(
        onClick = onClick,
        shape = CardShape,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(categoryEmoji(item.category), fontSize = 26.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(R.string.item_subtitle, stringResource(R.string.size_label, item.size), ownerName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SeasonChip(item.season)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                evaluation.statuses.forEach { MatchStatusBadge(it, today) }
            }
        }
    }
}

@Composable
private fun MatchStatusBadge(status: MatchStatus, today: LocalDate) {
    val name = status.child.name
    when (status) {
        is MatchStatus.Match -> {
            val period = status.period
            val text = when {
                period != null -> stringResource(R.string.status_match, name, periodInSentence(period), status.size)
                !status.from.isAfter(today) -> stringResource(R.string.status_match_now, name, status.size)
                else -> stringResource(R.string.status_match_from, name, monthYear(status.from), status.size)
            }
            StatusBadge(BadgeKind.MATCH, text)
        }
        is MatchStatus.WrongSeason -> {
            val reaches = status.reachesOn
            val outgrows = status.outgrowsOn
            val text = when {
                reaches != null -> stringResource(R.string.status_reaches, name, status.size, monthYear(reaches))
                outgrows != null -> stringResource(R.string.status_outgrows, name, status.size, monthYear(outgrows))
                else -> stringResource(R.string.status_wrong_season, name, status.size)
            }
            // En lekfull sol när storleken infaller på sommaren, annars en varning.
            val date = reaches ?: outgrows
            val icon = if (date != null && SeasonCalendar.seasonOf(date) == Season.SUMMER) "☀️" else "⚠️"
            StatusBadge(BadgeKind.MISMATCH, text, icon = icon)
        }
        is MatchStatus.Outgrown -> StatusBadge(BadgeKind.MISMATCH, stringResource(R.string.status_outgrown, name, status.size))
        is MatchStatus.BeyondForecast -> StatusBadge(BadgeKind.NEUTRAL, stringResource(R.string.status_beyond, name))
    }
}

@Composable
private fun SeasonMatrix(
    state: InventoryUiState,
    onCellClick: (Child, SeasonPeriod) -> Unit,
) {
    val needsByKey = state.needs.associateBy { Triple(it.child.id, it.period, it.category) }
    val gaps = state.needs.filter { it.isGap }
    val covered = state.needs.filter { !it.isGap }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "hint") {
            Text(
                stringResource(R.string.matrix_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.needs.isEmpty()) {
            item(key = "noPeriods") {
                Text(stringResource(R.string.matrix_no_periods), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            item(key = "grid") {
                ElevatedCard(
                    shape = CardShape,
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    MatrixGrid(
                        children = state.children,
                        periods = state.periods,
                        needFor = { child, period, category -> needsByKey[Triple(child.id, period, category)] },
                        onCellClick = onCellClick,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            item(key = "legend") {
                Text(stringResource(R.string.matrix_legend), style = MaterialTheme.typography.labelMedium)
            }
            item(key = "gapsHeader") {
                SectionHeader(stringResource(R.string.matrix_gaps_title))
            }
            if (gaps.isEmpty()) {
                item(key = "noGaps") { Text(stringResource(R.string.matrix_no_gaps)) }
            }
            state.children.forEach { child ->
                val childGaps = gaps.filter { it.child.id == child.id }
                if (childGaps.isNotEmpty()) {
                    item(key = "gaps-${child.id}") {
                        NeedGroup(child = child, needs = childGaps)
                    }
                }
            }
            if (covered.isNotEmpty()) {
                item(key = "coveredHeader") { SectionHeader(stringResource(R.string.matrix_covered_title)) }
                state.children.forEach { child ->
                    val childCovered = covered.filter { it.child.id == child.id }
                    if (childCovered.isNotEmpty()) {
                        item(key = "covered-${child.id}") {
                            NeedGroup(child = child, needs = childCovered)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NeedGroup(child: Child, needs: List<SeasonNeed>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChildAvatar(child, size = 28)
            Spacer(Modifier.width(8.dp))
            Text(child.name, style = MaterialTheme.typography.titleSmall)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            needs.forEach { NeedBadge(it) }
        }
    }
}

@Composable
private fun NeedBadge(need: SeasonNeed) {
    val prefix = categoryEmoji(need.category)
    if (need.isGap) {
        StatusBadge(
            BadgeKind.GAP,
            "$prefix ${stringResource(R.string.gap_text, periodLabel(need.period), need.size)}",
        )
    } else {
        StatusBadge(
            BadgeKind.MATCH,
            "$prefix ${stringResource(R.string.covered_text, periodLabel(need.period), need.coveredBy.first().title, need.size)}",
        )
    }
}

private val LabelColumnWidth = 124.dp
private val CellWidth = 88.dp

@Composable
private fun MatrixGrid(
    children: List<Child>,
    periods: List<SeasonPeriod>,
    needFor: (Child, SeasonPeriod, Category) -> SeasonNeed?,
    onCellClick: (Child, SeasonPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.horizontalScroll(rememberScrollState())) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Spacer(Modifier.width(LabelColumnWidth))
                children.forEach { child ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(CellWidth),
                    ) {
                        ChildAvatar(child, size = 32)
                        Text(
                            child.name,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            periods.forEach { period ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.width(LabelColumnWidth),
                    ) {
                        Text(phaseEmoji(period.phase), fontSize = 18.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(periodLabel(period), style = MaterialTheme.typography.labelLarge)
                    }
                    children.forEach { child ->
                        MatrixCell(
                            shoes = needFor(child, period, Category.SHOES),
                            clothes = needFor(child, period, Category.CLOTHES),
                            onClick = { onCellClick(child, period) },
                            modifier = Modifier
                                .width(CellWidth)
                                .padding(horizontal = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixCell(
    shoes: SeasonNeed?,
    clothes: SeasonNeed?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (shoes == null && clothes == null) {
        Box(modifier = modifier.height(64.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.matrix_out_of_range), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            listOfNotNull(shoes, clothes).forEach { need -> NeedChip(need) }
        }
    }
}

@Composable
private fun NeedChip(need: SeasonNeed) {
    val colors = MaterialTheme.extendedColors
    Surface(
        shape = PillShape,
        color = if (need.isGap) colors.gap else colors.match,
        contentColor = if (need.isGap) colors.onGap else colors.onMatch,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "${categoryEmoji(need.category)} ${need.size}",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun NeedDetailsDialog(
    child: Child,
    period: SeasonPeriod,
    needs: List<SeasonNeed>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${phaseEmoji(period.phase)} ${child.name} · ${periodLabel(period)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                needs.forEach { need ->
                    Text(
                        "${categoryEmoji(need.category)} ${categoryName(need.category)}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (need.isGap) {
                        StatusBadge(BadgeKind.GAP, stringResource(R.string.gap_text, periodLabel(period), need.size))
                    } else {
                        need.coveredBy.forEach { item ->
                            StatusBadge(
                                BadgeKind.MATCH,
                                stringResource(R.string.covered_text, periodLabel(period), item.title, need.size),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}
