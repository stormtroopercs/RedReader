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
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.cache.CacheManager
import com.stormtroopercs.materialreader.cache.CacheRequest
import com.stormtroopercs.materialreader.cache.CacheRequestCallbacks
import com.stormtroopercs.materialreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import com.stormtroopercs.materialreader.common.AndroidCommon
import com.stormtroopercs.materialreader.common.BugReporter
import com.stormtroopercs.materialreader.common.Constants
import com.stormtroopercs.materialreader.common.GenericFactory
import com.stormtroopercs.materialreader.common.Priority
import com.stormtroopercs.materialreader.common.PrefsUtility
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.common.datastream.SeekableInputStream
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.reddit.PostSort
import com.stormtroopercs.materialreader.reddit.RedditAPI
import com.stormtroopercs.materialreader.reddit.api.RedditAPIIndividualSubredditDataRequester
import com.stormtroopercs.materialreader.reddit.things.RedditSubreddit
import com.stormtroopercs.materialreader.reddit.things.SubredditCanonicalId
import com.stormtroopercs.materialreader.io.RequestResponseHandler
import com.stormtroopercs.materialreader.reddit.APIResponseHandler.ActionResponseHandler
import com.stormtroopercs.materialreader.reddit.kthings.JsonUtils.decodeRedditThingFromStream
import com.stormtroopercs.materialreader.reddit.kthings.RedditIdAndType
import com.stormtroopercs.materialreader.reddit.kthings.RedditThing
import com.stormtroopercs.materialreader.reddit.url.MultiredditPostListURL
import com.stormtroopercs.materialreader.reddit.url.PostListingURL
import com.stormtroopercs.materialreader.reddit.url.RedditURLParser
import com.stormtroopercs.materialreader.reddit.url.SearchPostListURL
import com.stormtroopercs.materialreader.reddit.url.SubredditPostListURL
import com.stormtroopercs.materialreader.reddit.url.UserPostListingURL

sealed class PostListUiState {
    data class Loading(val isInitialLoad: Boolean) : PostListUiState()
    data class Success(val posts: List<PostItem>) : PostListUiState()
    data class Error(val error: RRError) : PostListUiState()
}

/**
 * About-data for the community the listing belongs to (the swipe feed's
 * collapsing toolbar shows it as the community pill: name + subscriber
 * count). Null for non-community listings (frontpage / user / multireddit /
 * search) and while not yet fetched.
 */
data class CommunityInfo(
    val name: String,
    val subscribers: Int?,
    val headerImage: String?
)

data class PostItem(
    val id: String,
    val title: String?,
    val author: String?,
    val subreddit: String,
    val score: Int,
    val numComments: Int,
    val url: String?,
    val permalink: String,
    val isSelf: Boolean,
    val isOver18: Boolean,
    val isSpoiler: Boolean,
    val isStickied: Boolean,
    val isLocked: Boolean,
    val isVideo: Boolean,
    val isCrosspost: Boolean,
    val linkFlairText: String?,
    val authorFlairText: String?,
    val thumbnail: String?,
    val selftext: String?,
    val createdUtc: Long,
    val saved: Boolean = false,
    val hidden: Boolean = false
)

/**
 * A post action the user can invoke from the list. Maps onto the legacy
 * [RedditAPI] endpoints: the vote actions hit `api/vote` (dir +1/0/-1),
 * [SAVE]/[UNSAVE] hit `api/save` / `api/unsave`. [HIDE] and [UNHIDE] are
 * `api/hide` / `api/unhide`; [REPORT] opens the report flow (a dialog, not an
 * endpoint call here) and [SHARE] hands the permalink to the OS share sheet.
 */
enum class PostAction {
    UPVOTE, DOWNVOTE, SAVE, UNSAVE, HIDE, UNHIDE, REPORT, SHARE
}

@HiltViewModel
class PostListViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<PostListUiState>(PostListUiState.Loading(true))
    val state: StateFlow<PostListUiState> = _state.asStateFlow()

    private val _posts = MutableStateFlow<List<PostItem>>(emptyList())
    val posts: StateFlow<List<PostItem>> = _posts.asStateFlow()

    private val _sortOption = MutableStateFlow(FeedSortOption.forId("active"))
    val sortOption: StateFlow<FeedSortOption> = _sortOption.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _community = MutableStateFlow<CommunityInfo?>(null)
    val community: StateFlow<CommunityInfo?> = _community.asStateFlow()

    private var currentListPath: String = ""
    private var currentSearchQuery: String? = null

    /**
     * Derive a human-readable title for the listing the screen will show. For a
     * search, "<location>: <query>" (global when the location is empty).
     * Otherwise: the bare subreddit name becomes "r/<name>", the user listings
     * ("u/<user>/submitted", "u/<user>/saved", …) keep their path, and a
     * multireddit ("m/<name>" or "u/<user>/m/<name>") keeps its path.
     */
    private fun resolveTitle(listPath: String, searchQuery: String?) {
        val base = when {
            listPath.isBlank() || listPath == "frontpage" -> "frontpage"
            listPath == "popular" -> "popular"
            listPath == "all" -> "all"
            listPath.startsWith("u/") -> listPath
            listPath.startsWith("m/") -> listPath
            else -> "r/" + listPath
        }
        _title.value = if (searchQuery != null) {
            if (listPath.isBlank()) "Search: $searchQuery" else "$base: $searchQuery"
        } else {
            base
        }
    }

    fun fetchPosts(listPath: String, searchQuery: String? = null) {
        // A community name may arrive bare (`Palworld`) or `r/`-prefixed
        // (`r/Palworld`, `/r/Palworld` — the custom-slot dialog suggests
        // `r/...` paths and external deep links carry `r/`); both must map
        // to the same `r/<name>` listing (issue #21: the prefixed form
        // produced `r/r/<name>` 404s).
        currentListPath = normalizeListingPath(listPath)
        currentSearchQuery = searchQuery
        resolveTitle(currentListPath, searchQuery)
        // The feed's persisted sort (FINAL-DESIGN Phase 4.7) — loaded per
        // feed so each listing keeps its own order.
        _sortOption.value = FeedSortOption.forId(FeedPreferences.sortOptionIdFor(feedIdFor(currentListPath, searchQuery)))
        _state.value = PostListUiState.Loading(_state.value !is PostListUiState.Success)
        fetchList(currentListPath, searchQuery)
        fetchCommunity(currentListPath, searchQuery)
    }

    fun refresh() {
        if (currentListPath.isEmpty() && currentSearchQuery == null) return
        _state.value = PostListUiState.Loading(false)
        fetchList(currentListPath, currentSearchQuery)
        fetchCommunity(currentListPath, currentSearchQuery)
    }

    /**
     * The feed's sort (FINAL-DESIGN Phase 4.5): the reference's 9-option
     * dialog, persisted per feed. Changing it refetches the listing with
     * the option's sort (and re-orders "Old" locally).
     */
    fun setSortOption(option: FeedSortOption) {
        if (option.id == _sortOption.value.id) return
        _sortOption.value = option
        FeedPreferences.setSortOptionFor(
            feedIdFor(currentListPath, currentSearchQuery),
            option.id,
        )
        refresh()
    }

    /** A transient result for the last post action (success/failure text) to
     *  surface as a Snackbar. Null when nothing to show. */
    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult.asStateFlow()

    /**
     * Invoke [action] on [post] against the Reddit API, exactly as the legacy
     * `RedditPostActions` menu did: the vote actions hit `api/vote` (dir
     * +1/0/−1), `SAVE`/`UNSAVE` hit `api/save` / `api/unsave`, `HIDE`/`UNHIDE`
     * hit `api/hide` / `api/unhide`. The default account is used (the same the
     * listing fetches with) and the hosting [activity] is needed only to build
     * the [ActionResponseHandler] (it carries the error/dialog routing).
     *
     * `REPORT` and `SHARE` are not endpoint calls here — the screen opens the
     * report dialog / share sheet itself — so they are not handled by this
     * method.
     */
    fun performAction(activity: AppCompatActivity, post: PostItem, action: PostAction) {
        val account = RedditAccountManager.getInstance(context).getDefaultAccount()
        if (account == null) {
            _actionResult.value = "Not signed in"
            return
        }

        val idAndType = RedditIdAndType(post.id)
        val apiAction = when (action) {
            PostAction.UPVOTE -> RedditAPI.ACTION_UPVOTE
            PostAction.DOWNVOTE -> RedditAPI.ACTION_DOWNVOTE
            PostAction.SAVE -> RedditAPI.ACTION_SAVE
            PostAction.UNSAVE -> RedditAPI.ACTION_UNSAVE
            PostAction.HIDE -> RedditAPI.ACTION_HIDE
            PostAction.UNHIDE -> RedditAPI.ACTION_UNHIDE
            else -> return
        }

        val handler = object : ActionResponseHandler(activity) {
            override fun onSuccess() {
                AndroidCommon.runOnUiThread {
                    _posts.value = _posts.value.map { if (it.id == post.id) applyPostAction(it, action) else it }
                    _actionResult.value = resultMessageFor(action)
                }
            }

            override fun onFailure(error: RRError) {
                AndroidCommon.runOnUiThread {
                    _actionResult.value = error.message ?: "Action failed"
                }
            }

            override fun onCallbackException(t: Throwable) {
                BugReporter.handleGlobalError(activity, t)
            }
        }

        RedditAPI.action(
            CacheManager.getInstance(context),
            handler,
            account,
            idAndType,
            apiAction,
            activity
        )
    }

    /** Clear a shown action-result message (called after the Snackbar). */
    fun clearActionResult() {
        _actionResult.value = null
    }

    /** Re-derive a post's display score / saved / hidden flags locally after a
     *  successful action, so the list reflects the change without a refetch. */
    private fun applyPostAction(post: PostItem, action: PostAction): PostItem {
        return when (action) {
            PostAction.UPVOTE -> post.copy(score = post.score + 1)
            PostAction.DOWNVOTE -> post.copy(score = post.score - 1)
            PostAction.SAVE -> post.copy(saved = true)
            PostAction.UNSAVE -> post.copy(saved = false)
            PostAction.HIDE -> post.copy(hidden = true)
            PostAction.UNHIDE -> post.copy(hidden = false)
            else -> post
        }
    }

    private fun resultMessageFor(action: PostAction): String {
        return when (action) {
            PostAction.UPVOTE -> "Upvoted"
            PostAction.DOWNVOTE -> "Downvoted"
            PostAction.SAVE -> "Saved"
            PostAction.UNSAVE -> "Removed from saved"
            PostAction.HIDE -> "Hidden"
            PostAction.UNHIDE -> "Unhidden"
            else -> ""
        }
    }

    private fun fetchList(listPath: String, searchQuery: String?) {
        viewModelScope.launch {
            try {
                val account = RedditAccountManager.getInstance(context).getDefaultAccount()
                if (account == null) {
                    _state.value = PostListUiState.Error(
                        RRError(title = "Not signed in", message = "Sign in to view post listings")
                    )
                    return@launch
                }

                // The feed's current sort (FINAL-DESIGN Phase 4.5): the
                // option's urlSort builds the listing URL (null = the
                // listing's own default order).
                val sortOption = _sortOption.value
                val sort = sortOption.urlSort

                val jsonUri: Uri
                if (searchQuery != null) {
                    // A search listing: build a SearchPostListURL from the location
                    // (a subreddit name, a multireddit path m/<name> or
                    // u/<user>/m/<name>, or null for a global search) + the query.
                    val location = listPath.ifEmpty { null }
                    val searchUrl = SearchPostListURL.build(location, searchQuery)
                        .sort(sort)
                    val uri = searchUrl.generateJsonUri()
                    if (uri == null) {
                        _state.value = PostListUiState.Error(
                            RRError(title = "Invalid listing", message = "Could not build search URI")
                        )
                        return@launch
                    }
                    jsonUri = uri
                } else {
                    val rawUri = when {
                        listPath.isBlank() || listPath == "frontpage" -> "https://www.reddit.com/"
                        listPath == "popular" -> "https://www.reddit.com/r/popular/"
                        listPath == "all" -> "https://www.reddit.com/r/all/"
                        // A user multireddit (u/<user>/m/<name>): /me/ form.
                        listPath.startsWith("u/") && listPath.contains("/m/") -> "https://www.reddit.com/me/$listPath/"
                        listPath.startsWith("u/") -> "https://www.reddit.com/$listPath/"
                        listPath.startsWith("m/") -> "https://www.reddit.com/me/$listPath/"
                        else -> "https://www.reddit.com/r/$listPath/"
                    }
                    val postListingUrl = RedditURLParser.parseProbablePostListing(Uri.parse(rawUri))
                        .applySort(sort)
                    if (postListingUrl !is PostListingURL) {
                        _state.value = PostListUiState.Error(
                            RRError(title = "Invalid listing", message = "Invalid post listing URL")
                        )
                        return@launch
                    }
                    val uri = postListingUrl.generateJsonUri()
                    if (uri == null) {
                        _state.value = PostListUiState.Error(
                            RRError(title = "Invalid listing", message = "Could not build JSON URI")
                        )
                        return@launch
                    }
                    jsonUri = uri
                }

                val callbacks = object : CacheRequestCallbacks {
                    override fun onFailure(error: RRError) {
                        _state.value = PostListUiState.Error(error)
                    }

                    override fun onDataStreamComplete(
                        streamFactory: GenericFactory<SeekableInputStream, IOException>,
                        timestamp: TimestampUTC,
                        session: UUID,
                        fromCache: Boolean,
                        mimetype: String?
                    ) {
                        try {
                            val thing = decodeRedditThingFromStream(streamFactory.create())
                            val listing = (thing as? RedditThing.Listing)?.data
                                ?: throw RuntimeException(
                                    "Expected listing, got " + thing.javaClass.name
                                )

                            val posts = listing.children
                                .mapNotNull { it.ok() as? RedditThing.Post }
                                .map { it.toPostItem() }
                                // The "Old" option: the Reddit API has no
                                // oldest-first sort, so the listing is
                                // fetched newest-first (a strict
                                // created-utc order) and presented
                                // oldest-first.
                                .let { if (sortOption.reverse) it.reversed() else it }
                                // "Hide read posts" (the grid's "Hide read"
                                // action toggles this): drop posts the
                                // account's change data marks as read.
                                .let { filterReadPosts(it, account) }

                            _posts.value = posts
                            _state.value = PostListUiState.Success(posts)
                        } catch (e: Exception) {
                            _state.value = PostListUiState.Error(
                                RRError(
                                    title = "Parse error",
                                    message = e.message,
                                    t = e
                                )
                            )
                        }
                    }
                }

                val request = CacheRequest(
                    UriString(jsonUri.toString()),
                    account,
                    null,
                    Priority(Constants.Priority.API_POST_LIST),
                    DownloadStrategyIfNotCached.INSTANCE,
                    Constants.FileType.POST_LIST,
                    CacheRequest.DownloadQueueType.REDDIT_API,
                    context,
                    callbacks
                )
                CacheManager.getInstance(context).makeRequest(request)
            } catch (e: Exception) {
                _state.value = PostListUiState.Error(
                    RRError(title = "Error", message = e.message, t = e)
                )
            }
        }
    }

    /**
     * Fetch the about-data (subscribers, header image) of the community this
     * listing belongs to — for the swipe feed's collapsing community pill.
     * Only bare `r/<name>` subreddit listings have one; every other listing
     * shape (frontpage, user, multireddit, search) clears the state. The
     * request is best-effort: a failure leaves the pill showing the name
     * without a count.
     */
    private fun fetchCommunity(listPath: String, searchQuery: String?) {
        if (searchQuery != null || listPath.isBlank() ||
            listPath == "frontpage" || listPath == "popular" || listPath == "all" ||
            listPath.startsWith("u/") || listPath.startsWith("m/")
        ) {
            _community.value = null
            return
        }

        val account = RedditAccountManager.getInstance(context).getDefaultAccount()
        if (account == null) return

        // Seed the pill immediately with the new name so switching feeds
        // never shows the previous community's name while about-data loads.
        _community.value = CommunityInfo(name = listPath, subscribers = null, headerImage = null)

        try {
            val requester = RedditAPIIndividualSubredditDataRequester(context, account)
            requester.performRequest(
                SubredditCanonicalId(listPath),
                null,
                object : RequestResponseHandler<RedditSubreddit, RRError> {
                    override fun onRequestFailed(failureReason: RRError) {
                        // Best-effort: keep whatever the pill already shows.
                    }

                    override fun onRequestSuccess(result: RedditSubreddit, timeCached: TimestampUTC?) {
                        _community.value = CommunityInfo(
                            name = result.display_name ?: listPath,
                            subscribers = result.subscribers,
                            headerImage = result.header_img
                        )
                    }
                }
            )
        } catch (e: Exception) {
            // Best-effort — never break the listing for the pill.
        }
    }

    /**
     * Drop posts the default account's change data marks as read (the
     * "Hide read posts" setting — the More-actions grid's "Hide read"
     * action toggles it). A lookup failure never hides a post: on any
     * exception the full list is returned.
     */
    private fun filterReadPosts(
        posts: List<PostItem>,
        account: com.stormtroopercs.materialreader.account.RedditAccount,
    ): List<PostItem> {
        if (!PrefsUtility.pref_behaviour_hide_read_posts()) return posts
        return try {
            val changeData = com.stormtroopercs.materialreader.reddit.prepared.RedditChangeDataManager.getInstance(account)
            posts.filterNot { changeData.isRead(RedditIdAndType("t3_${it.id}")) }
        } catch (e: Exception) {
            posts
        }
    }
}

private fun RedditThing.Post.toPostItem(): PostItem {
    val p = data
    return PostItem(
        id = p.name.toString(),
        title = p.title?.decoded,
        author = p.author?.decoded,
        subreddit = p.subreddit.decoded,
        score = p.score,
        numComments = p.num_comments,
        url = p.findUrl()?.value,
        permalink = p.permalink.decoded,
        isSelf = p.is_self,
        isOver18 = p.over_18,
        isSpoiler = p.spoiler,
        isStickied = p.stickied,
        isLocked = p.locked,
        isVideo = p.is_video,
        isCrosspost = p.crosspost_parent != null,
        linkFlairText = p.link_flair_text?.decoded,
        authorFlairText = p.author_flair_text?.decoded,
        thumbnail = p.thumbnail?.decoded,
        selftext = p.selftext?.decoded,
        createdUtc = p.created_utc.value.toUtcSecs(),
        saved = p.saved,
        hidden = p.hidden
    )
}

/**
 * Normalise a feed path (a [PostList] route's `subreddit` field) for
 * listing construction. A community name may arrive bare (`Palworld`) or
 * `r/`-prefixed (`r/Palworld`, `/r/Palworld`, even a doubled `r/r/…`); all
 * forms map to the bare lowercase community name, which `fetchList` then
 * renders as the single `r/<name>` listing. Everything that is already a
 * full path or a default feed id passes through untouched (user listings
 * `u/…` keep their case-sensitive usernames; `m/…`/`me/…` multireddits,
 * `s/…` search, and `frontpage`/`popular`/`all` are not community names).
 * Issue #21: `r/`-prefixed paths produced
 * `https://www.reddit.com/r/r/<name>/` 404s.
 */
internal fun normalizeListingPath(path: String): String {
    val trimmed = path.trim()
    if (trimmed.isEmpty()) {
        return ""
    }
    if (trimmed == "frontpage" || trimmed == "popular" || trimmed == "all") {
        return trimmed
    }
    if (trimmed.startsWith("u/") || trimmed.startsWith("m/") ||
        trimmed.startsWith("me/") || trimmed.startsWith("s/")
    ) {
        return trimmed
    }
    var name = trimmed.removePrefix("/")
    while (name.startsWith("r/")) {
        name = name.substring(2)
    }
    return name.lowercase(java.util.Locale.US)
}

/**
 * Re-derive a parsed listing URL with the feed's sort (FINAL-DESIGN Phase
 * 4.5). Each listing URL type has its own `sort(...)` builder; an unknown
 * type (e.g. an unparseable URL) is returned unchanged.
 */
private fun RedditURLParser.RedditURL.applySort(sort: PostSort?): RedditURLParser.RedditURL {
    if (sort == null) return this
    return when (this) {
        is SubredditPostListURL -> sort(sort)
        is UserPostListingURL -> sort(sort)
        is MultiredditPostListURL -> sort(sort)
        else -> this
    }
}
