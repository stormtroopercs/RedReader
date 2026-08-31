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

import android.content.Context
import android.net.Uri
import com.stormtroopercs.materialreader.R.string
import com.stormtroopercs.materialreader.common.Constants.Reddit
import com.stormtroopercs.materialreader.common.General.getUriQueryParameterNames
import com.stormtroopercs.materialreader.common.StringUtils
import com.stormtroopercs.materialreader.reddit.UserCommentSort

class UserCommentListingURL internal constructor(
	val user: String?,
	val order: UserCommentSort?,
	val limit: Int?,
	val after: String?,
) : CommentListingURL() {
	override fun after(newAfter: String?): UserCommentListingURL = UserCommentListingURL(user, order, limit, newAfter)

	override fun limit(newLimit: Int?): UserCommentListingURL = UserCommentListingURL(user, order, newLimit, after)

	fun order(newOrder: UserCommentSort?): UserCommentListingURL = UserCommentListingURL(user, newOrder, limit, after)

	override fun generateJsonUri(): Uri? {
		val builder = Uri.Builder()
		builder.scheme(Reddit.scheme)
			.authority(Reddit.domain)

		builder.appendEncodedPath("user")
		builder.appendPath(user)
		builder.appendEncodedPath("comments")

		if (order != null) {
			order.addToUserCommentListingUri(builder)
		}

		if (after != null) {
			builder.appendQueryParameter("after", after)
		}

		if (limit != null) {
			builder.appendQueryParameter("limit", limit.toString())
		}

		builder.appendEncodedPath(".json")

		return builder.build()
	}

	@RedditURLParser.PathType
	override fun pathType(): Int = RedditURLParser.USER_COMMENT_LISTING_URL

	override fun humanReadableName(context: Context, shorter: Boolean): String {
		val name = context.getString(string.user_comments)

		if (shorter) {
			return name
		} else {
			return String.format("%s (%s)", name, user)
		}
	}

	companion object {
		fun parse(uri: Uri): UserCommentListingURL? {
			val pathSegments: Array<String>
			run {
				val pathSegmentsList = uri.getPathSegments()
				val pathSegmentsFiltered = ArrayList<String>(
					pathSegmentsList.size,
				)
				for (segment in pathSegmentsList) {
					var segment = segment
					while (StringUtils.asciiLowercase(segment).endsWith(".json") ||
						StringUtils.asciiLowercase(segment).endsWith(".xml")
					) {
						segment = segment.substring(0, segment.lastIndexOf('.'))
					}

					if (!segment.isEmpty()) {
						pathSegmentsFiltered.add(segment)
					}
				}

				pathSegments = pathSegmentsFiltered.toTypedArray()
			}

			val order: UserCommentSort?
			if (pathSegments.size > 0) {
				order = UserCommentSort.Companion.parse(
					uri.getQueryParameter("sort"),
					uri.getQueryParameter("t"),
				)
			} else {
				order = null
			}

			if (pathSegments.size < 3) {
				return null
			}

			if (!pathSegments[0].equals("user", ignoreCase = true) &&
				!pathSegments[0].equals(
					"u",
					ignoreCase = true,
				)
			) {
				return null
			}

			// TODO validate username with regex
			val username: String? = pathSegments[1]
			val typeName = pathSegments[2]

			if (!typeName.equals("comments", ignoreCase = true)) {
				return null
			}

			var limit: Int? = null
			var after: String? = null

			for (parameterKey in getUriQueryParameterNames(uri)) {
				if (parameterKey.equals("after", ignoreCase = true)) {
					after = uri.getQueryParameter(parameterKey)
				} else if (parameterKey.equals("limit", ignoreCase = true)) {
					try {
						limit = uri.getQueryParameter(parameterKey)!!.toInt()
					} catch (ignored: Throwable) {
					}
				}
			}

			return UserCommentListingURL(username, order, limit, after)
		}
	}
}
