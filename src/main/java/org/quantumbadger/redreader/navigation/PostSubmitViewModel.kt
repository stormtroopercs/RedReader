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
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequest.DownloadQueueType
import org.quantumbadger.redreader.cache.CacheRequestJSONParser
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyAlways
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.http.PostField
import org.quantumbadger.redreader.http.body.HTTPRequestBody.PostFields
import org.quantumbadger.redreader.jsonwrap.JsonValue
import org.quantumbadger.redreader.reddit.RedditSubredditHistory
import org.quantumbadger.redreader.reddit.things.SubredditCanonicalId
import java.util.LinkedList
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the Compose post-submission screen.
 *
 * Holds the post form state (subreddit, type, title, body/URL) and performs
 * the `api_type=json` `api/submit` request through the cache pipeline — the
 * same request the legacy `PostSubmitContentFragment` builds via
 * `RedditAPI.submit`, but without needing the hosting [AppCompatActivity]
 * (the raw `CacheRequestJSONParser.Listener` callbacks post to the UI thread
 * themselves). Flair selection and Imgur upload are intentionally out of
 * scope: flair is omitted from the request and "Upload to Imgur" is not
 * offered.
 */
@HiltViewModel
class PostSubmitViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    sealed class PostType {
        object Self : PostType()
        object Link : PostType()
    }

    sealed class SubmitUiState {
        object Idle : SubmitUiState()
        object Submitting : SubmitUiState()
        data class Success(val redirectUrl: String?) : SubmitUiState()
        data class Error(val message: String) : SubmitUiState()
    }

    private val _submitState = MutableStateFlow<SubmitUiState>(SubmitUiState.Idle)
    val submitState: StateFlow<SubmitUiState> = _submitState.asStateFlow()

    var subreddit: String? = null
        private set
    var postType: PostType = PostType.Link
        private set
    var title: String = ""
        private set
    var bodyText: String = ""
        private set
    var bodyUrl: String = ""
        private set

    /** The account this form submits under (default account; null if signed out). */
    fun account(): RedditAccount? =
        RedditAccountManager.getInstance(context).getDefaultAccount()

    /** Subreddits the account has posted to, most-recent first (offline history). */
    fun subredditSuggestions(): List<SubredditCanonicalId> {
        val account = account() ?: return emptyList()
        return RedditSubredditHistory.getSubredditsSorted(account)
    }

    fun setSubreddit(name: String) {
        val cleaned = normalizeSubreddit(name)
        if (cleaned.isNotBlank()) {
            subreddit = cleaned
        }
    }

    fun setPostType(type: PostType) {
        postType = type
    }

    fun setTitle(value: String) {
        title = value
    }

    fun setBodyText(value: String) {
        bodyText = value
    }

    fun setBodyUrl(value: String) {
        bodyUrl = value
    }

    fun clearSubmitState() {
        _submitState.value = SubmitUiState.Idle
    }

    /**
     * Submit the post. The [activity] parameter is the hosting activity, used
     * only as the `runOnUiThread` target for callback dispatch — the same
     * pattern the other navigation ViewModels use for their response handlers.
     */
    fun submit(activity: AppCompatActivity) {
        if (_submitState.value is SubmitUiState.Submitting) {
            return
        }

        val account = account()
        if (account == null) {
            postResult(activity) { SubmitUiState.Error("Not signed in") }
            return
        }

        val sr = subreddit
        if (sr.isNullOrBlank()) {
            postResult(activity) { SubmitUiState.Error("Choose a subreddit") }
            return
        }
        if (title.isBlank()) {
            postResult(activity) { SubmitUiState.Error("Title is required") }
            return
        }

        val isSelfPost = postType is PostType.Self
        val body = if (isSelfPost) bodyText else bodyUrl
        if (body.isBlank()) {
            postResult(activity) {
                SubmitUiState.Error(if (isSelfPost) "Body is empty" else "URL is required")
            }
            return
        }

        _submitState.value = SubmitUiState.Submitting

        val postFields = LinkedList<PostField>()
        postFields.add(PostField("api_type", "json"))
        postFields.add(PostField("kind", if (isSelfPost) "self" else "link"))
        postFields.add(PostField("sr", sr))
        postFields.add(PostField("title", title.trim()))
        if (isSelfPost) {
            postFields.add(PostField("text", body))
        } else {
            postFields.add(PostField("url", body.trim()))
        }

        val cacheRequest = CacheRequest(
            UriString("https://www.reddit.com/api/submit"),
            account,
            null,
            Priority(Constants.Priority.API_ACTION),
            DownloadStrategyAlways.Companion.INSTANCE,
            Constants.FileType.NOCACHE,
            DownloadQueueType.REDDIT_API,
            PostFields(postFields),
            context,
            CacheRequestJSONParser(
                context,
                object : CacheRequestJSONParser.Listener {
                override fun onJsonParsed(
                    result: JsonValue,
                    timestamp: org.quantumbadger.redreader.common.time.TimestampUTC,
                    session: UUID,
                    fromCache: Boolean
                ) {
                    try {
                        val errorsJson = result.getArrayAtPath("json", "errors")
                        if (errorsJson.isPresent) {
                            val errors = ArrayList<String?>()
                            for (errorValue in errorsJson.get()) {
                                val error = errorValue.asArray()
                                if (error != null && error.getString(1) != null) {
                                    errors.add(error.getString(1))
                                }
                            }
                            if (errors.isNotEmpty()) {
                                postResult(activity) {
                                    SubmitUiState.Error(errors.joinToString(" ") { it ?: "" })
                                }
                                return
                            }
                        }

                        // Mirror RedditAPI.findFailureType: a "success": false
                        // anywhere in the body means the submit failed.
                        if (!successFlagPresent(result)) {
                            postResult(activity) {
                                SubmitUiState.Error("Reddit rejected the submission")
                            }
                            return
                        }

                        val permalink = result
                            .getStringAtPath("json", "data", "things", 0, "data", "permalink")
                            .orElse(result.getStringAtPath("json", "data", "url"))
                        postResult(activity) {
                            SubmitUiState.Success(permalink.orElseNull())
                        }
                    } catch (e: Exception) {
                        postResult(activity) {
                            SubmitUiState.Error(e.message ?: "Failed to parse the submission response")
                        }
                    }
                }

                override fun onFailure(error: RRError) {
                    postResult(activity) {
                        SubmitUiState.Error(error.message ?: "Submission failed")
                    }
                }
                })
        )

        CacheManager.getInstance(context).makeRequest(cacheRequest)
    }

    /**
     * True if the response body does not contain a `"success": false` flag
     * (the submit-failure marker [org.quantumbadger.redreader.reddit.RedditAPI]
     * looks for). A missing flag counts as success — the legacy parser treats
     * "no recognizable failure" as success too.
     */
    private fun successFlagPresent(response: JsonValue?): Boolean {
        if (response == null) {
            return true
        }

        if (response.asObject() != null) {
            for (entry in response.asObject()!!) {
                if ("success" == entry.key && false == entry.value.asBoolean()) {
                    return false
                }
                if (!successFlagPresent(entry.value)) {
                    return false
                }
            }
            return true
        }

        if (response.asArray() != null) {
            for (entry in response.asArray()!!) {
                if (!successFlagPresent(entry)) {
                    return false
                }
            }
            return true
        }

        return true
    }

    private fun postResult(activity: AppCompatActivity, block: () -> SubmitUiState) {
        AndroidCommon.runOnUiThread {
            _submitState.value = block()
        }
    }

    private fun normalizeSubreddit(raw: String): String {
        var name = raw.trim().lowercase()
        while (name.startsWith("/")) {
            name = name.substring(1)
        }
        while (name.startsWith("r/")) {
            name = name.substring(2)
        }
        while (name.endsWith("/")) {
            name = name.substring(0, name.length - 1)
        }
        return name
    }
}
