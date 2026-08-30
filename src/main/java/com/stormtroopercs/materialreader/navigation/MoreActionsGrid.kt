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

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.stormtroopercs.materialreader.R
import com.stormtroopercs.materialreader.compose.prefs.ComposePrefsSingleton
import com.stormtroopercs.materialreader.common.PrefsUtility
import com.stormtroopercs.materialreader.settings.types.AppearanceTheme
import com.stormtroopercs.materialreader.settings.types.ThemeLightness
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.math.roundToInt

/**
 * The reference's "More actions" grid (FINAL-DESIGN Phase 5, audit §2.4):
 * the feed's power-user action surface — a 4-column grid of circular icon
 * buttons (icon + label beneath) in a bottom sheet. The default set is
 * Search / Profile / Hide read / About / Submit / Random / Dark mode /
 * Settings / Change View / Saved / Refresh + one customizable slot.
 *
 * The user can **long-press-drag** a cell to reorder it (persisted in
 * [FeedPreferences.actionOrder]) and use the **3-dot menu** (top-right) to
 * show/hide individual actions (persisted in [FeedPreferences.hiddenActions]).
 * Long-pressing the customizable slot **without** dragging edits its target
 * listing. A short tap always just fires the action.
 */
private const val LONG_PRESS_MS = 500L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreActionsSheet(
	posts: List<PostItem>,
	onDismiss: () -> Unit,
	onNavigateToSearch: () -> Unit,
	onNavigateToProfile: () -> Unit,
	onHideReadToggled: (Boolean) -> Unit,
	onOpenAbout: () -> Unit,
	onNavigateToSubmit: () -> Unit,
	onNavigateToRandomPost: (String) -> Unit,
	onNavigateToSettings: () -> Unit,
	onOpenChangeView: () -> Unit,
	onNavigateToSaved: () -> Unit,
	onRefresh: () -> Unit,
	onOpenListing: (String) -> Unit,
	onOpenLicense: () -> Unit,
) {
	val context = LocalContext.current

	// The user-ordered action list (hidden ones stay in the order — they
	// just don't render). Labels that depend on live state (hide-read
	// on/off, dark mode, custom target) are derived at composition time, so
	// a toggle re-labels the cell without disturbing the order.
	var order by remember { mutableStateOf(FeedPreferences.actionOrder()) }
	var hidden by remember { mutableStateOf(FeedPreferences.hiddenActions()) }
	val visible = order.filter { it !in hidden }

	var draggingId by remember { mutableStateOf<String?>(null) }
	val cells = remember { HashMap<String, LayoutCoordinates>() }
	var containerRoot by remember { mutableStateOf<LayoutCoordinates?>(null) }
	var dragOffset by remember { mutableStateOf(Offset.Zero) }
	var editCustomOpen by remember { mutableStateOf(false) }

	// The grid's action handlers, wired through the host screen's callbacks.
	fun fireAction(id: String) {
		when (id) {
			ACTION_SEARCH -> onNavigateToSearch()
			ACTION_PROFILE -> onNavigateToProfile()
			ACTION_HIDE_READ -> {
				val next = !PrefsUtility.pref_behaviour_hide_read_posts()
				PrefsUtility.pref_behaviour_hide_read_posts_set(next)
				onHideReadToggled(next)
			}
			ACTION_ABOUT -> onOpenAbout()
			ACTION_SUBMIT -> onNavigateToSubmit()
			ACTION_RANDOM -> posts.takeIf { it.isNotEmpty() }?.let {
				onNavigateToRandomPost(it.random().id)
			}
			ACTION_DARK_MODE -> toggleDarkMode()
			ACTION_SETTINGS -> onNavigateToSettings()
			ACTION_CHANGE_VIEW -> onOpenChangeView()
			ACTION_SAVED -> onNavigateToSaved()
			ACTION_REFRESH -> onRefresh()
			ACTION_CUSTOM -> onOpenListing(FeedPreferences.customTarget().ifBlank { "all" })
		}
	}

	ModalBottomSheet(onDismissRequest = onDismiss) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp, vertical = 8.dp),
		) {
			// Header: title + the 3-dot show/hide menu.
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween,
			) {
				Text(
					text = "More actions",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					modifier = Modifier.padding(start = 8.dp),
				)
				MoreActionsMenu(
					ids = order,
					hidden = hidden,
					onToggle = { id ->
						val next = hidden.toMutableSet()
						if (id in next) next.remove(id) else next.add(id)
						FeedPreferences.setHiddenActions(next)
						hidden = next
					},
					onShowAll = {
						FeedPreferences.setHiddenActions(emptySet())
						hidden = emptySet()
					},
				)
			}
			Spacer(Modifier.height(12.dp))

			// The 4-column grid: plain rows (12 cells always fit) so the
			// long-press drag-reorder hit-testing is a bounds check over the
			// recorded cell coordinates.
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.onGloballyPositioned { containerRoot = it },
			) {
				val rows = visible.chunked(4)
				rows.forEachIndexed { rowIndex, rowItems ->
					Row(modifier = Modifier.fillMaxWidth()) {
						rowItems.forEach { id ->
							Box(modifier = Modifier.weight(1f)) {
								MoreActionsCell(
									id = id,
									label = actionLabel(id),
									icon = actionIcon(id),
									dragging = id == draggingId,
									offset = if (id == draggingId) dragOffset else Offset.Zero,
									modifier = Modifier.onGloballyPositioned { cells[id] = it },
									onClick = {
										fireAction(id)
									},
									onDragState = { dragId, dragging ->
										draggingId = if (dragging) dragId else null
										if (!dragging) {
											dragOffset = Offset.Zero
											// Persist the (possibly new) order on drop.
											FeedPreferences.setActionOrder(order)
										}
									},
									onDragOffset = { delta, pointerInCell ->
										dragOffset += delta
										moveDraggedTo(
											draggingId,
											pointerInCell,
											cells,
											containerRoot,
											order,
											onReorder = { next ->
												order = next
												FeedPreferences.setActionOrder(next)
											},
										)
									},
									onLongPressNoDrag = {
										// The customizable slot: a long-press released
										// without dragging edits its target listing.
										if (id == "custom") editCustomOpen = true
									},
								)
							}
						}
					}
					if (rowIndex < rows.lastIndex) {
						Spacer(Modifier.height(14.dp))
					}
				}
			}
			Spacer(Modifier.height(24.dp))
		}
	}

	// The customizable slot's target editor (long-press without drag).
	if (editCustomOpen) {
		EditCustomTargetDialog(
			current = FeedPreferences.customTarget(),
			onDismiss = { editCustomOpen = false },
			onSave = { target ->
				FeedPreferences.setCustomTarget(target)
				editCustomOpen = false
			},
		)
	}
}

/** The default grid order (FINAL-DESIGN Phase 5) as (id, label, icon). */
private const val ACTION_SEARCH = "search"
private const val ACTION_PROFILE = "profile"
private const val ACTION_HIDE_READ = "hide_read"
private const val ACTION_ABOUT = "about"
private const val ACTION_SUBMIT = "submit"
private const val ACTION_RANDOM = "random"
private const val ACTION_DARK_MODE = "dark_mode"
private const val ACTION_SETTINGS = "settings"
private const val ACTION_CHANGE_VIEW = "change_view"
private const val ACTION_SAVED = "saved"
private const val ACTION_REFRESH = "refresh"
private const val ACTION_CUSTOM = "custom"

@Composable
private fun actionLabel(id: String): String = when (id) {
	ACTION_SEARCH -> "Search"
	ACTION_PROFILE -> "Profile"
	ACTION_HIDE_READ -> if (PrefsUtility.pref_behaviour_hide_read_posts()) "Show read" else "Hide read"
	ACTION_ABOUT -> "About"
	ACTION_SUBMIT -> "Submit"
	ACTION_RANDOM -> "Random"
	ACTION_DARK_MODE -> if (currentThemeIsDark()) "Light mode" else "Dark mode"
	ACTION_SETTINGS -> "Settings"
	ACTION_CHANGE_VIEW -> "Change view"
	ACTION_SAVED -> "Saved"
	ACTION_REFRESH -> "Refresh"
	ACTION_CUSTOM -> customSlotLabel()
	else -> id
}

private fun actionIcon(id: String): ImageVector = when (id) {
	ACTION_SEARCH -> Icons.Filled.Search
	ACTION_PROFILE -> Icons.Filled.Person
	ACTION_HIDE_READ -> Icons.Filled.VisibilityOff
	ACTION_ABOUT -> Icons.Filled.Info
	ACTION_SUBMIT -> Icons.Filled.Add
	ACTION_RANDOM -> Icons.Filled.Explore
	ACTION_DARK_MODE -> Icons.Filled.DarkMode
	ACTION_SETTINGS -> Icons.Filled.Settings
	ACTION_CHANGE_VIEW -> Icons.Filled.ViewAgenda
	ACTION_SAVED -> Icons.Filled.Bookmark
	ACTION_REFRESH -> Icons.Filled.Refresh
	ACTION_CUSTOM -> Icons.Filled.Apps
	else -> Icons.Filled.Apps
}

/**
 * The reference's "Dark mode" grid action: a direct, reversible
 * light↔dark switch. Toggling to dark remembers the current light theme
 * (so toggling back restores it); toggling back restores that theme.
 */
private fun toggleDarkMode() {
	val prefs = ComposePrefsSingleton.instance
	val theme = prefs.appearanceTheme
	if (theme.value.lightness == ThemeLightness.Dark) {
		val restore = FeedPreferences.lastLightTheme()
		theme.value = AppearanceTheme.entries.firstOrNull { it.stringValue == restore }
			?: AppearanceTheme.RED
	} else {
		FeedPreferences.setLastLightTheme(theme.value.stringValue)
		theme.value = AppearanceTheme.NIGHT
	}
}

private fun currentThemeIsDark(): Boolean = try {
	ComposePrefsSingleton.instance.appearanceTheme.value.lightness == ThemeLightness.Dark
} catch (e: Exception) {
	false
}

private fun customSlotLabel(): String {
	val target = FeedPreferences.customTarget().ifBlank { return "Custom" }
	// `u/<user>/submitted` → "u/<user>", `r/all` → "r/all", bare → itself.
	return target.substringBefore("/submitted").substringBefore("/comments")
		.takeIf { it.isNotBlank() } ?: "Custom"
}

/**
 * While a cell is being dragged, the pointer (in the dragged cell's own
 * coordinates) is mapped into the container space; the dragged item is
 * swapped into whichever cell the pointer is over (bounds check over the
 * recorded cell coordinates).
 */
private fun moveDraggedTo(
	draggingId: String?,
	pointerInCell: Offset,
	cells: Map<String, LayoutCoordinates>,
	containerRoot: LayoutCoordinates?,
	order: List<String>,
	onReorder: (List<String>) -> Unit,
) {
	if (draggingId == null || containerRoot == null) return
	val dragged = cells[draggingId] ?: return
	// Layout 1.12+: localToRoot(Offset) → IntOffset (no positionInRoot).
	val draggedOrigin = dragged.localToRoot(Offset.Zero)
	val pointerInRoot = Offset(
		draggedOrigin.x + pointerInCell.x,
		draggedOrigin.y + pointerInCell.y,
	)
	val containerTopLeft = containerRoot.localToRoot(Offset.Zero)
	val pointerInContainer = Offset(
		pointerInRoot.x - containerTopLeft.x,
		pointerInRoot.y - containerTopLeft.y,
	)

	val targetId = cells.entries.firstOrNull { (id, coords) ->
		if (id == draggingId) return@firstOrNull false
		val origin = coords.localToRoot(Offset.Zero)
		val left = origin.x - containerTopLeft.x
		val top = origin.y - containerTopLeft.y
		val w = coords.size.width
		val h = coords.size.height
		pointerInContainer.x in left..(left + w) && pointerInContainer.y in top..(top + h)
	}?.key ?: return

	val from = order.indexOf(draggingId)
	val to = order.indexOf(targetId)
	if (from < 0 || to < 0 || from == to) return
	val next = order.toMutableList()
	val item = next.removeAt(from)
	next.add(to, item)
	onReorder(next)
}

/**
 * One grid cell: a circular icon button with its label beneath.
 *
 * Gesture policy: a short tap fires [onClick] (the cell's [Surface]
 * handles it — this pointer input never consumes a quick release); a
 * long-press starts a drag (consuming every event so the Surface's
 * click never fires mid-drag); a long-press released without moving
 * fires [onLongPressNoDrag] (the customizable slot's edit affordance).
 */
@Composable
private fun MoreActionsCell(
	id: String,
	label: String,
	icon: ImageVector,
	dragging: Boolean,
	offset: Offset,
	modifier: Modifier = Modifier,
	onClick: () -> Unit,
	onDragState: (String, Boolean) -> Unit,
	onDragOffset: (Offset, Offset) -> Unit,
	onLongPressNoDrag: () -> Unit,
) {
	val haptics = LocalHapticFeedback.current

	Column(
		modifier = modifier
			.offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
			.zIndex(if (dragging) 1f else 0f)
			.padding(2.dp)
			.pointerInput(id) {
				awaitEachGesture {
					val down = awaitFirstDown(requireUnconsumed = false)
					if (!down.pressed) return@awaitEachGesture
					// Hold until the long-press fires. A release before it
					// returns (unconsumed) so the Surface's click still fires.
					var longPressed = false
					try {
						withTimeout(LONG_PRESS_MS) {
							while (true) {
								val event = awaitPointerEvent()
								val change = event.changes.firstOrNull { it.id == down.id } ?: continue
								if (!change.pressed) return@withTimeout
								change.consume()
							}
						}
					} catch (e: TimeoutCancellationException) {
						longPressed = true
					}
					if (!longPressed) return@awaitEachGesture
					haptics.performHapticFeedback(HapticFeedbackType.LongPress)
					onDragState(id, true)
					var moved = false
					var last = down.position
					while (true) {
						val event = awaitPointerEvent()
						val change = event.changes.firstOrNull { it.id == down.id } ?: break
						if (!change.pressed) break
						change.consume()
						val pos = change.position
						val delta = pos - last
						last = pos
						if (delta != Offset.Zero) moved = true
						onDragOffset(delta, pos)
					}
					if (!moved) onLongPressNoDrag()
					onDragState(id, false)
				}
			},
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Surface(
			shape = CircleShape,
			color = if (dragging) {
				MaterialTheme.colorScheme.primaryContainer
			} else {
				MaterialTheme.colorScheme.surfaceVariant
			},
			contentColor = if (dragging) {
				MaterialTheme.colorScheme.onPrimaryContainer
			} else {
				MaterialTheme.colorScheme.onSurfaceVariant
			},
			modifier = Modifier.size(56.dp),
			onClick = onClick,
		) {
			Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
				Icon(
					imageVector = icon,
					contentDescription = label,
					modifier = Modifier.size(24.dp),
				)
			}
		}
		Spacer(Modifier.height(4.dp))
		Text(
			text = label,
			style = MaterialTheme.typography.labelSmall,
			fontWeight = if (dragging) FontWeight.SemiBold else FontWeight.Normal,
			textAlign = TextAlign.Center,
			maxLines = 2,
			color = MaterialTheme.colorScheme.onSurface,
		)
	}
}

/**
 * The 3-dot menu (top-right of the sheet): show/hide individual grid
 * actions (the reference's per-action visibility, Phase 5.2).
 */
@Composable
private fun MoreActionsMenu(
	ids: List<String>,
	hidden: Set<String>,
	onToggle: (String) -> Unit,
	onShowAll: () -> Unit,
) {
	var expanded by remember { mutableStateOf(false) }
	Box {
		IconButton(onClick = { expanded = true }) {
			Icon(Icons.Filled.MoreVert, contentDescription = "Show or hide actions")
		}
		DropdownMenu(
			expanded = expanded,
			onDismissRequest = { expanded = false },
		) {
			DropdownMenuItem(
				text = { Text("Show all") },
				leadingIcon = {
					if (hidden.isEmpty()) {
						Icon(Icons.Filled.Check, contentDescription = null)
					}
				},
				onClick = {
					onShowAll()
					expanded = false
				},
			)
			ids.forEach { id ->
				DropdownMenuItem(
					text = { Text(actionLabel(id)) },
					leadingIcon = {
						if (id !in hidden) {
							Icon(Icons.Filled.Check, contentDescription = null)
						}
					},
					onClick = {
						onToggle(id)
						expanded = false
					},
				)
			}
		}
	}
}

/** Edit the customizable slot's target listing (long-press, no drag). */
@Composable
private fun EditCustomTargetDialog(
	current: String,
	onDismiss: () -> Unit,
	onSave: (String) -> Unit,
) {
	var value by remember { mutableStateOf(current) }
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Customize slot") },
		text = {
			Text("Listing path (e.g. r/all, u/<user>/submitted, m/<name>)")
		},
		confirmButton = {
			TextButton(onClick = {
				onSave(value.trim())
			}) { Text("Save") }
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text("Cancel") }
		},
	)
}

/** The About dialog (the grid's "About" action): app name/version + license. */
@Composable
fun AboutDialog(
	onDismiss: () -> Unit,
	onOpenLicense: () -> Unit,
) {
	val context = LocalContext.current
	val info = remember {
		runCatching {
			context.packageManager.getPackageInfo(context.packageName, 0)
		}.getOrNull()
	}
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(stringResource(R.string.app_name)) },
		text = {
			Text(
				"Version ${info?.versionName ?: "?"}\n\n" +
					"A fast, modern Reddit client.\n" +
					"Built in the open — report bugs, send ideas."
			)
		},
		confirmButton = {
			TextButton(onClick = onOpenLicense) { Text("License") }
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text("Dismiss") }
		},
	)
}
