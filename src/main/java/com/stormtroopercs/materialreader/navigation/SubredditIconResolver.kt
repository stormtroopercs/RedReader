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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.compose.net.NetRequestStatus
import com.stormtroopercs.materialreader.compose.net.fetchImage
import com.stormtroopercs.materialreader.io.RequestResponseHandler
import com.stormtroopercs.materialreader.reddit.api.RedditAPIIndividualSubredditDataRequester
import com.stormtroopercs.materialreader.reddit.things.RedditSubreddit
import com.stormtroopercs.materialreader.reddit.things.SubredditCanonicalId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Process-wide resolver for a subreddit's circular community icon, keyed by
 * (lower-cased) community name.
 *
 * The listing payload carries no per-post community icon (Reddit's modern
 * `subreddit_icon` field is absent from the JSON this app reads), so the
 * feed resolves it on demand from the community's own about endpoint
 * (`/r/<name>/about.json`) through the standard
 * [RedditAPIIndividualSubredditDataRequester] (same path the community pill
 * and the Explore directory use — the icon URL is the requester's
 * [RedditSubreddit.iconUrl] resolution: `icon_img` → `community_icon`
 * (HTML-escaped query unescaped) → `header_img`).
 *
 * Results are cached for the process lifetime (a failed lookup is cached as
 * "no icon" so a card list never re-hammers a dead community), and
 * concurrent lookups for the same name share one request. The fetch runs on
 * a process-owned scope (detached from any composition): a scrolling list
 * recycling its cards must not cancel a fetch other cards are awaiting.
 */
object SubredditIconResolver {

	/** Sentinel for a community whose about lookup failed (no icon). */
	private const val NO_ICON = "\u0000"

	private val resolved = HashMap<String, String>()
	private val inFlight = HashMap<String, CompletableDeferred<String?>>()

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	/** The icon URL if already resolved this process (null = unknown). */
	@Synchronized
	fun knownIcon(name: String): String? {
		val key = normalize(name) ?: return null
		return resolved[key]?.takeIf { it != NO_ICON }
	}

	/**
	 * Resolve the community's icon URL, awaiting an in-flight lookup when
	 * another caller already asked for it. Returns null for blank names,
	 * communities with no icon, or a failed lookup.
	 */
	suspend fun resolveIcon(name: String, context: Context): String? {
		val key = normalize(name) ?: return null
		if (resolved.containsKey(key)) return resolved[key]?.takeIf { it != NO_ICON }
		return getOrCreateInFlight(key, context).await()
			?.takeIf { it != NO_ICON }
	}

	private fun normalize(name: String): String? =
		name.trim().lowercase(Locale.US).takeIf { it.isNotBlank() }

	@Synchronized
	private fun getOrCreateInFlight(
		key: String,
		context: Context,
	): CompletableDeferred<String?> {
		inFlight[key]?.let { return it }
		val deferred = CompletableDeferred<String?>()
		inFlight[key] = deferred
		scope.launch {
			val icon = try {
				fetch(key, context) ?: NO_ICON
			} catch (e: Exception) {
				NO_ICON
			}
			synchronized(this@SubredditIconResolver) {
				resolved[key] = icon
				inFlight.remove(key)
			}
			deferred.complete(icon)
		}
		return deferred
	}

	private suspend fun fetch(key: String, context: Context): String? {
		val account = RedditAccountManager.getInstance(context).defaultAccount
		val requester = RedditAPIIndividualSubredditDataRequester(context, account)
		val deferred = CompletableDeferred<String?>()
		requester.performRequest(
			SubredditCanonicalId(key),
			null,
			object : RequestResponseHandler<RedditSubreddit, RRError> {
				override fun onRequestSuccess(result: RedditSubreddit, timeCached: TimestampUTC?) {
					deferred.complete(result.iconUrl?.toString())
				}

				override fun onRequestFailed(failureReason: RRError) {
					deferred.complete(null)
				}
			},
		)
		return deferred.await()
	}
}

/**
 * A community's circular icon (the community's own logo, fetched via
 * [SubredditIconResolver]) with the Snoo fallback for icon-less communities
 * (r/Home, …) or a failed lookup — the same treatment as the drawer's
 * Subscriptions rows. Sized by [size] and circular-clipped.
 */
@Composable
fun SubredditIcon(
	name: String,
	size: Dp = 20.dp,
	modifier: Modifier = Modifier,
) {
	val context = LocalContext.current
	// The process cache is authoritative at composition time; when it has
	// no entry yet, the resolver fills [value] asynchronously (a failed
	// lookup resolves to null → the Snoo fallback stays).
	val iconUrl by produceState<String?>(SubredditIconResolver.knownIcon(name), name) {
		if (value == null) {
			value = SubredditIconResolver.resolveIcon(name, context)
		}
	}

	Box(
		modifier = modifier.size(size).clip(CircleShape),
		contentAlignment = Alignment.Center,
	) {
		val url = iconUrl
		if (url != null) {
			val data by fetchImage(UriString(url), scaleToMaxAxis = 128)
			when (val it = data) {
				is NetRequestStatus.Success -> Image(
					bitmap = it.result.data,
					contentDescription = null,
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxSize().clip(CircleShape),
				)

				else -> CommunityDefaultAvatar()
			}
		} else {
			CommunityDefaultAvatar()
		}
	}
}
