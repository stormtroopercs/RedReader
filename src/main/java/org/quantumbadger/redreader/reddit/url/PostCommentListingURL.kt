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
import org.quantumbadger.redreader.common.General.getUriQueryParameterNames
import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.reddit.PostCommentSort
import org.quantumbadger.redreader.common.General

class PostCommentListingURL(
    after: String?,
    postId: String?,
    commentId: String?,
    context: Int?,
    limit: Int?,
    order: PostCommentSort?,
    video: Boolean
) : CommentListingURL() {
    val after: String?

    val postId: String?
    val commentId: String?

    val context: Int?
    val limit: Int?

    val order: PostCommentSort?

    val video: Boolean

    init {
        var postId = postId
        var commentId = commentId
        if (postId != null && postId.startsWith("t3_")) {
            postId = postId.substring(3)
        }

        if (commentId != null && commentId.startsWith("t1_")) {
            commentId = commentId.substring(3)
        }

        this.after = after
        this.postId = postId
        this.commentId = commentId
        this.context = context
        this.limit = limit
        this.order = order
        this.video = video
    }

    override fun after(after: String?): PostCommentListingURL {
        return PostCommentListingURL(after, postId, commentId, context, limit, order, video)
    }

    override fun limit(limit: Int?): PostCommentListingURL {
        return PostCommentListingURL(after, postId, commentId, context, limit, order, video)
    }

    fun context(context: Int?): PostCommentListingURL {
        return PostCommentListingURL(after, postId, commentId, context, limit, order, video)
    }

    fun order(order: PostCommentSort?): PostCommentListingURL {
        return PostCommentListingURL(after, postId, commentId, context, limit, order, video)
    }

    fun commentId(commentId: String?): PostCommentListingURL {
        var commentId = commentId
        if (commentId != null && commentId.startsWith("t1_")) {
            commentId = commentId.substring(3)
        }

        return PostCommentListingURL(after, postId, commentId, context, limit, order, video)
    }

    override fun generateJsonUri(): Uri? {
        val builder = Uri.Builder()
        builder.scheme(Reddit.scheme)
            .authority(Reddit.domain)

        internalGenerateCommon(builder)

        builder.appendEncodedPath(".json")

        return builder.build()
    }

    fun generateNonJsonUri(): Uri? {
        val builder = Uri.Builder()
        builder.scheme(Reddit.scheme)
            .authority(Reddit.humanReadableDomain)
        internalGenerateCommon(builder)
        return builder.build()
    }

    private fun internalGenerateCommon(builder: Uri.Builder) {
        if (video) {
            builder.encodedPath("/video")
        } else {
            builder.encodedPath("/comments")
        }
        builder.appendPath(postId)

        if (commentId != null) {
            builder.appendEncodedPath("comment")
            builder.appendPath(commentId)

            if (context != null) {
                builder.appendQueryParameter("context", context.toString())
            }
        }

        if (after != null) {
            builder.appendQueryParameter("after", after)
        }

        if (limit != null) {
            builder.appendQueryParameter("limit", limit.toString())
        }

        if (order != null) {
            builder.appendQueryParameter("sort", order.key)
        }
    }

    @RedditURLParser.PathType
    override fun pathType(): Int {
        return RedditURLParser.POST_COMMENT_LISTING_URL
    }

    override fun humanReadableName(context : Context, shorter: Boolean): String? {
        return super.humanReadableName(context, shorter)
    }

    companion object {
        fun forPostId(postId: String?): PostCommentListingURL {
            return PostCommentListingURL(null, postId, null, null, null, null, false)
        }

        fun parse(uri: Uri): PostCommentListingURL? {
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

                    pathSegmentsFiltered.add(segment)
                }

                pathSegments = pathSegmentsFiltered.toTypedArray<String?>()
            }

            if (pathSegments.size == 1) {
                if (uri.getHost() == "redd.it") {
                    return forPostId(pathSegments[0])
                }
                if (uri.getHost() == "v.redd.it") {
                    return PostCommentListingURL(
                        null,
                        pathSegments[0],
                        null,
                        null,
                        null,
                        null,
                        true
                    )
                }
            }

            if (pathSegments.size < 2) {
                return null
            }

            var offset = 0

            if (pathSegments[0].equals("r", ignoreCase = true)) {
                offset = 2

                if (pathSegments.size - offset < 2) {
                    return null
                }
            }

            var video = false

            if (pathSegments[offset].equals("video", ignoreCase = true)) {
                video = true
            } else if (!pathSegments[offset].equals("comments", ignoreCase = true) &&
                !pathSegments[offset].equals("gallery", ignoreCase = true)
            ) {
                return null
            }

            val postId: String?
            var commentId: String?=null

            postId = pathSegments[offset + 1]
            offset += 2

            if (pathSegments.size - offset >= 2) {
                commentId = pathSegments[offset + 1]
            }

            var after: String?=null
            var limit: Int?=null
            var context: Int?=null
            var order: PostCommentSort?=null

            for (parameterKey in getUriQueryParameterNames(uri)) {
                if (parameterKey.equals("after", ignoreCase = true)) {
                    after = uri.getQueryParameter(parameterKey)
                } else if (parameterKey.equals("limit", ignoreCase = true)) {
                    try {
                        limit = uri.getQueryParameter(parameterKey)!!.toInt()
                    } catch (ignored: Throwable) {
                    }
                } else if (parameterKey.equals("context", ignoreCase = true)) {
                    try {
                        context = uri.getQueryParameter(parameterKey)!!.toInt()
                    } catch (ignored: Throwable) {
                    }
                } else if (parameterKey.equals("sort", ignoreCase = true)) {
                    order = PostCommentSort.Companion.lookup(uri.getQueryParameter(parameterKey))
                }
            }

            return PostCommentListingURL(after, postId, commentId, context, limit, order, video)
        }
    }
}