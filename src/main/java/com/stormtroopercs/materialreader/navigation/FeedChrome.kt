/*******************************************************************************
 * This file is part of MaterialReader.
 *
 * MaterialReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MaterialReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MaterialReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.stormtroopercs.materialreader.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stormtroopercs.materialreader.reddit.PostSort
import com.stormtroopercs.materialreader.settings.types.PostViewMode

/**
 * The reference's top-of-feed filter chips (FINAL-DESIGN Phase 4.4): the
 * **Active** chip (the current sort — filled + caret, opens the 9-option
 * sort dialog) plus **Communities** / **Instances** (opening the respective
 * directories). Horizontal, scrollable, sits between the top bar and the
 * post list.
 */
@Composable
fun FeedFilterChips(
	sortLabel: String,
	onSortTap: () -> Unit,
	onCommunitiesTap: () -> Unit,
	onInstancesTap: () -> Unit,
	modifier: Modifier = Modifier,
) {
	LazyRow(
		modifier = modifier.fillMaxWidth(),
		contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
		horizontalArrangement = Arrangement.spacedBy(8.dp),
	) {
		item {
			FilterChip(
				selected = true,
				onClick = onSortTap,
				label = {
					Row(verticalAlignment = Alignment.CenterVertically) {
						Text(sortLabel, fontWeight = FontWeight.SemiBold)
						Icon(
							imageVector = Icons.Filled.KeyboardArrowDown,
							contentDescription = null,
							modifier = Modifier.padding(start = 2.dp),
						)
					}
				},
			)
		}
		item {
			FilterChip(
				selected = false,
				onClick = onCommunitiesTap,
				label = { Text("Communities") },
			)
		}
		item {
			FilterChip(
				selected = false,
				onClick = onInstancesTap,
				label = { Text("Instances") },
			)
		}
	}
}

/**
 * One of the reference's 9 sort dialog options (FINAL-DESIGN Phase 4.5,
 * DESIGN §2.3; the reference's `sort_*` string resources confirm the exact
 * set). Options are keyed by a stable [id] — two of them ("Old" and "Top")
 * both resolve to `top?t=all` against the Reddit API, so the id (not the
 * `PostSort`) is the identity for selection + persistence.
 *
 * Each option has two resolutions: [urlSort] — the sort param the listing
 * URL is built with (null = the listing's own default) — and [reverse] —
 * whether the fetched items are then presented in reverse order (the
 * "Old" option: the Reddit API has no oldest-first sort, so the listing
 * is fetched newest-first (`new` — a strict created-utc order) and
 * reversed locally to oldest-first).
 */
data class FeedSortOption(
	val id: String,
	val label: String,
	/** The sort the listing URL is built with (null = the listing default). */
	val urlSort: PostSort?,
	/** Present the fetched items in reverse order (oldest-first). */
	val reverse: Boolean = false,
) {
	companion object {
		/** The dialog's 9 options, in the reference's order. */
		val options: List<FeedSortOption> = listOf(
			FeedSortOption("active", "Active", null),
			FeedSortOption("hot", "Hot", PostSort.HOT),
			FeedSortOption("new", "New", PostSort.NEW),
			FeedSortOption("old", "Old", PostSort.NEW, reverse = true),
			FeedSortOption("most_comments", "Most comments", PostSort.COMMENTS_ALL),
			FeedSortOption("new_comments", "New comments", PostSort.COMMENTS_HOUR),
			FeedSortOption("scaled", "Scaled", PostSort.RISING),
			FeedSortOption("controversial", "Controversial", PostSort.CONTROVERSIAL_ALL),
			FeedSortOption("top", "Top", PostSort.TOP_ALL),
		)

		/** The option for a persisted [id] (unknown ids fall back to Active). */
		fun forId(id: String): FeedSortOption =
			options.firstOrNull { it.id == id } ?: options.first()
	}
}

@Composable
fun SortOptionsDialog(
	currentId: String,
	onDismiss: () -> Unit,
	onSelected: (FeedSortOption) -> Unit,
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Post sort") },
		text = {
			Column {
				FeedSortOption.options.forEach { option ->
					SelectableRow(
						label = option.label,
						selected = option.id == currentId,
						onClick = { onSelected(option) },
					)
				}
			}
		},
		confirmButton = {
			TextButton(onClick = onDismiss) { Text("Dismiss") }
		},
	)
}

/**
 * The reference's "Change View" bottom sheet (FINAL-DESIGN Phase 4.6): the
 * card modes (List / Compact / Smaller cards / Small cards / Cards /
 * Slides) + Dismiss + Customize. Selecting an entry closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeViewSheet(
	current: PostViewMode,
	onDismiss: () -> Unit,
	onSelect: (PostViewMode) -> Unit,
) {
	ModalBottomSheet(onDismissRequest = onDismiss) {
		Column(modifier = Modifier.padding(bottom = 24.dp)) {
			Text(
				text = "Change view",
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold,
				modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
			)
			SelectableRow("List", PostViewMode.LIST == current) { onSelect(PostViewMode.LIST) }
			SelectableRow("Compact", PostViewMode.COMPACT == current) { onSelect(PostViewMode.COMPACT) }
			SelectableRow("Smaller cards", PostViewMode.SMALLER == current) { onSelect(PostViewMode.SMALLER) }
			SelectableRow("Small cards", PostViewMode.SIMPLE == current) { onSelect(PostViewMode.SIMPLE) }
			SelectableRow("Cards", PostViewMode.CARDS == current) { onSelect(PostViewMode.CARDS) }
			SelectableRow("Slides", PostViewMode.SLIDES == current) { onSelect(PostViewMode.SLIDES) }
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp),
				horizontalArrangement = Arrangement.End,
			) {
				TextButton(onClick = onDismiss) { Text("Dismiss") }
				// Customize: the per-view options (swipe actions live in
				// Settings → Post options; there is no per-view customizer in
				// the reference beyond that).
				TextButton(onClick = onDismiss) { Text("Customize") }
			}
		}
	}
}

/** A tappable single-select row: label + optional check, ripple on the row. */
@Composable
private fun SelectableRow(
	label: String,
	selected: Boolean,
	onClick: () -> Unit,
) {
	ListItem(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 2.dp)
			.clickable(onClick = onClick),
		headlineContent = {
			Text(
				text = label,
				fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
			)
		},
		trailingContent = {
			if (selected) {
				Icon(
					imageVector = Icons.Filled.Check,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.primary,
				)
			}
		},
	)
}
