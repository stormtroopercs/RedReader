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

package org.quantumbadger.redreader.activities

import android.os.Bundle
import org.quantumbadger.redreader.compose.activity.ComposeBaseActivity
import org.quantumbadger.redreader.compose.ui.CommentReplyScreen

/**
 * Thin Compose wrapper around [CommentReplyScreen].
 * Keeps the legacy companion constants so existing call sites still work.
 */
class CommentReplyActivity : ComposeBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val parentIdAndType = intent.getStringExtra(PARENT_ID_AND_TYPE_KEY)
        val postId = parentIdAndType?.split(":")?.get(0) ?: ""

        setContentCompose {
            CommentReplyScreen(
                postId = postId,
                onNavigateBack = ::finish,
                onSubmit = { body ->
                    // TODO: wire up comment submission
                    finish()
                }
            )
        }
    }

    companion object {
        const val PARENT_TYPE: String = "parentType"
        const val PARENT_TYPE_MESSAGE: String = "parentTypeMessage"
        const val PARENT_ID_AND_TYPE_KEY: String = "parentIdAndType"
        const val PARENT_MARKDOWN_KEY: String = "parent_markdown"
    }
}
