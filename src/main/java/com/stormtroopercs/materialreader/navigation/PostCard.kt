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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stormtroopercs.materialreader.compose.theme.LocalComposeTheme
import com.stormtroopercs.materialreader.compose.ui.PostThumbnailPreview
import com.stormtroopercs.materialreader.settings.types.PostSwipeAction
import com.stormtroopercs.materialreader.settings.types.PostViewMode
import kotlin.math.abs

/**
 * The reference's list-view post action row (FINAL-DESIGN Phase 4.2, audit
 * §8.1 — the corrected design, NOT the six-button bar): a tappable stats
 * number on the left (score • comment count — tapping it opens the
 * thread; the score tap upvotes) and a compact icon row on the right
 * (comment + share by default, conditional reply/up/down/save, then a
 * kebab for the remaining post options).
 */
@Composable
fun StatsAndIconsRow(
	post: PostItem,
	onOpenThread: () -> Unit,
	onUpvote: () -> Unit,
	onPostAction: (PostItem, PostAction) -> Unit,
	modifier: Modifier = Modifier,
) {
	var moreExpanded by remember { mutableStateOf(false) }
	Row(
		modifier = modifier.fillMaxWidth(),
		verticalAlignment = Alignment.CenterVertically,
	) {
			// Left: the tappable stats — score + comment count as plain
			// numbers (the reference's "PostStats" text).
			Text(
				text = "${formatFeedCount(post.score)}  •  ${formatFeedCount(post.numComments)}",
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.clickable(
					interactionSource = null,
					indication = null,
					onClick = onOpenThread,
					role = Role.Button,
				),
			)
			Spacer(Modifier.weight(1f))
			// Right: compact icon row.
			IconSlot(icon = Icons.Filled.ChatBubbleOutline, label = "Comments", onClick = onOpenThread)
			IconSlot(
				icon = Icons.Filled.Share,
				label = "Share",
				onClick = { onPostAction(post, PostAction.SHARE) },
			)
			IconSlot(
				icon = Icons.AutoMirrored.Filled.Reply,
				label = "Reply",
				onClick = onOpenThread,
			)
			IconSlot(
				icon = Icons.Filled.ArrowUpward,
				label = "Upvote",
				onClick = onUpvote,
			)
			IconSlot(
				icon = Icons.Filled.ArrowDownward,
				label = "Downvote",
				onClick = { onPostAction(post, PostAction.DOWNVOTE) },
			)
			IconSlot(
				icon = Icons.Filled.Bookmark,
				label = if (post.saved) "Unsave" else "Save",
				onClick = { onPostAction(post, if (post.saved) PostAction.UNSAVE else PostAction.SAVE) },
			)
			Box {
				IconSlot(icon = Icons.Filled.MoreVert, label = "More options", onClick = { moreExpanded = true })
				DropdownMenu(
					expanded = moreExpanded,
					onDismissRequest = { moreExpanded = false },
				) {
					DropdownMenuItem(
						text = { Text("Report") },
						onClick = {
							moreExpanded = false
							onPostAction(post, PostAction.REPORT)
						},
					)
					DropdownMenuItem(
						text = { Text(if (post.hidden) "Unhide" else "Hide") },
						onClick = {
							moreExpanded = false
							onPostAction(post, if (post.hidden) PostAction.UNHIDE else PostAction.HIDE)
							},
							)
							}
						}
			}
}

/** One compact icon button of the [StatsAndIconsRow] right side. */
@Composable
private fun IconSlot(
	icon: androidx.compose.ui.graphics.vector.ImageVector,
	label: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	IconButton(onClick = onClick, modifier = modifier.size(36.dp)) {
		Icon(
			imageVector = icon,
			contentDescription = label,
			tint = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.size(18.dp),
		)
	}
}

/**
 * The reference's post-card horizontal swipe gestures (FINAL-DESIGN Phase
 * 4.3, DESIGN §17.7): a swipe right past the first threshold commits the
 * first action (default upvote), a swipe left the second (default
 * downvote), a deeper swipe left the third (default hide). The card
 * translates with the gesture and springs back; an optional haptic tick
 * fires on commit.
 *
 * The card's own clickable still handles taps — only a mostly-horizontal
 * drag past the 120dp threshold wins (a vertical drag is left to the
 * list's scroll and never commits).
 */
@Composable
fun Modifier.postSwipeToAction(
	post: PostItem,
	action1: PostSwipeAction,
	action2: PostSwipeAction,
	action3: PostSwipeAction,
	onSwipeUpvote: () -> Unit,
	onSwipeDownvote: () -> Unit,
	onSwipeHide: () -> Unit,
): Modifier {
	val haptics = LocalHapticFeedback.current
	var offset by remember { mutableFloatStateOf(0f) }
	val thresholdPx = with(LocalDensity.current) { 120.dp.toPx() }
	val hapticsOn = FeedPreferences.swipeVibrate()

	fun commit(action: PostSwipeAction?) {
		when (action) {
			PostSwipeAction.UPVOTE -> onSwipeUpvote()
			PostSwipeAction.DOWNVOTE -> onSwipeDownvote()
			PostSwipeAction.HIDE -> onSwipeHide()
			PostSwipeAction.SAVE, PostSwipeAction.COMMENTS, PostSwipeAction.NONE, null -> {}
		}
		if (action != null && action != PostSwipeAction.NONE && hapticsOn) {
			haptics.performHapticFeedback(HapticFeedbackType.LongPress)
		}
	}

	return this
		.graphicsLayer { translationX = offset }
		.pointerInput(post.id, action1, action2, action3) {
			awaitEachGesture {
				val start = awaitFirstDown(requireUnconsumed = false)
				offset = 0f
				var lastX = start.position.x
				var lastY = start.position.y
				while (true) {
					val event = awaitPointerEvent()
					val change = event.changes.firstOrNull() ?: break
					if (!change.pressed) break
					val dx = change.position.x - lastX
					val dy = change.position.y - lastY
					lastX = change.position.x
					lastY = change.position.y
					// A mostly-vertical drag belongs to the list's scroll.
					if (abs(dy) > abs(dx)) continue
					if (abs(dx) > 1f) {
						offset = (offset + dx).coerceIn(-size.width / 2f, size.width / 2f)
					}
				}
				// Finger lifted: commit if a threshold was crossed.
				when {
					offset > thresholdPx -> commit(action1)
					offset < -thresholdPx * 2f -> commit(action3)
					offset < -thresholdPx -> commit(action2)
				}
				offset = 0f
			}
		}
	}

/**
 * A single post in the list feed, in one of the reference's card modes
 * (FINAL-DESIGN Phase 4.1, DESIGN §4.3). Media (220dp, 8dp margin, rounded)
 * above the title → meta → body ordering; the corrected [StatsAndIconsRow]
 * underneath. The whole card is tappable (opens the thread).
 *
 * @param post the post to render.
 * @param mode the card mode (see [PostViewMode]).
 * @param onOpenThread opens the post's comment thread.
 * @param onAuthorClick opens the author's profile.
 * @param onPostAction a list action (vote / save / hide / share / report).
 * @param swipeEnabled whether the horizontal swipe-to-action gesture is on.
 * @param onSwipeUpvote / onSwipeDownvote / onSwipeHide the swipe actions
 *   (slots 1–3 from Settings → Post options).
 */
@Composable
fun PostCard(
	post: PostItem,
	mode: PostViewMode,
	modifier: Modifier = Modifier,
	onOpenThread: () -> Unit,
	onAuthorClick: (String) -> Unit,
	onPostAction: (PostItem, PostAction) -> Unit,
	swipeEnabled: Boolean = false,
	onSwipeUpvote: () -> Unit = {},
	onSwipeDownvote: () -> Unit = {},
	onSwipeHide: () -> Unit = {},
) {
	val theme = LocalComposeTheme.current.postCard

	// Mode geometry: media size + body line count.
	val mediaSize = when (mode) {
		PostViewMode.CARDS, PostViewMode.SMALLER, PostViewMode.THUMB_LEFT,
		PostViewMode.THUMB_RIGHT, PostViewMode.HORIZONTAL -> 220.dp
		PostViewMode.COMPACT, PostViewMode.LIST, PostViewMode.SIMPLE -> 0.dp
		PostViewMode.SLIDES -> 220.dp // unreachable (slides has its own screen)
	}
	val bodyLines = when (mode) {
		PostViewMode.CARDS -> 4
		PostViewMode.SMALLER -> 3
		PostViewMode.COMPACT, PostViewMode.LIST -> 2
		PostViewMode.SIMPLE -> 1
		else -> 4
	}

	Column(
		modifier = modifier
			.fillMaxWidth()
			.padding(8.dp)
			.background(theme.backgroundColor, RoundedCornerShape(12.dp))
			.then(
				if (swipeEnabled) {
					Modifier.postSwipeToAction(
						post = post,
						action1 = FeedPreferences.swipeAction1(),
						action2 = FeedPreferences.swipeAction2(),
						action3 = FeedPreferences.swipeAction3(),
						onSwipeUpvote = onSwipeUpvote,
						onSwipeDownvote = onSwipeDownvote,
						onSwipeHide = onSwipeHide,
					)
				} else {
					Modifier
				}
			)
			.clickable(onClick = onOpenThread)
	) {
		// ── Media (per mode) ──
		val media = rememberMedia(post)
		when (mode) {
			PostViewMode.THUMB_LEFT, PostViewMode.THUMB_RIGHT, PostViewMode.HORIZONTAL -> {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(8.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					if (mode == PostViewMode.THUMB_RIGHT) {
						PostCardTextColumn(
							post = post,
							bodyLines = bodyLines,
							onAuthorClick = onAuthorClick,
						)
					}
					if (mediaSize > 0.dp && media != null) {
						PostThumbnailPreview(
							uri = media,
							isVideo = post.isVideo,
							size = 96.dp,
							modifier = Modifier.padding(if (mode == PostViewMode.THUMB_LEFT) 8.dp else 0.dp),
						)
					}
					if (mode == PostViewMode.THUMB_LEFT) {
						PostCardTextColumn(
							post = post,
							bodyLines = bodyLines,
							onAuthorClick = onAuthorClick,
						)
					}
					if (mode == PostViewMode.HORIZONTAL && media == null) {
						PostCardTextColumn(
							post = post,
							bodyLines = bodyLines,
							onAuthorClick = onAuthorClick,
						)
					}
				}
			}
			else -> {
				if (mediaSize > 0.dp && media != null) {
					PostThumbnailPreview(
						uri = media,
						isVideo = post.isVideo,
						size = mediaSize,
						modifier = Modifier
							.fillMaxWidth()
							.padding(8.dp)
							.clip(RoundedCornerShape(8.dp)),
					)
				}
				PostCardTextColumn(
					post = post,
					bodyLines = bodyLines,
					onAuthorClick = onAuthorClick,
					modifier = Modifier.padding(horizontal = 12.dp),
				)
			}
		}

		// ── The corrected action row (stats left, icons right) ──
		StatsAndIconsRow(
			post = post,
			onOpenThread = onOpenThread,
			onUpvote = { onPostAction(post, PostAction.UPVOTE) },
			onPostAction = onPostAction,
			modifier = Modifier.padding(horizontal = 12.dp),
		)
		Spacer(Modifier.height(8.dp))
	}
}

/** Resolve the media URI for a post's card (post url, else thumbnail). */
private fun rememberMedia(post: PostItem): String? {
	return post.url?.takeIf { it.isNotBlank() && !it.startsWith("reddit.com") }
		?: post.thumbnail?.takeIf { it.isNotBlank() && it != "default" }
}

/**
 * The card's text column: title → meta (author • time • flair) → body, in
 * the reference's title→desc→body ordering.
 */
@Composable
private fun PostCardTextColumn(
	post: PostItem,
	bodyLines: Int,
	onAuthorClick: (String) -> Unit,
	modifier: Modifier = Modifier,
) {
	val theme = LocalComposeTheme.current.postCard
	Column(modifier = modifier.padding(vertical = 4.dp)) {
		Text(
			text = post.title ?: "Untitled",
			style = theme.title,
			maxLines = 3,
			overflow = TextOverflow.Ellipsis,
		)
		Spacer(Modifier.height(2.dp))
		Row(
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier.horizontalScroll(rememberScrollState()),
		) {
			post.author?.takeIf { it.isNotBlank() }?.let { author ->
				Text(
					text = author,
					style = theme.subtitle,
					modifier = Modifier.clickable { onAuthorClick(author) },
				)
			}
			Text(text = "  •  ${formatTimeAgoShort(post.createdUtc)}", style = theme.subtitle)
			post.linkFlairText?.takeIf { it.isNotBlank() }?.let { flair ->
				Spacer(Modifier.width(6.dp))
				Box(
					modifier = Modifier
						.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
						.padding(horizontal = 6.dp, vertical = 1.dp),
				) {
					Text(
						text = flair,
						style = MaterialTheme.typography.labelSmall,
						color = MaterialTheme.colorScheme.onPrimaryContainer,
					)
				}
			}
		}
		post.selftext?.takeIf { it.isNotBlank() }?.let { selftext ->
			Spacer(Modifier.height(4.dp))
			Text(
				text = selftext,
				style = theme.subtitle,
				maxLines = bodyLines,
				overflow = TextOverflow.Ellipsis,
			)
		}
	}
}

/** Compact feed counts (1.2K / 3.4M) — the stats row + action buttons. */
internal fun formatFeedCount(value: Int): String {
	return when {
		value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
		value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
		else -> value.toString()
	}
}

/** Short relative age for card meta lines (5h, 2d, 3w). */
internal fun formatTimeAgoShort(timestampSeconds: Long): String {
	val diff = System.currentTimeMillis() / 1000 - timestampSeconds
	return when {
		diff < 60 -> "${diff}s"
		diff < 3600 -> "${diff / 60}m"
		diff < 86400 -> "${diff / 3600}h"
		diff < 604800 -> "${diff / 86400}d"
		else -> "${diff / 604800}w"
	}
}
