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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.cache.CacheManager
import com.stormtroopercs.materialreader.cache.CacheRequest
import com.stormtroopercs.materialreader.cache.CacheRequestJSONParser
import com.stormtroopercs.materialreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import com.stormtroopercs.materialreader.common.Constants
import com.stormtroopercs.materialreader.common.Priority
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.common.datastream.parseDataUri
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.compose.net.NetRequestStatus
import com.stormtroopercs.materialreader.compose.net.fetchImage
import com.stormtroopercs.materialreader.jsonwrap.JsonValue
import com.stormtroopercs.materialreader.reddit.things.RedditThing
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Process-wide resolver for a user's circular account picture, keyed by
 * (lower-cased) username.
 *
 * Reddit's comment listing payload carries no per-commenter picture (there is
 * no `author_pic` field), so — like [SubredditIconResolver] for communities —
 * the feed resolves it on demand from the user's own about endpoint
 * (`/user/<name>/about.json`) through the standard cache manager, parsing the
 * `RedditThing` envelope the same way the profile screen does
 * (`asUser().iconUrl`). The modern API returns the picture as a base64 data
 * URI in `icon` (with `icon_size`); the legacy `icon_img` is a plain URL.
 * `iconUrl` prefers the data URI and falls back to the URL.
 *
 * Results are cached for the process lifetime (a failed lookup is cached as
 * "no icon" so a comment list never re-hammers a dead account) and concurrent
 * lookups for the same name share one request. The fetch runs on a
 * process-owned scope (detached from any composition): a scrolling list
 * recycling its rows must not cancel a fetch other rows are awaiting.
 */
object UserIconResolver {

	/** Sentinel for a user whose about lookup failed / has no picture. */
	private const val NO_ICON = ""

	private val resolved = HashMap<String, String>()
	private val inFlight = HashMap<String, CompletableDeferred<String?>>()

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	/** The picture (data URI or URL) if already resolved this process (null = unknown). */
	@Synchronized
	fun knownIcon(name: String): String? {
		val key = normalize(name) ?: return null
		return resolved[key]?.takeIf { it != NO_ICON }
	}

	/**
	 * Resolve the user's account-picture value, awaiting an in-flight lookup
	 * when another caller already asked for it. Returns null for blank names,
	 * accounts with no picture, or a failed lookup.
	 */
	suspend fun resolveIcon(name: String, context: Context): String? {
		val key = normalize(name) ?: return null
		if (resolved.containsKey(key)) return resolved[key]?.takeIf { it != NO_ICON }
		return getOrCreateInFlight(key, context).await()
			?.takeIf { it != NO_ICON }
	}

	private fun normalize(name: String): String? = name.trim().lowercase(Locale.US).takeIf { it.isNotBlank() }

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
				android.util.Log.w("UserIconResolver", "Failed to fetch icon for $key", e)
				NO_ICON
			}
			synchronized(this@UserIconResolver) {
				resolved[key] = icon
				inFlight.remove(key)
			}
			deferred.complete(icon)
		}
		return deferred
	}

	private suspend fun fetch(key: String, context: Context): String? {
		val account = RedditAccountManager.getInstance(context).defaultAccount
		val cacheManager = CacheManager.getInstance(context)
		val deferred = CompletableDeferred<String?>()
		val listener = object : CacheRequestJSONParser.Listener {
			override fun onJsonParsed(
				result: JsonValue,
				timestamp: TimestampUTC,
				session: UUID,
				fromCache: Boolean,
			) {
				val user = result.asObject(RedditThing::class.java)?.asUser()
				deferred.complete(user?.iconUrl?.value)
			}

			override fun onFailure(error: RRError) {
				deferred.complete(null)
			}
		}
		val request = CacheRequest(
			Constants.Reddit.getUri("/user/$key/about.json"),
			account,
			null,
			Priority(Constants.Priority.API_USER_ABOUT),
			DownloadStrategyIfNotCached.INSTANCE,
			Constants.FileType.USER_ABOUT,
			CacheRequest.DownloadQueueType.REDDIT_API,
			context,
			CacheRequestJSONParser(context, listener),
		)
		cacheManager.makeRequest(request)
		return deferred.await()
	}
}

/**
 * A user's circular account picture (7.1): the real profile picture resolved
 * via [UserIconResolver] — a modern base64 data URI decoded in memory (no
 * network) or a legacy plain URL through the [fetchImage] pipeline — circular
 * clipped. When the account has no picture (or the lookup is still in flight
 * / failed), it falls back to [CommentAvatar] (the coloured initial).
 */
@Composable
fun UserAvatar(
	name: String?,
	modifier: Modifier = Modifier,
	size: Dp = 16.dp,
) {
	if (name == null || name.isBlank()) {
		CommentAvatar(name = name, show = false)
		return
	}

	val context = LocalContext.current
	// The process cache is authoritative at composition time; when it has no
	// entry yet, the resolver fills [value] asynchronously (a failed lookup
	// resolves to null → the initial fallback stays).
	val iconValue by produceState<String?>(UserIconResolver.knownIcon(name), name) {
		if (value == null) {
			value = UserIconResolver.resolveIcon(name, context)
		}
	}

	val value = iconValue
	Box(
		modifier = modifier.size(size).clip(CircleShape),
		contentAlignment = Alignment.Center,
	) {
		// Fallback layer: the coloured initial always sits underneath so a
		// pending or failed picture fetch never leaves an empty circle.
		CommentAvatar(name = name, show = true, size = size)
		if (value != null) {
			AccountPicture(value = value, size = size)
		}
	}
}

/**
 * Renders a resolved account picture [value]: a base64 data URI is decoded
 * in memory (no network round-trip), a plain URL goes through [fetchImage].
 * While a URL fetch is in flight (or on failure) nothing is drawn here — the
 * coloured initial in [UserAvatar]'s fallback layer shows through.
 */
@Composable
private fun AccountPicture(
	value: String,
	size: Dp,
) {
	val parsed = remember(value) {
		value.takeIf { it.isNotEmpty() }?.let { parseDataUri(it) }
	}

	// A data URI: decode directly to a bitmap (no network).
	val dataUriBitmap = parsed?.let { decodeDataUriImage(it.bytes, 128) }
	if (dataUriBitmap != null) {
		Image(
			bitmap = dataUriBitmap,
			contentDescription = null,
			contentScale = ContentScale.Crop,
			modifier = Modifier.fillMaxSize().clip(CircleShape),
		)
		return
	}

	// Otherwise it's a plain URL: fetch it.
	val data by fetchImage(UriString(value), scaleToMaxAxis = 128)
	if (data is NetRequestStatus.Success) {
		Image(
			bitmap = (data as NetRequestStatus.Success).result.data,
			contentDescription = null,
			contentScale = ContentScale.Crop,
			modifier = Modifier.fillMaxSize().clip(CircleShape),
		)
	}
}

/**
 * Decodes a data-URI payload to an [ImageBitmap], downscaled so its longest
 * axis is at most [maxAxis] px (an avatar is shown at 16dp — the oversized
 * decode is only a transient memory cost of a few hundred KB).
 */
private fun decodeDataUriImage(bytes: ByteArray, maxAxis: Int = 128): ImageBitmap? {
	return try {
		val decoded: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
		val w = decoded.width
		val h = decoded.height
		val result = if (max(w, h) <= maxAxis) {
			decoded
		} else {
			val scale = maxAxis / max(w, h).toFloat()
			decoded.scale((w * scale).roundToInt(), (h * scale).roundToInt(), true)
		}
		result.asImageBitmap()
	} catch (e: Exception) {
		null
	}
}
