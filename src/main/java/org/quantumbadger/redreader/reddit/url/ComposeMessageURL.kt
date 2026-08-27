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
package org.quantumbadger.redreader.reddit.url

import android.net.Uri
import org.quantumbadger.redreader.common.Constants.Reddit
import org.quantumbadger.redreader.common.General.getUriQueryParameterNames
import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL
import org.quantumbadger.redreader.common.General

class ComposeMessageURL(val recipient: String?, val subject: String?, val message: String?) :
    RedditURL() {
    override fun generateJsonUri(): Uri? {
        val builder = Uri.Builder()
        builder.scheme(Reddit.scheme)
            .authority(Reddit.domain)

        builder.appendEncodedPath("message")
        builder.appendEncodedPath("compose")

        if (recipient != null) {
            builder.appendQueryParameter("to", recipient)
        }

        if (subject != null) {
            builder.appendQueryParameter("subject", subject)
        }

        if (message != null) {
            builder.appendQueryParameter("message", message)
        }

        builder.appendEncodedPath(".json")

        return builder.build()
    }

    @RedditURLParser.PathType
    override fun pathType(): Int {
        return RedditURLParser.COMPOSE_MESSAGE_URL
    }

    companion object {
        fun parse(uri: Uri): ComposeMessageURL? {
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

            if (pathSegments.size != 2) {
                return null
            }

            if (!pathSegments[0].equals("message", ignoreCase = true)
                || !pathSegments[1].equals("compose", ignoreCase = true)
            ) {
                return null
            }

            var recipient: String?=null
            var subject: String?=null
            var message: String?=null
            for (parameterKey in getUriQueryParameterNames(uri)) {
                if (parameterKey.equals("to", ignoreCase = true)) {
                    // TODO validate username with regex
                    recipient = uri.getQueryParameter(parameterKey)
                } else if (parameterKey.equals("subject", ignoreCase = true)) {
                    subject = uri.getQueryParameter(parameterKey)
                } else if (parameterKey.equals("message", ignoreCase = true)) {
                    message = uri.getQueryParameter(parameterKey)
                }
            }

            return ComposeMessageURL(recipient, subject, message)
        }
    }
}
