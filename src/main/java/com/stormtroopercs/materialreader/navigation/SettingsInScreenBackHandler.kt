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

/**
 * Bridges the Settings screen's in-screen sub-states to the host activity's
 * system-back handling, so that a back press closes the topmost sub-state
 * before it can pop the whole Settings route (bug report 2026-08-30: the
 * category "folder" sub-screen must not be skipped by the system back key).
 *
 * [SettingsScreen] publishes the currently open topmost sub-state (if any)
 * and a close callback; the activity consults [goBack] / [canGoBack] from its
 * back-press overrides (see
 * [com.stormtroopercs.materialreader.activities.MainActivityCompose.baseActivityOnBackPressed])
 * and re-evaluates [onBackChanged] so the platform back-callback stays in the
 * right enabled/disabled state.
 *
 * This follows the [HtmlViewBackHandler] pattern rather than
 * `androidx.activity.compose.BackHandler`: at these androidx versions
 * (activity 1.12.0) `BackHandler` resolves a single owner from
 * `LocalNavigationEventDispatcherOwner` — provided by the Compose hosts as a
 * no-op dispatcher for `NavDisplay` — and only registers an
 * `OnBackPressedCallback` when that same owner is also an
 * `OnBackPressedDispatcherOwner`, which it is not here, so `BackHandler` would
 * silently never fire. The activity-consulted singleton works on every API
 * level because [com.stormtroopercs.materialreader.activities.BaseActivity]'s
 * legacy callback is the registered handler for the system back key.
 */
object SettingsInScreenBackHandler {

	/** The in-screen sub-states that can be open (topmost-first for back). */
	enum class SubState {
		/** The Theme colours panel (highest; opened from inside a category). */
		THEME_COLOURS,

		/** The Appbar sub-screen. */
		APPBAR,

		/** A category "folder" sub-screen. */
		CATEGORY,
	}

	/** The topmost open sub-state, or null when the root list is showing. */
	private var subState: SubState? = null

	/**
	 * Invoked on the main thread whenever [canGoBack] might have changed
	 * (a sub-state opened/closed), so that a host activity can re-evaluate
	 * whether it must intercept the system back key (see
	 * BaseActivity.invalidateBackPressedCallback()).
	 */
	var onBackChanged: (() -> Unit)? = null

	/** Invoked with the sub-state that [goBack] just closed. */
	var onClosed: ((SubState) -> Unit)? = null

	/** Whether a Settings sub-state is open and could consume a back press. */
	val canGoBack: Boolean
		get() = subState != null

	/** Publish (or clear) the currently open topmost sub-state. */
	fun setTopState(sub: SubState?) {
		if (this.subState != sub) {
			this.subState = sub
			onBackChanged?.invoke()
		}
	}

	/** Close the topmost open sub-state. `true` if the back press was consumed. */
	fun goBack(): Boolean {
		val current = subState ?: return false
		subState = null
		onClosed?.invoke(current)
		onBackChanged?.invoke()
		return true
	}

	/** Drop the registered sub-state (e.g. when the Settings route is disposed). */
	fun clear() {
		setTopState(null)
	}
}
