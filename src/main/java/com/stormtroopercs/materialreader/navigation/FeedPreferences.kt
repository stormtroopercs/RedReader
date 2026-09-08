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

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.stormtroopercs.materialreader.common.General
import com.stormtroopercs.materialreader.settings.types.PostSwipeAction
import com.stormtroopercs.materialreader.settings.types.PostViewMode

/**
 * Per-feed and global preferences for the post feed card/swipe behaviour
 * (FINAL-DESIGN Phase 4): the list-view card mode, the horizontal
 * swipe-to-action slots + haptics toggle, and the sort currently selected
 * for the **community directory** (the reference's Explore screen).
 *
 * Per-feed values (the view mode) live in one shared-prefs key as a
 * `feedId=mode` comma list; an unknown feed falls back to the feed default
 * (community feeds → [PostViewMode.SLIDES], everything else
 * [PostViewMode.CARDS] — see [effectiveViewMode]). The swipe slots are
 * global (the reference has a single Post-options set, not per-feed).
 *
 * Plain Kotlin object (no `@Singleton`): it reads/writes the app's
 * SharedPreferences directly, needs no DI graph, and is constructible in
 * plain-JUnit tests via [init].
 *
 * The view-mode map is additionally held as Compose snapshot state
 * ([viewModes]): the navigation entry that decides which feed surface to
 * render reads it reactively, so a Change-View selection swaps the surface
 * in place via recomposition instead of re-navigating (a re-navigation
 * pushed a second, equal `NavKey` entry and blanked the screen).
 */
object FeedPreferences {

	private const val KEY_VIEW_MODES = "feed_view_modes"
	private const val KEY_SORTS = "feed_sorts"
	private const val KEY_SWIPE_1 = "post_swipe_action_1"
	private const val KEY_SWIPE_2 = "post_swipe_action_2"
	private const val KEY_SWIPE_3 = "post_swipe_action_3"
	private const val KEY_SWIPE_VIBRATE = "post_swipe_vibrate"
	private const val KEY_DEFAULT_VIEW_MODE = "feed_default_view_mode"

	// ── "More actions" grid (FINAL-DESIGN Phase 5) ──
	private const val KEY_ACTION_ORDER = "more_actions_order"
	private const val KEY_HIDDEN_ACTIONS = "more_actions_hidden"
	private const val KEY_CUSTOM_TARGET = "more_actions_custom_target"
	private const val KEY_LAST_LIGHT_THEME = "last_light_theme"

	/** The host context, resolved lazily (set by [init] from the Application). */
	private var contextRef: Context? = null

	private val prefs get() = General.getSharedPrefs(
		contextRef ?: error(
			"FeedPreferences not initialized (call init(context) from the Application)",
		),
	)

	/**
	 * The sort option the feed identified by [feedId] was last browsed
	 * with (FINAL-DESIGN Phase 4.7: sort is persisted per feed). Unknown
	 * feeds fall back to "Best" (the listing's own default order).
	 */
	fun sortOptionIdFor(feedId: String): String {
		val stored = parseMapping(KEY_SORTS)
		stored[feedId]?.let { return it }
		// Legacy keys from before feed ids were normalized (bare `Art`,
		// `r/Art`): match by the normalized form, case-insensitively.
		return stored.entries
			.firstOrNull { it.key.normalizeListingPath().equals(feedId, ignoreCase = true) }
			?.value ?: "best"
	}

	fun setSortOptionFor(feedId: String, optionId: String) {
		putMapping(KEY_SORTS, feedId, optionId)
	}

	/**
	 * Parse a `key=value` comma list from [key]. Keys/values are restricted
	 * (no `=` or `,`), so a naive split is safe; a malformed entry is
	 * dropped rather than throwing.
	 */
	private fun parseMapping(key: String): Map<String, String> {
		val raw = prefs.getString(key, null) ?: return emptyMap()
		val result = HashMap<String, String>()
		raw.split(",").forEach { entry ->
			val idx = entry.indexOf('=')
			if (idx > 0) {
				result[entry.substring(0, idx).trim()] = entry.substring(idx + 1).trim()
			}
		}
		return result
	}

	private fun putMapping(key: String, feedId: String, value: String) {
		val current = parseMapping(key).toMutableMap()
		current[feedId] = value
		prefs.edit()
			.putString(key, current.entries.joinToString(",") { "${it.key}=${it.value}" })
			.apply()
		if (key == KEY_VIEW_MODES) {
			viewModes = current
		}
	}

	/**
	 * The per-feed view-mode map as Compose snapshot state (raw
	 * `feedId=mode` pairs). Written by [setViewModeFor]; read reactively by
	 * the feed surfaces (the PostList entry, the community detail) so a
	 * Change-View selection recomposes the surface in place.
	 */
	var viewModes by mutableStateOf<Map<String, String>>(emptyMap())
		private set

	/**
	 * The global default view mode (Settings → Post options → "Default feed
	 * view"): the mode feeds open in when they have no per-feed choice from
	 * the Change-View sheet. `null` = the built-in per-feed defaults
	 * (communities → [PostViewMode.SLIDES], everything else
	 * [PostViewMode.CARDS]). Held as snapshot state so a change in Settings
	 * recomposes the feed surfaces (including ones kept composed under a
	 * newer nav entry).
	 */
	var defaultViewModeValue by mutableStateOf<String?>(null)
		private set

	/** The global default view mode, or null for the built-in defaults. */
	fun defaultViewMode(): PostViewMode? = defaultViewModeValue
		?.let { value -> PostViewMode.entries.firstOrNull { it.stringValue == value } }

	/** Store the global default ("default" / blank = the built-in defaults). */
	fun setDefaultViewMode(value: String) {
		prefs.edit()
			.putString(KEY_DEFAULT_VIEW_MODE, value.ifBlank { "default" })
			.apply()
		defaultViewModeValue = value.ifBlank { "default" }.takeIf { it != "default" }
	}

	/**
	 * The mode a feed opens in: the user's per-feed selection when one
	 * exists (Change View), then the global default (Settings), otherwise
	 * the feed default — community feeds open in the signature swipe feed
	 * (Phase 3), everything else in the list view (FINAL-DESIGN Phase 4.7).
	 * [feedId] must be the normalized [effectiveKey] form.
	 */
	fun effectiveViewMode(feedId: String): PostViewMode = if (hasViewModeFor(feedId)) {
		viewModeFor(feedId)
	} else {
		defaultViewMode()
			?: if (!feedId.startsWith("search:") && isCommunityFeedPath(feedId)) {
				PostViewMode.SLIDES
			} else {
				PostViewMode.CARDS
			}
	}

	/**
	 * The shared preference key for a listing: the normalized listing
	 * path (bare community names lowercased, `r/` prefixes stripped) with
	 * a `search:` prefix for search listings. Every surface that shows a
	 * listing (the PostList route, the community detail) resolves the
	 * same key here, so one feed has one view mode.
	 */
	fun effectiveKey(subreddit: String, searchQuery: String?): String {
		val base = subreddit.normalizeListingPath().ifBlank { "frontpage" }
		return if (searchQuery != null) "search:$base:$searchQuery" else base
	}

	/**
	 * True when [subreddit] (a normalized or raw listing path) is a
	 * community feed (r/<name>) — the feeds that open in the signature
	 * swipe feed by default.
	 */
	fun isCommunityFeedPath(subreddit: String): Boolean = subreddit.isNotBlank() &&
		!subreddit.startsWith("u/") &&
		!subreddit.startsWith("m/") &&
		!subreddit.startsWith("s/") &&
		subreddit != "frontpage" &&
		subreddit != "popular" &&
		subreddit != "all"

	/** The raw persisted mode for [feedId] (exact key, then a legacy key). */
	private fun storedMode(feedId: String): String? {
		viewModes[feedId]?.let { return it }
		// Legacy keys from before keys were normalized (bare `Art`,
		// `r/Art`): match by the normalized form, case-insensitively.
		return viewModes.entries
			.firstOrNull { it.key.normalizeListingPath().equals(feedId, ignoreCase = true) }
			?.value
	}

	/** The card mode the feed identified by [feedId] was last browsed in. */
	fun viewModeFor(feedId: String): PostViewMode {
		val stored = storedMode(feedId) ?: return PostViewMode.CARDS
		return PostViewMode.entries.firstOrNull { it.stringValue == stored } ?: PostViewMode.CARDS
	}

	fun setViewModeFor(feedId: String, mode: PostViewMode) {
		putMapping(KEY_VIEW_MODES, feedId, mode.stringValue)
	}

	/** Whether the feed has an explicit view-mode selection (Phase 4.7). */
	fun hasViewModeFor(feedId: String): Boolean = storedMode(feedId) != null

	fun swipeAction1(): PostSwipeAction = swipeAction(KEY_SWIPE_1, PostSwipeAction.UPVOTE)
	fun swipeAction2(): PostSwipeAction = swipeAction(KEY_SWIPE_2, PostSwipeAction.DOWNVOTE)
	fun swipeAction3(): PostSwipeAction = swipeAction(KEY_SWIPE_3, PostSwipeAction.HIDE)
	fun setSwipeAction1(action: PostSwipeAction) = putEnum(KEY_SWIPE_1, action)
	fun setSwipeAction2(action: PostSwipeAction) = putEnum(KEY_SWIPE_2, action)
	fun setSwipeAction3(action: PostSwipeAction) = putEnum(KEY_SWIPE_3, action)

	/** Optional haptic tick when a swipe action commits. Default on. */
	fun swipeVibrate(): Boolean = prefs.getBoolean(KEY_SWIPE_VIBRATE, true)
	fun setSwipeVibrate(on: Boolean) = prefs.edit().putBoolean(KEY_SWIPE_VIBRATE, on).apply()

	private fun swipeAction(key: String, default: PostSwipeAction): PostSwipeAction {
		val stored = prefs.getString(key, null) ?: return default
		return PostSwipeAction.entries.firstOrNull { it.stringValue == stored } ?: default
	}

	private fun putEnum(key: String, value: PostSwipeAction) {
		prefs.edit().putString(key, value.stringValue).apply()
	}

	/** The persisted per-feed view-mode map (Compose-observable). */
	internal fun parseViewModes(): Map<String, String> = viewModes

	// ── "More actions" grid (FINAL-DESIGN Phase 5) ──

	/**
	 * The user's "More actions" grid: a 4-column sheet of quick actions
	 * (FINAL-DESIGN Phase 5, audit §2.4). The reference ships 12 defaults in
	 * a fixed order; the user can long-press-drag to reorder (persisted as
	 * the ordered action-id list) and show/hide individual actions (persisted
	 * as the hidden set). [actionOrder] returns the visible order;
	 * [hiddenActions] the ids to omit.
	 */

	/** All known action ids, in the reference's default grid order. */
	fun defaultActionOrder(): List<String> = listOf(
		"search", "profile", "hide_read", "about",
		"submit", "random", "dark_mode", "settings",
		"change_view", "saved", "refresh", "custom",
	)

	/** The persisted grid order (defaults to [defaultActionOrder], ignoring any ids that no longer exist). */
	fun actionOrder(): List<String> {
		val raw = prefs.getString(KEY_ACTION_ORDER, null) ?: return defaultActionOrder()
		val saved = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
		val known = defaultActionOrder().toSet()
		val ordered = saved.filter { it in known }.toMutableList()
		// Re-append any default that the saved list dropped (a newer action),
		// so nothing is permanently lost to a stale order.
		defaultActionOrder().filter { it !in ordered }.forEach { ordered.add(it) }
		return ordered
	}

	fun setActionOrder(order: List<String>) {
		prefs.edit().putString(KEY_ACTION_ORDER, order.joinToString(",")).apply()
	}

	/** The set of action ids the user has hidden from the grid. */
	fun hiddenActions(): Set<String> {
		val raw = prefs.getString(KEY_HIDDEN_ACTIONS, null) ?: return emptySet()
		return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
	}

	fun setHiddenActions(hidden: Set<String>) {
		prefs.edit().putString(KEY_HIDDEN_ACTIONS, hidden.joinToString(",")).apply()
	}

	/**
	 * What the "custom" grid slot targets — the reference has one free slot
	 * the user can point at any other listing. Persisted as a list path
	 * (e.g. `u/<user>/submitted`, `r/all`, `m/<name>`); blank = the slot
	 * shows the default (frontpage) behaviour.
	 */
	fun customTarget(): String = prefs.getString(KEY_CUSTOM_TARGET, "") ?: ""

	fun setCustomTarget(target: String) {
		prefs.edit().putString(KEY_CUSTOM_TARGET, target).apply()
	}

	// ── Dark-mode quick toggle (FINAL-DESIGN Phase 5, "Dark mode" action) ──

	/**
	 * The light [AppearanceTheme] to restore when the user toggles back out
	 * of a dark theme. Remembered the first time the toggle is used, so
	 * "Dark mode" is a reversible light↔dark switch rather than always
	 * landing on one fixed light theme. (Post read-state itself is the
	 * account-scoped [RedditChangeDataManager]; this only stores the theme
	 * the grid toggle came from.)
	 */
	fun lastLightTheme(): String? = prefs.getString(KEY_LAST_LIGHT_THEME, null)

	fun setLastLightTheme(themeValue: String) {
		prefs.edit().putString(KEY_LAST_LIGHT_THEME, themeValue).apply()
	}

	// ── Init / test plumbing ──

	/** Point the singleton at an explicit context (Application.onCreate). */
	fun init(context: Context) {
		contextRef = context
		// Prime the observable view-mode map from the persisted value so
		// first composition reads the saved selections.
		viewModes = parseMapping(KEY_VIEW_MODES)
		// Prime the global default too ("default" = the built-in defaults).
		val storedDefault = prefs.getString(KEY_DEFAULT_VIEW_MODE, null)
		defaultViewModeValue = storedDefault?.takeIf { it != "default" }
	}

	/** Drop the context override (plain-JUnit tests reset state between cases). */
	fun resetForTesting() {
		contextRef = null
		viewModes = emptyMap()
		defaultViewModeValue = null
	}
}
