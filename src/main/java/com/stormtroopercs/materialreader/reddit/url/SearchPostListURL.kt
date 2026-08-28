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
import com.stormtroopercs.materialreader.common.PrefsUtility
import com.stormtroopercs.materialreader.common.StringUtils
import com.stormtroopercs.materialreader.reddit.PostSort
import com.stormtroopercs.materialreader.reddit.kthings.RedditIdAndType
import com.stormtroopercs.materialreader.common.General

class SearchPostListURL : PostListingURL {
    val type: Type

    val subreddit: String?

    val username: String?
    val name: String?

    val query: String?
    override var order: PostSort?
    val limit: Int?
    val before: String?
    val after: RedditIdAndType?

    enum class Type {
        SUB_OR_SUB_COMBO, MULTI
    }

    internal constructor(
        subreddit: String?,
        query: String?,
        order: PostSort?,
        limit: Int?,
        before: String?,
        after: RedditIdAndType?
    ) {
        this.subreddit = subreddit
        this.query = query
        this.order = order
        this.limit = limit
        this.before = before
        this.after = after

        this.type = Type.SUB_OR_SUB_COMBO
        this.username = null
        this.name = null
    }

    internal constructor(
        subreddit: String?,
        query: String?,
        limit: Int?,
        before: String?,
        after: RedditIdAndType?
    ) : this(subreddit, query, PostSort.RELEVANCE_ALL, limit, before, after)

    internal constructor(
        username: String?,
        name: String?,
        query: String?,
        order: PostSort?,
        limit: Int?,
        before: String?,
        after: RedditIdAndType?
    ) {
        this.username = username
        this.name = name
        this.query = query
        this.order = order
        this.limit = limit
        this.before = before
        this.after = after

        this.type = Type.MULTI
        this.subreddit = null
    }

    internal constructor(
        username: String?,
        name: String?,
        query: String?,
        limit: Int?,
        before: String?,
        after: RedditIdAndType?
    ) : this(username, name, query, PostSort.RELEVANCE_ALL, limit, before, after)

    override fun after(after : RedditIdAndType): PostListingURL {
        if (type == Type.SUB_OR_SUB_COMBO) {
            return SearchPostListURL(subreddit, query, order, limit, before, after)
        } else {
            return SearchPostListURL(username, name, query, order, limit, before, after)
        }
    }

    override fun limit(limit: Int?): PostListingURL {
        if (type == Type.SUB_OR_SUB_COMBO) {
            return SearchPostListURL(subreddit, query, order, limit, before, after)
        } else {
            return SearchPostListURL(username, name, query, order, limit, before, after)
        }
    }

    fun sort(newOrder: PostSort?): SearchPostListURL {
        if (type == Type.SUB_OR_SUB_COMBO) {
            return SearchPostListURL(subreddit, query, newOrder, limit, before, after)
        } else {
            return SearchPostListURL(username, name, query, newOrder, limit, before, after)
        }
    }

    override fun generateJsonUri(): Uri? {
        val builder = Uri.Builder()
        builder.scheme(Reddit.scheme)
            .authority(Reddit.domain)

        if (type == Type.SUB_OR_SUB_COMBO && subreddit != null) {
            builder.encodedPath("/r/")
            builder.appendPath(subreddit)
            builder.appendQueryParameter("restrict_sr", "on")
        } else if (type == Type.MULTI && name != null) {
            if (username != null) {
                builder.encodedPath("/user/")
                builder.appendPath(username)
            } else {
                builder.encodedPath("/me/")
            }

            builder.appendPath("m")
            builder.appendPath(name)
            builder.appendQueryParameter("restrict_sr", "on")
        } else {
            builder.encodedPath("/")
        }

        builder.appendEncodedPath("search")

        if (query != null) {
            builder.appendQueryParameter("q", query)
        }

        if (order != null) {
            val sortOrder = order
            when (sortOrder) {
                PostSort.RELEVANCE_HOUR, PostSort.RELEVANCE_DAY, PostSort.RELEVANCE_WEEK, PostSort.RELEVANCE_MONTH, PostSort.RELEVANCE_YEAR, PostSort.RELEVANCE_ALL, PostSort.NEW_HOUR, PostSort.NEW_DAY, PostSort.NEW_WEEK, PostSort.NEW_MONTH, PostSort.NEW_YEAR, PostSort.NEW_ALL, PostSort.HOT_HOUR, PostSort.HOT_DAY, PostSort.HOT_WEEK, PostSort.HOT_MONTH, PostSort.HOT_YEAR, PostSort.HOT_ALL, PostSort.TOP_HOUR, PostSort.TOP_DAY, PostSort.TOP_WEEK, PostSort.TOP_MONTH, PostSort.TOP_YEAR, PostSort.TOP_ALL, PostSort.COMMENTS_HOUR, PostSort.COMMENTS_DAY, PostSort.COMMENTS_WEEK, PostSort.COMMENTS_MONTH, PostSort.COMMENTS_YEAR, PostSort.COMMENTS_ALL -> {
                    val parts: Array<String> =                         sortOrder.name.split("_".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    builder.appendQueryParameter("sort", StringUtils.asciiLowercase(parts[0]))
                    builder.appendQueryParameter("t", StringUtils.asciiLowercase(parts[1]))
                }
                else -> {
                    // Java switch had no default: non-searchable sorts fall through to no-op
                }
            }
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

        // Only set over18 when NSFW content is enabled, to save on bandwidth and loading times
        if (PrefsUtility.pref_behaviour_nsfw()) {
            builder.appendQueryParameter("include_over_18", "on")
        }

        return builder.build()
    }

    @RedditURLParser.PathType
    override fun pathType(): Int {
        return RedditURLParser.SEARCH_POST_LISTING_URL
    }

    override fun humanReadableName(context: Context, shorter: Boolean): String {
        if (shorter) {
            return context.getString(string.search_results_short)
        }

        val formattedLocation: String?
        if (type == Type.SUB_OR_SUB_COMBO) {
            if (subreddit != null) {
                formattedLocation = "/r/" + subreddit
            } else {
                formattedLocation = null
            }
        } else {
            if (name != null) {
                if (username != null) {
                    formattedLocation = "/u/" + username + "/m/" + name
                } else {
                    formattedLocation = "/me/m/" + name
                }
            } else {
                formattedLocation = null
            }
        }

        if (query != null && formattedLocation != null) {
            return String.format(
                context.getString(string.search_results_query_and_location),
                query,
                formattedLocation
            )
        } else if (query != null) {
            return String.format(
                context.getString(string.search_results_query_only),
                query
            )
        } else if (formattedLocation != null) {
            return String.format(
                context.getString(string.search_results_location_only),
                formattedLocation
            )
        }

        return context.getString(string.action_search)
    }

    override fun humanReadablePath(): String {
        val builder = StringBuilder(super.humanReadablePath())

        if (query != null) {
            builder.append("?q=").append(query)
        }

        return builder.toString()
    }

    companion object {
        fun build(location: String?, query: String?): SearchPostListURL {
            var location = location
            if (location != null) {
                while (location!!.startsWith("/")) {
                    location = location.substring(1)
                }

                //Create a multi SearchPostListURL, if needed
                if (location.startsWith("user/")
                    || location.startsWith("u/")
                    || location.startsWith("me/m/")
                    || location.startsWith("m/")
                ) {
                    val locationSegments: Array<String?> =                         location.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                    val username: String?
                    val name: String?
                    if ((location.startsWith("user/") || location.startsWith("u/"))
                        && locationSegments.size == 4
                    ) {
                        username = locationSegments[1]
                        name = locationSegments[3]
                    } else if (location.startsWith("me/m/") && locationSegments.size == 3) {
                        username = null
                        name = locationSegments[2]
                    } else if (location.startsWith("m/") && locationSegments.size == 2) {
                        username = null
                        name = locationSegments[1]
                    } else {
                        // This will fail, but the user can fix it instead of typing from scratch.
                        return SearchPostListURL(location, query, null, null, null)
                    }

                    return SearchPostListURL(username, name, query, null, null, null)
                }

                while (location!!.startsWith("r/")) {
                    location = location.substring(2)
                }
            }

            return SearchPostListURL(location, query, null, null, null)
        }

        fun build(
            username: String?,
            name: String?,
            query: String?
        ): SearchPostListURL {
            return SearchPostListURL(username, name, query, null, null, null)
        }

        fun parse(uri: Uri): SearchPostListURL? {
            var restrictSubreddit = false
            var query: String?=""
            val order: PostSort?
            var limit: Int?=null
            var before: String?=null
            var after: RedditIdAndType?=null

            var sortParam: String?=null
            var timeParam: String?=null

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
                } else if (parameterKey.equals("sort", ignoreCase = true)) {
                    sortParam = uri.getQueryParameter(parameterKey)
                } else if (parameterKey.equals("t", ignoreCase = true)) {
                    timeParam = uri.getQueryParameter(parameterKey)
                } else if (parameterKey.equals("q", ignoreCase = true)) {
                    query = uri.getQueryParameter(parameterKey)
                } else if (parameterKey.equals("restrict_sr", ignoreCase = true)) {
                    restrictSubreddit =                         "on".equals(uri.getQueryParameter(parameterKey), ignoreCase = true)
                }
            }

            order = PostSort.Companion.parseSearch(sortParam, timeParam)

            val pathSegments: Array<String?>
            run {
                val pathSegmentsList = uri.getPathSegments()
                val pathSegmentsFiltered =                     ArrayList<String?>(pathSegmentsList.size)
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
                pathSegments =                     pathSegmentsFiltered.toTypedArray<String?>()
            }

            if (pathSegments.size != 1 && (pathSegments.size < 3 || pathSegments.size > 5)) {
                return null
            }
            if (!pathSegments[pathSegments.size - 1].equals("search", ignoreCase = true)) {
                return null
            }

            when (pathSegments.size) {
                1 -> {
                    return SearchPostListURL(null, query, order, limit, before, after)
                }

                3 -> {
                    if (pathSegments[0] != "r") {
                        return null
                    }

                    val subreddit = pathSegments[1]
                    return SearchPostListURL(
                        if (restrictSubreddit) subreddit else null,
                        query,
                        order,
                        limit,
                        before,
                        after
                    )
                }

                4 -> {
                    run {
                        if (pathSegments[0] == "me") {
                            if (pathSegments[1] != "m") {
                                return null
                            }

                            val name = pathSegments[2]
                            return SearchPostListURL(
                                null,
                                name,
                                query,
                                order,
                                limit,
                                before,
                                after
                            )
                        }
                    }
                    run {
                        if (!(pathSegments[0] == "user" || pathSegments[0] == "u")
                            || pathSegments[2] != "m"
                        ) {
                            return null
                        }
                        val username = pathSegments[1]
                        val name = pathSegments[3]
                        return SearchPostListURL(
                            username,
                            name,
                            query,
                            order,
                            limit,
                            before,
                            after
                        )
                    }
                }

                5 -> {
                    if (!(pathSegments[0] == "user" || pathSegments[0] == "u")
                        || pathSegments[2] != "m"
                    ) {
                        return null
                    }

                    val username = pathSegments[1]
                    val name = pathSegments[3]
                    return SearchPostListURL(
                        username,
                        name,
                        query,
                        order,
                        limit,
                        before,
                        after
                    )
                }

                else -> return null
            }
        }
    }
}
