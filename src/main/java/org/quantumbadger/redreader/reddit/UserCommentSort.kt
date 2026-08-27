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

enum class UserCommentSort {
    NEW,
    HOT,
    CONTROVERSIAL_HOUR,
    CONTROVERSIAL_DAY,
    CONTROVERSIAL_WEEK,
    CONTROVERSIAL_MONTH,
    CONTROVERSIAL_YEAR,
    CONTROVERSIAL_ALL,
    TOP_HOUR,
    TOP_DAY,
    TOP_WEEK,
    TOP_MONTH,
    TOP_YEAR,
    TOP_ALL;
    fun addToUserCommentListingUri(builder: Uri.Builder) {
        when (this) {
            UserCommentSort.HOT, UserCommentSort.NEW -> builder.appendQueryParameter(
                "sort",
                StringUtils.asciiLowercase(name)
            )

            UserCommentSort.CONTROVERSIAL_HOUR, UserCommentSort.CONTROVERSIAL_DAY, UserCommentSort.CONTROVERSIAL_WEEK, UserCommentSort.CONTROVERSIAL_MONTH, UserCommentSort.CONTROVERSIAL_YEAR, UserCommentSort.CONTROVERSIAL_ALL, UserCommentSort.TOP_HOUR, UserCommentSort.TOP_DAY, UserCommentSort.TOP_WEEK, UserCommentSort.TOP_MONTH, UserCommentSort.TOP_YEAR, UserCommentSort.TOP_ALL -> {
                val parts: Array<String?> =                     name.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                builder.appendQueryParameter(
                    "sort",
                    StringUtils.asciiLowercase(parts[0]!!)
                )
                builder.appendQueryParameter("t", StringUtils.asciiLowercase(parts[1]!!))
            }
        }
    }


    companion object {
        fun parse(sort: String?, t: String?): UserCommentSort? {
            var sort = sort
            var t = t
            if (sort == null) {
                return null
            }

            sort = StringUtils.asciiLowercase(sort)
            t = if (t != null) StringUtils.asciiLowercase(t) else null

            if (sort == "hot") {
                return UserCommentSort.HOT
            } else if (sort == "new") {
                return UserCommentSort.NEW
            } else if (sort == "controversial") {
                if (t == null) {
                    return UserCommentSort.CONTROVERSIAL_ALL
                } else if (t == "all") {
                    return UserCommentSort.CONTROVERSIAL_ALL
                } else if (t == "hour") {
                    return UserCommentSort.CONTROVERSIAL_HOUR
                } else if (t == "day") {
                    return UserCommentSort.CONTROVERSIAL_DAY
                } else if (t == "week") {
                    return UserCommentSort.CONTROVERSIAL_WEEK
                } else if (t == "month") {
                    return UserCommentSort.CONTROVERSIAL_MONTH
                } else if (t == "year") {
                    return UserCommentSort.CONTROVERSIAL_YEAR
                } else {
                    return UserCommentSort.CONTROVERSIAL_ALL
                }
            } else if (sort == "top") {
                if (t == null) {
                    return UserCommentSort.TOP_ALL
                } else if (t == "all") {
                    return UserCommentSort.TOP_ALL
                } else if (t == "hour") {
                    return UserCommentSort.TOP_HOUR
                } else if (t == "day") {
                    return UserCommentSort.TOP_DAY
                } else if (t == "week") {
                    return UserCommentSort.TOP_WEEK
                } else if (t == "month") {
                    return UserCommentSort.TOP_MONTH
                } else if (t == "year") {
                    return UserCommentSort.TOP_YEAR
                } else {
                    return UserCommentSort.TOP_ALL
                }
            } else {
                return null
            }
        }
    }
}
