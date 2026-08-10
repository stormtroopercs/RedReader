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
import androidx.annotation.IntDef
import org.quantumbadger.redreader.common.Constants.Reddit
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.common.UriString

object RedditURLParser {
    const val SUBREDDIT_POST_LISTING_URL: Int = 0
    const val USER_POST_LISTING_URL: Int = 1
    const val SEARCH_POST_LISTING_URL: Int = 2
    const val UNKNOWN_POST_LISTING_URL: Int = 3
    const val USER_PROFILE_URL: Int = 4
    const val USER_COMMENT_LISTING_URL: Int = 5
    const val UNKNOWN_COMMENT_LISTING_URL: Int = 6
    const val POST_COMMENT_LISTING_URL: Int = 7
    const val MULTIREDDIT_POST_LISTING_URL: Int = 8
    const val COMPOSE_MESSAGE_URL: Int = 9
    const val OPAQUE_SHARED_URL: Int = 10

    private fun tryGetRedditUri(uri: Uri?): Optional<Uri> {
        if (uri == null || uri.getHost() == null || uri.getPath() == null) {
            return Optional.Companion.empty<Uri?>()
        }

        if ("reddit" == uri.getScheme() && "reddit" == uri.getHost()) {
            return Optional.Companion.of<Uri?>(
                uri.buildUpon()
                    .scheme("https")
                    .authority("reddit.com")
                    .build()
            )
        }

        if ("reddit.app.link" == uri.getHost()) {
            val redirect = uri.getQueryParameter("\$og_redirect")

            if (redirect != null) {
                return Optional.Companion.ofNullable<Uri?>(Uri.parse(redirect))
            }
        }

        val ampPrefix = "/amp/s/amp.reddit.com"

        if ((("google.com" == uri.getHost()
                    || uri.getHost()!!.endsWith(".google.com"))
                    && uri.getPath()!!.startsWith(ampPrefix))
        ) {
            return Optional.Companion.ofNullable<Uri?>(
                Uri.parse(
                    "https://reddit.com" + uri.getPath()!!.substring(ampPrefix.length)
                )
            )
        }

        val hostSegments: Array<String?> =
            StringUtils.asciiLowercase(uri.getHost()!!).split("\\.".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()

        if (hostSegments.size < 2) {
            return Optional.Companion.empty<Uri?>()
        }

        if (hostSegments[hostSegments.size - 1] == "com"
            && hostSegments[hostSegments.size - 2] == "reddit"
        ) {
            return Optional.Companion.of<Uri?>(uri)
        }

        if (hostSegments[hostSegments.size - 1] == "it"
            && hostSegments[hostSegments.size - 2] == "redd"
        ) {
            return Optional.Companion.of<Uri?>(uri)
        }

        return Optional.Companion.empty<Uri?>()
    }

    fun parse(rawUri: Uri?): RedditURL? {
        if (rawUri == null) {
            return null
        }

        val optionalUri = tryGetRedditUri(rawUri)

        if (optionalUri.isEmpty()) {
            return null
        }

        val uri = optionalUri.get()

        run {
            val opaqueSharedURL: OpaqueSharedURL? = OpaqueSharedURL.Companion.parse(uri)
            if (opaqueSharedURL != null) {
                return opaqueSharedURL
            }
        }

        run {
            val subredditPostListURL: SubredditPostListURL? =
                SubredditPostListURL.Companion.parse(uri)
            if (subredditPostListURL != null) {
                return subredditPostListURL
            }
        }

        run {
            val multiredditPostListURL
                    : MultiredditPostListURL? = MultiredditPostListURL.Companion.parse(uri)
            if (multiredditPostListURL != null) {
                return multiredditPostListURL
            }
        }

        run {
            val searchPostListURL: SearchPostListURL? = SearchPostListURL.Companion.parse(uri)
            if (searchPostListURL != null) {
                return searchPostListURL
            }
        }

        run {
            val userPostListURL: UserPostListingURL? = UserPostListingURL.Companion.parse(uri)
            if (userPostListURL != null) {
                return userPostListURL
            }
        }

        run {
            val userCommentListURL: UserCommentListingURL? = UserCommentListingURL.Companion.parse(
                uri
            )
            if (userCommentListURL != null) {
                return userCommentListURL
            }
        }

        run {
            val commentListingURL: PostCommentListingURL? = PostCommentListingURL.Companion.parse(
                uri
            )
            if (commentListingURL != null) {
                return commentListingURL
            }
        }

        run {
            val userProfileURL: UserProfileURL? = UserProfileURL.Companion.parse(uri)
            if (userProfileURL != null) {
                return userProfileURL
            }
        }

        run {
            val composeMessageURL: ComposeMessageURL? = ComposeMessageURL.Companion.parse(uri)
            if (composeMessageURL != null) {
                return composeMessageURL
            }
        }

        return null
    }

    fun parseProbableCommentListing(uri: Uri?): RedditURL {
        val matchURL = parse(uri)
        if (matchURL != null) {
            return matchURL
        }

        return UnknownCommentListURL(uri)
    }

    fun parseProbablePostListing(uri: Uri?): RedditURL {
        val matchURL = parse(uri)
        if (matchURL != null) {
            return matchURL
        }

        return UnknownPostListURL(uri)
    }

    @IntDef(
        [SUBREDDIT_POST_LISTING_URL, USER_POST_LISTING_URL, SEARCH_POST_LISTING_URL, UNKNOWN_POST_LISTING_URL, USER_PROFILE_URL, USER_COMMENT_LISTING_URL, UNKNOWN_COMMENT_LISTING_URL, POST_COMMENT_LISTING_URL, MULTIREDDIT_POST_LISTING_URL, COMPOSE_MESSAGE_URL, OPAQUE_SHARED_URL
        ]
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class PathType

    abstract class RedditURL {
        abstract fun generateJsonUri(): Uri

        @PathType
        abstract fun pathType(): Int

        fun asSubredditPostListURL(): SubredditPostListURL {
            return this as SubredditPostListURL
        }

        fun asMultiredditPostListURL(): MultiredditPostListURL {
            return this as MultiredditPostListURL
        }

        fun asSearchPostListURL(): SearchPostListURL {
            return this as SearchPostListURL
        }

        fun asUserPostListURL(): UserPostListingURL {
            return this as UserPostListingURL
        }

        fun asUserProfileURL(): UserProfileURL {
            return this as UserProfileURL
        }

        fun asPostCommentListURL(): PostCommentListingURL {
            return this as PostCommentListingURL
        }

        fun asUserCommentListURL(): UserCommentListingURL {
            return this as UserCommentListingURL
        }

        fun asComposeMessageURL(): ComposeMessageURL {
            return this as ComposeMessageURL
        }

        open fun humanReadableName(context: Context?, shorter: Boolean): String {
            return humanReadablePath()
        }

        fun humanReadableUrl(): String {
            return "reddit.com" + humanReadablePath()
        }

        open fun humanReadablePath(): String {
            val src = generateJsonUri()

            val builder = StringBuilder()

            for (pathElement in src.getPathSegments()) {
                if (pathElement != ".json") {
                    builder.append("/")
                    builder.append(pathElement)
                }
            }

            return builder.toString()
        }

        fun browserUrl(): UriString {
            return UriString(Reddit.SCHEME_HTTPS + "://" + humanReadableUrl())
        }

        override fun toString(): String {
            return generateJsonUri().toString()
        }

        fun toUriString(): UriString {
            return UriString(toString())
        }
    }
}
