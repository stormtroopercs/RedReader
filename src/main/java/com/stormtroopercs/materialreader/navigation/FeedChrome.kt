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
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stormtroopercs.materialreader.reddit.PostSort
import com.stormtroopercs.materialreader.settings.types.PostViewMode

/**
 * The top-of-feed filter chips (FINAL-DESIGN Phase 4.4): the sort chip
 * (the current sort — filled + caret, opens the 6-option sort dialog)
 * plus **Subreddits** (the signed-in user's subscribed subreddits — the
 * same list the drawer's Subscriptions section shows).
 * Horizontal, scrollable, sits between the top bar and the post list.
 *
 * (Lemmy remnant removed: the old **Communities** / **Instances** chips were
 * the Lemmy app's community / instance directory. This is a single-instance
 * Reddit app — "Communities" became "Subreddits" and opened the subscribed
 * list, and "Instances" (a Lemmy-only concept) was dropped.)
 */
@Composable
fun FeedFilterChips(
	sortLabel: String,
	onSortTap: () -> Unit,
	onSubredditsTap: () -> Unit,
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
				onClick = onSubredditsTap,
				label = { Text("Subreddits") },
			)
		}
	}
}

/**
 * One of the feed's six sort options (the reference's `sort_*` string
 * resources confirm the set). Options are keyed by a stable [id] — the id
 * (not the `PostSort`) is the identity for selection + persistence, since
 * "Best" is the listing's own default order (no URL sort param).
 *
 * Top and Controversial carry a time window (hour / day / week / month /
 * year / all): tapping either in the sort dialog opens the window list,
 * never applies the option itself. Selecting a window resolves the option
 * with [timeWindow] set — its [id] then carries the window
 * (`top:day`) so the choice persists per feed.
 */
data class FeedSortOption(
	val id: String,
	val label: String,
	/** The sort the listing URL is built with (null = the listing's own "Best" default). */
	val urlSort: PostSort?,
	/** True for the options with a time window (Top, Controversial). */
	val hasTimeWindows: Boolean = false,
	/** The chosen window (hour / day / …) — non-null only once the user picked one. */
	val timeWindow: String? = null,
) {
	/** The feed chip's label: plain for the windowless options,
	 *  `Top · Day` once a window is chosen. */
	val chipLabel: String
		get() = if (timeWindow != null) "$label · ${windowLabel(timeWindow)}" else label

	companion object {
		/** The time windows, in dialog order (top to bottom). */
		val timeWindows: List<TimeWindow> = listOf(
			TimeWindow("hour", "Hour"),
			TimeWindow("day", "Day"),
			TimeWindow("week", "Week"),
			TimeWindow("month", "Month"),
			TimeWindow("year", "Year"),
			TimeWindow("all", "All Time"),
		)

		/** The label for a persisted window id. */
		fun windowLabel(window: String): String =
			timeWindows.firstOrNull { it.id == window }?.label ?: window

		/** The [PostSort] for a (base sort, window) pair. */
		private fun windowSort(base: PostSort, window: String): PostSort = when (base) {
			PostSort.TOP_ALL -> when (window) {
				"hour" -> PostSort.TOP_HOUR
				"day" -> PostSort.TOP_DAY
				"week" -> PostSort.TOP_WEEK
				"month" -> PostSort.TOP_MONTH
				"year" -> PostSort.TOP_YEAR
				else -> PostSort.TOP_ALL
			}
			PostSort.CONTROVERSIAL_ALL -> when (window) {
				"hour" -> PostSort.CONTROVERSIAL_HOUR
				"day" -> PostSort.CONTROVERSIAL_DAY
				"week" -> PostSort.CONTROVERSIAL_WEEK
				"month" -> PostSort.CONTROVERSIAL_MONTH
				"year" -> PostSort.CONTROVERSIAL_YEAR
				else -> PostSort.CONTROVERSIAL_ALL
			}
			else -> base
		}

		/** The dialog's six options, top to bottom (Best first). */
		val options: List<FeedSortOption> = listOf(
			FeedSortOption("best", "Best", null),
			FeedSortOption("hot", "Hot", PostSort.HOT),
			FeedSortOption("new", "New", PostSort.NEW),
			FeedSortOption("rising", "Rising", PostSort.RISING),
			FeedSortOption("top", "Top", PostSort.TOP_ALL, hasTimeWindows = true),
			FeedSortOption("controversial", "Controversial", PostSort.CONTROVERSIAL_ALL, hasTimeWindows = true),
		)

		/** The option for a persisted [id] (unknown ids fall back to Best).
		 *  Windowed ids (`top:day`) reconstruct the option with its window. */
		fun forId(id: String): FeedSortOption {
			val colon = id.indexOf(':')
			if (colon > 0) {
				val base = id.substring(0, colon)
				val window = id.substring(colon + 1)
				options.firstOrNull { it.id == base }?.let { option ->
					if (option.hasTimeWindows && timeWindows.any { it.id == window }) {
						return option.copy(
							id = id,
							timeWindow = window,
							urlSort = windowSort(option.urlSort ?: return@let, window),
						)
					}
				}
			}
			return options.firstOrNull { it.id == id } ?: options.first()
		}

		/** Apply [window] to [option] — the resolved, persistable form. */
		fun withWindow(option: FeedSortOption, window: String): FeedSortOption {
			val sort = windowSort(option.urlSort ?: return option, window)
			return option.copy(id = "${option.id}:$window", timeWindow = window, urlSort = sort)
		}
	}
}

/** One of Top / Controversial's time windows. */
data class TimeWindow(val id: String, val label: String)

/**
 * The feed's two-level sort dialog (FINAL-DESIGN Phase 4.5). Level 1 lists
 * the six options; tapping **Top** or **Controversial** — the two options
 * with a time window — opens level 2, the window list (Hour / Day / Week /
 * Month / Year / All Time). Picking a window applies the resolved option
 * (e.g. `Top · Day`) and closes both levels; dismissing level 2 (outside
 * tap / Dismiss) returns to level 1 without changing the sort.
 */
@Composable
fun SortOptionsDialog(
	currentId: String,
	onDismiss: () -> Unit,
	onSelected: (FeedSortOption) -> Unit,
) {
	var windowOption by remember { mutableStateOf<FeedSortOption?>(null) }
	// The resolved current selection (a windowed id like `top:day` carries
	// its window) — level 1 uses it to check the active base option and
	// show the current window in its label.
	val current = remember(currentId) { FeedSortOption.forId(currentId) }
	val windowOptionSnapshot = windowOption
	if (windowOptionSnapshot != null) {
		// Level 2: the time-window list for the tapped option.
		AlertDialog(
			onDismissRequest = { windowOption = null },
			title = { Text("${windowOptionSnapshot.label} · time window") },
			text = {
				Column {
					FeedSortOption.timeWindows.forEach { window ->
						SelectableRow(
							label = window.label,
							selected = windowOptionSnapshot.timeWindow == window.id,
							onClick = {
								onSelected(FeedSortOption.withWindow(windowOptionSnapshot, window.id))
							},
						)
					}
				}
			},
			confirmButton = {
				TextButton(onClick = { windowOption = null }) { Text("Cancel") }
			},
		)
	} else {
		// Level 1: the six options. Top / Controversial open level 2; the
		// rest apply directly. The active row (matched by base id, so a
		// windowed `top:day` checks "Top") shows the current window in its
		// label.
		AlertDialog(
			onDismissRequest = onDismiss,
			title = { Text("Post sort") },
			text = {
				Column {
					FeedSortOption.options.forEach { option ->
						val active = option.id == currentId.substringBefore(':')
						SelectableRow(
							label = if (active) current.chipLabel else option.label,
							selected = active,
							onClick = {
								if (option.hasTimeWindows) {
									windowOption = option
								} else {
									onSelected(option)
								}
							},
						)
					}
				}
			},
			confirmButton = {
				TextButton(onClick = onDismiss) { Text("Dismiss") }
			},
		)
	}
}

/**
 * The reference's "Change View" bottom sheet (FINAL-DESIGN Phase 4.6): the
 * card modes (List / Compact / Smaller cards / Small cards / Cards /
 * Slides) + Dismiss + Customize. Selecting an entry applies the mode
 * (the surface switches in place) and closes the sheet; Customize opens
 * the per-view options (Settings → Post options).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeViewSheet(
	current: PostViewMode,
	onDismiss: () -> Unit,
	onSelect: (PostViewMode) -> Unit,
	onCustomize: () -> Unit = {},
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
				TextButton(onClick = onCustomize) { Text("Customize") }
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

/**
 * The reference's feed "more" FAB (FINAL-DESIGN Phase 5 / the decompiled
 * `posts_fab`): a bottom-right floating action button carrying a horizontal
 * three-dot ("more options") glyph — a separate resource id from `submit`,
 * confirmed by the reference layout. Present on the Posts-tab feed surfaces
 * (home / community / slides) and absent from Explore, search, threads and
 * settings. Tapping it opens the same [MoreActionsSheet] grid the top bar's
 * "More actions" icon opens — a prominent duplicate entry point, not a
 * distinct action. Wired via [FloatingActionButton] in each feed's Scaffold.
 */
@Composable
fun FeedMoreFab(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	FloatingActionButton(
		modifier = modifier,
		onClick = onClick,
	) {
		Icon(
			imageVector = Icons.Filled.MoreHoriz,
			contentDescription = "More actions",
		)
	}
}
