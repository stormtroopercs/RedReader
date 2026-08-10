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

package org.quantumbadger.redreader.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing Reddit posts locally.
 */
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "url")
    val url: String?,

    @ColumnInfo(name = "url_overridden_by_dest")
    val urlOverriddenByDest: String?,

    @ColumnInfo(name = "title")
    val title: String?,

    @ColumnInfo(name = "author")
    val author: String?,

    @ColumnInfo(name = "domain")
    val domain: String?,

    @ColumnInfo(name = "subreddit")
    val subreddit: String,

    @ColumnInfo(name = "num_comments")
    val numComments: Int,

    @ColumnInfo(name = "score")
    val score: Int,

    @ColumnInfo(name = "gilded")
    val gilded: Int = 0,

    @ColumnInfo(name = "crosspost_parent")
    val crosspostParent: String?,

    @ColumnInfo(name = "upvote_ratio")
    val upvoteRatio: Double?,

    @ColumnInfo(name = "archived")
    val archived: Boolean = false,

    @ColumnInfo(name = "over_18")
    val over18: Boolean = false,

    @ColumnInfo(name = "hidden")
    val hidden: Boolean = false,

    @ColumnInfo(name = "saved")
    val saved: Boolean = false,

    @ColumnInfo(name = "is_self")
    val isSelf: Boolean = false,

    @ColumnInfo(name = "clicked")
    val clicked: Boolean = false,

    @ColumnInfo(name = "stickied")
    val stickied: Boolean = false,

    @ColumnInfo(name = "can_mod_post")
    val canModPost: Boolean = false,

    @ColumnInfo(name = "edited")
    val edited: String?,

    @ColumnInfo(name = "likes")
    val likes: Boolean?,

    @ColumnInfo(name = "spoiler")
    val spoiler: Boolean = false,

    @ColumnInfo(name = "locked")
    val locked: Boolean = false,

    @ColumnInfo(name = "created_utc")
    val createdUtc: Long,

    @ColumnInfo(name = "selftext")
    val selftext: String?,

    @ColumnInfo(name = "selftext_html")
    val selftextHtml: String?,

    @ColumnInfo(name = "permalink")
    val permalink: String,

    @ColumnInfo(name = "link_flair_text")
    val linkFlairText: String?,

    @ColumnInfo(name = "author_flair_text")
    val authorFlairText: String?,

    @ColumnInfo(name = "thumbnail")
    val thumbnail: String?,

    @ColumnInfo(name = "media")
    val media: String?,

    @ColumnInfo(name = "preview")
    val preview: String?,

    @ColumnInfo(name = "is_video")
    val isVideo: Boolean = false,

    @ColumnInfo(name = "distinguished")
    val distinguished: String?,

    @ColumnInfo(name = "suggested_sort")
    val suggestedSort: String?,

    @ColumnInfo(name = "media_metadata")
    val mediaMetadata: String?,

    @ColumnInfo(name = "gallery_data")
    val galleryData: String?,

    @ColumnInfo(name = "removed_by_category")
    val removedByCategory: String?
)
