/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
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
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.reddit.APIResponseHandler.ActionResponseHandler
import org.quantumbadger.redreader.reddit.RedditAPI
import javax.inject.Inject

/**
 * Process-wide draft memory for the PM composer, mirroring the companion
 * state the legacy `PMSendActivity` kept: the last unsent recipient / subject
 * / body, prefilled into the next composer opened without explicit values and
 * cleared after a successful send. Lives here (not in the ViewModel) because
 * the Compose route re-creates the ViewModel per navigation entry.
 */
object PMSendDraft {
    var recipient: String? = null
    var subject: String? = null
    var text: String? = null

    fun save(recipient: String, subject: String, text: String) {
        this.recipient = recipient
        this.subject = subject
        this.text = text
    }

    fun clear() {
        recipient = null
        subject = null
        text = null
    }
}

/**
 * ViewModel for the Compose PM composer (replaces the legacy
 * `PMSendActivity`).
 *
 * Holds the send state (progress / success / failure) and performs the
 * `api/compose` request exactly as the legacy activity's Send menu item did
 * ([RedditAPI.compose] with the selected account). The raw
 * `ActionResponseHandler` callbacks post to the UI thread, the same pattern
 * the other navigation ViewModels use — the hosting [AppCompatActivity] is
 * only needed to construct the handler (it carries the dialog/error routing).
 */
@HiltViewModel
class PMViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    sealed class PMUiState {
        object Idle : PMUiState()
        object Submitting : PMUiState()
        object Success : PMUiState()
        data class Failed(val error: RRError) : PMUiState()
    }

    private val _state = MutableStateFlow<PMUiState>(PMUiState.Idle)
    val state: StateFlow<PMUiState> = _state.asStateFlow()

    /** Non-anonymous account usernames, in account order (the legacy spinner list). */
    val accounts: List<String> =
        RedditAccountManager.getInstance(context).accounts
            .filter { !it.isAnonymous }
            .map { it.username }

    /**
     * Issue the `api/compose` request with the given recipient / subject /
     * body under the given account — the same call and handler semantics as
     * the legacy `PMSendActivity.onOptionsItemSelected` (success -> [PMUiState.Success],
     * failure -> [PMUiState.Failed], exception -> global bug report).
     */
    fun submit(
        activity: AppCompatActivity,
        accountUsername: String,
        recipient: String,
        subject: String,
        body: String,
    ) {
        val account: RedditAccount? =
            RedditAccountManager.getInstance(context).accounts.firstOrNull {
                !it.isAnonymous && it.username.equals(accountUsername, ignoreCase = true)
            }
        if (account == null) return

        _state.value = PMUiState.Submitting

        val handler = object : ActionResponseHandler(activity) {
            override fun onSuccess() {
                _state.value = PMUiState.Success
            }

            override fun onFailure(error: RRError) {
                _state.value = PMUiState.Failed(error)
            }

            override fun onCallbackException(t: Throwable) {
                BugReportActivity.handleGlobalError(activity, t)
            }
        }

        RedditAPI.compose(
            CacheManager.getInstance(context),
            handler,
            account,
            recipient,
            subject,
            body,
            context
        )
    }

    /** Reset to Idle (used after a success dismisses the screen). */
    fun onDone() {
        _state.value = PMUiState.Idle
    }
}
