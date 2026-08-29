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
 * `feedId=mode` comma list; an unknown feed falls back to the app default
 * ([PostViewMode.CARDS]). The swipe slots are global (the reference has a
 * single Post-options set, not per-feed).
 *
 * Plain Kotlin object (no `@Singleton`): it reads/writes the app's
 * SharedPreferences directly, needs no DI graph, and is constructible in
 * plain-JUnit tests via [init].
 */
object FeedPreferences {

	private const val KEY_VIEW_MODES = "feed_view_modes"
	private const val KEY_SORTS = "feed_sorts"
	private const val KEY_SWIPE_1 = "post_swipe_action_1"
	private const val KEY_SWIPE_2 = "post_swipe_action_2"
	private const val KEY_SWIPE_3 = "post_swipe_action_3"
	private const val KEY_SWIPE_VIBRATE = "post_swipe_vibrate"

	/** The host context, resolved lazily (set by [init] from the Application). */
	private var contextRef: Context? = null

	private val prefs get() = General.getSharedPrefs(contextRef ?: error(
		"FeedPreferences not initialized (call init(context) from the Application)"
	))

	/**
	 * The sort option the feed identified by [feedId] was last browsed
	 * with (FINAL-DESIGN Phase 4.7: sort is persisted per feed). Unknown
	 * feeds fall back to "Active" (the listing's own default order).
	 */
	fun sortOptionIdFor(feedId: String): String = parseMapping(KEY_SORTS)[feedId] ?: "active"

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
	}

	/** The card mode for the feed identified by [feedId] (e.g. `r/<name>`). */
	fun viewModeFor(feedId: String): PostViewMode {
		val stored = parseMapping(KEY_VIEW_MODES)[feedId] ?: return PostViewMode.CARDS
		return PostViewMode.entries.firstOrNull { it.stringValue == stored } ?: PostViewMode.CARDS
	}

	fun setViewModeFor(feedId: String, mode: PostViewMode) {
		putMapping(KEY_VIEW_MODES, feedId, mode.stringValue)
	}

	/** Whether the feed has an explicit view-mode selection (Phase 4.7). */
	fun hasViewModeFor(feedId: String): Boolean = parseMapping(KEY_VIEW_MODES)[feedId] != null

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

	/**
	 * Parse a `key=value` comma list. Feed ids and modes are both
	 * restricted (no `=` or `,`), so a naive split is safe; a malformed
	 * entry is dropped rather than throwing.
	 */
	internal fun parseViewModes(): Map<String, String> {
		return parseMapping(KEY_VIEW_MODES)
	}

	// ── Init / test plumbing ──

	/** Point the singleton at an explicit context (Application.onCreate). */
	fun init(context: Context) {
		contextRef = context
	}

	/** Drop the context override (plain-JUnit tests reset state between cases). */
	fun resetForTesting() {
		contextRef = null
	}
}
