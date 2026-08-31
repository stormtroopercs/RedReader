/*******************************************************************************
 * This file is part of MaterialReader.
 *
 * MaterialReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MaterialReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MaterialReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.stormtroopercs.materialreader.navigation

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.cache.CacheManager
import com.stormtroopercs.materialreader.common.BugReporter
import com.stormtroopercs.materialreader.common.General
import com.stormtroopercs.materialreader.common.Optional
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.reddit.APIResponseHandler.ActionResponseHandler
import com.stormtroopercs.materialreader.reddit.APIResponseHandler.SubmitResponseHandler
import com.stormtroopercs.materialreader.reddit.RedditAPI
import com.stormtroopercs.materialreader.reddit.kthings.RedditIdAndType
import javax.inject.Inject

/**
 * ViewModel for the Compose comment-reply screen.
 *
 * Performs the `api/comment` request exactly as the legacy
 * `CommentReplyActivity` did ([RedditAPI.comment] — the same endpoint the
 * legacy reply flow used): the parent is passed as its full thing id
 * (`t3_…` for a post, `t1_…` for a comment), the markdown body is posted
 * with the default account, and replies-to-inbox stays on (the legacy
 * checkbox default). Success/failure is exposed as [ReplyUiState]; the
 * hosting [AppCompatActivity] builds the handlers (it carries the
 * dialog/error routing).
 */
@HiltViewModel
class CommentReplyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountManager: RedditAccountManager,
    private val cacheManager: CacheManager
) : ViewModel() {

    sealed class ReplyUiState {
        object Idle : ReplyUiState()
        object Submitting : ReplyUiState()
        object Success : ReplyUiState()
        data class Error(val message: String) : ReplyUiState()
    }

    private val _state = MutableStateFlow<ReplyUiState>(ReplyUiState.Idle)
    val state: StateFlow<ReplyUiState> = _state.asStateFlow()

    private var parentThingId: String? = null

    /**
     * Seed the (per-navigation-entry) ViewModel with the thing being
     * replied to (its full `t3_…` / `t1_…` name).
     */
    fun setParent(thingId: String) {
        parentThingId = thingId
    }

    /**
     * Submit the reply with the given markdown. Success -> [ReplyUiState.Success]
     * (the screen toasts + goes back); submit errors / API failure ->
     * [ReplyUiState.Error]; exception -> global bug report.
     */
    fun submit(activity: AppCompatActivity, markdown: String) {
        val thingId = parentThingId ?: return
        val account = accountManager.getDefaultAccount()
        _state.value = ReplyUiState.Submitting

        val submitHandler = object : SubmitResponseHandler(activity) {
            override fun onSubmitErrors(errors: ArrayList<String?>) {
                _state.value = ReplyUiState.Error(
                    errors.filterNotNull().joinToString(" ").ifEmpty { "Reply failed" }
                )
            }

            override fun onSuccess(
                redirectUrl: Optional<String>,
                thingId: Optional<String>
            ) {
                _state.value = ReplyUiState.Success
            }

            override fun onFailure(error: RRError) {
                _state.value = ReplyUiState.Error(error.message ?: "Reply failed")
            }

            override fun onCallbackException(t: Throwable) {
                BugReporter.handleGlobalError(activity, t)
            }
        }

        val inboxHandler = object : ActionResponseHandler(activity) {
            override fun onSuccess() {
                // Expected — nothing to surface.
            }

            override fun onFailure(error: RRError) {
                // The comment posted fine; only the "stop replies going to
                // the inbox" follow-up failed — surface it, stay on screen.
                General.quickToast(context, error.message ?: "Reply submitted")
            }

            override fun onCallbackException(t: Throwable) {
                BugReporter.handleGlobalError(activity, t)
            }
        }

        RedditAPI.comment(
            cacheManager,
            submitHandler,
            inboxHandler,
            account,
            RedditIdAndType(thingId),
            markdown,
            true,
            activity
        )
    }

    /** Reset to Idle after a success (used to dismiss back to the list). */
    fun onDone() {
        _state.value = ReplyUiState.Idle
    }
}
