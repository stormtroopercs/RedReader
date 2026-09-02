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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * The reference app's reusable list row: a 32dp circular avatar/icon, a title,
 * an optional subtitle, 32dp min height, ripple on the whole row. Used by the
 * drawer, settings, and account lists (DESIGN §3.2).
 */
@Composable
fun MaterialRow(
	title: String,
	icon: ImageVector? = null,
	subtitle: String? = null,
	modifier: Modifier = Modifier,
	onClick: (() -> Unit)? = null,
) {
	Row(
		modifier = modifier
			.fillMaxWidth()
			.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
			.padding(horizontal = 12.dp, vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		if (icon != null) {
			Icon(
				imageVector = icon,
				contentDescription = null,
				modifier = Modifier.size(32.dp),
				tint = MaterialTheme.colorScheme.onSurfaceVariant,
			)
			Spacer(Modifier.width(16.dp))
		}
		Column {
			Text(
				text = title,
				style = MaterialTheme.typography.bodyLarge,
				color = MaterialTheme.colorScheme.onSurface,
			)
			if (subtitle != null) {
				Text(
					text = subtitle,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		}
	}
}

/**
 * The app shell (FINAL-DESIGN Phase 2): the reference's 2-tab bottom
 * navigation bar (Posts / Explore). Shown at the root of a top-level tab and —
 * per FINAL-DESIGN Phase 3.4 — beneath the Posts tab's feed screen (a direct
 * `PostList` child), where the swipe slides render above it; deeper pushes
 * (comments, post submit, …) go full-bleed with their own app bar. The left
 * navigation drawer (the `BOTH`/`DRAWER` modes of the `navigation_type` pref)
 * is added in a later increment; the default bottom-bar mode is what ships
 * here.
 */
@Composable
fun AppShell(
	navigationState: NavigationState,
	content: @Composable () -> Unit,
) {
	val isPostsRoot = navigationState.topLevelRoute == Main && navigationState.activeBackStack.size == 1
	// The feed screen: exactly one child (PostList) directly under the Posts
	// root — other depth-2 pushes (settings, inbox, …) stay full-bleed.
	val postsStack = navigationState.activeBackStack
	val isPostsFeed = navigationState.topLevelRoute == Main && postsStack.size == 2 && postsStack[1] is PostList
	val isExploreRoot = navigationState.topLevelRoute == Explore && navigationState.activeBackStack.size == 1
	val showBottomBar = isPostsRoot || isPostsFeed || isExploreRoot

	Scaffold(
		bottomBar = {
			if (showBottomBar) {
				NavigationBar {
					NavigationBarItem(
						selected = isPostsRoot || isPostsFeed,
						onClick = {
							navigationState.switchTopLevel(Main)
							// Tapping the current tab returns to its root (the
							// reference's tab behaviour).
							val stack = navigationState.activeBackStack
							if (stack.size > 1) {
								navigationState.popToRoot()
							}
						},
						icon = {
							// The reference's Posts tab uses a home icon (FINAL-DESIGN
							// Phase 2.1) — not a chat/forum bubble.
							Icon(
								imageVector = Icons.Filled.Home,
								contentDescription = "Posts",
							)
						},
						label = { Text("Posts") },
					)
					NavigationBarItem(
						selected = isExploreRoot,
						onClick = {
							navigationState.switchTopLevel(Explore)
							val stack = navigationState.activeBackStack
							if (stack.size > 1) {
								navigationState.popToRoot()
							}
						},
						icon = {
							Icon(
								imageVector = Icons.Filled.Explore,
								contentDescription = "Explore",
							)
						},
						label = { Text("Explore") },
					)
				}
			}
		},
	) { paddingValues ->
		Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
			content()
		}
	}
}
