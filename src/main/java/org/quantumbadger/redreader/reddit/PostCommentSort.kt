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
package org.quantumbadger.redreader.reddit

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.activities.OptionsMenuUtility
import org.quantumbadger.redreader.activities.OptionsMenuUtility.OptionsMenuCommentsListener
import org.quantumbadger.redreader.common.StringUtils

enum class PostCommentSort(
    key: String,
    @StringRes menuTitle: Int,
    @StringRes suggestedTitle: Int
) : OptionsMenuUtility.Sort {
    BEST("confidence", string.sort_comments_best, string.sort_comments_best_suggested),
    HOT("hot", string.sort_comments_hot, string.sort_comments_hot_suggested),
    NEW("new", string.sort_comments_new, string.sort_comments_new_suggested),
    OLD("old", string.sort_comments_old, string.sort_comments_old_suggested),
    TOP("top", string.sort_comments_top, string.sort_comments_top_suggested),
    CONTROVERSIAL(
        "controversial",
        string.sort_comments_controversial,
        string.sort_comments_controversial_suggested
    ),
    QA("qa", string.sort_comments_qa, string.sort_comments_qa_suggested);

    val key: String?

    @StringRes
    private val menuTitle: Int

    @StringRes
    val suggestedTitle: Int

    init {
        this.key = key
        this.menuTitle = menuTitle
        this.suggestedTitle = suggestedTitle
    }

    override val menuTitle: Int get() = menuTitle

    override fun onSortSelected(activity: AppCompatActivity) {
        (activity as OptionsMenuCommentsListener).onSortSelected(this)
    }

    companion object {
        fun lookup(name: String): PostCommentSort? {
            var name = name
            name = StringUtils.asciiUppercase(name)

            if (name == "CONFIDENCE") {
                return PostCommentSort.BEST // oh, reddit...
            }

            try {
                return valueOf(name)
            } catch (e: IllegalArgumentException) {
                return null
            }
        }
    }
}
