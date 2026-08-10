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
 * Room entity for storing Reddit comments locally.
 */
@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "parent_id")
    val parentId: String?,

    @ColumnInfo(name = "link_id")
    val linkId: String?,

    @ColumnInfo(name = "subreddit_id")
    val subredditId: String?,

    @ColumnInfo(name = "author")
    val author: String?,

    @ColumnInfo(name = "subreddit")
    val subreddit: String?,

    @ColumnInfo(name = "body")
    val body: String?,

    @ColumnInfo(name = "body_html")
    val bodyHtml: String?,

    @ColumnInfo(name = "score")
    val score: Int,

    @ColumnInfo(name = "ups")
    val ups: Int = 0,

    @ColumnInfo(name = "downs")
    val downs: Int = 0,

    @ColumnInfo(name = "gilded")
    val gilded: Int = 0,

    @ColumnInfo(name = "controversiality")
    val controversiality: Int = 0,

    @ColumnInfo(name = "likes")
    val likes: Boolean?,

    @ColumnInfo(name = "score_hidden")
    val scoreHidden: Boolean = false,

    @ColumnInfo(name = "archived")
    val archived: Boolean = false,

    @ColumnInfo(name = "locked")
    val locked: Boolean = false,

    @ColumnInfo(name = "can_mod_post")
    val canModPost: Boolean = false,

    @ColumnInfo(name = "author_flair_text")
    val authorFlairText: String?,

    @ColumnInfo(name = "edited")
    val edited: String?,

    @ColumnInfo(name = "created_utc")
    val createdUtc: Long,

    @ColumnInfo(name = "saved")
    val saved: Boolean = false,

    @ColumnInfo(name = "distinguished")
    val distinguished: String?,

    @ColumnInfo(name = "stickied")
    val stickied: Boolean = false,

    @ColumnInfo(name = "collapsed_reason_code")
    val collapsedReasonCode: String?,

    @ColumnInfo(name = "context")
    val context: String?,

    @ColumnInfo(name = "removed_by_category")
    val removedByCategory: String?
)
