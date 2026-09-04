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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.State
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stormtroopercs.materialreader.R
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.compose.net.FileRequestResult
import com.stormtroopercs.materialreader.compose.net.NetRequestStatus
import com.stormtroopercs.materialreader.compose.net.fetchImage
import com.stormtroopercs.materialreader.compose.prefs.ComposePrefsSingleton
import com.stormtroopercs.materialreader.common.datastream.parseDataUri
import com.stormtroopercs.materialreader.settings.types.AppearanceTheme
import com.stormtroopercs.materialreader.settings.types.NavigationType
import com.stormtroopercs.materialreader.settings.types.ThemeLightness
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

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
	val onInbox: () -> Unit = {
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
	// The user's own action listings — u/<user>/<type> resolves through
	// UserPostListingURL (SAVED / HIDDEN / UPVOTED / DOWNVOTED / HISTORY).
	val onUpvoted: () -> Unit = {
		if (accountName != null) {
			navigator.navigate(PostList("u/$accountName/upvoted"))
			closeDrawer()
		} else {
			navigator.navigate(OAuthLogin)
		}
	}
	val onDownvoted: () -> Unit = {
		if (accountName != null) {
			navigator.navigate(PostList("u/$accountName/downvoted"))
			closeDrawer()
		} else {
			navigator.navigate(OAuthLogin)
		}
	}
	val onHidden: () -> Unit = {
		if (accountName != null) {
			navigator.navigate(PostList("u/$accountName/hidden"))
			closeDrawer()
		} else {
			navigator.navigate(OAuthLogin)
		}
	}
	val onSaved: () -> Unit = {
		if (accountName != null) {
			navigator.navigate(PostList("u/$accountName/saved"))
			closeDrawer()
		} else {
			navigator.navigate(OAuthLogin)
		}
	}
	val onHistory: () -> Unit = {
		if (accountName != null) {
			navigator.navigate(PostList("u/$accountName/history"))
			closeDrawer()
		} else {
			navigator.navigate(OAuthLogin)
		}
	}
	// Live preference toggles (no navigation, no drawer close): flip the light
	// theme to the opposite lightness (keeping the accent colour), and invert
	// the NSFW behaviour pref — same keys the Settings rows read.
	val prefs = ComposePrefsSingleton.instance
	val onLightTheme: () -> Unit = {
		val current = prefs.appearanceTheme.value
		val opposite = if (current.lightness == ThemeLightness.Light) {
			AppearanceTheme.NIGHT
		} else {
			AppearanceTheme.RED
		}
		prefs.appearanceTheme.value = opposite
	}
	val onNsfwToggle: () -> Unit = {
		prefs.behaviourNsfw.value = !prefs.behaviourNsfw.value
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
						onInbox = onInbox,
						onProfile = onProfile,
						onUpvoted = onUpvoted,
						onDownvoted = onDownvoted,
						onHidden = onHidden,
						onSaved = onSaved,
						onHistory = onHistory,
						onLightTheme = onLightTheme,
						onNsfwToggle = onNsfwToggle,
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
 * The left navigation drawer — the reference app's main menu (its
 * `fragment_drawer` / `holder_drawer_header` layout): a stacked account header
 * (circular avatar above the username and karma), then the Account / Post /
 * Preferences sections of icon rows, the last three with switches. Opened
 * from the tab screens' hamburger in the `Drawer` and `Both` navigation
 * styles. Rows without a destination in this app (the reference's
 * Subscriptions / MultiReddits) are omitted rather than shipped as dead
 * buttons.
 */
@Composable
private fun AppDrawer(
	accountName: String?,
	onInbox: () -> Unit,
	onProfile: () -> Unit,
	onUpvoted: () -> Unit,
	onDownvoted: () -> Unit,
	onHidden: () -> Unit,
	onSaved: () -> Unit,
	onHistory: () -> Unit,
	onLightTheme: () -> Unit,
	onNsfwToggle: () -> Unit,
) {
	val signedIn = !accountName.isNullOrBlank()

	val accountViewModel = hiltViewModel<DrawerAccountViewModel>()
	val karma by accountViewModel.karma.collectAsStateWithLifecycle()
	val iconUrl by accountViewModel.iconUrl.collectAsStateWithLifecycle()

	val prefs = ComposePrefsSingleton.instance
	// The Switch reads the live pref (ComposePrefsImpl registers a shared-prefs
	// observer that recomposes on change); the theme row likewise.
	val nsfwEnabled = prefs.behaviourNsfw.value

	// Kick off the karma/avatar fetch once per username (the ViewModel no-ops
	// on a repeat).
	LaunchedEffect(accountName) {
		accountViewModel.loadUser(accountName)
	}

	// The drawer panel is transparent by default (ModalNavigationDrawer only
	// paints the scrim) — a solid page-background fill keeps the content
	// behind it from bleeding through the rows.
	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background),
	) {
		// Stacked account header (the reference's holder_drawer_header):
		// circular avatar, username beneath, karma beneath that.
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp),
			horizontalAlignment = Alignment.Start,
		) {
			DrawerAvatar(iconUrl = iconUrl)
			Spacer(Modifier.height(10.dp))
			Text(
				text = accountName?.takeIf { it.isNotBlank() } ?: "Sign in to Reddit",
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.SemiBold,
				color = MaterialTheme.colorScheme.onSurface,
				maxLines = 1,
			)
			if (signedIn && karma != null) {
				Spacer(Modifier.height(2.dp))
				Text(
					text = "Karma: $karma",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					maxLines = 1,
				)
			}
			if (!signedIn) {
				Spacer(Modifier.height(8.dp))
				TextButton(onClick = onProfile) {
					Text("Log in to Reddit")
				}
			}
		}

		// ACCOUNT section.
		DrawerSectionHeader("Account")
		DrawerRow(
			title = "Profile",
			icon = Icons.Filled.Person,
			onClick = onProfile,
		)
		DrawerRow(
			title = "Inbox",
			icon = Icons.Filled.AddComment,
			onClick = onInbox,
		)
		DrawerRow(
			title = "History",
			icon = Icons.Filled.History,
			onClick = onHistory,
		)

		// POST section — the user's own action listings (u/<user>/<type>).
		DrawerSectionHeader("Post")
		DrawerRow(
			title = "Upvoted",
			icon = Icons.Filled.ArrowUpward,
			onClick = onUpvoted,
		)
		DrawerRow(
			title = "Downvoted",
			icon = Icons.Filled.ArrowDownward,
			onClick = onDownvoted,
		)
		DrawerRow(
			title = "Hidden",
			icon = Icons.Filled.Lock,
			onClick = onHidden,
		)
		DrawerRow(
			title = "Saved",
			icon = Icons.Filled.Bookmark,
			onClick = onSaved,
		)

		// PREFERENCES section — live switches, no navigation.
		DrawerSectionHeader("Preferences")
		DrawerSwitchRow(
			title = "Light Theme",
			icon = Icons.Filled.LightMode,
			checked = prefs.appearanceTheme.value.lightness == ThemeLightness.Light,
			onCheckedChange = { onLightTheme() },
		)
		DrawerSwitchRow(
			title = "Disable NSFW",
			icon = Icons.Filled.VisibilityOff,
			checked = !nsfwEnabled,
			onCheckedChange = { onNsfwToggle() },
		)

		Spacer(Modifier.weight(1f))
	}
}

/** A section header in the drawer: a caption + hairline divider. */
@Composable
private fun DrawerSectionHeader(text: String) {
	Column(modifier = Modifier.fillMaxWidth()) {
		Text(
			text = text,
			modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
			style = MaterialTheme.typography.labelLarge,
			fontWeight = FontWeight.SemiBold,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)
		HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
	}
}

/** A drawer list row: 24dp icon + label, ripple on the whole row. */
@Composable
private fun DrawerRow(
	title: String,
	icon: androidx.compose.ui.graphics.vector.ImageVector,
	onClick: () -> Unit,
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick)
			.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Icon(
			imageVector = icon,
			contentDescription = null,
			modifier = Modifier.size(24.dp),
			tint = MaterialTheme.colorScheme.onSurfaceVariant,
		)
		Spacer(Modifier.width(20.dp))
		Text(
			text = title,
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onSurface,
			maxLines = 1,
		)
	}
}

/** A drawer list row with a trailing switch (the Preferences section). */
@Composable
private fun DrawerSwitchRow(
	title: String,
	icon: androidx.compose.ui.graphics.vector.ImageVector,
	checked: Boolean,
	onCheckedChange: (Boolean) -> Unit,
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Icon(
			imageVector = icon,
			contentDescription = null,
			modifier = Modifier.size(24.dp),
			tint = MaterialTheme.colorScheme.onSurfaceVariant,
		)
		Spacer(Modifier.width(20.dp))
		Text(
			text = title,
			modifier = Modifier.weight(1f),
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onSurface,
			maxLines = 1,
		)
		Switch(
			checked = checked,
			onCheckedChange = onCheckedChange,
		)
	}
}

/**
 * The drawer header's circular account avatar: the modern `icon` field (a
 * base64 data URI, decoded in memory) or the legacy `icon_img` URL through
 * the fetch pipeline — the same two-source logic as the profile screen —
 * with a default-Snoo fallback while loading or when there is no picture.
 */
@Composable
private fun DrawerAvatar(iconUrl: String?) {
	val parsed = remember(iconUrl) {
		iconUrl?.takeIf { it.isNotEmpty() }?.let { parseDataUri(it) }
	}
	val dataUriBitmap: ImageBitmap? = parsed?.let { decodeDrawerAvatarImage(it.bytes) }

	val imageState: State<NetRequestStatus<FileRequestResult<ImageBitmap>>>? =
		if (dataUriBitmap == null && !iconUrl.isNullOrEmpty()) {
			fetchImage(UriString(iconUrl), scaleToMaxAxis = 128)
		} else null

	val st = imageState?.value
	Box(
		modifier = Modifier
			.size(72.dp)
			.clip(CircleShape)
			.background(
				Brush.linearGradient(
					listOf(
						MaterialTheme.colorScheme.secondaryContainer,
						MaterialTheme.colorScheme.primaryContainer,
					),
				),
			),
		contentAlignment = Alignment.Center,
	) {
		when {
			dataUriBitmap != null -> {
				Image(
					bitmap = dataUriBitmap,
					contentDescription = "Account avatar",
					modifier = Modifier
						.size(72.dp)
						.clip(CircleShape),
				)
			}
			st is NetRequestStatus.Success -> {
				Image(
					bitmap = st.result.data,
					contentDescription = "Account avatar",
					modifier = Modifier
						.size(72.dp)
						.clip(CircleShape),
				)
			}
			else -> {
				Icon(
					painter = painterResource(R.drawable.ic_community_default_snoo),
					contentDescription = "Account avatar",
					modifier = Modifier.size(44.dp),
					tint = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		}
	}
}

/** Decodes a data-URI avatar payload, downscaled to a 72dp header size. */
private fun decodeDrawerAvatarImage(bytes: ByteArray, maxAxis: Int = 160): ImageBitmap? {
	return try {
		val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
		val result = if (max(decoded.width, decoded.height) <= maxAxis) {
			decoded
		} else {
			val scale = maxAxis / max(decoded.width, decoded.height).toFloat()
			decoded.scale(
				(decoded.width * scale).roundToInt(),
				(decoded.height * scale).roundToInt(),
			)
		}
		result.asImageBitmap()
	} catch (e: Exception) {
		null
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
