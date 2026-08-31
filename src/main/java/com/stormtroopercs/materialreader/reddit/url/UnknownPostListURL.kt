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
import com.stormtroopercs.materialreader.reddit.kthings.RedditIdAndType

class UnknownPostListURL internal constructor(private val uri: Uri) : PostListingURL() {
	override fun after(after: RedditIdAndType): PostListingURL = UnknownPostListURL(
		uri.buildUpon()
			.appendQueryParameter("after", after.value)
			.build(),
	)

	override fun limit(limit: Int?): PostListingURL = UnknownPostListURL(
		uri.buildUpon()
			.appendQueryParameter(
				"limit",
				limit.toString(),
			)
			.build(),
	)

	// TODO handle this better
	override fun generateJsonUri(): Uri? {
		if (uri.getPath()!!.endsWith(".json")) {
			return uri
		} else {
			return uri.buildUpon().appendEncodedPath(".json").build()
		}
	}

	@RedditURLParser.PathType
	override fun pathType(): Int = RedditURLParser.UNKNOWN_POST_LISTING_URL
}
