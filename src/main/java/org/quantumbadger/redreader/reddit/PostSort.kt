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
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.quantumbadger.redreader.reddit

import android.net.Uri
import org.quantumbadger.redreader.common.StringUtils

enum class PostSort {
    HOT,
    NEW,
    RISING,
    TOP_HOUR,
    TOP_DAY,
    TOP_WEEK,
    TOP_MONTH,
    TOP_YEAR,
    TOP_ALL,
    CONTROVERSIAL_HOUR,
    CONTROVERSIAL_DAY,
    CONTROVERSIAL_WEEK,
    CONTROVERSIAL_MONTH,
    CONTROVERSIAL_YEAR,
    CONTROVERSIAL_ALL,
    BEST,
    // Sorts related to Search Listings
    RELEVANCE_HOUR,
    RELEVANCE_DAY,
    RELEVANCE_WEEK,
    RELEVANCE_MONTH,
    RELEVANCE_YEAR,
    RELEVANCE_ALL,
    NEW_HOUR,
    NEW_DAY,
    NEW_WEEK,
    NEW_MONTH,
    NEW_YEAR,
    NEW_ALL,
    COMMENTS_HOUR,
    COMMENTS_DAY,
    COMMENTS_WEEK,
    COMMENTS_MONTH,
    COMMENTS_YEAR,
    COMMENTS_ALL,
    HOT_HOUR,
    HOT_DAY,
    HOT_WEEK,
    HOT_MONTH,
    HOT_YEAR,
    HOT_ALL;
    fun addToUserPostListingUri(builder: Uri.Builder) {
        when (this) {
            PostSort.HOT, PostSort.NEW -> builder.appendQueryParameter(
                "sort",
                StringUtils.asciiLowercase(name)
            )

            PostSort.CONTROVERSIAL_HOUR, PostSort.CONTROVERSIAL_DAY, PostSort.CONTROVERSIAL_WEEK, PostSort.CONTROVERSIAL_MONTH, PostSort.CONTROVERSIAL_YEAR, PostSort.CONTROVERSIAL_ALL, PostSort.TOP_HOUR, PostSort.TOP_DAY, PostSort.TOP_WEEK, PostSort.TOP_MONTH, PostSort.TOP_YEAR, PostSort.TOP_ALL -> {
                val parts: Array<String?> =                     name.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                builder.appendQueryParameter("sort", StringUtils.asciiLowercase(parts[0]!!))
                builder.appendQueryParameter("t", StringUtils.asciiLowercase(parts[1]!!))
            }

            // RISING / BEST / search-listing sorts add no params to a user post listing
            // (original Java switch had no default case).
            else -> {}
        }
    }

    fun addToSubredditListingUri(builder: Uri.Builder) {
        when (this) {
            PostSort.HOT, PostSort.NEW, PostSort.RISING, PostSort.BEST -> builder.appendEncodedPath(
                StringUtils.asciiLowercase(name)
            )

            PostSort.CONTROVERSIAL_HOUR, PostSort.CONTROVERSIAL_DAY, PostSort.CONTROVERSIAL_WEEK, PostSort.CONTROVERSIAL_MONTH, PostSort.CONTROVERSIAL_YEAR, PostSort.CONTROVERSIAL_ALL, PostSort.TOP_HOUR, PostSort.TOP_DAY, PostSort.TOP_WEEK, PostSort.TOP_MONTH, PostSort.TOP_YEAR, PostSort.TOP_ALL -> {
                val parts: Array<String?> =                     name.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                builder.appendEncodedPath(
                    StringUtils.asciiLowercase(
                        name.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                    )
                )
                builder.appendQueryParameter("t", StringUtils.asciiLowercase(parts[1]!!))
            }

            // Search-listing sorts add no path to a subreddit listing
            // (original Java switch had no default case).
            else -> {}
        }
    }


    companion object {
        fun valueOfOrNull(string: String): PostSort? {
            try {
                return valueOf(StringUtils.asciiUppercase(string))
            } catch (e: IllegalArgumentException) {
                return null
            }
        }

        fun parse(sort: String?, t: String?): PostSort? {
            var sort = sort
            var t = t
            if (sort == null) {
                return null
            }

            sort = StringUtils.asciiLowercase(sort)
            t = if (t != null) StringUtils.asciiLowercase(t) else null

            if (sort == "hot") {
                return PostSort.HOT
            } else if (sort == "new") {
                return PostSort.NEW
            } else if (sort == "best") {
                return PostSort.BEST
            } else if (sort == "controversial") {
                if (t == null) {
                    return PostSort.CONTROVERSIAL_ALL
                } else if (t == "all") {
                    return PostSort.CONTROVERSIAL_ALL
                } else if (t == "hour") {
                    return PostSort.CONTROVERSIAL_HOUR
                } else if (t == "day") {
                    return PostSort.CONTROVERSIAL_DAY
                } else if (t == "week") {
                    return PostSort.CONTROVERSIAL_WEEK
                } else if (t == "month") {
                    return PostSort.CONTROVERSIAL_MONTH
                } else if (t == "year") {
                    return PostSort.CONTROVERSIAL_YEAR
                } else {
                    return PostSort.CONTROVERSIAL_ALL
                }
            } else if (sort == "rising") {
                return PostSort.RISING
            } else if (sort == "top") {
                if (t == null) {
                    return PostSort.TOP_ALL
                } else if (t == "all") {
                    return PostSort.TOP_ALL
                } else if (t == "hour") {
                    return PostSort.TOP_HOUR
                } else if (t == "day") {
                    return PostSort.TOP_DAY
                } else if (t == "week") {
                    return PostSort.TOP_WEEK
                } else if (t == "month") {
                    return PostSort.TOP_MONTH
                } else if (t == "year") {
                    return PostSort.TOP_YEAR
                } else {
                    return PostSort.TOP_ALL
                }
            } else {
                return null
            }
        }

        fun parseSearch(sort: String?, t: String?): PostSort? {
            var sort = sort
            var t = t
            if (sort == null) {
                return null
            }

            sort = StringUtils.asciiLowercase(sort)
            t = if (t != null) StringUtils.asciiLowercase(t) else null

            if (sort == "relevance") {
                if (t == null) {
                    return PostSort.RELEVANCE_ALL
                } else if (t == "all") {
                    return PostSort.RELEVANCE_ALL
                } else if (t == "hour") {
                    return PostSort.RELEVANCE_HOUR
                } else if (t == "day") {
                    return PostSort.RELEVANCE_DAY
                } else if (t == "week") {
                    return PostSort.RELEVANCE_WEEK
                } else if (t == "month") {
                    return PostSort.RELEVANCE_MONTH
                } else if (t == "year") {
                    return PostSort.RELEVANCE_YEAR
                } else {
                    return PostSort.RELEVANCE_ALL
                }
            } else if (sort == "new") {
                if (t == null) {
                    return PostSort.NEW_ALL
                } else if (t == "all") {
                    return PostSort.NEW_ALL
                } else if (t == "hour") {
                    return PostSort.NEW_HOUR
                } else if (t == "day") {
                    return PostSort.NEW_DAY
                } else if (t == "week") {
                    return PostSort.NEW_WEEK
                } else if (t == "month") {
                    return PostSort.NEW_MONTH
                } else if (t == "year") {
                    return PostSort.NEW_YEAR
                } else {
                    return PostSort.NEW_ALL
                }
            } else if (sort == "hot") {
                if (t == null) {
                    return PostSort.HOT_ALL
                } else if (t == "all") {
                    return PostSort.HOT_ALL
                } else if (t == "hour") {
                    return PostSort.HOT_HOUR
                } else if (t == "day") {
                    return PostSort.HOT_DAY
                } else if (t == "week") {
                    return PostSort.HOT_WEEK
                } else if (t == "month") {
                    return PostSort.HOT_MONTH
                } else if (t == "year") {
                    return PostSort.HOT_YEAR
                } else {
                    return PostSort.HOT_ALL
                }
            } else if (sort == "top") {
                if (t == null) {
                    return PostSort.TOP_ALL
                } else if (t == "all") {
                    return PostSort.TOP_ALL
                } else if (t == "hour") {
                    return PostSort.TOP_HOUR
                } else if (t == "day") {
                    return PostSort.TOP_DAY
                } else if (t == "week") {
                    return PostSort.TOP_WEEK
                } else if (t == "month") {
                    return PostSort.TOP_MONTH
                } else if (t == "year") {
                    return PostSort.TOP_YEAR
                } else {
                    return PostSort.TOP_ALL
                }
            } else if (sort == "comments") {
                if (t == null) {
                    return PostSort.COMMENTS_ALL
                } else if (t == "all") {
                    return PostSort.COMMENTS_ALL
                } else if (t == "hour") {
                    return PostSort.COMMENTS_HOUR
                } else if (t == "day") {
                    return PostSort.COMMENTS_DAY
                } else if (t == "week") {
                    return PostSort.COMMENTS_WEEK
                } else if (t == "month") {
                    return PostSort.COMMENTS_MONTH
                } else if (t == "year") {
                    return PostSort.COMMENTS_YEAR
                } else {
                    return PostSort.COMMENTS_ALL
                }
            } else {
                return null
            }
        }
    }
}
