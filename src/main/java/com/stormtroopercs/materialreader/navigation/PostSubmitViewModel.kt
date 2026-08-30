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
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.stormtroopercs.materialreader.account.RedditAccount
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.cache.CacheManager
import com.stormtroopercs.materialreader.cache.CacheRequest
import com.stormtroopercs.materialreader.cache.CacheRequest.DownloadQueueType
import com.stormtroopercs.materialreader.cache.CacheRequest.RequestFailureType
import com.stormtroopercs.materialreader.cache.CacheRequestJSONParser
import com.stormtroopercs.materialreader.cache.downloadstrategy.DownloadStrategyAlways
import com.stormtroopercs.materialreader.common.AndroidCommon
import com.stormtroopercs.materialreader.common.Constants
import com.stormtroopercs.materialreader.common.General.getGeneralErrorForFailure
import com.stormtroopercs.materialreader.common.Optional
import com.stormtroopercs.materialreader.common.Priority
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.http.FailedRequestBody
import com.stormtroopercs.materialreader.http.PostField
import com.stormtroopercs.materialreader.http.body.HTTPRequestBody.Multipart
import com.stormtroopercs.materialreader.http.body.HTTPRequestBody.PostFields
import com.stormtroopercs.materialreader.http.body.multipart.Part.FormDataBinary
import com.stormtroopercs.materialreader.jsonwrap.JsonValue
import com.stormtroopercs.materialreader.reddit.RedditFlairChoice
import com.stormtroopercs.materialreader.reddit.RedditSubredditHistory
import com.stormtroopercs.materialreader.reddit.things.SubredditCanonicalId
import java.io.ByteArrayOutputStream
import java.util.LinkedList
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the Compose post-submission screen.
 *
 * Holds the post form state (subreddit, type, title, body/URL, flair) and
 * performs all three requests of the legacy `PostSubmitActivity` flow through
 * the cache pipeline — without needing the hosting [AppCompatActivity] for
 * the requests themselves (the raw `CacheRequestJSONParser.Listener` callbacks
 * post to the UI thread, the same pattern the other navigation ViewModels use):
 *
 *  - **submit** — the `api_type=json` `api/submit` request, built exactly as
 *    the legacy `PostSubmitContentFragment` did (kind self|link, sr, title,
 *    text|url, flair_id);
 *  - **flair** — the POST `<sr>/api/flairselector` request the legacy
 *    `requestSubredditDetails()` issued (same `is_newlink=true` body and
 *    `choices` parsing via [RedditFlairChoice.fromJsonList]);
 *  - **Imgur upload** — the multipart POST to `api.imgur.com/3/image` the
 *    legacy `ImgurUploadActivity` issued (10MB cap, `data.id` ->
 *    `https://imgur.com/<id>`).
 */
@HiltViewModel
class PostSubmitViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountManager: RedditAccountManager,
    private val cacheManager: CacheManager
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

    sealed class FlairState {
        object Loading : FlairState()
        object None : FlairState()
        data class Available(val choices: List<RedditFlairChoice>) : FlairState()
        object Unavailable : FlairState()
    }

    sealed class ImgurState {
        object Idle : ImgurState()
        object Uploading : ImgurState()
        data class Success(val url: String, val summary: String) : ImgurState()
        data class Error(val message: String) : ImgurState()
    }

    private val _submitState = MutableStateFlow<SubmitUiState>(SubmitUiState.Idle)
    val submitState: StateFlow<SubmitUiState> = _submitState.asStateFlow()

    private val _flairState = MutableStateFlow<FlairState>(FlairState.None)
    val flairState: StateFlow<FlairState> = _flairState.asStateFlow()

    private val _imgurState = MutableStateFlow<ImgurState>(ImgurState.Idle)
    val imgurState: StateFlow<ImgurState> = _imgurState.asStateFlow()

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
    var selectedFlair: RedditFlairChoice? = null
        private set

    /** The account this form submits under (default account; null if signed out). */
    fun account(): RedditAccount? =
        accountManager.getDefaultAccount()

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

    fun setSelectedFlair(choice: RedditFlairChoice?) {
        selectedFlair = choice
    }

    fun clearSubmitState() {
        _submitState.value = SubmitUiState.Idle
    }

    fun clearImgurState() {
        _imgurState.value = ImgurState.Idle
    }

    /**
     * Load the flair choices for [sr] (the flair selector the legacy
     * `PostSubmitContentFragment.requestSubredditDetails()` fetched when the
     * content screen opened).
     */
    fun loadFlairFor(sr: String) {
        val account = account() ?: run {
            _flairState.value = FlairState.Unavailable
            return
        }
        _flairState.value = FlairState.Loading

        val postFields = LinkedList<PostField>()
        postFields.add(PostField("is_newlink", "true"))

        val cacheRequest = CacheRequest(
            UriString("https://www.reddit.com" + SubredditCanonicalId(sr).toString() + "/api/flairselector"),
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
                        timestamp: com.stormtroopercs.materialreader.common.time.TimestampUTC,
                        session: UUID,
                        fromCache: Boolean
                    ) {
                        // Mirror RedditAPI.flairSelectorForNewLink's parsing.
                        val root = result.asObject()
                        if (root != null && root.isEmpty) {
                            postFlair(FlairState.None)
                            return
                        }
                        if (result.asString() == "{}") {
                            postFlair(FlairState.None)
                            return
                        }

                        val array = result.getArrayAtPath("choices")
                        if (array.isEmpty) {
                            postFlair(FlairState.Unavailable)
                            return
                        }

                        val choices = RedditFlairChoice.fromJsonList(array.get())
                        if (choices.isEmpty) {
                            postFlair(FlairState.Unavailable)
                            return
                        }
                        postFlair(FlairState.Available(choices.get()))
                    }

                    override fun onFailure(error: RRError) {
                        // 404 (subreddit doesn't exist) / 403 (no permission) /
                        // any other failure: flair is simply unavailable.
                        postFlair(FlairState.Unavailable)
                    }
                })
        )

        cacheManager.makeRequest(cacheRequest)
    }

    /**
     * Upload the image at [uri] to Imgur (the request the legacy
     * `ImgurUploadActivity.uploadImage()` issued) and report the resulting
     * `https://imgur.com/<id>` URL through [imgurState].
     */
    fun uploadImgur(activity: AppCompatActivity, uri: Uri) {
        if (_imgurState.value is ImgurState.Uploading) {
            return
        }
        _imgurState.value = ImgurState.Uploading

        viewModelScope.launch {
            try {
                val (bytes, summary) = withContext(Dispatchers.IO) {
                    val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
                    try {
                        val statSize = descriptor?.statSize ?: 0L
                        if (statSize >= 10 * 1000 * 1000L) {
                            throw IllegalArgumentException("File is too large (max 10MB)")
                        }
                        val rawBitmap = descriptor?.let {
                            BitmapFactory.decodeFileDescriptor(it.fileDescriptor)
                        }
                        val width = rawBitmap?.width ?: 0
                        val height = rawBitmap?.height ?: 0
                        rawBitmap?.recycle()

                        val byteOutput = ByteArrayOutputStream()
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            input.copyTo(byteOutput)
                        } ?: throw java.io.IOException("Could not open the image")
                        byteOutput.toByteArray() to
                            "${width}x${height} (${statSize / 1024} kB)"
                    } finally {
                        descriptor?.close()
                    }
                }

                val account = account() ?: RedditAccountManager.getAnon()
                val apiUrl = UriString("https://api.imgur.com/3/image")

                val cacheRequest = CacheRequest(
                    apiUrl,
                    account,
                    null,
                    Priority(Constants.Priority.API_ACTION),
                    DownloadStrategyAlways.Companion.INSTANCE,
                    Constants.FileType.NOCACHE,
                    DownloadQueueType.IMGUR_API,
                    Multipart().addPart(FormDataBinary("image", bytes)),
                    context,
                    CacheRequestJSONParser(
                        context,
                        object : CacheRequestJSONParser.Listener {
                            override fun onJsonParsed(
                                result: JsonValue,
                                timestamp: com.stormtroopercs.materialreader.common.time.TimestampUTC,
                                session: UUID,
                                fromCache: Boolean
                            ) {
                                try {
                                    val root = result.asObject()
                                        ?: throw RuntimeException("Response root object is null")
                                    if (true != root.getBoolean("success")) {
                                        throw RuntimeException("Imgur rejected the upload")
                                    }
                                    val id = root.getObject("data")?.getString("id")
                                        ?: throw RuntimeException("Missing image id")
                                    postImgur(ImgurState.Success("https://imgur.com/" + id, summary))
                                } catch (t: Throwable) {
                                    postImgur(
                                        ImgurState.Error(
                                            getGeneralErrorForFailure(
                                                context,
                                                RequestFailureType.PARSE_IMGUR,
                                                t,
                                                null,
                                                apiUrl,
                                                Optional.of(FailedRequestBody(result))
                                            ).message ?: "Imgur upload failed"
                                        )
                                    )
                                }
                            }

                            override fun onFailure(error: RRError) {
                                postImgur(
                                    ImgurState.Error(
                                        getGeneralErrorForFailure(
                                            context,
                                            RequestFailureType.UPLOAD_FAIL_IMGUR,
                                            null,
                                            null,
                                            null,
                                            Optional.empty()
                                        ).message ?: "Imgur upload failed"
                                    )
                                )
                            }
                        })
                )

                cacheManager.makeRequest(cacheRequest)
            } catch (e: Exception) {
                postImgur(ImgurState.Error(e.message ?: "Could not read the image"))
            }
        }
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
        selectedFlair?.let { flair ->
            postFields.add(PostField("flair_id", flair.templateId))
        }
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
                        timestamp: com.stormtroopercs.materialreader.common.time.TimestampUTC,
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

        cacheManager.makeRequest(cacheRequest)
    }

    /**
     * True if the response body does not contain a `"success": false` flag
     * (the submit-failure marker [com.stormtroopercs.materialreader.reddit.RedditAPI]
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

    private fun postFlair(state: FlairState) {
        AndroidCommon.runOnUiThread {
            _flairState.value = state
            if (state is FlairState.Available) {
                selectedFlair = null
            }
        }
    }

    private fun postImgur(state: ImgurState) {
        AndroidCommon.runOnUiThread {
            _imgurState.value = state
        }
    }
}

/**
 * Normalise user-typed subreddit input for the post form: trim whitespace,
 * lowercase, and strip any leading `/` and `r/` and trailing `/` so that
 * `R/Kotlin/`, `r/Kotlin` and `Kotlin` all resolve to `kotlin`. Top-level so
 * it can be unit-tested directly (see `PostSubmitViewModelTest`) without
 * constructing the Hilt ViewModel.
 */
internal fun normalizeSubreddit(raw: String): String {
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
