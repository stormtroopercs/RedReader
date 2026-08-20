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
import org.quantumbadger.redreader.common.Constants.Reddit
import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL

class UserProfileURL(val username: String) : RedditURL() {
    override fun generateJsonUri(): Uri? {
        val builder = Uri.Builder()
        builder.scheme(Reddit.scheme)
            .authority(Reddit.domain)

        builder.appendEncodedPath("user")
        builder.appendPath(username)

        builder.appendEncodedPath(".json")

        return builder.build()
    }

    @RedditURLParser.PathType
    override fun pathType(): Int {
        return RedditURLParser.USER_PROFILE_URL
    }

    override fun humanReadableName(context : Context, shorter: Boolean): String {
        return username
    }

    companion object {
        fun parse(uri: Uri): UserProfileURL? {
            val pathSegments: Array<String>
            run {
                val pathSegmentsList = uri.getPathSegments()
                val pathSegmentsFiltered = ArrayList<String>(
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

                pathSegments = pathSegmentsFiltered.toTypedArray()
            }

            if (pathSegments.size != 2) {
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

            return UserProfileURL(username)
        }
    }
}