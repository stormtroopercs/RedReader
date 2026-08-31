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
import com.stormtroopercs.materialreader.reddit.PostSort
import com.stormtroopercs.materialreader.reddit.kthings.RedditIdAndType
import com.stormtroopercs.materialreader.common.General

class UserPostListingURL internal constructor(
    val type: Type,
    val user: String?,
    order: PostSort?,
    val limit: Int?,
    val before: String?,
    val after: RedditIdAndType?
) : PostListingURL() {
    override val order: PostSort?

    init {
        this.order = if (order == PostSort.RISING) PostSort.NEW else order
    }

    enum class Type {
        SAVED, HIDDEN, UPVOTED, DOWNVOTED, SUBMITTED
    }

    override fun after(newAfter : RedditIdAndType): UserPostListingURL {
        return UserPostListingURL(type, user, order, limit, before, newAfter)
    }

    override fun limit(newLimit: Int?): UserPostListingURL {
        return UserPostListingURL(type, user, order, newLimit, before, after)
    }

    fun sort(newOrder: PostSort?): UserPostListingURL {
        return UserPostListingURL(type, user, newOrder, limit, before, after)
    }

    override fun generateJsonUri(): Uri {
        val builder = Uri.Builder()
        builder.scheme(Reddit.scheme)
            .authority(Reddit.domain)

        builder.appendEncodedPath("user")
        builder.appendPath(user)
        builder.appendEncodedPath(StringUtils.asciiLowercase(type.name))

        if (order != null) {
            order.addToUserPostListingUri(builder)
        }

        if (before != null) {
            builder.appendQueryParameter("before", before)
        }

        if (after != null) {
            builder.appendQueryParameter("after", after.value)
        }

        if (limit != null) {
            builder.appendQueryParameter("limit", limit.toString())
        }

        builder.appendEncodedPath(".json")

        return builder.build()
    }

    @RedditURLParser.PathType
    override fun pathType(): Int {
        return RedditURLParser.USER_POST_LISTING_URL
    }

    override fun humanReadablePath(): String {
        val path = super.humanReadablePath()

        if (order == null || type != Type.SUBMITTED) {
            return path
        }

        when (order) {
            PostSort.CONTROVERSIAL_HOUR, PostSort.CONTROVERSIAL_DAY, PostSort.CONTROVERSIAL_WEEK, PostSort.CONTROVERSIAL_MONTH, PostSort.CONTROVERSIAL_YEAR, PostSort.CONTROVERSIAL_ALL, PostSort.TOP_HOUR, PostSort.TOP_DAY, PostSort.TOP_WEEK, PostSort.TOP_MONTH, PostSort.TOP_YEAR, PostSort.TOP_ALL -> {
                val parts: Array<String?> =                     order.name.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                return (path + "?sort=" + StringUtils.asciiLowercase(parts[0]!!)
                        + "&t=" + StringUtils.asciiLowercase(parts[1]!!))
            }

            else -> return path + "?sort=" + StringUtils.asciiLowercase(order.name)
        }
    }

    override fun humanReadableName(context: Context, shorter: Boolean): String {
        val name: String

        when (type) {
            Type.SAVED -> name = context.getString(string.mainmenu_saved)
            Type.HIDDEN -> name = context.getString(string.mainmenu_hidden)
            Type.UPVOTED -> name = context.getString(string.mainmenu_upvoted)
            Type.DOWNVOTED -> name = context.getString(string.mainmenu_downvoted)
            Type.SUBMITTED -> name = context.getString(string.mainmenu_submitted)
        }

        if (shorter) {
            return name
        } else {
            return String.format("%s (%s)", name, user)
        }
    }

    companion object {
        fun getSaved(username: String?): UserPostListingURL {
            return UserPostListingURL(Type.SAVED, username, null, null, null, null)
        }

        fun getHidden(username: String?): UserPostListingURL {
            return UserPostListingURL(Type.HIDDEN, username, null, null, null, null)
        }

        fun getLiked(username: String?): UserPostListingURL {
            return UserPostListingURL(Type.UPVOTED, username, null, null, null, null)
        }

        fun getDisliked(username: String?): UserPostListingURL {
            return UserPostListingURL(Type.DOWNVOTED, username, null, null, null, null)
        }

        fun getSubmitted(username: String?): UserPostListingURL {
            return UserPostListingURL(Type.SUBMITTED, username, null, null, null, null)
        }

        fun parse(uri: Uri): UserPostListingURL? {
            var limit: Int?=null
            var before: String?=null
            var after: RedditIdAndType?=null

            for (parameterKey in getUriQueryParameterNames(uri)) {
                if (parameterKey.equals("after", ignoreCase = true)) {
                    after = RedditIdAndType(uri.getQueryParameter(parameterKey)!!)
                } else if (parameterKey.equals("before", ignoreCase = true)) {
                    before = uri.getQueryParameter(parameterKey)
                } else if (parameterKey.equals("limit", ignoreCase = true)) {
                    try {
                        limit = uri.getQueryParameter(parameterKey)!!.toInt()
                    } catch (ignored: Throwable) {
                    }
                }
            }

            val pathSegments: Array<String?>
            run {
                val pathSegmentsList = uri.getPathSegments()
                val pathSegmentsFiltered = ArrayList<String?>(
                    pathSegmentsList.size
                )
                for (segment in pathSegmentsList) {
                    var segment = segment
                    while (StringUtils.asciiLowercase(segment).endsWith(".json")
                        || StringUtils.asciiLowercase(segment).endsWith(".xml")
                    ) {
                        segment = segment.substring(0, segment.lastIndexOf('.'))
                    }

                    if (!segment.isEmpty()) {
                        pathSegmentsFiltered.add(segment)
                    }
                }

                pathSegments = pathSegmentsFiltered.toTypedArray<String?>()
            }

            val order: PostSort?
            if (pathSegments.size > 0) {
                order = PostSort.Companion.parse(
                    uri.getQueryParameter("sort"),
                    uri.getQueryParameter("t")
                )
            } else {
                order = null
            }

            if (pathSegments.size < 3) {
                return null
            }

            if (!pathSegments[0].equals("user", ignoreCase = true) && !pathSegments[0].equals(
                    "u", ignoreCase = true
                )
            ) {
                return null
            }

            // TODO validate username with regex
            val username = pathSegments[1]
            val typeName = StringUtils.asciiUppercase(pathSegments[2]!!)
            val type: Type

            try {
                type = Type.valueOf(typeName)
            } catch (t: Throwable) {
                return null
            }

            return UserPostListingURL(type, username, order, limit, before, after)
        }
    }
}
