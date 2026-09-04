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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.stormtroopercs.materialreader.compose.prefs.ComposePrefsSingleton
import com.stormtroopercs.materialreader.settings.types.NavigationType

/**
 * Set by [AppShell] to a lambda that opens the navigation drawer, or `null`
 * when the drawer is disabled (the `Bottom` navigation style). Tab-root
 * screens read this to decide whether to render a hamburger affordance.
 */
val LocalOpenDrawer = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * The reference app's reusable list row: a 32dp circular avatar/icon, a title,
 * an optional subtitle, 32dp min height, ripple on the whole row. Used by the
 * drawer, settings, and account lists (DESIGN §3.2).
 */
@Composable
fun MaterialRow(
	title: String,
	icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
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
 * navigation bar (Posts / Explore), shown at the root of a top-level tab and —
 * per FINAL-DESIGN Phase 3.4 — beneath the Posts tab's feed screen (a direct
 * `PostList` child), where the swipe slides render above it; deeper pushes
 * (comments, post submit, …) go full-bleed with their own app bar.
 *
 * The primary navigation style is driven by the `navigation_type` pref
 * (FINAL-DESIGN Phase 2.2):
 *   - [NavigationType.BOTTOM] (default) — bottom bar only.
 *   - [NavigationType.DRAWER] — a left [ModalNavigationDrawer] instead of the
 *     bottom bar; opened from a hamburger in the tab screens' app bars.
 *   - [NavigationType.BOTH] — the bottom bar **and** the drawer.
 */
@Composable
fun AppShell(
	navigationState: NavigationState,
	accountName: String? = null,
	content: @Composable () -> Unit,
) {
	val navType = ComposePrefsSingleton.instance.navigationType.value
	val showDrawer = navType != NavigationType.BOTTOM

	val isPostsRoot = navigationState.topLevelRoute == Main && navigationState.activeBackStack.size == 1
	// The feed screen: exactly one child (PostList) directly under the Posts
	// root — other depth-2 pushes (settings, inbox, …) stay full-bleed.
	val postsStack = navigationState.activeBackStack
	val isPostsFeed = navigationState.topLevelRoute == Main && postsStack.size == 2 && postsStack[1] is PostList
	val isExploreRoot = navigationState.topLevelRoute == Explore && navigationState.activeBackStack.size == 1
	// In DRAWER style the bottom bar is replaced by the drawer.
	val showBottomBar = navType != NavigationType.DRAWER && (isPostsRoot || isPostsFeed || isExploreRoot)

	val drawerState = remember { DrawerState(initialValue = DrawerValue.Closed) }
	val scope = rememberCoroutineScope()
	val navigator = Navigator(navigationState)
	val closeDrawer: () -> Unit = { scope.launch { drawerState.close() } }

	// Drawer navigation actions. Each navigates, then closes the drawer.
	val onPosts: () -> Unit = {
		navigationState.switchTopLevel(Main)
		navigationState.popToRoot()
		closeDrawer()
	}
	val onExplore: () -> Unit = {
		navigationState.switchTopLevel(Explore)
		navigationState.popToRoot()
		closeDrawer()
	}
	val onPopular: () -> Unit = {
		navigator.navigate(PostList("popular"))
		closeDrawer()
	}
	val onMessages: () -> Unit = {
		navigator.navigate(Inbox)
		closeDrawer()
	}
	val onProfile: () -> Unit = {
		if (accountName.isNullOrBlank()) {
			navigator.navigate(OAuthLogin)
		} else {
			navigator.navigate(UserProfile(accountName))
		}
		closeDrawer()
	}
	val onSettings: () -> Unit = {
		navigator.navigate(Settings)
		closeDrawer()
	}
	val onSearch: () -> Unit = {
		navigator.navigate(SubredditSearch)
		closeDrawer()
	}

	val scaffold: @Composable () -> Unit = {
		AppShellScaffold(
			navigationState = navigationState,
			showBottomBar = showBottomBar,
			isPostsRoot = isPostsRoot,
			isPostsFeed = isPostsFeed,
			isExploreRoot = isExploreRoot,
			content = content,
		)
	}

	CompositionLocalProvider(
		LocalOpenDrawer provides (if (showDrawer) { { scope.launch { drawerState.open() } } } else null),
	) {
		if (showDrawer) {
			ModalNavigationDrawer(
				drawerState = drawerState,
				drawerContent = {
					AppDrawer(
						accountName = accountName,
						onPosts = onPosts,
						onExplore = onExplore,
						onPopular = onPopular,
						onMessages = onMessages,
						onProfile = onProfile,
						onSettings = onSettings,
						onSearch = onSearch,
					)
				},
			) {
				scaffold()
			}
		} else {
			scaffold()
		}
	}
}

/**
 * The shell's [Scaffold]: the Posts/Explore bottom navigation bar (when
 * enabled) + the screen content. Split out from [AppShell] so the drawer and
 * the no-drawer paths share the identical scaffold.
 */
@Composable
private fun AppShellScaffold(
	navigationState: NavigationState,
	showBottomBar: Boolean,
	isPostsRoot: Boolean,
	isPostsFeed: Boolean,
	isExploreRoot: Boolean,
	content: @Composable () -> Unit,
) {
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

/**
 * The left navigation drawer (FINAL-DESIGN Phase 2.2, DESIGN §3.2): an account
 * header (circular avatar + username + subtitle), a search field, and a list of
 * [MaterialRow] destinations. Opened from the tab screens' hamburger in the
 * `Drawer` and `Both` navigation styles.
 */
@Composable
private fun AppDrawer(
	accountName: String?,
	onPosts: () -> Unit,
	onExplore: () -> Unit,
	onPopular: () -> Unit,
	onMessages: () -> Unit,
	onProfile: () -> Unit,
	onSettings: () -> Unit,
	onSearch: () -> Unit,
) {
	val signedIn = !accountName.isNullOrBlank()
	// The drawer panel is transparent by default (ModalNavigationDrawer only
	// paints the scrim) — a solid page-background fill keeps the content
	// behind it from bleeding through the rows.
	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background),
	) {
		// Account header: circular avatar + username + subtitle (DESIGN §3.2).
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp, vertical = 16.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Box(
				modifier = Modifier
					.size(40.dp)
					.clip(CircleShape)
					.background(MaterialTheme.colorScheme.primaryContainer),
				contentAlignment = Alignment.Center,
			) {
				Icon(
					imageVector = Icons.Filled.Person,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.onPrimaryContainer,
				)
			}
			Spacer(Modifier.width(16.dp))
			Column {
				Text(
					text = accountName?.takeIf { it.isNotBlank() }?.let { "u/$it" } ?: "Sign in to Reddit",
					style = MaterialTheme.typography.titleMedium,
					color = MaterialTheme.colorScheme.onSurface,
					maxLines = 1,
				)
				Text(
					text = if (signedIn) "Profile and sign out" else "Login required",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					maxLines = 1,
				)
			}
		}

		HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

		// The account row opens the signed-in user's profile (or the login flow
		// when signed out) — mirrors the reference's header-tap behaviour.
		MaterialRow(
			title = if (signedIn) "Profile" else "Sign in",
			icon = Icons.Filled.Person,
			subtitle = if (signedIn) accountName else "Log in to Reddit",
			onClick = onProfile,
		)

		// Search field (single line, IME action "search" → subreddit search).
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp, vertical = 8.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			OutlinedTextField(
				value = "",
				onValueChange = {},
				placeholder = { Text("Search") },
				leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
				readOnly = true,
				keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
				keyboardActions = KeyboardActions(onSearch = { onSearch() }),
			)
		}

		HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

		MaterialRow(
			title = "Posts",
			icon = Icons.Filled.Home,
			subtitle = "Your home feed",
			onClick = onPosts,
		)
		MaterialRow(
			title = "Explore",
			icon = Icons.Filled.Explore,
			subtitle = "Discover communities",
			onClick = onExplore,
		)
		MaterialRow(
			title = "Popular",
			icon = Icons.Filled.Public,
			subtitle = "Popular across Reddit",
			onClick = onPopular,
		)
		MaterialRow(
			title = "Messages",
			icon = Icons.Filled.Message,
			subtitle = "Inbox",
			onClick = onMessages,
		)
		MaterialRow(
			title = "Settings",
			icon = Icons.Filled.Settings,
			subtitle = "Preferences and appearance",
			onClick = onSettings,
		)

		Spacer(Modifier.weight(1f))
	}
}

/**
 * A slim app bar shown on the tab roots only when the drawer is enabled
 * (the `Drawer` / `Both` navigation styles): a hamburger that opens the
 * navigation drawer + the tab title. Kept out of [MainScreen]/[ExploreScreen]
 * so the default `Bottom` style stays byte-for-byte the same as before.
 */
@Composable
internal fun DrawerHamburgerBar(
	title: String,
	modifier: Modifier = Modifier,
) {
	val openDrawer = LocalOpenDrawer.current ?: return
	Row(
		modifier = modifier
			.fillMaxWidth()
			.height(56.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		IconButton(onClick = openDrawer) {
			Icon(
				imageVector = Icons.Filled.Menu,
				contentDescription = "Open navigation menu",
			)
		}
		Text(
			text = title,
			modifier = Modifier.padding(start = 8.dp),
			style = MaterialTheme.typography.titleLarge,
			fontWeight = FontWeight.SemiBold,
			color = MaterialTheme.colorScheme.onSurface,
		)
	}
}
