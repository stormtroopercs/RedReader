/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package org.quantumbadger.redreader.reddit.url

import android.content.Context
import android.net.Uri
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.common.Constants.Reddit
import org.quantumbadger.redreader.common.General.getUriQueryParameterNames
import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.reddit.PostSort
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import org.quantumbadger.redreader.reddit.things.InvalidSubredditNameException
import org.quantumbadger.redreader.reddit.things.SubredditCanonicalId
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL
import org.quantumbadger.redreader.common.General

class SubredditPostListURL private constructor(
    val type: Type,
    val subreddit: String?,
    val order: PostSort?,
    val limit: Int?,
    val before: String?,
    val after: RedditIdAndType?
) : PostListingURL() {
    enum class Type {
        FRONTPAGE, ALL, SUBREDDIT, SUBREDDIT_COMBINATION, ALL_SUBTRACTION, POPULAR
    }

    override fun after(newAfter : RedditIdAndType): SubredditPostListURL {
        return SubredditPostListURL(type, subreddit, order, limit, before, newAfter)
    }

    override fun limit(newLimit: Int?): SubredditPostListURL {
        return SubredditPostListURL(type, subreddit, order, newLimit, before, after)
    }

    fun sort(newOrder: PostSort?): SubredditPostListURL {
        return SubredditPostListURL(type, subreddit, newOrder, limit, before, after)
    }

    override fun getOrder(): PostSort? {
        return order
    }

    override fun generateJsonUri(): Uri? {
        val builder = Uri.Builder()
        builder.scheme(Reddit.scheme)
            .authority(Reddit.domain)

        when (type) {
            Type.FRONTPAGE -> builder.encodedPath("/")
            Type.ALL -> builder.encodedPath("/r/all")
            Type.SUBREDDIT, Type.SUBREDDIT_COMBINATION, Type.ALL_SUBTRACTION -> {
                builder.encodedPath("/r/")
                builder.appendPath(subreddit)
            }

            Type.POPULAR -> builder.encodedPath("/r/popular")
        }

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
        return RedditURLParser.SUBREDDIT_POST_LISTING_URL
    }

    override fun humanReadablePath(): String? {
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

    override fun humanReadableName(context: Context, shorter: Boolean): String? {
        when (type) {
            Type.FRONTPAGE -> return context.getString(string.mainmenu_frontpage)

            Type.ALL -> return context.getString(string.mainmenu_all)

            Type.POPULAR -> return context.getString(string.mainmenu_popular)

            Type.SUBREDDIT -> {
                try {
                    return SubredditCanonicalId(subreddit!!).toString()
                } catch (e: InvalidSubredditNameException) {
                    return subreddit
                }

                return subreddit
            }

            Type.SUBREDDIT_COMBINATION, Type.ALL_SUBTRACTION -> return subreddit

            else -> return super.humanReadableName(context, shorter)
        }
    }

    fun changeSubreddit(newSubreddit: String?): SubredditPostListURL {
        return SubredditPostListURL(type, newSubreddit, order, limit, before, after)
    }

    companion object {
        val frontPage: SubredditPostListURL
            get() = SubredditPostListURL(
                Type.FRONTPAGE,
                null,
                null,
                null,
                null,
                null
            )

        val popular: SubredditPostListURL
            get() = SubredditPostListURL(
                Type.POPULAR,
                null,
                null,
                null,
                null,
                null
            )

        val all: SubredditPostListURL
            get() = SubredditPostListURL(
                Type.ALL,
                null,
                null,
                null,
                null,
                null
            )

        @Throws(InvalidSubredditNameException::class)
        fun getSubreddit(subreddit: String): RedditURL? {
            return getSubreddit(SubredditCanonicalId(subreddit))
        }

        fun getSubreddit(subreddit: SubredditCanonicalId): RedditURL? {
            return RedditURLParser.parse(
                Uri.Builder()
                    .scheme(Reddit.scheme)
                    .authority(Reddit.domain)
                    .encodedPath(subreddit.toString()).build()
            )
        }

        fun parse(uri: Uri): SubredditPostListURL? {
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

            when (pathSegments.size) {
                0 -> return SubredditPostListURL(
                    Type.FRONTPAGE,
                    null,
                    null,
                    limit,
                    before,
                    after
                )

                1 -> {
                    if (order != null) {
                        return SubredditPostListURL(
                            Type.FRONTPAGE,
                            null,
                            order,
                            limit,
                            before,
                            after
                        )
                    } else {
                        return null
                    }
                }

                2, 3 -> {
                    if (pathSegments[0] != "r") {
                        return null
                    }

                    val subreddit = StringUtils.asciiLowercase(pathSegments[1]!!)

                    if (subreddit == "all") {
                        if (pathSegments.size == 2) {
                            return SubredditPostListURL(
                                Type.ALL,
                                null,
                                null,
                                limit,
                                before,
                                after
                            )
                        } else if (order != null) {
                            return SubredditPostListURL(
                                Type.ALL,
                                null,
                                order,
                                limit,
                                before,
                                after
                            )
                        } else {
                            return null
                        }
                    } else if (subreddit == "popular") {
                        return SubredditPostListURL(
                            Type.POPULAR,
                            null,
                            order,
                            limit,
                            before,
                            after
                        )
                    } else if (subreddit.matches("all(\\-[\\w\\.]+)+".toRegex())) {
                        if (pathSegments.size == 2) {
                            return SubredditPostListURL(
                                Type.ALL_SUBTRACTION,
                                subreddit,
                                null,
                                limit,
                                before,
                                after
                            )
                        } else if (order != null) {
                            return SubredditPostListURL(
                                Type.ALL_SUBTRACTION,
                                subreddit,
                                order,
                                limit,
                                before,
                                after
                            )
                        } else {
                            return null
                        }
                    } else if (subreddit.matches("\\w+(\\+[\\w\\.]+)+".toRegex())) {
                        if (pathSegments.size == 2) {
                            return SubredditPostListURL(
                                Type.SUBREDDIT_COMBINATION,
                                subreddit,
                                null,
                                limit,
                                before,
                                after
                            )
                        } else if (order != null) {
                            return SubredditPostListURL(
                                Type.SUBREDDIT_COMBINATION,
                                subreddit,
                                order,
                                limit,
                                before,
                                after
                            )
                        } else {
                            return null
                        }
                    } else if (subreddit.matches("[\\w\\.]+".toRegex())) {
                        if (pathSegments.size == 2) {
                            return SubredditPostListURL(
                                Type.SUBREDDIT,
                                subreddit,
                                null,
                                limit,
                                before,
                                after
                            )
                        } else if (order != null) {
                            return SubredditPostListURL(
                                Type.SUBREDDIT,
                                subreddit,
                                order,
                                limit,
                                before,
                                after
                            )
                        } else {
                            return null
                        }
                    } else {
                        return null
                    }
                }

                else -> return null
            }
        }
    }
}