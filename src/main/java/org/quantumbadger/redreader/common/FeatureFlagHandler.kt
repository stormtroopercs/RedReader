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
package org.quantumbadger.redreader.common

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.StringRes
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.common.General.getSharedPrefs

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
    ): MutableSet<String> {
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

        getSharedPrefs(context).performActionWithWriteLock(Consumer { prefs: SharedPreferences ->
            if (FeatureFlagHandler.getAndSetFeatureFlag(
                    prefs,
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
        val name = featureFlag.getId()

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
