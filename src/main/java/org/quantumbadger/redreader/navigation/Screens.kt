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
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.\
 ******************************************************************************/

package org.quantumbadger.redreader.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation route keys for RedReader.
 *
 * Top-level routes (each has its own back stack):
 *   - Main (Home)
 *   - Settings
 *
 * Child routes (pushed onto the active top-level back stack):
 *   - PostList(subreddit)
 *   - CommentList(postId)
 *   - UserProfile(username)
 *   - Inbox
 *   - PostSubmit(subreddit)
 *   - SubredditSearch
 *   - CommentReply(postId, commentId?)
 *   - Album(url)
 *   - Image(url)
 */
@Serializable
data object Main : NavKey

@Serializable
data object Settings : NavKey

@Serializable
data class PostList(val subreddit: String, val searchQuery: String? = null) : NavKey

@Serializable
data class CommentList(val postId: String) : NavKey

@Serializable
data class UserProfile(val username: String) : NavKey

@Serializable
data object Inbox : NavKey

@Serializable
data class PostSubmit(val subreddit: String) : NavKey

@Serializable
data object SubredditSearch : NavKey

@Serializable
data class CommentReply(val postId: String, val commentId: String? = null) : NavKey

@Serializable
data object RedditTerms : NavKey

@Serializable
data object Changelog : NavKey

@Serializable
data object BugReport : NavKey

@Serializable
data class WebViewRoute(val url: String, val title: String? = null) : NavKey

@Serializable
data class HtmlView(val html: String, val title: String) : NavKey

@Serializable
data object OAuthLogin : NavKey

@Serializable
data class Album(val url: String) : NavKey

/** Full-screen image viewer (direct still-image/GIF/video file URLs, or an album). */
@Serializable
data class Image(
    val url: String,
    val isGif: Boolean = false,
    val isVideo: Boolean = false,
    val albumUrl: String? = null,
    val albumIndex: Int = 0
) : NavKey

/** All top-level routes (displayed in navigation bar/rail/drawer). */
val TOP_LEVEL_ROUTES = setOf<NavKey>(Main, Settings)
