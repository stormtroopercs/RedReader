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
package org.quantumbadger.redreader.reddit.prepared

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.util.Log
import androidx.annotation.StringRes
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequest.DownloadQueueType
import org.quantumbadger.redreader.cache.CacheRequestCallbacks
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import org.quantumbadger.redreader.common.BetterSSB
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.common.General.isSensitiveDebugLoggingEnabled
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.LinkHandler.isProbablyAnImage
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.PrefsUtility.AppearancePostSubtitleItem
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.ScreenreaderPronunciation
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import org.quantumbadger.redreader.common.time.TimeFormatHelper.format
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.common.time.TimestampUTC.Companion.now
import org.quantumbadger.redreader.image.ThumbnailScaler
import org.quantumbadger.redreader.reddit.api.RedditPostActions
import org.quantumbadger.redreader.reddit.api.RedditPostActions.onActionMenuItemSelected
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import org.quantumbadger.redreader.views.RedditPostView
import java.io.IOException
import java.util.EnumSet
import java.util.UUID
import kotlin.concurrent.Volatile

class RedditPreparedPost(
    context: Context,
    cm: CacheManager,
    listId: Int,
    val src: RedditParsedPost,
    timestamp: TimestampUTC?,
    val showSubreddit: Boolean,
    showThumbnails: Boolean,
    allowHighResThumbnails: Boolean,
    private val mShowInlinePreviews: Boolean
) : RedditChangeDataManager.Listener {
    private val mChangeDataManager: RedditChangeDataManager

    val isArchived: Boolean
    val isLocked: Boolean
    val canModerate: Boolean
    val hasThumbnail: Boolean
    val mIsProbablyAnImage: Boolean

    // TODO make it possible to turn off in-memory caching when out of memory
    @Volatile
    private var thumbnailCache: Bitmap?=null

    private var thumbnailCallback: ThumbnailLoadedCallback?=null
    private var usageId = -1

    var lastChange: TimestampUTC?

    private var mBoundView: RedditPostView?=null

    // TODO too many parameters
    init {
        val user: RedditAccount?=RedditAccountManager.Companion.getInstance(context).getDefaultAccount()
        mChangeDataManager = RedditChangeDataManager.Companion.getInstance(user)

        isArchived = src.isArchived
        isLocked = src.isLocked
        canModerate = src.canModerate

        mIsProbablyAnImage = isProbablyAnImage(src.url)

        hasThumbnail = showThumbnails && hasThumbnail(src)

        val thumbnailWidth = dpToPixels(
            context,
            PrefsUtility.images_thumbnail_size_dp().toFloat()
        )

        if (hasThumbnail && hasThumbnail(src) && !shouldShowInlinePreview()) {
            downloadThumbnail(context, allowHighResThumbnails, thumbnailWidth, cm, listId)
        }

        lastChange = timestamp
        mChangeDataManager.update(timestamp, src.src)
    }

    fun shouldShowInlinePreview(): Boolean {
        return mShowInlinePreviews && (src.isPreviewEnabled
                || "gfycat.com" == src.domain
                || "i.imgur.com" == src.domain
                || "streamable.com" == src.domain
                || "i.redd.it" == src.domain
                || "v.redd.it" == src.domain)
    }

    val isVideoPreview: Boolean
        get() = src.isVideoPreview

    fun performAction(activity: BaseActivity, action: RedditPostActions.Action) {
        onActionMenuItemSelected(this, activity, action)
    }

    fun computeScore(): Int {
        var score = src.scoreExcludingOwnVote

        if (this.isUpvoted) {
            score++
        } else if (this.isDownvoted) {
            score--
        }

        return score
    }

    fun buildSubtitle(
        context: Context,
        headerMode: Boolean
    ): SpannableStringBuilder? {
        val mPostSubtitleItems: EnumSet<AppearancePostSubtitleItem?>
        val mPostAgeUnits: Int
        if (headerMode
            && PrefsUtility.appearance_post_subtitle_items_use_different_settings()
        ) {
            mPostSubtitleItems = PrefsUtility.appearance_post_header_subtitle_items()
            mPostAgeUnits = PrefsUtility.appearance_post_header_age_units()
        } else {
            mPostSubtitleItems = PrefsUtility.appearance_post_subtitle_items()
            mPostAgeUnits = PrefsUtility.appearance_post_age_units()
        }

        val appearance = context.obtainStyledAttributes(
            intArrayOf(
                R.attr.rrPostSubtitleBoldCol,
                R.attr.rrPostSubtitleUpvoteCol,
                R.attr.rrPostSubtitleDownvoteCol,
                R.attr.rrFlairBackCol,
                R.attr.rrFlairTextCol,
                R.attr.rrGoldTextCol,
                R.attr.rrGoldBackCol,
                R.attr.rrCrosspostTextCol,
                R.attr.rrCrosspostBackCol
            )
        )

        val boldCol: Int
        if (headerMode) {
            boldCol = Color.WHITE
        } else {
            boldCol = appearance.getColor(0, 255)
        }

        val rrPostSubtitleUpvoteCol = appearance.getColor(1, 255)
        val rrPostSubtitleDownvoteCol = appearance.getColor(2, 255)
        val rrFlairBackCol = appearance.getColor(3, 255)
        val rrFlairTextCol = appearance.getColor(4, 255)
        val rrGoldTextCol = appearance.getColor(5, 255)
        val rrGoldBackCol = appearance.getColor(6, 255)
        val rrCrosspostTextCol = appearance.getColor(7, 255)
        val rrCrosspostBackCol = appearance.getColor(8, 255)

        appearance.recycle()

        val postListDescSb = BetterSSB()

        val pointsCol: Int

        val score = computeScore()

        if (this.isUpvoted) {
            pointsCol = rrPostSubtitleUpvoteCol
        } else if (this.isDownvoted) {
            pointsCol = rrPostSubtitleDownvoteCol
        } else {
            pointsCol = boldCol
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.SPOILER)) {
            if (src.isSpoiler) {
                postListDescSb.append(
                    " SPOILER ",
                    (BetterSSB.Companion.BOLD
                            or BetterSSB.Companion.FOREGROUND_COLOR
                            or BetterSSB.Companion.BACKGROUND_COLOR),
                    Color.WHITE,
                    Color.rgb(50, 50, 50),
                    1f
                )
                postListDescSb.append("  ", 0)
            }
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.STICKY)) {
            if (src.isStickied) {
                postListDescSb.append(
                    " STICKY ",
                    (BetterSSB.Companion.BOLD
                            or BetterSSB.Companion.FOREGROUND_COLOR
                            or BetterSSB.Companion.BACKGROUND_COLOR),
                    Color.WHITE,
                    Color.rgb(0, 170, 0),
                    1f
                ) // TODO color?
                postListDescSb.append("  ", 0)
            }
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.CROSSPOST)) {
            if (src.isCrosspost != null) {
                postListDescSb.append(
                    (" "
                            + context.getString(string.crosspost)
                            + " "),
                    (BetterSSB.Companion.BOLD
                            or BetterSSB.Companion.FOREGROUND_COLOR
                            or BetterSSB.Companion.BACKGROUND_COLOR),
                    rrCrosspostTextCol,
                    rrCrosspostBackCol,
                    1f
                )
                postListDescSb.append("  ", 0)
            }
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.NSFW)) {
            if (src.isNsfw) {
                postListDescSb.append(
                    " NSFW ",
                    (BetterSSB.Companion.BOLD
                            or BetterSSB.Companion.FOREGROUND_COLOR
                            or BetterSSB.Companion.BACKGROUND_COLOR),
                    Color.WHITE,
                    Color.RED,
                    1f
                ) // TODO color?
                postListDescSb.append("  ", 0)
            }
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.FLAIR)) {
            if (src.flairText != null) {
                postListDescSb.append(
                    (" "
                            + src.flairText
                            + General.LTR_OVERRIDE_MARK
                            + " "),
                    (BetterSSB.Companion.BOLD
                            or BetterSSB.Companion.FOREGROUND_COLOR
                            or BetterSSB.Companion.BACKGROUND_COLOR),
                    rrFlairTextCol,
                    rrFlairBackCol,
                    1f
                )
                postListDescSb.append("  ", 0)
            }
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.COMMENTS)) {
            postListDescSb.append(
                src.commentCount.toString(),
                BetterSSB.Companion.BOLD or BetterSSB.Companion.FOREGROUND_COLOR,
                boldCol,
                0,
                1f
            )
            postListDescSb.append(
                BetterSSB.Companion.NBSP.toString() + context.getString(string.subtitle_comments) + " ",
                0
            )
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.SCORE)) {
            postListDescSb.append(
                score.toString(),
                BetterSSB.Companion.BOLD or BetterSSB.Companion.FOREGROUND_COLOR,
                pointsCol,
                0,
                1f
            )
            postListDescSb.append(
                BetterSSB.Companion.NBSP.toString() + context.getString(string.subtitle_points) + " ",
                0
            )
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.UPVOTE_RATIO)) {
            postListDescSb.append("(", 0)
            postListDescSb.append(
                src.upvotePercentage.toString() + "%",
                BetterSSB.Companion.BOLD or BetterSSB.Companion.FOREGROUND_COLOR,
                boldCol,
                0,
                1f
            )
            postListDescSb.append(
                BetterSSB.Companion.NBSP.toString() + context.getString(string.subtitle_upvote_ratio) + ") ",
                0
            )
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.GOLD)) {
            if (src.goldAmount > 0) {
                if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.SCORE)
                    || mPostSubtitleItems.contains(
                        AppearancePostSubtitleItem.UPVOTE_RATIO
                    )
                ) {
                    postListDescSb.append(" ", 0)
                }
                postListDescSb.append(
                    (" "
                            + context.getString(string.gold)
                            + BetterSSB.Companion.NBSP
                            + "x"
                            + src.goldAmount
                            + " "),
                    BetterSSB.Companion.FOREGROUND_COLOR or BetterSSB.Companion.BACKGROUND_COLOR,
                    rrGoldTextCol,
                    rrGoldBackCol,
                    1f
                )
                postListDescSb.append("  ", 0)
            }
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.AGE)) {
            postListDescSb.append(
                format(
                    src.createdTimeUTC.elapsedPeriod(),
                    context,
                    string.time_ago,
                    mPostAgeUnits
                ),
                BetterSSB.Companion.BOLD or BetterSSB.Companion.FOREGROUND_COLOR,
                boldCol,
                0,
                1f
            )
            postListDescSb.append(" ", 0)
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.AUTHOR)) {
            postListDescSb.append(context.getString(string.subtitle_by) + " ", 0)

            val setBackgroundColour: Boolean
            val backgroundColour: Int // TODO color from theme

            if ("moderator" == src.distinguished) {
                setBackgroundColour = true
                backgroundColour = Color.rgb(0, 170, 0)
            } else if ("admin" == src.distinguished) {
                setBackgroundColour = true
                backgroundColour = Color.rgb(170, 0, 0)
            } else {
                setBackgroundColour = false
                backgroundColour = 0
            }

            if (setBackgroundColour) {
                postListDescSb.append(
                    BetterSSB.Companion.NBSP.toString() + src.author + BetterSSB.Companion.NBSP,
                    (BetterSSB.Companion.BOLD
                            or BetterSSB.Companion.FOREGROUND_COLOR
                            or BetterSSB.Companion.BACKGROUND_COLOR),
                    Color.WHITE,
                    backgroundColour,
                    1f
                )
            } else {
                postListDescSb.append(
                    src.author!!,
                    BetterSSB.Companion.BOLD or BetterSSB.Companion.FOREGROUND_COLOR,
                    boldCol,
                    0,
                    1f
                )
            }

            postListDescSb.append(" ", 0)
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.SUBREDDIT)) {
            if (showSubreddit) {
                postListDescSb.append(context.getString(string.subtitle_to) + " ", 0)
                postListDescSb.append(
                    src.subreddit,
                    BetterSSB.Companion.BOLD or BetterSSB.Companion.FOREGROUND_COLOR,
                    boldCol,
                    0,
                    1f
                )
                postListDescSb.append(" ", 0)
            }
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.DOMAIN)) {
            postListDescSb.append("(" + src.domain + ")", 0)
        }

        return postListDescSb.get()
    }

    fun buildAccessibilitySubtitle(
        context: Context,
        headerMode: Boolean
    ): String {
        val mPostSubtitleItems: EnumSet<AppearancePostSubtitleItem?>
        val mPostAgeUnits: Int
        if (headerMode
            && PrefsUtility.appearance_post_subtitle_items_use_different_settings()
        ) {
            mPostSubtitleItems = PrefsUtility.appearance_post_header_subtitle_items()
            mPostAgeUnits = PrefsUtility.appearance_post_header_age_units()
        } else {
            mPostSubtitleItems = PrefsUtility.appearance_post_subtitle_items()
            mPostAgeUnits = PrefsUtility.appearance_post_age_units()
        }

        val accessibilitySubtitle = StringBuilder()

        val score = computeScore()

        val separator = " \n"

        val conciseMode = PrefsUtility.pref_accessibility_concise_mode()

        // When not in concise mode, add embellishments to the subtitle for greater clarity and
        // retention of familiar behaviour.
        if (!conciseMode) {
            accessibilitySubtitle.append(buildAccessibilityEmbellishments(context, headerMode))
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.COMMENTS)) {
            accessibilitySubtitle
                .append(
                    context.getResources().getQuantityString(
                        R.plurals.accessibility_subtitle_comments_withperiod_plural,
                        src.commentCount,
                        src.commentCount
                    )
                )
                .append(separator)
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.SCORE)) {
            accessibilitySubtitle
                .append(
                    context.getResources().getQuantityString(
                        if (conciseMode)
                            R.plurals.accessibility_subtitle_points_withperiod_concise_plural
                        else
                            R.plurals.accessibility_subtitle_points_withperiod_plural,
                        score,
                        score
                    )
                )
                .append(separator)

            if (this.isUpvoted) {
                accessibilitySubtitle
                    .append(
                        context.getString(
                            string.accessibility_subtitle_upvoted_withperiod
                        )
                    )
                    .append(separator)
            }

            if (this.isDownvoted) {
                accessibilitySubtitle
                    .append(
                        context.getString(
                            string.accessibility_subtitle_downvoted_withperiod
                        )
                    )
                    .append(separator)
            }
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.UPVOTE_RATIO)) {
            accessibilitySubtitle
                .append(
                    context.getString(
                        if (conciseMode)
                            string.accessibility_subtitle_upvote_ratio_withperiod_concise
                        else
                            string.accessibility_subtitle_upvote_ratio_withperiod,
                        src.upvotePercentage
                    )
                )
                .append(separator)
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.GOLD)) {
            if (src.goldAmount > 0) {
                accessibilitySubtitle
                    .append(
                        context.getString(
                            string.accessibility_subtitle_gold_withperiod,
                            src.goldAmount
                        )
                    )
                    .append(separator)
            }
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.AGE)) {
            accessibilitySubtitle
                .append(
                    context.getString(
                        string.accessibility_subtitle_age_withperiod,
                        format(
                            src.createdTimeUTC.elapsedPeriod(),
                            context,
                            string.time_ago,
                            mPostAgeUnits
                        )
                    )
                )
                .append(separator)
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.SUBREDDIT)) {
            if (showSubreddit) {
                accessibilitySubtitle
                    .append(
                        context.getString(
                            if (conciseMode)
                                (string.accessibility_subtitle_subreddit_withperiod_concise
                                        )
                            else
                                string.accessibility_subtitle_subreddit_withperiod,
                            ScreenreaderPronunciation.getPronunciation(
                                context,
                                src.subreddit
                            )
                        )
                    )
                    .append(separator)
            }
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.DOMAIN)) {
            val domain = src.domain.lowercase()

            if (src.isSelfPost) {
                accessibilitySubtitle
                    .append(
                        context.getString(
                            if (conciseMode)
                                string.accessibility_subtitle_selfpost_withperiod_concise
                            else
                                string.accessibility_subtitle_selfpost_withperiod
                        )
                    )
                    .append(separator)
            } else {
                accessibilitySubtitle
                    .append(
                        context.getString(
                            if (conciseMode)
                                string.accessibility_subtitle_domain_withperiod_concise
                            else
                                string.accessibility_subtitle_domain_withperiod,
                            ScreenreaderPronunciation.getPronunciation(context, domain)
                        )
                    )
                    .append(separator)

                if (src.hasSelfText()) {
                    accessibilitySubtitle
                        .append(
                            context.getString(
                                if (conciseMode)
                                    string.accessibility_subtitle_selfpost_withperiod_concise
                                else
                                    string.accessibility_subtitle_has_selftext_withperiod
                            )
                        )
                        .append(separator)
                }
            }
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.AUTHOR)) {
            @StringRes val authorString: Int

            if ("moderator" == src.distinguished) {
                authorString = if (conciseMode)
                    string.accessibility_subtitle_author_moderator_withperiod_concise_post
                else
                    string.accessibility_subtitle_author_moderator_withperiod
            } else if ("admin" == src.distinguished) {
                authorString = if (conciseMode)
                    string.accessibility_subtitle_author_admin_withperiod_concise_post
                else
                    string.accessibility_subtitle_author_admin_withperiod
            } else {
                authorString = if (conciseMode)
                    string.accessibility_subtitle_author_withperiod_concise_post
                else
                    string.accessibility_subtitle_author_withperiod
            }

            accessibilitySubtitle
                .append(
                    context.getString(
                        authorString,
                        ScreenreaderPronunciation.getPronunciation(
                            context,
                            src.author!!
                        )
                    )
                )
                .append(separator)
        }

        return accessibilitySubtitle.toString()
    }

    fun buildAccessibilityTitle(
        context: Context,
        headerMode: Boolean
    ): String {
        val a11yTitle = StringBuilder()

        // When in concise mode, add embellishments to the title for greater interruptability when
        // navigating quickly.
        if (PrefsUtility.pref_accessibility_concise_mode()) {
            a11yTitle.append(buildAccessibilityEmbellishments(context, headerMode))
        }

        a11yTitle.append(src.title)

        // Append full stop so that subtitle doesn't become part of title
        a11yTitle.append(".\n")

        return a11yTitle.toString()
    }

    private fun buildAccessibilityEmbellishments(
        context: Context,
        headerMode: Boolean
    ): String {
        val mPostSubtitleItems: EnumSet<AppearancePostSubtitleItem?>
        if (headerMode
            && PrefsUtility.appearance_post_subtitle_items_use_different_settings()
        ) {
            mPostSubtitleItems = PrefsUtility.appearance_post_header_subtitle_items()
        } else {
            mPostSubtitleItems = PrefsUtility.appearance_post_subtitle_items()
        }

        val a11yEmbellish = StringBuilder()

        val separator = " \n"

        val conciseMode = PrefsUtility.pref_accessibility_concise_mode()

        if (this.isRead) {
            a11yEmbellish
                .append(
                    ScreenreaderPronunciation.getAccessibilityString(
                        context,
                        string.accessibility_post_already_read_withperiod
                    )
                )
                .append(separator)
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.SPOILER)) {
            if (src.isSpoiler) {
                a11yEmbellish
                    .append(
                        context.getString(
                            string.accessibility_subtitle_spoiler_withperiod
                        )
                    )
                    .append(separator)
            }
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.STICKY)) {
            if (src.isStickied) {
                a11yEmbellish
                    .append(
                        context.getString(
                            string.accessibility_subtitle_sticky_withperiod
                        )
                    )
                    .append(separator)
            }
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.NSFW)) {
            if (src.isNsfw) {
                a11yEmbellish
                    .append(
                        context.getString(
                            if (conciseMode)
                                string.accessibility_subtitle_nsfw_withperiod_concise
                            else
                                string.accessibility_subtitle_nsfw_withperiod
                        )
                    )
                    .append(separator)
            }
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.FLAIR)) {
            if (src.flairText != null) {
                a11yEmbellish
                    .append(
                        context.getString(
                            if (conciseMode)
                                string.accessibility_subtitle_flair_withperiod_concise
                            else
                                string.accessibility_subtitle_flair_withperiod,
                            src.flairText
                                    + General.LTR_OVERRIDE_MARK
                        )
                    )
                    .append(separator)
            }
        }

        if (mPostSubtitleItems.contains(AppearancePostSubtitleItem.CROSSPOST)) {
            if (src.isCrosspost != null) {
                a11yEmbellish
                    .append(
                        context.getString(
                            string.accessibility_subtitle_crosspost
                        )
                    )
                    .append(separator)
            }
        }

        return a11yEmbellish.toString()
    }

    private fun downloadThumbnail(
        context: Context,
        allowHighRes: Boolean,
        sizePixels: Int,
        cm: CacheManager,
        listId: Int
    ) {
        val preview = if (allowHighRes)
            src.getPreview(sizePixels, sizePixels)
        else
            null

        val uri: UriString?

        if (preview != null) {
            uri = preview.url
        } else {
            uri = src.thumbnailUrl
        }

        val priority = Constants.Priority.THUMBNAIL
        val fileType = Constants.FileType.THUMBNAIL

        val anon: RedditAccount = RedditAccountManager.Companion.getAnon()

        cm.makeRequest(
            CacheRequest(
                uri!!,
                anon,
                null,
                Priority(priority, listId),
                DownloadStrategyIfNotCached.Companion.INSTANCE,
                fileType,
                DownloadQueueType.IMMEDIATE,
                context,
                object : CacheRequestCallbacks {
                    override fun onDataStreamComplete(
                        factory: GenericFactory<SeekableInputStream, IOException?>,
                        timestamp: TimestampUTC?,
                        session: UUID,
                        fromCache: Boolean,
                        mimetype: String?
                    ) {
                        onThumbnailStreamAvailable(factory, sizePixels)
                    }

                    override fun onFailure(error: RRError) {
                        if (isSensitiveDebugLoggingEnabled) {
                            Log.e(
                                TAG,
                                ("Failed to download thumbnail "
                                        + uri
                                        + " with error "
                                        + error),
                                error.t
                            )
                        }
                    }
                })
        )
    }

    // These operations are ordered so as to avoid race conditions
    fun getThumbnail(
        callback: ThumbnailLoadedCallback?,
        usageId: Int
    ): Bitmap? {
        this.thumbnailCallback = callback
        this.usageId = usageId
        return thumbnailCache
    }

    val isSelf: Boolean
        get() = src.isSelfPost

    val isRead: Boolean
        get() = mChangeDataManager.isRead(src.getIdAndType())

    fun bind(boundView: RedditPostView?) {
        mBoundView = boundView
        mChangeDataManager.addListener(src.getIdAndType(), this)
    }

    fun unbind(boundView: RedditPostView?) {
        if (mBoundView == boundView) {
            mBoundView = null
            mChangeDataManager.removeListener(src.getIdAndType(), this)
        }
    }

    override fun onRedditDataChange(thingIdAndType: RedditIdAndType?) {
        if (mBoundView != null) {
            val context = mBoundView!!.getContext()

            if (context != null) {
                mBoundView!!.updateAppearance()
            }
        }
    }

    // TODO handle download failure - show red "X" or something
    interface ThumbnailLoadedCallback {
        fun betterThumbnailAvailable(thumbnail: Bitmap?, usageId: Int)
    }

    fun markAsRead(context: Context?) {
        if (PrefsUtility.pref_behaviour_mark_posts_as_read()) {
            markAsRead(context, true)
        }
    }

    fun markAsRead(
        context: Context?,
        read: Boolean?
    ) {
        val user: RedditAccount?=RedditAccountManager.Companion.getInstance(context).getDefaultAccount()
        RedditChangeDataManager.Companion.getInstance(user)
            .markRead(now(), src.getIdAndType(), read)
    }

    val isUpvoted: Boolean
        get() = mChangeDataManager.isUpvoted(src.getIdAndType())

    val isDownvoted: Boolean
        get() = mChangeDataManager.isDownvoted(src.getIdAndType())

    val voteDirection: Int
        get() = if (this.isUpvoted) 1 else (if (this.isDownvoted) -1 else 0)

    val isSaved: Boolean
        get() = mChangeDataManager.isSaved(src.getIdAndType())

    val isHidden: Boolean
        get() = java.lang.Boolean.TRUE == mChangeDataManager.isHidden(src.getIdAndType())

    private fun onThumbnailStreamAvailable(
        factory: GenericFactory<SeekableInputStream, IOException?>,
        desiredSizePixels: Int
    ) {
        try {
            factory.create().use { seekableInputStream ->
                val justDecodeBounds = BitmapFactory.Options()
                justDecodeBounds.inJustDecodeBounds = true
                BitmapFactory.decodeStream(seekableInputStream, null, justDecodeBounds)
                val width = justDecodeBounds.outWidth
                val height = justDecodeBounds.outHeight

                var factor = 1

                while (width / (factor + 1) > desiredSizePixels
                    && height / (factor + 1) > desiredSizePixels
                ) {
                    factor *= 2
                }

                val scaledOptions = BitmapFactory.Options()
                scaledOptions.inSampleSize = factor

                seekableInputStream.seek(0)
                seekableInputStream.mark(0)

                val data = BitmapFactory.decodeStream(
                    seekableInputStream,
                    null,
                    scaledOptions
                )

                if (data == null) {
                    return
                }
                thumbnailCache = ThumbnailScaler.scale(data, desiredSizePixels)
                if (thumbnailCache != data) {
                    data.recycle()
                }
                if (thumbnailCallback != null) {
                    thumbnailCallback!!.betterThumbnailAvailable(
                        thumbnailCache,
                        usageId
                    )
                }
            }
        } catch (t: Throwable) {
            Log.e(
                TAG,
                "Exception while downloading thumbnail",
                t
            )
        }
    }

    companion object {
        private const val TAG = "RedditPreparedPost"

        // lol, reddit api
        private fun hasThumbnail(post: RedditParsedPost): Boolean {
            val url = post.thumbnailUrl

            return url != null && !url.value.isEmpty() && !url.value.equals(
                "nsfw",
                ignoreCase = true
            ) && !url.value.equals("self", ignoreCase = true) && !url.value.equals(
                "default",
                ignoreCase = true
            )
        }
    }
}
