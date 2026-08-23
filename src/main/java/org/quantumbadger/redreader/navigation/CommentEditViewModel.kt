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
package org.quantumbadger.redreader.navigation

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.quantumbadger.redreader.activities.BugReportActivity
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.reddit.APIResponseHandler.ActionResponseHandler
import org.quantumbadger.redreader.reddit.RedditAPI
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import javax.inject.Inject

/**
 * ViewModel for the Compose comment/post edit screen.
 *
 * Holds the edit state (text + submit progress) and performs the
 * `api/editusertext` request exactly as the legacy CommentEditActivity did
 * ([RedditAPI.editComment] — the same endpoint edits both comments and self
 * posts, so one screen covers the legacy activity's two titles). The raw
 * `ActionResponseHandler` callbacks post to the UI thread, the same pattern
 * the other navigation ViewModels use — the hosting [AppCompatActivity] is
 * only needed to construct the handler (it carries the dialog/error routing).
 */
@HiltViewModel
class CommentEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    sealed class EditUiState {
        object Idle : EditUiState()
        object Submitting : EditUiState()
        object Success : EditUiState()
        data class Error(val message: String) : EditUiState()
    }

    private val _state = MutableStateFlow<EditUiState>(EditUiState.Idle)
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    private var thingIdAndType: RedditIdAndType? = null
    private var initialText: String = ""

    /**
     * Seed the (per-navigation-entry) ViewModel with the thing being edited
     * and its current markdown.
     */
    fun setThing(idAndType: RedditIdAndType, text: String) {
        thingIdAndType = idAndType
        initialText = text
    }

    /** The text to show in the editor (current, or the seeded markdown). */
    fun initialText(): String = initialText

    /**
     * Issue the `api/editusertext` request with the given markdown, exactly
     * as the legacy `CommentEditActivity.onOptionsItemSelected` did (default
     * account, same handler semantics: success -> [EditUiState.Success],
     * failure -> [EditUiState.Error] with the [RRError] message, exception
     * -> global bug report).
     */
    fun submit(activity: AppCompatActivity, markdown: String) {
        val idAndType = thingIdAndType ?: return
        _state.value = EditUiState.Submitting

        val handler = object : ActionResponseHandler(activity) {
            override fun onSuccess() {
                _state.value = EditUiState.Success
            }

            override fun onFailure(error: RRError) {
                _state.value = EditUiState.Error(error.message ?: "Edit failed")
            }

            override fun onCallbackException(t: Throwable) {
                BugReportActivity.handleGlobalError(activity, t)
            }
        }

        val account = RedditAccountManager.getInstance(context).getDefaultAccount()
        RedditAPI.editComment(
            CacheManager.getInstance(context),
            handler,
            account,
            idAndType,
            markdown,
            activity
        )
    }

    /** Reset to Idle after a success (used to dismiss back to the list). */
    fun onDone() {
        _state.value = EditUiState.Idle
    }
}
