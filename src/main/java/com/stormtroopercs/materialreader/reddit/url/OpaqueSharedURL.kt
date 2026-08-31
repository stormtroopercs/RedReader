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
package com.stormtroopercs.materialreader.reddit.url

import android.net.Uri
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.reddit.url.RedditURLParser.RedditURL

class OpaqueSharedURL private constructor(
	val subreddit: String?,
	val user: String?,
	val shareKey: String?,
) : RedditURL() {
	override fun generateJsonUri(): Uri? = null

	override fun pathType(): Int = RedditURLParser.OPAQUE_SHARED_URL

	val urlToFetch: UriString
		get() {
			if (subreddit != null) {
				return UriString(
					String.format("https://www.reddit.com/r/%s/s/%s", subreddit, shareKey),
				)
			} else if (user != null) {
				return UriString(
					String.format("https://www.reddit.com/u/%s/s/%s", user, shareKey),
				)
			} else {
				throw RuntimeException("Neither subreddit nor user set")
			}
		}

	companion object {
		fun parse(uri: Uri): OpaqueSharedURL? {
			// URLs look like https://reddit.com/r/MaterialReader/s/<alphanumeric>
			// first pull out the path segments and ensure they match the example (should be 4)
			val pathSegments = uri.getPathSegments()
			if (pathSegments.size != 4) {
				return null
			}

			// Ensure the first segment is "r" or "u", and the third is "s"
			if (pathSegments.get(2) == "s") {
				if (pathSegments.get(0) == "r") {
					return OpaqueSharedURL(pathSegments.get(1), null, pathSegments.get(3))
				} else if (pathSegments.get(0) == "u") {
					return OpaqueSharedURL(pathSegments.get(1), pathSegments.get(3), null)
				} else {
					return null
				}
			} else {
				return null
			}
		}
	}
}
