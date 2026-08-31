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

package com.stormtroopercs.materialreader.compose.ctx

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.net.toUri
import com.stormtroopercs.materialreader.account.RedditAccountChangeListener
import com.stormtroopercs.materialreader.account.RedditAccountId
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.activities.BaseActivity
import com.stormtroopercs.materialreader.activities.MainActivityCompose
import com.stormtroopercs.materialreader.common.AndroidCommon
import com.stormtroopercs.materialreader.common.General
import com.stormtroopercs.materialreader.common.LinkHandler
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.compose.prefs.ComposePrefsSingleton
import com.stormtroopercs.materialreader.compose.prefs.LocalComposePrefs
import com.stormtroopercs.materialreader.compose.prefs.Preference
import com.stormtroopercs.materialreader.compose.theme.RRComposeContextTheme
import com.stormtroopercs.materialreader.fragments.ErrorPropertiesDialog
import com.stormtroopercs.materialreader.image.AlbumInfo

/**
 * @param controlsStatusBar Whether to set the status bar appearance to match
 * the theme -- see [RRComposeContextTheme].
 */
@Composable
fun RRComposeContext(
	activity: BaseActivity,
	controlsStatusBar: Boolean = false,
	content: @Composable () -> Unit
) {
	val accountManager = remember { RedditAccountManager.getInstance(activity) }

	var currentAccountId by remember {
		mutableStateOf(RedditAccountId(accountManager.defaultAccount.canonicalUsername))
	}

	DisposableEffect(Unit) {
		val updateListener = RedditAccountChangeListener {
			AndroidCommon.runOnUiThread {
				currentAccountId = RedditAccountId(accountManager.defaultAccount.canonicalUsername)
			}
		}

		accountManager.addUpdateListener(updateListener)

		onDispose {
			accountManager.removeUpdateListener(updateListener)
		}
	}

	CompositionLocalProvider(
		LocalRedditUser provides currentAccountId,
		LocalComposePrefs provides ComposePrefsSingleton.instance,
		LocalLauncher provides {
			when (it) {
				Dest.Settings -> {
					// Legacy hosts (e.g. the image viewer) can't switch the
					// Compose stack themselves: hand the settings deep link to
					// MainActivityCompose, which owns the Navigation 3 state.
					val intent = Intent(activity, MainActivityCompose::class.java)
					intent.putExtra(
						MainActivityCompose.EXTRA_DEEP_LINK,
						MainActivityCompose.DEEP_LINK_SETTINGS
					)
					activity.startActivity(intent)
				}

				is Dest.Link -> {
					LinkHandler.onLinkClicked(
						activity = activity,
						url = it.url,
						albumInfo = it.albumInfo,
						albumImageIndex = it.albumImageIndex
					)
				}

				is Dest.LinkLongClick -> {
					LinkHandler.onLinkLongClicked(
						activity = activity,
						uri = it.url,
						forceNoImage = false
					)
				}

				is Dest.ResultDialog -> {
					General.showResultDialog(activity, it.error)
				}

				is Dest.SaveMedia -> {
					LinkHandler.onActionMenuItemSelected(
						uri = it.url,
						activity = activity,
						action = LinkHandler.LinkAction.SAVE_IMAGE
					)
				}

				is Dest.ShareLink -> {
					LinkHandler.onActionMenuItemSelected(
						uri = it.url,
						activity = activity,
						action = LinkHandler.LinkAction.SHARE
					)
				}

				is Dest.ShareMedia -> {
					LinkHandler.onActionMenuItemSelected(
						uri = it.url,
						activity = activity,
						action = LinkHandler.LinkAction.SHARE_IMAGE
					)
				}

				is Dest.WebBrowser -> {
					LinkHandler.openWebBrowser(activity, it.url.toUri(), false)
				}

				is Dest.ErrorPropertiesDialog -> {
					ErrorPropertiesDialog.newInstance(it.error)
						.show(activity.supportFragmentManager, null)
				}

				Dest.AccountsList -> {
					// The account list is a Nav3 route (Main + Accounts):
					// hand it to MainActivityCompose as a deep link, mirroring
					// how the legacy host reached the AccountListDialog.
					val intent = Intent(activity, MainActivityCompose::class.java)
					intent.putExtra(
						MainActivityCompose.EXTRA_DEEP_LINK,
						MainActivityCompose.DEEP_LINK_ACCOUNTS
					)
					activity.startActivity(intent)
				}

				Dest.RedditTerms -> {
					val intent = Intent(activity, MainActivityCompose::class.java)
					intent.putExtra(
						MainActivityCompose.EXTRA_DEEP_LINK,
						MainActivityCompose.DEEP_LINK_TERMS
					)
					activity.startActivity(intent)
				}
			}
		},
	) {
		RRComposeContextTheme(controlsStatusBar = controlsStatusBar) {
			content()
		}
	}
}

val LocalRedditUser = staticCompositionLocalOf { RedditAccountId.ANON }

val LocalLauncher = staticCompositionLocalOf<(Dest) -> Unit> {
	throw Exception("LocalLauncher not set")
}

// Increment this to retry all failed in-scope network requests
@Suppress("PropertyName")
val GlobalNetworkRetry = mutableIntStateOf(0)

sealed interface Dest {

	data object Settings : Dest

	data class Link(
		val url: UriString,
		val albumInfo: AlbumInfo?=null,
		val albumImageIndex: Int?=null
	) : Dest

	data class LinkLongClick(
		val url: UriString
	) : Dest

	data class ResultDialog(
		val error: RRError
	) : Dest

	data class ErrorPropertiesDialog(
		val error: RRError
	) : Dest

	data class SaveMedia(
		val url: UriString
	) : Dest

	data class ShareMedia(
		val url: UriString
	) : Dest

	data class ShareLink(
		val url: UriString
	) : Dest

	data class WebBrowser(
		val url: String
	) : Dest

	data object RedditTerms : Dest

	data object AccountsList : Dest
}

private fun <T> testPref(value: T) = object : Preference<T> {
	override var value: T
		get() = value
		set(value) {}
}
