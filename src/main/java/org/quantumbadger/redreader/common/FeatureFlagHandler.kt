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
package org.quantumbadger.redreader.common

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.common.General.getSharedPrefs
import org.quantumbadger.redreader.common.General.quickToast
import org.quantumbadger.redreader.common.General.startNewThread
import org.quantumbadger.redreader.fragments.AccountListDialog.Companion.show
import org.quantumbadger.redreader.fragments.ChangelogDialog
import org.quantumbadger.redreader.fragments.ReportDialog.Companion.show

object FeatureFlagHandler {
    const val PREF_LAST_VERSION: String = "lastVersion"
    const val PREF_FIRST_RUN_MESSAGE_SHOWN: String = "firstRunMessageShown"

    private const val TAG = "FeatureFlagHandler"

    private fun getBoolean(
        @StringRes id: Int,
        defaultBoolean: Boolean,
        context: Context,
        sharedPreferences: SharedPreferences
    ): Boolean {
        return sharedPreferences.getBoolean(context.getString(id), defaultBoolean)
    }

    private fun getString(
        @StringRes id: Int,
        defaultString: String?,
        context: Context,
        sharedPreferences: SharedPreferences
    ): String? {
        return sharedPreferences.getString(context.getString(id), defaultString)
    }

    private fun getStringSet(
        id: Int,
        defaultArrayRes: Int,
        context: Context,
        sharedPreferences: SharedPreferences
    ): MutableSet<String?> {
        return sharedPreferences.getStringSet(
            context.getString(id),
            org.quantumbadger.redreader.common.General.hashsetFromArray<kotlin.String?>(
                *context.getResources().getStringArray(defaultArrayRes)
            )
        )!!
    }

    fun handleUpgrade(context: Context) {
        // getAndSetFeatureFlag() will return UPGRADE_NEEDED if the app has been
        // upgraded from a version which did not support the specified feature.
        // It will return ALREADY_UPGRADED if the feature was already present
        // in the last version, or if this is a fresh install of the app.

        getSharedPrefs(context).performActionWithWriteLock(Consumer { prefs: SharedPreferences? ->
            if (FeatureFlagHandler.getAndSetFeatureFlag(
                    prefs!!,
                    FeatureFlag.COMMENT_HEADER_SUBREDDIT_FEATURE
                )
                == FeatureFlagStatus.UPGRADE_NEEDED
            ) {
                Log.i(TAG, "Upgrading, show comment subreddit in header by default")

                val existingCommentHeaderItems = FeatureFlagHandler.getStringSet(
                    string.pref_appearance_comment_header_items_key,
                    R.array.pref_appearance_comment_header_items_default,
                    context,
                    prefs
                )

                existingCommentHeaderItems.add("subreddit")

                prefs.edit()
                    .putStringSet(
                        context.getString(
                            string.pref_appearance_comment_header_items_key
                        ),
                        existingCommentHeaderItems
                    )
                    .apply()
            }
            if (FeatureFlagHandler.getAndSetFeatureFlag(
                    prefs,
                    FeatureFlag.CONTROVERSIAL_DATE_SORTS_FEATURE
                )
                == FeatureFlagStatus.UPGRADE_NEEDED
            ) {
                Log.i(TAG, "Upgrading, add date sorting for controversial posts/user comments")

                val existingDefaultPostsSort = FeatureFlagHandler.getString(
                    string.pref_behaviour_postsort_key,
                    "hot",
                    context,
                    prefs
                )

                val existingDefaultMultiPostsSort = FeatureFlagHandler.getString(
                    string.pref_behaviour_multi_postsort_key,
                    "hot",
                    context,
                    prefs
                )

                val existingDefaultUserPostsSort = FeatureFlagHandler.getString(
                    string.pref_behaviour_user_postsort_key,
                    "new",
                    context,
                    prefs
                )

                val existingDefaultUserCommentsSort = FeatureFlagHandler.getString(
                    string.pref_behaviour_user_commentsort_key,
                    "new",
                    context,
                    prefs
                )

                if (existingDefaultPostsSort == "controversial") {
                    prefs.edit().putString(
                        context.getString(string.pref_behaviour_postsort_key),
                        "controversial_day"
                    )
                        .apply()
                }

                if (existingDefaultMultiPostsSort == "controversial") {
                    prefs.edit().putString(
                        context.getString(string.pref_behaviour_multi_postsort_key),
                        "controversial_day"
                    )
                        .apply()
                }

                if (existingDefaultUserPostsSort == "controversial") {
                    prefs.edit().putString(
                        context.getString(string.pref_behaviour_user_postsort_key),
                        "controversial_all"
                    )
                        .apply()
                }

                if (existingDefaultUserCommentsSort == "controversial") {
                    prefs.edit().putString(
                        context.getString(string.pref_behaviour_user_commentsort_key),
                        "controversial_all"
                    )
                        .apply()
                }
            }

            if (FeatureFlagHandler.getAndSetFeatureFlag(
                    prefs,
                    FeatureFlag.HIDE_STATUS_BAR_FOR_MEDIA_FEATURE
                )
                == FeatureFlagStatus.UPGRADE_NEEDED
            ) {
                Log.i(TAG, "Upgrading, add setting to hide status bar on media.")

                val existingHideStatusSetting = FeatureFlagHandler.getBoolean(
                    string.pref_appearance_hide_android_status_key,
                    false,
                    context,
                    prefs
                )

                if (existingHideStatusSetting) {
                    prefs.edit().putString(
                        context.getString(string.pref_appearance_android_status_key),
                        "always_hide"
                    )
                        .apply()
                } else {
                    prefs.edit().putString(
                        context.getString(string.pref_appearance_android_status_key),
                        "never_hide"
                    )
                        .apply()
                }
            }

            if (FeatureFlagHandler.getAndSetFeatureFlag(
                    prefs,
                    FeatureFlag.REPLY_IN_POST_ACTION_MENU_FEATURE
                )
                == FeatureFlagStatus.UPGRADE_NEEDED
            ) {
                Log.i(TAG, "Upgrading, add reply button to post action menu.")

                val existingPostActionMenuItems = FeatureFlagHandler.getStringSet(
                    string.pref_menus_post_context_items_key,
                    R.array.pref_menus_post_context_items_default,
                    context,
                    prefs
                )

                existingPostActionMenuItems.add("reply")

                prefs.edit()
                    .putStringSet(
                        context.getString(
                            string.pref_menus_post_context_items_key
                        ),
                        existingPostActionMenuItems
                    )
                    .apply()
            }

            if (FeatureFlagHandler.getAndSetFeatureFlag(
                    prefs,
                    FeatureFlag.MAIN_MENU_FIND_SUBREDDIT_FEATURE
                )
                == FeatureFlagStatus.UPGRADE_NEEDED
            ) {
                Log.i(TAG, "Upgrading, add find subreddit to main menu.")

                val existingShortcutPreferences = PrefsUtility.getStringSet(
                    string.pref_menus_mainmenu_shortcutitems_key,
                    R.array.pref_menus_mainmenu_shortcutitems_items_default
                )

                existingShortcutPreferences.add("subreddit_search")

                prefs.edit().putStringSet(
                    context.getString(string.pref_menus_mainmenu_shortcutitems_key),
                    existingShortcutPreferences
                ).apply()
            }

            if (FeatureFlagHandler.getAndSetFeatureFlag(
                    prefs,
                    FeatureFlag.OPEN_COMMENT_EXTERNALLY_FEATURE
                )
                == FeatureFlagStatus.UPGRADE_NEEDED
            ) {
                Log.i(TAG, "Upgrading, add external browser option to comment action menu.")

                val existingCommentActionMenuItems = FeatureFlagHandler.getStringSet(
                    string.pref_menus_comment_context_items_key,
                    R.array.pref_menus_comment_context_items_return,
                    context,
                    prefs
                )

                existingCommentActionMenuItems.add("external")

                prefs.edit()
                    .putStringSet(
                        context.getString(string.pref_menus_comment_context_items_key),
                        existingCommentActionMenuItems
                    )
                    .apply()
            }

            if (FeatureFlagHandler.getAndSetFeatureFlag(
                    prefs,
                    FeatureFlag.POST_TITLE_TAP_ACTION_FEATURE
                )
                == FeatureFlagStatus.UPGRADE_NEEDED
            ) {
                if (FeatureFlagHandler.getBoolean(
                        string.pref_behaviour_post_title_opens_comments_key,
                        false,
                        context,
                        prefs
                    )
                ) {
                    Log.i(TAG, "Updating new post tap action preference.")

                    prefs.edit().putString(
                        context.getString(string.pref_behaviour_post_tap_action_key),
                        "comments"
                    ).apply()
                }
            }

            if (FeatureFlagHandler.getAndSetFeatureFlag(
                    prefs,
                    FeatureFlag.DEFAULT_PREF_VIDEO_PLAYBACK_CONTROLS
                )
                == FeatureFlagStatus.UPGRADE_NEEDED
            ) {
                prefs.edit().putBoolean(
                    context.getString(string.pref_behaviour_video_playback_controls_key),
                    true
                )
                    .apply()
            }

            if (FeatureFlagHandler.getAndSetFeatureFlag(
                    prefs,
                    FeatureFlag.DEFAULT_PREF_CUSTOM_TABS
                )
                == FeatureFlagStatus.UPGRADE_NEEDED
            ) {
                prefs.edit()
                    .putBoolean(
                        context.getString(string.pref_behaviour_usecustomtabs_key),
                        true
                    )
                    .apply()
            }

            if (FeatureFlagHandler.getAndSetFeatureFlag(
                    prefs,
                    FeatureFlag.CROSSPOST_ORIGIN_MENU_ITEM
                )
                == FeatureFlagStatus.UPGRADE_NEEDED
            ) {
                Log.i(TAG, "Upgrading, add crosspost origin button to post action menu.")

                val existingPostActionMenuItems = FeatureFlagHandler.getStringSet(
                    string.pref_menus_post_context_items_key,
                    R.array.pref_menus_post_context_items_default,
                    context,
                    prefs
                )

                existingPostActionMenuItems.add("crosspost_origin")

                prefs.edit()
                    .putStringSet(
                        context.getString(
                            string.pref_menus_post_context_items_key
                        ),
                        existingPostActionMenuItems
                    )
                    .apply()
            }
            if (FeatureFlagHandler.getAndSetFeatureFlag(
                    prefs,
                    FeatureFlag.MAIN_MENU_RANDOM_REMOVED
                )
                == FeatureFlagStatus.UPGRADE_NEEDED
            ) {
                Log.i(TAG, "Upgrading, removing random from main menu.")

                val existingShortcutPreferences = PrefsUtility.getStringSet(
                    string.pref_menus_mainmenu_shortcutitems_key,
                    R.array.pref_menus_mainmenu_shortcutitems_items_default
                )

                existingShortcutPreferences.remove("random")
                existingShortcutPreferences.remove("random_nsfw")

                prefs.edit().putStringSet(
                    context.getString(string.pref_menus_mainmenu_shortcutitems_key),
                    existingShortcutPreferences
                ).apply()
            }
        })
    }

    private fun setFeatureFlag(
        sharedPreferences: SharedPrefsWrapper,
        featureFlag: FeatureFlag
    ) {
        sharedPreferences.edit().putBoolean(featureFlag.getId(), true).apply()
    }

    private fun getAndSetFeatureFlag(
        sharedPreferences: SharedPreferences,
        featureFlag: FeatureFlag
    ): FeatureFlagStatus {
        val name = "rr_feature_flag_" + featureFlag.id

        val current = sharedPreferences.getBoolean(name, false)

        if (!current) {
            sharedPreferences.edit().putBoolean(name, true).apply()
        }

        return if (current) FeatureFlagStatus.ALREADY_UPGRADED else FeatureFlagStatus.UPGRADE_NEEDED
    }

    @JvmStatic
    fun handleFirstInstall(sharedPrefs: SharedPrefsWrapper) {
        // Set all feature flags when first installing

        for (flag in FeatureFlag.entries) {
            setFeatureFlag(sharedPrefs, flag)
        }
    }


    fun handleLegacyUpgrade(
        activity: AppCompatActivity,
        appVersion: Int,
        versionName: String
    ) {
        val sharedPreferences = getSharedPrefs(activity)

        val lastVersion = sharedPreferences.getInt(PREF_LAST_VERSION, 0)

        Log.i(TAG, "[Migration] Last version: " + lastVersion)

        if (lastVersion < 63) {
            // Upgrading across the 1.9.0 boundary (when oAuth was introduced)

            MaterialAlertDialogBuilder(activity)
                .setTitle(string.firstrun_login_title)
                .setMessage(string.upgrade_v190_login_message)
                .setPositiveButton(
                    string.firstrun_login_button_now,
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        show(
                            activity
                        )
                    })
                .setNegativeButton(string.firstrun_login_button_later, null)
                .show()
        }

        if (lastVersion != appVersion) {
            quickToast(
                activity,
                String.format(
                    activity.getString(string.upgrade_message),
                    versionName
                )
            )

            sharedPreferences.edit().putInt(PREF_LAST_VERSION, appVersion).apply()
            ChangelogDialog.Companion.newInstance().show(activity.getSupportFragmentManager(), null)

            if (lastVersion <= 51) {
                // Upgrading from v1.8.6.3 or lower

                val existingCommentHeaderItems = PrefsUtility.getStringSet(
                    string.pref_appearance_comment_header_items_key,
                    R.array.pref_appearance_comment_header_items_default
                )

                existingCommentHeaderItems.add("gold")

                sharedPreferences.edit().putStringSet(
                    activity.getString(string.pref_appearance_comment_header_items_key),
                    existingCommentHeaderItems
                ).apply()

                startNewThread(
                    "EmptyCache",
                    Runnable { CacheManager.Companion.getInstance(activity).emptyTheWholeCache() })
            }

            if (lastVersion <= 76) {
                // Upgrading from v1.9.6.1 or lower, enable image sharing from post context menu

                val existingPostContextItems = PrefsUtility.getStringSet(
                    string.pref_menus_post_context_items_key,
                    R.array.pref_menus_post_context_items_return
                )

                existingPostContextItems.add("share_image")

                sharedPreferences.edit().putStringSet(
                    activity.getString(string.pref_menus_post_context_items_key),
                    existingPostContextItems
                ).apply()
            }

            if (lastVersion <= 77) {
                // Upgrading from 77/1.9.7 or lower, enable pinning/subscribing/blocking a
                // subreddit and editing self-posts in the post context menu

                val existingPostContextItems = PrefsUtility.getStringSet(
                    string.pref_menus_post_context_items_key,
                    R.array.pref_menus_post_context_items_return
                )

                existingPostContextItems.add("edit")
                existingPostContextItems.add("pin")
                existingPostContextItems.add("subscribe")
                existingPostContextItems.add("block")

                sharedPreferences.edit().putStringSet(
                    activity.getString(string.pref_menus_post_context_items_key),
                    existingPostContextItems
                ).apply()
            }

            if (lastVersion <= 84) {
                // Upgrading from 84/1.9.8.5 or lower, change CheckBoxPreferences for
                // Main Menu Shortcuts into new MultiSelectListPreferences

                val existingShortcutPreferences = PrefsUtility.getStringSet(
                    string.pref_menus_mainmenu_shortcutitems_key,
                    R.array.pref_menus_mainmenu_shortcutitems_items_default
                )

                if (PrefsUtility.pref_show_popular_main_menu()) {
                    existingShortcutPreferences.add("popular")
                }

                sharedPreferences.edit().putStringSet(
                    activity.getString(string.pref_menus_mainmenu_shortcutitems_key),
                    existingShortcutPreferences
                ).apply()
            }

            if (lastVersion <= 87) {
                // + Context menu of post header will now appear also on
                // post self-text long click
                // + "Copy Self-Text" context menu item added

                val existingPostContextItems = PrefsUtility.getStringSet(
                    string.pref_menus_post_context_items_key,
                    R.array.pref_menus_post_context_items_return
                )

                existingPostContextItems.add("copy_selftext")

                sharedPreferences.edit().putStringSet(
                    activity.getString(string.pref_menus_post_context_items_key),
                    existingPostContextItems
                ).apply()
            }

            if (lastVersion <= 89) {
                //Upgrading from 89/1.9.11 or lower, enable finer control over font scales
                //and set them to match the existing settings
                //The old Inbox Font Scale setting is ignored

                Log.i(TAG, "[Migration] Upgrading from v89")

                val existingPostFontscalePreference = PrefsUtility.getString(
                    string.pref_appearance_fontscale_posts_key,
                    "-1"
                )

                val existingCommentSelfTextFontscalePreference = PrefsUtility.getString(
                    string.pref_appearance_fontscale_bodytext_key,
                    "-1"
                )

                if (existingPostFontscalePreference == existingCommentSelfTextFontscalePreference) {
                    Log.i(
                        TAG, "[Migration] Old font preferences were both "
                                + existingPostFontscalePreference
                    )

                    // Avoid setting the global font scale to -1
                    if (existingPostFontscalePreference != "-1") {
                        Log.i(TAG, "[Migration] Migrating font preferences")

                        sharedPreferences.edit().putString(
                            activity.getString(string.pref_appearance_fontscale_global_key),
                            existingPostFontscalePreference
                        ).apply()

                        sharedPreferences.edit().putString(
                            activity.getString(string.pref_appearance_fontscale_posts_key),
                            "-1"
                        ).apply()

                        sharedPreferences.edit().putString(
                            activity.getString(string.pref_appearance_fontscale_bodytext_key),
                            "-1"
                        ).apply()
                    }
                } else {
                    Log.i(
                        TAG, ("[Migration] Old font prefs: comments="
                                + existingCommentSelfTextFontscalePreference
                                + ", posts="
                                + existingPostFontscalePreference
                                + ". Migrating.")
                    )

                    sharedPreferences.edit().putString(
                        activity.getString(
                            string.pref_appearance_fontscale_post_subtitles_key
                        ),
                        existingPostFontscalePreference
                    ).apply()

                    sharedPreferences.edit().putString(
                        activity.getString(
                            string.pref_appearance_fontscale_post_header_titles_key
                        ),
                        existingPostFontscalePreference
                    ).apply()

                    sharedPreferences.edit().putString(
                        activity.getString(
                            string.pref_appearance_fontscale_post_header_subtitles_key
                        ),
                        existingPostFontscalePreference
                    ).apply()

                    sharedPreferences.edit().putString(
                        activity.getString(
                            string.pref_appearance_fontscale_comment_headers_key
                        ),
                        existingCommentSelfTextFontscalePreference
                    ).apply()

                    sharedPreferences.edit().putString(
                        activity.getString(string.pref_appearance_fontscale_linkbuttons_key),
                        existingCommentSelfTextFontscalePreference
                    ).apply()
                }

                //Upgrading from 89/1.9.11 or lower, switch to ListPreference for
                //appearance_thumbnails_show, cache_precache_images, cache_precache_comments
                val existingThumbnailsShowPreference = StringUtils.asciiLowercase(
                    PrefsUtility.appearance_thumbnails_show_old().toString()
                )

                val existingPrecacheImagesPreference = StringUtils.asciiLowercase(
                    PrefsUtility.cache_precache_images_old().toString()
                )

                val existingPrecacheCommentsPreference = StringUtils.asciiLowercase(
                    PrefsUtility.cache_precache_comments_old().toString()
                )

                sharedPreferences.edit().putString(
                    activity.getString(string.pref_appearance_thumbnails_show_list_key),
                    existingThumbnailsShowPreference
                ).apply()

                sharedPreferences.edit().putString(
                    activity.getString(string.pref_cache_precache_images_list_key),
                    existingPrecacheImagesPreference
                ).apply()

                sharedPreferences.edit().putString(
                    activity.getString(string.pref_cache_precache_comments_list_key),
                    existingPrecacheCommentsPreference
                ).apply()
            }

            if (lastVersion <= 92) {
                // Upgrading from 92/1.12 or lower

                // Switch to individual ListPreference's for
                // pref_menus_appbar (formerly pref_menus_optionsmenu_items)

                val existingOptionsMenuItems = PrefsUtility.getStringSet(
                    string.pref_menus_optionsmenu_items_key,
                    R.array.pref_menus_optionsmenu_items_items_return
                )

                class AppbarItemStrings internal constructor(
                    val stringRes: Int,
                    val returnValue: String?
                )

                val appbarItemsPrefStrings = arrayOf<AppbarItemStrings?>(
                    AppbarItemStrings(
                        string.pref_menus_appbar_accounts_key,
                        "accounts"
                    ),
                    AppbarItemStrings(
                        string.pref_menus_appbar_theme_key,
                        "theme"
                    ),
                    AppbarItemStrings(
                        string.pref_menus_appbar_close_all_key,
                        "close_all"
                    ),
                    AppbarItemStrings(
                        string.pref_menus_appbar_past_key,
                        "past"
                    ),
                    AppbarItemStrings(
                        string.pref_menus_appbar_submit_post_key,
                        "submit_post"
                    ),
                    AppbarItemStrings(
                        string.pref_menus_appbar_search_key,
                        "search"
                    ),
                    AppbarItemStrings(
                        string.pref_menus_appbar_reply_key,
                        "reply"
                    ),
                    AppbarItemStrings(
                        string.pref_menus_appbar_pin_key,
                        "pin"
                    ),
                    AppbarItemStrings(
                        string.pref_menus_appbar_block_key,
                        "block"
                    )
                )

                for (item in appbarItemsPrefStrings) {
                    val showAsAction: String

                    if (existingOptionsMenuItems.contains(item!!.returnValue)) {
                        showAsAction = "0" // Show only in three-dot menu
                    } else {
                        showAsAction = "-1" // Never show
                    }

                    sharedPreferences.edit().putString(
                        activity.getString(item.stringRes),
                        showAsAction
                    ).apply()
                }
            }
        }
    }

    private enum class FeatureFlagStatus {
        ALREADY_UPGRADED, UPGRADE_NEEDED
    }

    private enum class FeatureFlag(private val id: String) {
        COMMENT_HEADER_SUBREDDIT_FEATURE("commentHeaderSubredditFeature"),
        CONTROVERSIAL_DATE_SORTS_FEATURE("controversialDateSortsFeature"),
        HIDE_STATUS_BAR_FOR_MEDIA_FEATURE("hideStatusBarForMediaFeature"),
        REPLY_IN_POST_ACTION_MENU_FEATURE("replyInPostActionMenuFeature"),
        MAIN_MENU_FIND_SUBREDDIT_FEATURE("mainMenuFindSubreddit"),
        OPEN_COMMENT_EXTERNALLY_FEATURE("openCommentExternallyFeature"),
        POST_TITLE_TAP_ACTION_FEATURE("postTitleTapActionFeature"),
        DEFAULT_PREF_VIDEO_PLAYBACK_CONTROLS("defaultPrefVideoPlaybackControls"),
        DEFAULT_PREF_CUSTOM_TABS("defaultPrefCustomTabs"),
        CROSSPOST_ORIGIN_MENU_ITEM("crosspostOriginMenuItem"),
        MAIN_MENU_RANDOM_REMOVED("mainMenuRandomRemoved");

        fun getId(): String {
            return "rr_feature_flag_" + id
        }
    }
}
