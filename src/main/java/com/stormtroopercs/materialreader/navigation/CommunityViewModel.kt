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
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.cache.CacheManager
import com.stormtroopercs.materialreader.cache.CacheRequest
import com.stormtroopercs.materialreader.cache.CacheRequestCallbacks
import com.stormtroopercs.materialreader.cache.downloadstrategy.DownloadStrategyAlways
import com.stormtroopercs.materialreader.common.Constants
import com.stormtroopercs.materialreader.common.GenericFactory
import com.stormtroopercs.materialreader.common.Priority
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.common.datastream.SeekableInputStream
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.io.RequestResponseHandler
import com.stormtroopercs.materialreader.jsonwrap.JsonValue
import com.stormtroopercs.materialreader.jsonwrap.JsonObject
import com.stormtroopercs.materialreader.reddit.RedditAPI
import com.stormtroopercs.materialreader.reddit.api.RedditAPIIndividualSubredditDataRequester
import com.stormtroopercs.materialreader.reddit.api.RedditSubredditSubscriptionManager
import com.stormtroopercs.materialreader.reddit.api.SubredditSubscriptionState
import com.stormtroopercs.materialreader.reddit.things.RedditSubreddit
import com.stormtroopercs.materialreader.reddit.things.SubredditCanonicalId

/** One community about-fact row (the About tab, FINAL-DESIGN 6.3). */
data class CommunityAboutFact(val label: String, val value: String)

/** One moderator row (the Mods tab). */
data class CommunityModerator(val name: String, val iconUrl: String? = null)

/**
 * The community detail's about-data + tab content (FINAL-DESIGN Phase 6).
 * The post feed itself is the shared [PostListViewModel] (the screen feeds
 * it the `r/<name>` listing path); this ViewModel owns everything else:
 *
 *  - the scrolling header (display name, subscriber count, icon, banner),
 *  - the About tab (description + creation date + active users + NSFW tag),
 *  - the Favorite tab (the account's subscription state + the join/leave
 *    action, which uses the account's [RedditSubredditSubscriptionManager]
 *    — the legacy `api/subscribe` path; the reference has no subscribe CTA
 *    in the header, join/favourite lives here),
 *  - the Mods tab (`about/moderators.json`).
 *
 * All fetches are best-effort: a failure leaves the relevant tab showing
 * its error state without breaking the post feed.
 */
@HiltViewModel
class CommunityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountManager: RedditAccountManager,
    private val cacheManager: CacheManager
) : ViewModel() {

    data class About(
        val name: String,
        val subscribers: Int?,
        val accountsActive: Int?,
        val iconUrl: String?,
        val bannerUrl: String?,
        val description: String?,
        val createdUtc: Long?,
        val isOver18: Boolean,
    )

    sealed class TabContent {
        object Loading : TabContent()
        data class Success(val facts: List<CommunityAboutFact>) : TabContent()
        data class Error(val message: String) : TabContent()
    }

    sealed class ModsContent {
        object Loading : ModsContent()
        data class Success(val moderators: List<CommunityModerator>) : ModsContent()
        data class Error(val message: String) : ModsContent()
    }

    /** The Favorite tab: null = unknown (not signed in / not loaded yet). */
    sealed class FavoriteContent {
        object Loading : FavoriteContent()
        data class Subscribed(val state: SubredditSubscriptionState) : FavoriteContent()
        object NotSignedIn : FavoriteContent()
    }

    private val _about = MutableStateFlow<About?>(null)
    val about: StateFlow<About?> = _about.asStateFlow()

    private val _aboutTab = MutableStateFlow<TabContent>(TabContent.Loading)
    val aboutTab: StateFlow<TabContent> = _aboutTab.asStateFlow()

    private val _mods = MutableStateFlow<ModsContent>(ModsContent.Loading)
    val mods: StateFlow<ModsContent> = _mods.asStateFlow()

    private val _favorite = MutableStateFlow<FavoriteContent>(FavoriteContent.Loading)
    val favorite: StateFlow<FavoriteContent> = _favorite.asStateFlow()

    /** Transient join/leave result text for the screen's Snackbar. */
    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult.asStateFlow()

    private var currentName: String = ""
    private var subscriptionManager: RedditSubredditSubscriptionManager? = null
    private var listenerContext: RedditSubredditSubscriptionManager.ListenerContext? = null

    fun load(name: String) {
        currentName = name
        _about.value = About(
            name = name,
            subscribers = null,
            accountsActive = null,
            iconUrl = null,
            bannerUrl = null,
            description = null,
            createdUtc = null,
            isOver18 = false,
        )
        _aboutTab.value = TabContent.Loading
        _mods.value = ModsContent.Loading
        loadAbout(name)
        loadMods(name)
        loadFavorite(name)
    }

    fun clearActionResult() {
        _actionResult.value = null
    }

    override fun onCleared() {
        listenerContext?.removeListener()
        listenerContext = null
        subscriptionManager = null
        super.onCleared()
    }

    /**
     * Join/leave the community (the Favorite tab's primary action). Uses the
     * account's [RedditSubredditSubscriptionManager] — the legacy subscribe
     * path (`api/subscribe` with `sr=<name>` + `action=sub|unsub`), which
     * toasts the result and updates the local subscription set on success.
     * The screen observes the manager's change to flip the tab state.
     */
    fun toggleFavorite(activity: AppCompatActivity) {
        val manager = subscriptionManager
        if (manager == null) {
            _favorite.value = FavoriteContent.NotSignedIn
            return
        }
        val name = currentName
        if (name.isBlank()) return
        val id = SubredditCanonicalId(name)
        val subscribed = manager.getSubscriptionState(id) == SubredditSubscriptionState.SUBSCRIBED
        if (subscribed) {
            manager.unsubscribe(id, activity)
        } else {
            manager.subscribe(id, activity)
        }
    }

    /** The current subscription state (null until loaded / not signed in). */
    fun favoriteState(): SubredditSubscriptionState? {
        val name = currentName
        if (name.isBlank()) return null
        return subscriptionManager?.getSubscriptionState(SubredditCanonicalId(name))
    }

    // ── Header + About data ──────────────────────────────────────────────

    private fun loadAbout(name: String) {
        val account = accountManager.getDefaultAccount()
        try {
            val requester = RedditAPIIndividualSubredditDataRequester(context, account)
            requester.performRequest(
                SubredditCanonicalId(name),
                null,
                object : RequestResponseHandler<RedditSubreddit, RRError> {
                    override fun onRequestFailed(failureReason: RRError) {
                        _aboutTab.value = TabContent.Error(failureReason.message ?: "Failed to load")
                    }

                    override fun onRequestSuccess(result: RedditSubreddit, timeCached: TimestampUTC?) {
                        _about.value = About(
                            name = result.display_name ?: name,
                            subscribers = result.subscribers,
                            accountsActive = result.accounts_active,
                            iconUrl = result.header_img?.takeIf { it.isNotBlank() },
                            bannerUrl = result.header_img,
                            description = result.description,
                            createdUtc = result.created_utc.takeIf { it > 0 },
                            isOver18 = result.over18 == true,
                        )
                        _aboutTab.value = TabContent.Success(buildFacts(result))
                    }
                }
            )
        } catch (e: Exception) {
            _aboutTab.value = TabContent.Error(e.message ?: "Failed to load")
        }
    }

    private fun buildFacts(result: RedditSubreddit): List<CommunityAboutFact> {
        val facts = mutableListOf<CommunityAboutFact>()
        result.description?.takeIf { it.isNotBlank() }?.let {
            facts.add(CommunityAboutFact("Description", it))
        }
        if (result.created_utc > 0) {
            facts.add(
                CommunityAboutFact(
                    "Created",
                    java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.US)
                        .format(java.util.Date(result.created_utc * 1000L)),
                )
            )
        }
        result.accounts_active?.let {
            facts.add(CommunityAboutFact("Active users", formatCount(it)))
        }
        if (result.over18 == true) {
            facts.add(CommunityAboutFact("Tag", "NSFW"))
        }
        return facts
    }

    // ── Favorite tab ─────────────────────────────────────────────────────

    private fun loadFavorite(name: String) {
        val account = accountManager.getDefaultAccount()
        try {
            val manager = RedditSubredditSubscriptionManager.getSingleton(context, account)
            subscriptionManager = manager
            // Observe the account's subscription set so the Favorite tab flips
            // live when a join/leave (or the manager's own re-sync) lands.
            listenerContext?.removeListener()
            listenerContext = manager.addListener(object :
                RedditSubredditSubscriptionManager.SubredditSubscriptionStateChangeListener {
                override fun onSubredditSubscriptionListUpdated(
                    subredditSubscriptionManager: RedditSubredditSubscriptionManager,
                ) {
                    _favorite.value = FavoriteContent.Subscribed(
                        subredditSubscriptionManager.getSubscriptionState(SubredditCanonicalId(currentName))
                            ?: SubredditSubscriptionState.NOT_SUBSCRIBED
                    )
                }

                override fun onSubredditSubscriptionAttempted(
                    subredditSubscriptionManager: RedditSubredditSubscriptionManager,
                ) {
                    _favorite.value = FavoriteContent.Subscribed(SubredditSubscriptionState.SUBSCRIBING)
                }

                override fun onSubredditUnsubscriptionAttempted(
                    subredditSubscriptionManager: RedditSubredditSubscriptionManager,
                ) {
                    _favorite.value = FavoriteContent.Subscribed(SubredditSubscriptionState.UNSUBSCRIBING)
                }
            })
            _favorite.value = FavoriteContent.Subscribed(
                manager.getSubscriptionState(SubredditCanonicalId(name))
                    ?: SubredditSubscriptionState.NOT_SUBSCRIBED
            )
        } catch (e: Exception) {
            _favorite.value = FavoriteContent.NotSignedIn
        }
    }

    // ── Mods tab ─────────────────────────────────────────────────────────

    private fun loadMods(name: String) {
        val account = accountManager.getDefaultAccount()
        val uriBuilder = Constants.Reddit.getUriBuilder("/r/$name/about/moderators.json")
            .appendQueryParameter("limit", "100")
        val jsonUri = UriString(uriBuilder.build().toString())
        val callbacks = object : CacheRequestCallbacks {
            override fun onDataStreamComplete(
                streamFactory: GenericFactory<SeekableInputStream, IOException>,
                timestamp: TimestampUTC,
                session: UUID,
                fromCache: Boolean,
                mimetype: String?,
            ) {
                try {
                    val result = streamFactory.create().use { JsonValue.parse(it) }
                    val children = result.getArrayAtPath("data", "children").get()
                    val moderators = children.mapNotNull { child ->
                        val data = child.getObjectAtPath("data").orElseNull()
                            ?: return@mapNotNull null
                        val modName = data.getString("name") ?: return@mapNotNull null
                        CommunityModerator(
                            name = modName,
                            iconUrl = data.getString("icon_img"),
                        )
                    }
                    _mods.value = ModsContent.Success(moderators)
                } catch (e: Exception) {
                    _mods.value = ModsContent.Error(e.message ?: "Failed to load moderators")
                }
            }

            override fun onFailure(error: RRError) {
                _mods.value = ModsContent.Error(error.message ?: "Failed to load moderators")
            }
        }
        val request = CacheRequest(
            jsonUri,
            account,
            null,
            Priority(Constants.Priority.API_SUBREDDIT_INVIDIVUAL),
            DownloadStrategyAlways.INSTANCE,
            Constants.FileType.SUBREDDIT_LIST,
            CacheRequest.DownloadQueueType.REDDIT_API,
            context,
            callbacks,
        )
        cacheManager.makeRequest(request)
    }

    companion object {
        /** Compact count (87.6k, 1.2M) — the reference's `N subs` format. */
        fun formatCount(value: Int): String {
            return when {
                value >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", value / 1_000_000.0)
                value >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", value / 1_000.0)
                else -> value.toString()
            }
        }
    }
}
