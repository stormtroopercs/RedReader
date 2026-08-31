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
import com.stormtroopercs.materialreader.common.Constants.Reddit
import com.stormtroopercs.materialreader.common.General.getUriQueryParameterNames
import com.stormtroopercs.materialreader.common.StringUtils
import com.stormtroopercs.materialreader.reddit.PostSort
import com.stormtroopercs.materialreader.reddit.kthings.RedditIdAndType
import com.stormtroopercs.materialreader.reddit.url.RedditURLParser.RedditURL
import java.util.Locale
import com.stormtroopercs.materialreader.common.General

class MultiredditPostListURL private constructor(
    val username: String?,
    val name: String,
    override val order: PostSort?,
    val limit: Int?,
    val before: String?,
    val after: RedditIdAndType?
) : PostListingURL() {
    override fun after(newAfter : RedditIdAndType): MultiredditPostListURL {
        return MultiredditPostListURL(username, name, order, limit, before, newAfter)
    }

    override fun limit(newLimit: Int?): MultiredditPostListURL {
        return MultiredditPostListURL(username, name, order, newLimit, before, after)
    }

    fun sort(newOrder: PostSort?): MultiredditPostListURL {
        return MultiredditPostListURL(username, name, newOrder, limit, before, after)
    }

    override fun generateJsonUri(): Uri {
        val builder = Uri.Builder()
        builder.scheme(Reddit.scheme)
            .authority(Reddit.domain)

        if (username != null) {
            builder.encodedPath("/user/")
            builder.appendPath(username)
        } else {
            builder.encodedPath("/me/")
        }

        builder.appendPath("m")
        builder.appendPath(name)

        if (order != null) {
            order.addToSubredditListingUri(builder)
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
        return RedditURLParser.MULTIREDDIT_POST_LISTING_URL
    }

    override fun humanReadablePath(): String {
        val path = super.humanReadablePath()

        if (order == null) {
            return path
        }

        when (order) {
            PostSort.CONTROVERSIAL_HOUR, PostSort.CONTROVERSIAL_DAY, PostSort.CONTROVERSIAL_WEEK, PostSort.CONTROVERSIAL_MONTH, PostSort.CONTROVERSIAL_YEAR, PostSort.CONTROVERSIAL_ALL, PostSort.TOP_HOUR, PostSort.TOP_DAY, PostSort.TOP_WEEK, PostSort.TOP_MONTH, PostSort.TOP_YEAR, PostSort.TOP_ALL -> return path + "?t=" + StringUtils.asciiLowercase(
                order.name.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
            )

            else -> return path
        }
    }

    override fun humanReadableName(context : Context, shorter: Boolean): String {
        if (username == null) {
            return name
        } else {
            return String.format(Locale.US, "%s (%s)", name, username)
        }
    }

    companion object {
        fun getMultireddit(
            name: String
        ): RedditURL? {
            val builder = Uri.Builder()
            builder.scheme(Reddit.scheme)
                .authority(Reddit.domain)

            builder.encodedPath("/me/m/")
            builder.appendPath(name)

            return RedditURLParser.parse(builder.build())
        }

        fun getMultireddit(
            username: String,
            name: String
        ): RedditURL? {
            val builder = Uri.Builder()
            builder.scheme(Reddit.scheme)
                .authority(Reddit.domain)

            builder.encodedPath("/user/")
            builder.appendPath(username)
            builder.appendPath("m")
            builder.appendPath(name)

            return RedditURLParser.parse(builder.build())
        }

        fun parse(uri: Uri): MultiredditPostListURL? {
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
                    pathSegments[pathSegments.size - 1],
                    uri.getQueryParameter("t")
                )
            } else {
                order = null
            }

            if (pathSegments.size < 3) {
                return null
            }

            if (pathSegments[pathSegments.size - 1].equals("search", ignoreCase = true)) {
                return null
            }

            if (pathSegments[0].equals("me", ignoreCase = true)) {
                if (!pathSegments[1].equals("m", ignoreCase = true)) {
                    return null
                }

                return MultiredditPostListURL(
                    null,
                    pathSegments[2]!!,
                    order,
                    limit,
                    before,
                    after
                )
            } else {
                if (!(pathSegments[0].equals(
                        "user",
                        ignoreCase = true
                    ) || pathSegments[0].equals("u", ignoreCase = true)) || !pathSegments[2].equals(
                        "m",
                        ignoreCase = true
                    ) || pathSegments.size < 4
                ) {
                    return null
                }

                return MultiredditPostListURL(
                    pathSegments[1],
                    pathSegments[3]!!,
                    order,
                    limit,
                    before,
                    after
                )
            }
        }
    }
}
