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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.util.Log
import android.view.MenuItem
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.activities.OptionsMenuUtility
import org.quantumbadger.redreader.activities.OptionsMenuUtility.AppbarItemsPref
import org.quantumbadger.redreader.adapters.MainMenuListingManager.SubredditAction
import org.quantumbadger.redreader.common.ConfigProviders.ConfigProvider
import org.quantumbadger.redreader.common.ConfigProviders.register
import org.quantumbadger.redreader.common.General.getSharedPrefs
import org.quantumbadger.redreader.common.General.hashsetFromArray
import org.quantumbadger.redreader.common.General.initAppConfig
import org.quantumbadger.redreader.common.General.quickToast
import org.quantumbadger.redreader.common.time.TimeDuration
import org.quantumbadger.redreader.common.time.TimeDuration.Companion.hours
import org.quantumbadger.redreader.fragments.MainMenuFragment.MainMenuShortcutItems
import org.quantumbadger.redreader.fragments.MainMenuFragment.MainMenuUserItems
import org.quantumbadger.redreader.io.WritableHashSet
import org.quantumbadger.redreader.reddit.PostCommentSort
import org.quantumbadger.redreader.reddit.PostSort
import org.quantumbadger.redreader.reddit.UserCommentSort
import org.quantumbadger.redreader.reddit.api.RedditPostActions
import org.quantumbadger.redreader.reddit.things.InvalidSubredditNameException
import org.quantumbadger.redreader.reddit.things.SubredditCanonicalId
import org.quantumbadger.redreader.settings.types.AppearanceTheme
import java.util.EnumMap
import java.util.EnumSet
import java.util.Locale
import java.util.Objects

object PrefsUtility {
    const val PREF_LANGUAGE_SETTING_MIGRATED: String="pref_language_setting_migrated"

    private var sharedPrefs: SharedPrefsWrapper?=null
    private var mRes: Resources?=null

    // Application context, so cannot leak
    @SuppressLint("StaticFieldLeak")
    private var appContext: Context?=null

    // The SharedPreferences implementation only holds weak references to
    // listeners, so keep a strong reference here for the process lifetime.
    @Suppress("PropertyName")
    private val LANGUAGE_CHANGE_LISTENER =         SharedPrefsWrapper.OnSharedPreferenceChangeListener { prefs: SharedPrefsWrapper?, key: String? ->
            if (getPrefKey(
                    string.pref_appearance_langforce_key
                ) == key
            ) {
                setAppLocales(
                    languagePrefToLocales(
                        getString(
                            string.pref_appearance_langforce_key,
                            "auto"
                        )
                    )
                )
            }
        }

    private fun getPrefKey(@StringRes prefKey: Int): String {
        return mRes!!.getString(prefKey)
    }

    fun getString(
        id: Int,
        defaultValue: String?
    ): String? {
        return sharedPrefs!!.getString(getPrefKey(id), defaultValue)
    }

    fun getStringSet(
        id: Int,
        defaultArrayRes: Int
    ): MutableSet<String> {
        return sharedPrefs!!.getStringSet(
            getPrefKey(id),
            hashsetFromArray<String>(*mRes!!.getStringArray(defaultArrayRes))
        )!!
    }

    private fun getBoolean(
        id: Int,
        defaultValue: Boolean
    ): Boolean {
        return sharedPrefs!!.getBoolean(getPrefKey(id), defaultValue)
    }

    private fun setBoolean(
        id: Int,
        newValue: Boolean
    ) {
        sharedPrefs!!.edit().putBoolean(getPrefKey(id), newValue).apply()
    }

    private fun setEnumString(id: Int, value: Enum<*>) {
        sharedPrefs!!.edit()
            .putString(getPrefKey(id), StringUtils.asciiLowercase(value.name))
            .apply()
    }

    fun pref_appearance_linkbuttons_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_linkbuttons_key, enabled)
    }

    fun pref_appearance_hide_comments_from_blocked_users_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_hide_comments_from_blocked_users_key, enabled)
    }

    fun appearance_theme_set(theme: AppearanceTheme) {
        setEnumString(string.pref_appearance_theme_key, theme)
    }

    fun images_high_res_thumbnails_set(value: NeverAlwaysOrWifiOnly) {
        setEnumString(string.pref_images_high_res_thumbnails_key, value)
    }

    fun images_inline_image_previews_set(value: NeverAlwaysOrWifiOnly) {
        setEnumString(string.pref_images_inline_image_previews_key, value)
    }

    fun appearance_thumbnails_show_set(value: NeverAlwaysOrWifiOnly) {
        setEnumString(string.pref_appearance_thumbnails_show_list_key, value)
    }

    fun images_inline_image_previews_nsfw_set(enabled: Boolean) {
        setBoolean(string.pref_images_inline_image_previews_nsfw_key, enabled)
    }

    fun images_inline_image_previews_spoiler_set(enabled: Boolean) {
        setBoolean(string.pref_images_inline_image_previews_spoiler_key, enabled)
    }

    fun appearance_thumbnails_nsfw_show_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_thumbnails_nsfw_show_key, enabled)
    }

    fun appearance_thumbnails_spoiler_show_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_thumbnails_spoiler_show_key, enabled)
    }

    fun images_thumbnail_size_set(dp: String) {
        sharedPrefs!!.edit().putString(getPrefKey(string.pref_images_thumbnail_size_key), dp).apply()
    }

    fun pref_behaviour_video_playback_controls_set(enabled: Boolean) {
        setBoolean(string.pref_behaviour_video_playback_controls_key, enabled)
    }

    fun pref_behaviour_video_frame_step_set(enabled: Boolean) {
        setBoolean(string.pref_behaviour_video_frame_step_key, enabled)
    }

    fun pref_behaviour_video_mute_default_set(enabled: Boolean) {
        setBoolean(string.pref_behaviour_video_mute_default_key, enabled)
    }

    fun pref_behaviour_video_zoom_default_set(enabled: Boolean) {
        setBoolean(string.pref_behaviour_video_zoom_default_key, enabled)
    }

    fun pref_behaviour_imagevideo_tap_close_set(enabled: Boolean) {
        setBoolean(string.pref_behaviour_imagevideo_tap_close_key, enabled)
    }

    fun pref_videos_download_before_playing_set(enabled: Boolean) {
        setBoolean(string.pref_videos_download_before_playing_key, enabled)
    }

    fun pref_behaviour_gallery_swipe_length_set(dp: String) {
        sharedPrefs!!.edit().putString(getPrefKey(string.pref_behaviour_gallery_swipe_length_key), dp).apply()
    }

    fun pref_appearance_image_viewer_show_floating_toolbar_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_image_viewer_show_floating_toolbar_key, enabled)
    }

    fun pref_appearance_show_aspect_ratio_indicator_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_show_aspect_ratio_indicator_key, enabled)
    }

    fun pref_album_skip_to_first_set(enabled: Boolean) {
        setBoolean(string.pref_album_skip_to_first_key, enabled)
    }

    fun pref_behaviour_post_tap_action_set(action: PostTapAction) {
        setEnumString(string.pref_behaviour_post_tap_action_key, action)
    }

    fun pref_behaviour_actions_comment_tap_set(action: CommentAction) {
        setEnumString(string.pref_behaviour_actions_comment_tap_key, action)
    }

    fun pref_behaviour_postsort_set(sort: PostSort) {
        setEnumString(string.pref_behaviour_postsort_key, sort)
    }

    fun pref_behaviour_commentsort_set(sort: PostCommentSort) {
        setEnumString(string.pref_behaviour_commentsort_key, sort)
    }

    fun pref_behaviour_imageview_mode_set(mode: ImageViewMode) {
        setEnumString(string.pref_behaviour_imageview_mode_key, mode)
    }

    fun pref_behaviour_albumview_mode_set(mode: AlbumViewMode) {
        setEnumString(string.pref_behaviour_albumview_mode_key, mode)
    }

    fun pref_appearance_android_status_set(mode: AppearanceStatusBarMode) {
        setEnumString(string.pref_appearance_android_status_key, mode)
    }

    fun pref_appearance_bottom_toolbar_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_bottom_toolbar_key, enabled)
    }

    fun pref_appearance_hide_toolbar_on_scroll_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_hide_toolbar_on_scroll_key, enabled)
    }

    fun pref_behaviour_videoview_mode_set(mode: VideoViewMode) {
        setEnumString(string.pref_behaviour_videoview_mode_key, mode)
    }

    fun pref_behaviour_gifview_mode_set(mode: GifViewMode) {
        setEnumString(string.pref_behaviour_gifview_mode_key, mode)
    }

    fun pref_behaviour_fling_post_left_set(action: PostFlingAction) {
        setEnumString(string.pref_behaviour_fling_post_left_key, action)
    }

    fun pref_behaviour_fling_post_right_set(action: PostFlingAction) {
        setEnumString(string.pref_behaviour_fling_post_right_key, action)
    }

    fun pref_behaviour_actions_comment_longclick_set(action: CommentAction) {
        setEnumString(string.pref_behaviour_actions_comment_longclick_key, action)
    }

    fun network_tor_set(enabled: Boolean) {
        setBoolean(string.pref_network_tor_key, enabled)
    }

    fun pref_behaviour_skiptofrontpage_set(enabled: Boolean) {
        setBoolean(string.pref_behaviour_skiptofrontpage_key, enabled)
    }

    fun pref_behaviour_useinternalbrowser_set(enabled: Boolean) {
        setBoolean(string.pref_behaviour_useinternalbrowser_key, enabled)
    }

    fun pref_behaviour_usecustomtabs_set(enabled: Boolean) {
        setBoolean(string.pref_behaviour_usecustomtabs_key, enabled)
    }

    fun pref_behaviour_screen_orientation_set(orientation: ScreenOrientation) {
        setEnumString(string.pref_behaviour_screenorientation_key, orientation)
    }

    fun pref_behaviour_enable_swipe_refresh_set(enabled: Boolean) {
        setBoolean(string.pref_behaviour_enable_swipe_refresh_key, enabled)
    }

    fun pref_behaviour_save_location_set(location: SaveLocation) {
        setEnumString(string.pref_behaviour_save_location_key, location)
    }

    fun pref_behaviour_block_screenshots_set(enabled: Boolean) {
        setBoolean(string.pref_behaviour_block_screenshots_key, enabled)
    }

    fun pref_behaviour_keep_screen_awake_set(enabled: Boolean) {
        setBoolean(string.pref_behaviour_keep_screen_awake_key, enabled)
    }

    fun pref_behaviour_user_postsort_set(sort: PostSort) {
        setEnumString(string.pref_behaviour_user_postsort_key, sort)
    }

    fun pref_behaviour_multi_postsort_set(sort: PostSort) {
        setEnumString(string.pref_behaviour_multi_postsort_key, sort)
    }

    fun pref_behaviour_user_commentsort_set(sort: UserCommentSort) {
        setEnumString(string.pref_behaviour_user_commentsort_key, sort)
    }

    fun pref_behaviour_pinned_subredditsort_set(sort: PinnedSubredditSort) {
        setEnumString(string.pref_behaviour_pinned_subredditsort_key, sort)
    }

    fun pref_behaviour_blocked_subredditsort_set(sort: BlockedSubredditSort) {
        setEnumString(string.pref_behaviour_blocked_subredditsort_key, sort)
    }

    fun pref_behaviour_self_post_tap_actions_set(action: SelfpostAction) {
        setEnumString(string.pref_behaviour_self_post_tap_actions_key, action)
    }

    fun pref_behaviour_fling_comment_left_set(action: CommentFlingAction) {
        setEnumString(string.pref_behaviour_fling_comment_left_key, action)
    }

    fun pref_behaviour_fling_comment_right_set(action: CommentFlingAction) {
        setEnumString(string.pref_behaviour_fling_comment_right_key, action)
    }

    fun pref_behaviour_nsfw_set(enabled: Boolean) {
        setBoolean(string.pref_behaviour_nsfw_key, enabled)
    }

    fun pref_behaviour_hide_read_posts_set(enabled: Boolean) {
        setBoolean(string.pref_behaviour_hide_read_posts_key, enabled)
    }

    fun pref_behaviour_mark_posts_as_read_set(enabled: Boolean) {
        setBoolean(string.pref_behaviour_mark_posts_as_read_key, enabled)
    }

    fun pref_behaviour_post_count_set(count: PostCount) {
        setEnumString(string.pref_behaviour_postcount_key, count)
    }

    fun pref_behaviour_comment_min_set(value: String?) {
        sharedPrefs!!.edit().putString(getPrefKey(string.pref_behaviour_comment_min_key), value).apply()
    }

    fun pref_behaviour_collapse_sticky_comments_set(value: BehaviourCollapseStickyComments) {
        setEnumString(string.pref_behaviour_collapse_sticky_comments_key, value)
    }

    fun pref_behaviour_sharing_domain_set(domain: SharingDomain) {
        setEnumString(string.pref_behaviour_sharing_domain_key, domain)
    }

    fun pref_behaviour_share_permalink_set(enabled: Boolean) {
        setBoolean(string.pref_behaviour_share_permalink_key, enabled)
    }

    fun pref_behaviour_sharing_include_desc_set(enabled: Boolean) {
        setBoolean(string.pref_behaviour_sharing_include_desc_key, enabled)
    }

    fun pref_behaviour_sharing_share_text_set(enabled: Boolean) {
        setBoolean(string.pref_behaviour_sharing_share_text_key, enabled)
    }

    fun pref_behaviour_sharing_share_dialog_set(enabled: Boolean) {
        setBoolean(string.pref_behaviour_sharing_share_dialog_key, enabled)
    }

    fun pref_behaviour_bezel_toolbar_swipezone_set(dp: String) {
        sharedPrefs!!.edit().putString(getPrefKey(string.pref_behaviour_bezel_toolbar_swipezone_key), dp).apply()
    }

    fun pref_behaviour_postlist_back_again_set(enabled: Boolean) {
        setBoolean(string.pref_behaviour_postlist_back_again_key, enabled)
    }

    fun pref_appearance_left_handed_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_left_handed_key, enabled)
    }

    fun pref_appearance_twopane_set(value: AppearanceTwopane) {
        setEnumString(string.pref_appearance_twopane_key, value)
    }

    fun pref_appearance_navbar_colour_set(value: AppearanceNavbarColour) {
        setEnumString(string.pref_appearance_navbar_color_key, value)
    }

    fun pref_appearance_langforce_set(value: String) {
        sharedPrefs!!.edit()
            .putString(getPrefKey(string.pref_appearance_langforce_key), value)
            .apply()
        setAppLocales(languagePrefToLocales(value))
    }

    fun pref_appearance_post_age_units_set(value: String) {
        sharedPrefs!!.edit()
            .putString(getPrefKey(string.pref_appearance_post_age_units_key), value)
            .apply()
    }

    fun pref_appearance_post_header_age_units_set(value: String) {
        sharedPrefs!!.edit()
            .putString(getPrefKey(string.pref_appearance_post_header_age_units_key), value)
            .apply()
    }

    fun pref_appearance_comment_age_units_set(value: String) {
        sharedPrefs!!.edit()
            .putString(getPrefKey(string.pref_appearance_comment_age_units_key), value)
            .apply()
    }

    fun pref_appearance_inbox_age_units_set(value: String) {
        sharedPrefs!!.edit()
            .putString(getPrefKey(string.pref_appearance_inbox_age_units_key), value)
            .apply()
    }

    fun pref_appearance_comment_age_mode_set(value: CommentAgeMode) {
        setEnumString(string.pref_appearance_comment_age_mode_key, value)
    }

    fun pref_appearance_post_subtitle_items_set(items: Set<String>) {
        sharedPrefs!!.edit()
            .putStringSet(getPrefKey(string.pref_appearance_post_subtitle_items_key), items.toMutableSet())
            .apply()
    }

    fun pref_appearance_post_subtitle_items_use_different_settings_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_post_subtitle_items_use_different_settings_key, enabled)
    }

    fun pref_appearance_post_header_subtitle_items_set(items: Set<String>) {
        sharedPrefs!!.edit()
            .putStringSet(getPrefKey(string.pref_appearance_post_header_subtitle_items_key), items.toMutableSet())
            .apply()
    }

    fun pref_appearance_post_show_comments_button_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_post_show_comments_button_key, enabled)
    }

    fun pref_appearance_post_hide_subreddit_header_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_post_hide_subreddit_header_key, enabled)
    }

    fun pref_appearance_hide_headertoolbar_postlist_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_hide_headertoolbar_postlist_key, enabled)
    }

    fun pref_appearance_comments_show_floating_toolbar_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_comments_show_floating_toolbar_key, enabled)
    }

    fun pref_appearance_comment_header_items_set(items: Set<String>) {
        sharedPrefs!!.edit()
            .putStringSet(getPrefKey(string.pref_appearance_comment_header_items_key), items.toMutableSet())
            .apply()
    }

    fun pref_appearance_link_text_clickable_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_link_text_clickable_key, enabled)
    }

    fun pref_appearance_indentlines_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_indentlines_key, enabled)
    }

    fun pref_appearance_hide_headertoolbar_commentlist_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_hide_headertoolbar_commentlist_key, enabled)
    }

    // ── Menus panel setters (30th) ──
    private fun setStringSet(id: Int, values: Set<String>) {
        sharedPrefs!!.edit()
            .putStringSet(getPrefKey(id), values.toMutableSet())
            .apply()
    }

    fun pref_menus_quick_account_switcher_set(enabled: Boolean) {
        setBoolean(string.pref_menus_quick_account_switcher_key, enabled)
    }

    fun pref_appearance_hide_username_main_menu_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_hide_username_main_menu_key, enabled)
    }

    fun pref_appearance_show_blocked_subreddits_main_menu_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_show_blocked_subreddits_main_menu_key, enabled)
    }

    fun pref_show_multireddit_main_menu_set(enabled: Boolean) {
        setBoolean(string.pref_menus_show_multireddit_main_menu_key, enabled)
    }

    fun pref_show_subscribed_subreddits_main_menu_set(enabled: Boolean) {
        setBoolean(string.pref_menus_show_subscribed_subreddits_main_menu_key, enabled)
    }

    fun pref_menus_mainmenu_dev_announcements_set(enabled: Boolean) {
        setBoolean(string.pref_menus_mainmenu_dev_announcements_key, enabled)
    }

    fun pref_menus_link_context_items_set(items: Set<String>) {
        setStringSet(string.pref_menus_link_context_items_key, items)
    }

    fun pref_menus_subreddit_context_items_set(items: Set<String>) {
        setStringSet(string.pref_menus_subreddit_context_items_key, items)
    }

    fun pref_menus_mainmenu_shortcutitems_set(items: Set<String>) {
        setStringSet(string.pref_menus_mainmenu_shortcutitems_key, items)
    }

    fun pref_menus_mainmenu_useritems_set(items: Set<String>) {
        setStringSet(string.pref_menus_mainmenu_useritems_key, items)
    }

    fun pref_menus_post_context_items_set(items: Set<String>) {
        setStringSet(string.pref_menus_post_context_items_key, items)
    }

    fun pref_menus_post_toolbar_items_set(items: Set<String>) {
        setStringSet(string.pref_menus_post_toolbar_items_key, items)
    }

    fun pref_menus_comment_context_items_set(items: Set<String>) {
        setStringSet(string.pref_menus_comment_context_items_key, items)
    }

    // ── Appbar-screen panel setters (30th) ── each pref stores an Int string:
    // -1 = never, 0 = overflow-only, 1 = if room, 2 = always
    fun pref_menus_appbar_sort_set(value: String) {
        sharedPrefs!!.edit().putString(getPrefKey(string.pref_menus_appbar_sort_key), value).apply()
    }

    fun pref_menus_appbar_refresh_set(value: String) {
        sharedPrefs!!.edit().putString(getPrefKey(string.pref_menus_appbar_refresh_key), value).apply()
    }

    fun pref_menus_appbar_past_set(value: String) {
        sharedPrefs!!.edit().putString(getPrefKey(string.pref_menus_appbar_past_key), value).apply()
    }

    fun pref_menus_appbar_submit_post_set(value: String) {
        sharedPrefs!!.edit().putString(getPrefKey(string.pref_menus_appbar_submit_post_key), value).apply()
    }

    fun pref_menus_appbar_pin_set(value: String) {
        sharedPrefs!!.edit().putString(getPrefKey(string.pref_menus_appbar_pin_key), value).apply()
    }

    fun pref_menus_appbar_subscribe_set(value: String) {
        sharedPrefs!!.edit().putString(getPrefKey(string.pref_menus_appbar_subscribe_key), value).apply()
    }

    fun pref_menus_appbar_block_set(value: String) {
        sharedPrefs!!.edit().putString(getPrefKey(string.pref_menus_appbar_block_key), value).apply()
    }

    fun pref_menus_appbar_sidebar_set(value: String) {
        sharedPrefs!!.edit().putString(getPrefKey(string.pref_menus_appbar_sidebar_key), value).apply()
    }

    fun pref_menus_appbar_accounts_set(value: String) {
        sharedPrefs!!.edit().putString(getPrefKey(string.pref_menus_appbar_accounts_key), value).apply()
    }

    fun pref_menus_appbar_theme_set(value: String) {
        sharedPrefs!!.edit().putString(getPrefKey(string.pref_menus_appbar_theme_key), value).apply()
    }

    fun pref_menus_appbar_settings_set(value: String) {
        sharedPrefs!!.edit().putString(getPrefKey(string.pref_menus_appbar_settings_key), value).apply()
    }

    fun pref_menus_appbar_close_all_set(value: String) {
        sharedPrefs!!.edit().putString(getPrefKey(string.pref_menus_appbar_close_all_key), value).apply()
    }

    fun pref_menus_appbar_reply_set(value: String) {
        sharedPrefs!!.edit().putString(getPrefKey(string.pref_menus_appbar_reply_key), value).apply()
    }

    fun pref_menus_appbar_search_set(value: String) {
        sharedPrefs!!.edit().putString(getPrefKey(string.pref_menus_appbar_search_key), value).apply()
    }

    // ── Cache panel setters (31st) ──
    fun pref_cache_rerequest_postlist_age_set(hours: String) {
        sharedPrefs!!.edit()
            .putString(getPrefKey(string.pref_cache_rerequest_postlist_age_key), hours)
            .apply()
    }

    fun pref_cache_precache_images_set(value: String) {
        sharedPrefs!!.edit()
            .putString(getPrefKey(string.pref_cache_precache_images_list_key), value)
            .apply()
    }

    fun pref_cache_precache_comments_set(value: String) {
        sharedPrefs!!.edit()
            .putString(getPrefKey(string.pref_cache_precache_comments_list_key), value)
            .apply()
    }

    fun pref_cache_maxage_listing_set(hours: String) {
        sharedPrefs!!.edit()
            .putString(getPrefKey(string.pref_cache_maxage_listing_key), hours)
            .apply()
    }

    fun pref_cache_maxage_thumb_set(hours: String) {
        sharedPrefs!!.edit()
            .putString(getPrefKey(string.pref_cache_maxage_thumb_key), hours)
            .apply()
    }

    fun pref_cache_maxage_image_set(hours: String) {
        sharedPrefs!!.edit()
            .putString(getPrefKey(string.pref_cache_maxage_image_key), hours)
            .apply()
    }

    fun pref_cache_maxage_entry_set(hours: String) {
        sharedPrefs!!.edit()
            .putString(getPrefKey(string.pref_cache_maxage_entry_key), hours)
            .apply()
    }

    fun pref_appearance_highlight_own_username_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_highlight_own_username_key, enabled)
    }

    fun pref_appearance_user_show_avatars_set(enabled: Boolean) {
        setBoolean(string.pref_appearance_user_show_avatars_key, enabled)
    }

    @Suppress("unused")
    private fun getLong(
        id: Int,
        defaultValue: Long
    ): Long {
        return sharedPrefs!!.getLong(getPrefKey(id), defaultValue)
    }

    fun isReLayoutRequired(context: Context, key: String?): Boolean {
        return context.getString(string.pref_appearance_theme_key) == key
                || (context.getString(string.pref_menus_mainmenu_useritems_key)
                == key)
                || (context.getString(string.pref_menus_mainmenu_shortcutitems_key)
                == key)
    }

    fun isRefreshRequired(context: Context, key: String): Boolean {
        return key.startsWith("pref_appearance")
                || key == context.getString(string.pref_behaviour_fling_post_left_key)
                || key == context.getString(string.pref_behaviour_fling_post_right_key)
                || key == context.getString(string.pref_behaviour_nsfw_key)
                || key == context.getString(string.pref_behaviour_postcount_key)
                || key == context.getString(string.pref_behaviour_comment_min_key)
                || key == context.getString(string.pref_behaviour_pinned_subredditsort_key)
                || key == context.getString(
            string.pref_behaviour_blocked_subredditsort_key
        )
                || key == context.getString(
            string.pref_appearance_hide_headertoolbar_commentlist_key
        )
                || key == context.getString(
            string.pref_appearance_hide_headertoolbar_postlist_key
        )
                || key == context.getString(
            string.pref_appearance_hide_comments_from_blocked_users_key
        )
                || key == context.getString(
            string.pref_appearance_highlight_own_username_key
        )
                || key == context.getString(string.pref_images_thumbnail_size_key)
                || key == context.getString(string.pref_images_inline_image_previews_key)
                || key == context.getString(
            string.pref_images_inline_image_previews_nsfw_key
        )
                || key == context.getString(
            string.pref_images_inline_image_previews_spoiler_key
        )
                || key == context.getString(string.pref_images_high_res_thumbnails_key)
                || key == context.getString(
            string.pref_accessibility_separate_body_text_lines_key
        )
                || key == context.getString(
            string.pref_accessibility_min_comment_height_key
        )
                || key == context.getString(
            string.pref_behaviour_post_title_opens_comments_key
        )
                || key == context.getString(
            string.pref_behaviour_post_tap_action_key
        )
                || key == context.getString(
            string.pref_accessibility_say_comment_indent_level_key
        )
                || key == context.getString(
            string.pref_behaviour_collapse_sticky_comments_key
        )
                || key == context.getString(
            string.pref_accessibility_concise_mode_key
        )
                || key == context.getString(
            string.pref_appearance_post_hide_subreddit_header_key
        )
                || key == REDDIT_USER_AGREEMENT_PREF
                || key == context.getString(string.pref_reddit_client_id_override_key)
    }

    fun isRestartRequired(context: Context, key: String?): Boolean {
        return context.getString(string.pref_appearance_twopane_key) == key
                || context.getString(string.pref_appearance_theme_key) == key
                || context.getString(string.pref_appearance_navbar_color_key) == key
                || (context.getString(string.pref_behaviour_bezel_toolbar_swipezone_key)
                == key)
                || (context.getString(string.pref_appearance_hide_username_main_menu_key)
                == key)
                || context.getString(string.pref_appearance_android_status_key) == key
                || (context.getString(string.pref_appearance_comments_show_floating_toolbar_key)
                == key)
                || context.getString(string.pref_behaviour_enable_swipe_refresh_key) == key
                || context.getString(string.pref_menus_show_multireddit_main_menu_key) == key
                || (context.getString(string.pref_menus_show_subscribed_subreddits_main_menu_key)
                == key)
                || context.getString(string.pref_menus_mainmenu_dev_announcements_key) == key
                || context.getString(string.pref_appearance_bottom_toolbar_key) == key
                || (context.getString(string.pref_appearance_hide_toolbar_on_scroll_key)
                == key)
                || context.getString(string.pref_behaviour_block_screenshots_key) == key
                || context.getString(string.pref_behaviour_keep_screen_awake_key) == key
    }

    fun init(context: Context) {
        register(ConfigProvider {
            "IJuC7OVo2SgR0QVvEZXr913LYMKU4r7pTqrmPe3MpddGEB+YheeH3jTZ+" +
                    "GbEQgpSutsgJugRCPETQGRwkZrw1LJxR93RpgC1iO+G/hN9BaPU1c0Qt33SSMzHCqLzU66dpD/L0yC42" +
                    "GhcJF+GUAaRzCnk0BxPjN09aO2H5rQPnUGB1kurxxCExKzWy4gEyWokgYzGGNQwAA=="
        })

        if (sharedPrefs != null) {
            sharedPrefs!!.unregisterOnSharedPreferenceChangeListener(LANGUAGE_CHANGE_LISTENER)
        }

        appContext = context.getApplicationContext()
        sharedPrefs = getSharedPrefs(context)
        mRes = Objects.requireNonNull<Resources>(context.getResources())
        initAppConfig(context)

        // Applies the language setting whenever the preference changes, including
        // when settings are restored from a backup.
        sharedPrefs!!.registerOnSharedPreferenceChangeListener(LANGUAGE_CHANGE_LISTENER)
    }

    fun appearance_twopane(): AppearanceTwopane {
        return AppearanceTwopane.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_appearance_twopane_key,
                    "auto"
                )!!
            )
        )
    }

    val isNightMode: Boolean
        get() {
            val theme = appearance_theme()

            return theme == AppearanceTheme.NIGHT || theme == AppearanceTheme.NIGHT_LOWCONTRAST || theme == AppearanceTheme.ULTRABLACK
        }

    fun appearance_theme(): AppearanceTheme {
        return AppearanceTheme.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_appearance_theme_key,
                    "red"
                )!!
            )
        )
    }

    fun appearance_navbar_colour(): AppearanceNavbarColour {
        return AppearanceNavbarColour.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_appearance_navbar_color_key,
                    "black"
                )!!
            )
        )
    }

    fun applyTheme(activity: Activity) {
        val theme = appearance_theme()

        when (theme) {
            AppearanceTheme.RED -> activity.setTheme(R.style.RR_Light_Red)
            AppearanceTheme.GREEN -> activity.setTheme(R.style.RR_Light_Green)
            AppearanceTheme.BLUE -> activity.setTheme(R.style.RR_Light_Blue)
            AppearanceTheme.LTBLUE -> activity.setTheme(R.style.RR_Light_LtBlue)
            AppearanceTheme.ORANGE -> activity.setTheme(R.style.RR_Light_Orange)
            AppearanceTheme.GRAY -> activity.setTheme(R.style.RR_Light_Gray)
            AppearanceTheme.NIGHT -> activity.setTheme(R.style.RR_Dark)
            AppearanceTheme.NIGHT_LOWCONTRAST -> activity.setTheme(R.style.RR_Dark_LowContrast)
            AppearanceTheme.ULTRABLACK -> activity.setTheme(R.style.RR_Dark_UltraBlack)
        }
    }

    fun applySettingsTheme(activity: Activity) {
        activity.setTheme(R.style.RR_Settings)
    }

    private fun languagePrefToLocales(value: String?): LocaleListCompat {
        if (value == null || value == "auto") {
            return LocaleListCompat.getEmptyLocaleList()
        }

        if (value.contains("-r")) {
            val split: Array<String?> =                 value.split("-r".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return LocaleListCompat.create(Locale(split[0], split[1]))
        }

        return LocaleListCompat.create(Locale(value))
    }

    private fun localesToLanguagePref(locales: LocaleListCompat): String {
        val locale = locales.get(0)

        if (locale == null) {
            return "auto"
        }

        if (locale.getCountry().isEmpty()) {
            return locale.getLanguage()
        }

        return locale.getLanguage() + "-r" + locale.getCountry()
    }

    // AppCompatDelegate.setApplicationLocales() can silently fail on Android 13+
    // when no activity has been created yet, so use the platform LocaleManager
    // directly there. Below Android 13, the AppCompat backport applies the
    // locales to each activity as it is created.
    private fun setAppLocales(locales: LocaleListCompat) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Objects.requireNonNull<LocaleManager>(
                appContext!!.getSystemService<LocaleManager?>(
                    LocaleManager::class.java
                )
            )
                .setApplicationLocales((locales.unwrap() as android.os.LocaleList?)!!)
        } else {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    // Applies the language preference process-wide, and keeps it in sync with the
    // Android 13+ per-app language setting. Must be called on app startup, before
    // any activities are created.
    fun applyLanguageSetting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && sharedPrefs!!.getBoolean(PREF_LANGUAGE_SETTING_MIGRATED, false)
        ) {
            // The system's per-app language setting is authoritative here, as the
            // user may have changed it while the app wasn't running.

            val systemLocales = LocaleListCompat.wrap(
                Objects.requireNonNull<LocaleManager>(
                    appContext!!.getSystemService<LocaleManager?>(LocaleManager::class.java)
                )
                    .getApplicationLocales()
            )

            val systemValue = localesToLanguagePref(systemLocales)

            if (systemValue != getString(
                    string.pref_appearance_langforce_key,
                    "auto"
                )
            ) {
                sharedPrefs!!.edit()
                    .putString(
                        getPrefKey(string.pref_appearance_langforce_key),
                        systemValue
                    )
                    .apply()
            }
        } else {
            sharedPrefs!!.edit().putBoolean(PREF_LANGUAGE_SETTING_MIGRATED, true).apply()

            setAppLocales(
                languagePrefToLocales(
                    getString(
                        string.pref_appearance_langforce_key,
                        "auto"
                    )
                )
            )
        }
    }

    // Below Android 13, AppCompat only applies the language setting to
    // AppCompatActivity contexts. Use this for strings shown from background code
    // (e.g. notifications).
    fun getLocalisedContext(context: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context
        }

        val locales = languagePrefToLocales(
            getString(
                string.pref_appearance_langforce_key,
                "auto"
            )
        )

        if (locales.isEmpty()) {
            return context
        }

        val conf = Configuration(context.getResources().getConfiguration())
        conf.setLocale(locales.get(0))

        return context.createConfigurationContext(conf)
    }

    fun appearance_thumbnails_show(): NeverAlwaysOrWifiOnly {
        return NeverAlwaysOrWifiOnly.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_appearance_thumbnails_show_list_key,
                    "always"
                )!!
            )
        )
    }

    fun appearance_thumbnails_show_old(): NeverAlwaysOrWifiOnly {
        if (!getBoolean(
                string.pref_appearance_thumbnails_show_key,
                true
            )
        ) {
            return NeverAlwaysOrWifiOnly.NEVER
        } else if (getBoolean(
                string.pref_appearance_thumbnails_wifionly_key,
                false
            )
        ) {
            return NeverAlwaysOrWifiOnly.WIFIONLY
        } else {
            return NeverAlwaysOrWifiOnly.ALWAYS
        }
    }

    fun appearance_thumbnails_nsfw_show(): Boolean {
        return getBoolean(
            string.pref_appearance_thumbnails_nsfw_show_key,
            false
        )
    }

    fun appearance_thumbnails_spoiler_show(): Boolean {
        return getBoolean(
            string.pref_appearance_thumbnails_spoiler_show_key,
            false
        )
    }

    fun appearance_fontscale_global(): Float {
        return PrefsUtility.getString(
            string.pref_appearance_fontscale_global_key,
            "1"
        )!!.toFloat()
    }

    fun appearance_fontscale_bodytext(): Float {
        if (getString(
                string.pref_appearance_fontscale_bodytext_key,
                "-1"
            ) == "-1"
        ) {
            return appearance_fontscale_global()
        }
        return PrefsUtility.getString(
            string.pref_appearance_fontscale_bodytext_key,
            "-1"
        )!!.toFloat()
    }

    fun appearance_fontscale_comment_headers(): Float {
        if (getString(
                string.pref_appearance_fontscale_comment_headers_key,
                "-1"
            ) == "-1"
        ) {
            return appearance_fontscale_global()
        }
        return PrefsUtility.getString(
            string.pref_appearance_fontscale_comment_headers_key,
            "-1"
        )!!.toFloat()
    }

    fun appearance_fontscale_linkbuttons(): Float {
        if (getString(
                string.pref_appearance_fontscale_linkbuttons_key,
                "-1"
            ) == "-1"
        ) {
            return appearance_fontscale_global()
        }
        return PrefsUtility.getString(
            string.pref_appearance_fontscale_linkbuttons_key,
            "-1"
        )!!.toFloat()
    }

    fun appearance_fontscale_posts(): Float {
        if (getString(
                string.pref_appearance_fontscale_posts_key,
                "-1"
            ) == "-1"
        ) {
            return appearance_fontscale_global()
        }
        return PrefsUtility.getString(
            string.pref_appearance_fontscale_posts_key,
            "-1"
        )!!.toFloat()
    }

    fun appearance_fontscale_post_subtitles(): Float {
        if (getString(
                string.pref_appearance_fontscale_post_subtitles_key,
                "-1"
            ) == "-1"
        ) {
            return appearance_fontscale_global()
        }
        return PrefsUtility.getString(
            string.pref_appearance_fontscale_post_subtitles_key,
            "-1"
        )!!.toFloat()
    }

    fun appearance_fontscale_post_header_titles(): Float {
        if (getString(
                string.pref_appearance_fontscale_post_header_titles_key,
                "-1"
            ) == "-1"
        ) {
            return appearance_fontscale_global()
        }
        return PrefsUtility.getString(
            string.pref_appearance_fontscale_post_header_titles_key,
            "-1"
        )!!.toFloat()
    }

    fun appearance_fontscale_post_header_subtitles(): Float {
        if (getString(
                string.pref_appearance_fontscale_post_header_subtitles_key,
                "-1"
            ) == "-1"
        ) {
            return appearance_fontscale_global()
        }
        return PrefsUtility.getString(
            string.pref_appearance_fontscale_post_header_subtitles_key,
            "-1"
        )!!.toFloat()
    }

    fun pref_appearance_hide_username_main_menu(): Boolean {
        return getBoolean(
            string.pref_appearance_hide_username_main_menu_key,
            false
        )
    }

    fun pref_show_popular_main_menu(): Boolean {
        return getBoolean(
            string.pref_menus_show_popular_main_menu_key,
            false
        )
    }

    fun pref_show_multireddit_main_menu(): Boolean {
        return getBoolean(
            string.pref_menus_show_multireddit_main_menu_key,
            true
        )
    }

    fun pref_show_subscribed_subreddits_main_menu(): Boolean {
        return getBoolean(
            string.pref_menus_show_subscribed_subreddits_main_menu_key,
            true
        )
    }

    fun pref_menus_mainmenu_dev_announcements(): Boolean {
        return getBoolean(
            string.pref_menus_mainmenu_dev_announcements_key,
            true
        )
    }

    fun pref_appearance_show_blocked_subreddits_main_menu(): Boolean {
        return getBoolean(
            string.pref_appearance_show_blocked_subreddits_main_menu_key,
            false
        )
    }

    fun pref_appearance_linkbuttons(): Boolean {
        return getBoolean(
            string.pref_appearance_linkbuttons_key,
            true
        )
    }

    fun pref_appearance_android_status(): AppearanceStatusBarMode {
        return AppearanceStatusBarMode.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_appearance_android_status_key,
                    "never_hide"
                )!!
            )
        )
    }

    fun pref_appearance_link_text_clickable(): Boolean {
        return getBoolean(
            string.pref_appearance_link_text_clickable_key,
            true
        )
    }

    fun pref_appearance_image_viewer_show_floating_toolbar(): Boolean {
        return getBoolean(
            string.pref_appearance_image_viewer_show_floating_toolbar_key,
            true
        )
    }

    fun pref_appearance_show_aspect_ratio_indicator(): Boolean {
        return getBoolean(
            string.pref_appearance_show_aspect_ratio_indicator_key,
            false
        )
    }

    fun pref_album_skip_to_first(): Boolean {
        return getBoolean(
            string.pref_album_skip_to_first_key,
            false
        )
    }

    fun pref_appearance_comments_show_floating_toolbar(): Boolean {
        return getBoolean(
            string.pref_appearance_comments_show_floating_toolbar_key,
            true
        )
    }

    fun pref_appearance_indentlines(): Boolean {
        return getBoolean(
            string.pref_appearance_indentlines_key,
            false
        )
    }

    fun pref_appearance_left_handed(): Boolean {
        return getBoolean(
            string.pref_appearance_left_handed_key,
            false
        )
    }

    fun pref_appearance_bottom_toolbar(): Boolean {
        return getBoolean(
            string.pref_appearance_bottom_toolbar_key,
            false
        )
    }

    fun pref_appearance_hide_toolbar_on_scroll(): Boolean {
        return getBoolean(
            string.pref_appearance_hide_toolbar_on_scroll_key,
            false
        )
    }

    fun pref_appearance_post_hide_subreddit_header(): Boolean {
        return getBoolean(
            string.pref_appearance_post_hide_subreddit_header_key,
            false
        )
    }

    fun pref_appearance_hide_headertoolbar_postlist(): Boolean {
        return getBoolean(
            string.pref_appearance_hide_headertoolbar_postlist_key,
            false
        )
    }

    fun pref_appearance_hide_headertoolbar_commentlist(): Boolean {
        return getBoolean(
            string.pref_appearance_hide_headertoolbar_commentlist_key,
            false
        )
    }

    fun pref_appearance_hide_comments_from_blocked_users(): Boolean {
        return getBoolean(
            string.pref_appearance_hide_comments_from_blocked_users_key,
            false
        )
    }

    fun pref_appearance_highlight_own_username(): Boolean {
        return getBoolean(
            string.pref_appearance_highlight_own_username_key,
            true
        )
    }

    fun appearance_post_subtitle_items(): EnumSet<AppearancePostSubtitleItem> {
        val strings = getStringSet(
            string.pref_appearance_post_subtitle_items_key,
            R.array.pref_appearance_post_subtitle_items_default
        )

        val result = EnumSet.noneOf<AppearancePostSubtitleItem>(
            AppearancePostSubtitleItem::class.java
        )
        for (s in strings!!) {
            result.add(AppearancePostSubtitleItem.valueOf(StringUtils.asciiUppercase(s)))
        }

        return result
    }

    fun appearance_post_age_units(): Int {
        try {
            return PrefsUtility.getString(
                org.quantumbadger.redreader.R.string.pref_appearance_post_age_units_key,
                "2"
            )!!.toInt()
        } catch (e: Throwable) {
            return 2
        }
    }

    fun appearance_post_subtitle_items_use_different_settings(): Boolean {
        return getBoolean(
            string.pref_appearance_post_subtitle_items_use_different_settings_key,
            false
        )
    }

    fun appearance_post_header_subtitle_items(): EnumSet<AppearancePostSubtitleItem> {
        val strings = getStringSet(
            string.pref_appearance_post_header_subtitle_items_key,
            R.array.pref_appearance_post_subtitle_items_default
        )

        val result = EnumSet.noneOf<AppearancePostSubtitleItem>(
            AppearancePostSubtitleItem::class.java
        )
        for (s in strings!!) {
            result.add(AppearancePostSubtitleItem.valueOf(StringUtils.asciiUppercase(s)))
        }

        return result
    }

    fun appearance_post_header_age_units(): Int {
        try {
            return PrefsUtility.getString(
                org.quantumbadger.redreader.R.string.pref_appearance_post_header_age_units_key,
                "2"
            )!!.toInt()
        } catch (e: Throwable) {
            return 2
        }
    }

    fun appearance_post_show_comments_button(): Boolean {
        return getBoolean(
            string.pref_appearance_post_show_comments_button_key,
            true
        )
    }

    fun appearance_comment_header_items(): EnumSet<AppearanceCommentHeaderItem> {
        val strings = getStringSet(
            string.pref_appearance_comment_header_items_key,
            R.array.pref_appearance_comment_header_items_default
        )

        val result = EnumSet.noneOf<AppearanceCommentHeaderItem>(
            AppearanceCommentHeaderItem::class.java
        )
        for (s in strings!!) {
            if (s.equals("ups_downs", ignoreCase = true)) {
                continue
            }

            try {
                result.add(AppearanceCommentHeaderItem.valueOf(StringUtils.asciiUppercase(s)))
            } catch (e: IllegalArgumentException) {
                // Ignore -- this option no longer exists
            }
        }

        return result
    }

    fun appearance_comment_age_units(): Int {
        try {
            return PrefsUtility.getString(
                org.quantumbadger.redreader.R.string.pref_appearance_comment_age_units_key,
                "2"
            )!!.toInt()
        } catch (e: Throwable) {
            return 2
        }
    }

    fun appearance_user_show_avatars(): Boolean {
        return getBoolean(
            string.pref_appearance_user_show_avatars_key,
            true
        )
    }

    fun appearance_comment_age_mode(): CommentAgeMode {
        return CommentAgeMode.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_appearance_comment_age_mode_key,
                    "absolute"
                )!!
            )
        )
    }

    fun appearance_inbox_age_units(): Int {
        try {
            return PrefsUtility.getString(
                org.quantumbadger.redreader.R.string.pref_appearance_inbox_age_units_key,
                "2"
            )!!.toInt()
        } catch (e: Throwable) {
            return 2
        }
    }

    fun images_thumbnail_size_dp(): Int {
        try {
            return PrefsUtility.getString(
                org.quantumbadger.redreader.R.string.pref_images_thumbnail_size_key,
                "64"
            )!!.toInt()
        } catch (e: Throwable) {
            return 64
        }
    }

    fun images_inline_image_previews(): NeverAlwaysOrWifiOnly {
        return NeverAlwaysOrWifiOnly.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_images_inline_image_previews_key,
                    "always"
                )!!
            )
        )
    }

    fun images_inline_image_previews_nsfw(): Boolean {
        return getBoolean(
            string.pref_images_inline_image_previews_nsfw_key,
            false
        )
    }

    fun images_inline_image_previews_spoiler(): Boolean {
        return getBoolean(
            string.pref_images_inline_image_previews_spoiler_key,
            false
        )
    }

    fun images_high_res_thumbnails(): NeverAlwaysOrWifiOnly {
        return NeverAlwaysOrWifiOnly.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_images_high_res_thumbnails_key,
                    "wifionly"
                )!!
            )
        )
    }

    /**//////////////////////////// */ // pref_behaviour
    /**//////////////////////////// */
    fun pref_behaviour_skiptofrontpage(): Boolean {
        return getBoolean(
            string.pref_behaviour_skiptofrontpage_key,
            false
        )
    }

    fun pref_behaviour_useinternalbrowser(): Boolean {
        return getBoolean(
            string.pref_behaviour_useinternalbrowser_key,
            true
        )
    }

    fun pref_behaviour_usecustomtabs(): Boolean {
        return getBoolean(
            string.pref_behaviour_usecustomtabs_key,
            true
        ) && !network_tor()
    }

    fun set_pref_behaviour_notifications(enabled: Boolean) {
        setBoolean(
            string.pref_behaviour_notifications_key,
            enabled
        )
    }

    fun pref_behaviour_notifications(): Boolean {
        return getBoolean(
            string.pref_behaviour_notifications_key,
            true
        )
    }

    fun pref_behaviour_enable_swipe_refresh(): Boolean {
        return getBoolean(
            string.pref_behaviour_enable_swipe_refresh_key,
            true
        )
    }

    fun pref_behaviour_video_playback_controls(): Boolean {
        return getBoolean(
            string.pref_behaviour_video_playback_controls_key,
            true
        )
    }

    fun pref_behaviour_video_frame_step(): Boolean {
        return getBoolean(
            string.pref_behaviour_video_frame_step_key,
            false
        )
    }

    fun pref_behaviour_video_mute_default(): Boolean {
        return getBoolean(
            string.pref_behaviour_video_mute_default_key,
            true
        )
    }

    fun pref_behaviour_video_zoom_default(): Boolean {
        return getBoolean(
            string.pref_behaviour_video_zoom_default_key,
            false
        )
    }

    fun pref_videos_download_before_playing(): Boolean {
        return getBoolean(
            string.pref_videos_download_before_playing_key,
            false
        )
    }

    fun pref_behaviour_imagevideo_tap_close(): Boolean {
        return getBoolean(
            string.pref_behaviour_imagevideo_tap_close_key,
            true
        )
    }

    fun pref_behaviour_bezel_toolbar_swipezone_dp(): Int {
        try {
            return PrefsUtility.getString(
                org.quantumbadger.redreader.R.string.pref_behaviour_bezel_toolbar_swipezone_key,
                "10"
            )!!.toInt()
        } catch (e: Throwable) {
            return 10
        }
    }

    fun pref_behaviour_back_again(): Boolean {
        return getBoolean(
            string.pref_behaviour_postlist_back_again_key,
            false
        )
    }

    fun pref_behaviour_gallery_swipe_length_dp(): Int {
        try {
            return PrefsUtility.getString(
                org.quantumbadger.redreader.R.string.pref_behaviour_gallery_swipe_length_key,
                "150"
            )!!.toInt()
        } catch (e: Throwable) {
            return 150
        }
    }

    fun pref_behaviour_comment_min(): Int? {
        val defaultValue = -4

        val value = getString(
            string.pref_behaviour_comment_min_key,
            defaultValue.toString()
        )

        if (value == null || value.trim { it <= ' ' }.isEmpty()) {
            return null
        }

        try {
            return value.toInt()
        } catch (e: Throwable) {
            return defaultValue
        }
    }

    fun pref_behaviour_post_tap_action(): PostTapAction {
        return PostTapAction.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_post_tap_action_key,
                    "link"
                )!!
            )
        )
    }

    fun pref_behaviour_post_title_opens_comments(): Boolean {
        return getBoolean(
            string.pref_behaviour_post_title_opens_comments_key,
            false
        )
    }

    fun pref_behaviour_imageview_mode(): ImageViewMode {
        return ImageViewMode.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_imageview_mode_key,
                    "internal_opengl"
                )!!
            )
        )
    }

    fun pref_behaviour_albumview_mode(): AlbumViewMode {
        return AlbumViewMode.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_albumview_mode_key,
                    "internal_list"
                )!!
            )
        )
    }

    fun pref_behaviour_gifview_mode(): GifViewMode {
        return GifViewMode.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_gifview_mode_key,
                    "internal_movie"
                )!!
            )
        )
    }

    fun pref_behaviour_videoview_mode(): VideoViewMode {
        return VideoViewMode.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_videoview_mode_key,
                    "internal_videoview"
                )!!
            )
        )
    }

    fun pref_behaviour_fling_post_left(): PostFlingAction {
        return PostFlingAction.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_fling_post_left_key,
                    "downvote"
                )!!
            )
        )
    }

    fun pref_behaviour_fling_post_right(): PostFlingAction {
        return PostFlingAction.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_fling_post_right_key,
                    "upvote"
                )!!
            )
        )
    }

    fun pref_behaviour_self_post_tap_actions(): SelfpostAction {
        return SelfpostAction.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_self_post_tap_actions_key,
                    "collapse"
                )!!
            )
        )
    }

    fun pref_behaviour_fling_comment_left(): CommentFlingAction {
        return CommentFlingAction.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_fling_comment_left_key,
                    "downvote"
                )!!
            )
        )
    }

    fun pref_behaviour_fling_comment_right(): CommentFlingAction {
        return CommentFlingAction.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_fling_comment_right_key,
                    "upvote"
                )!!
            )
        )
    }

    fun pref_behaviour_actions_comment_tap(): CommentAction {
        return CommentAction.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_actions_comment_tap_key,
                    "collapse"
                )!!
            )
        )
    }

    fun pref_behaviour_actions_comment_longclick(): CommentAction {
        return CommentAction.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_actions_comment_longclick_key,
                    "action_menu"
                )!!
            )
        )
    }

    fun pref_behaviour_sharing_share_text(): Boolean {
        return getBoolean(
            string.pref_behaviour_sharing_share_text_key,
            true
        )
    }

    fun pref_behaviour_sharing_include_desc(): Boolean {
        return getBoolean(
            string.pref_behaviour_sharing_include_desc_key,
            true
        )
    }

    fun pref_behaviour_sharing_dialog(): Boolean {
        return getBoolean(
            string.pref_behaviour_sharing_share_dialog_key,
            false
        )
    }

    fun pref_behaviour_sharing_dialog_data_get(): String? {
        return getString(
            string.pref_behaviour_sharing_share_dialog_data,
            ""
        )
    }

    fun pref_behaviour_sharing_dialog_data_set(
        context: Context,
        appNames: String?
    ) {
        sharedPrefs!!.edit()
            .putString(
                context.getString(string.pref_behaviour_sharing_share_dialog_data),
                appNames
            )
            .apply()
    }

    fun pref_behaviour_postsort(): PostSort {
        return PostSort.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_postsort_key,
                    "hot"
                )!!
            )
        )
    }

    fun pref_behaviour_user_postsort(): PostSort {
        return PostSort.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_user_postsort_key,
                    "new"
                )!!
            )
        )
    }

    fun pref_behaviour_multi_postsort(): PostSort {
        return PostSort.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_multi_postsort_key,
                    "hot"
                )!!
            )
        )
    }

    fun pref_behaviour_commentsort(): PostCommentSort {
        return PostCommentSort.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_commentsort_key,
                    "best"
                )!!
            )
        )
    }

    fun pref_behaviour_user_commentsort(): UserCommentSort {
        return UserCommentSort.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_user_commentsort_key,
                    "new"
                )!!
            )
        )
    }

    fun pref_behaviour_pinned_subredditsort(): PinnedSubredditSort {
        return PinnedSubredditSort.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_pinned_subredditsort_key,
                    "name"
                )!!
            )
        )
    }

    fun pref_behaviour_blocked_subredditsort(): BlockedSubredditSort {
        return BlockedSubredditSort.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_blocked_subredditsort_key,
                    "name"
                )!!
            )
        )
    }

    fun pref_behaviour_nsfw(): Boolean {
        return getBoolean(
            string.pref_behaviour_nsfw_key,
            false
        )
    }

    //Show Visited Posts? True hides them.
    // See strings.xml, prefs_behaviour.xml
    fun pref_behaviour_hide_read_posts(): Boolean {
        return getBoolean(
            string.pref_behaviour_hide_read_posts_key,
            false
        )
    }

    fun pref_behaviour_mark_posts_as_read(): Boolean {
        return getBoolean(
            string.pref_behaviour_mark_posts_as_read_key,
            true
        )
    }

    fun pref_behaviour_sharing_domain(): SharingDomain {
        return SharingDomain.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_sharing_domain_key,
                    "standard_reddit"
                )!!
            )
        )
    }

    fun pref_behaviour_share_permalink(): Boolean {
        return getBoolean(
            string.pref_behaviour_share_permalink_key,
            false
        )
    }

    fun pref_behaviour_post_count(): PostCount {
        return PostCount.valueOf(
            PrefsUtility.getString(
                string.pref_behaviour_postcount_key,
                "ALL"
            )!!
        )
    }

    fun pref_behaviour_screen_orientation(): ScreenOrientation {
        return ScreenOrientation.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_screenorientation_key,
                    org.quantumbadger.redreader.common.StringUtils.asciiLowercase(org.quantumbadger.redreader.common.PrefsUtility.ScreenOrientation.AUTO.name)
                )!!
            )
        )
    }

    fun pref_behaviour_save_location(): SaveLocation {
        return SaveLocation.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_save_location_key,
                    org.quantumbadger.redreader.common.StringUtils.asciiLowercase(SaveLocation.PROMPT_EVERY_TIME.name)
                )!!
            )
        )
    }

    fun behaviour_block_screenshots(): Boolean {
        return getBoolean(
            string.pref_behaviour_block_screenshots_key,
            false
        )
    }

    /**//////////////////////////// */ // pref_cache
    /**//////////////////////////// */ // pref_cache_location
    fun pref_cache_location(
        context: Context
    ): String? {
        var defaultCacheDir = context.getExternalCacheDir()
        if (defaultCacheDir == null) {
            defaultCacheDir = context.getCacheDir()
        }
        return getString(
            string.pref_cache_location_key,
            defaultCacheDir.getAbsolutePath()
        )
    }

    fun pref_cache_location(
        context: Context,
        path: String?
    ) {
        sharedPrefs!!.edit()
            .putString(context.getString(string.pref_cache_location_key), path)
            .apply()
    }

    fun pref_cache_rerequest_postlist_age(): TimeDuration {
        try {
            val hours = PrefsUtility.getString(
                org.quantumbadger.redreader.R.string.pref_cache_rerequest_postlist_age_key,
                "1"
            )!!.toInt()

            return hours(hours.toLong())
        } catch (e: Throwable) {
            return hours(1)
        }
    }

    // pref_cache_maxage
    fun <E : Any> createFileTypeMap(
        listings: E,
        thumbnails: E,
        images: E
    ): HashMap<Int, E> {
        val maxAgeMap = HashMap<Int, E>(10)

        maxAgeMap.put(Constants.FileType.POST_LIST, listings)
        maxAgeMap.put(Constants.FileType.COMMENT_LIST, listings)
        maxAgeMap.put(Constants.FileType.SUBREDDIT_LIST, listings)
        maxAgeMap.put(Constants.FileType.SUBREDDIT_ABOUT, listings)
        maxAgeMap.put(Constants.FileType.USER_ABOUT, listings)
        maxAgeMap.put(Constants.FileType.INBOX_LIST, listings)
        maxAgeMap.put(Constants.FileType.THUMBNAIL, thumbnails)
        maxAgeMap.put(Constants.FileType.IMAGE, images)
        maxAgeMap.put(Constants.FileType.IMAGE_INFO, images)
        maxAgeMap.put(Constants.FileType.CAPTCHA, images)
        maxAgeMap.put(Constants.FileType.INLINE_IMAGE_PREVIEW, images)

        return maxAgeMap
    }

    fun pref_cache_maxage(): HashMap<Int, TimeDuration> {
        val maxAgeListing = hours(
            PrefsUtility.getString(
                string.pref_cache_maxage_listing_key,
                "168"
            )!!.toLong()
        )

        val maxAgeThumb = hours(
            PrefsUtility.getString(
                string.pref_cache_maxage_thumb_key,
                "168"
            )!!.toLong()
        )

        val maxAgeImage = hours(
            PrefsUtility.getString(
                string.pref_cache_maxage_image_key,
                "72"
            )!!.toLong()
        )

        return createFileTypeMap(maxAgeListing, maxAgeThumb, maxAgeImage)
    }

    fun pref_cache_maxage_entry(): TimeDuration {
        return hours(
            PrefsUtility.getString(
                string.pref_cache_maxage_entry_key,
                "168"
            )!!.toLong()
        )
    }

    // pref_cache_precache_images
    fun cache_precache_images(): NeverAlwaysOrWifiOnly {
        return NeverAlwaysOrWifiOnly.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_cache_precache_images_list_key,
                    "wifionly"
                )!!
            )
        )
    }

    fun cache_precache_images_old(): NeverAlwaysOrWifiOnly {
        if (network_tor()) {
            return NeverAlwaysOrWifiOnly.NEVER
        }

        if (!getBoolean(
                string.pref_cache_precache_images_key,
                true
            )
        ) {
            return NeverAlwaysOrWifiOnly.NEVER
        } else if (getBoolean(
                string.pref_cache_precache_images_wifionly_key,
                true
            )
        ) {
            return NeverAlwaysOrWifiOnly.WIFIONLY
        } else {
            return NeverAlwaysOrWifiOnly.ALWAYS
        }
    }

    // pref_cache_precache_comments
    fun cache_precache_comments(): NeverAlwaysOrWifiOnly {
        return NeverAlwaysOrWifiOnly.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_cache_precache_comments_list_key,
                    "always"
                )!!
            )
        )
    }

    fun cache_precache_comments_old(): NeverAlwaysOrWifiOnly {
        if (!getBoolean(
                string.pref_cache_precache_comments_key,
                true
            )
        ) {
            return NeverAlwaysOrWifiOnly.NEVER
        } else if (getBoolean(
                string.pref_cache_precache_comments_wifionly_key,
                false
            )
        ) {
            return NeverAlwaysOrWifiOnly.WIFIONLY
        } else {
            return NeverAlwaysOrWifiOnly.ALWAYS
        }
    }

    /**//////////////////////////// */ // pref_network
    /**//////////////////////////// */
    fun network_tor(): Boolean {
        return getBoolean(
            string.pref_network_tor_key,
            false
        )
    }

    /**//////////////////////////// */ // pref_menus
    /**//////////////////////////// */
    fun pref_menus_post_context_items(): EnumSet<RedditPostActions.Action> {
        val strings = getStringSet(
            string.pref_menus_post_context_items_key,
            R.array.pref_menus_post_context_items_default
        )

        val result = EnumSet.noneOf<RedditPostActions.Action>(
            RedditPostActions.Action::class.java
        )
        for (s in strings!!) {
            result.add(RedditPostActions.Action.valueOf(StringUtils.asciiUppercase(s)))
        }

        return result
    }

    fun pref_menus_post_toolbar_items(): EnumSet<RedditPostActions.Action> {
        val strings = getStringSet(
            string.pref_menus_post_toolbar_items_key,
            R.array.pref_menus_post_toolbar_items_return
        )

        val result = EnumSet.noneOf<RedditPostActions.Action>(
            RedditPostActions.Action::class.java
        )
        for (s in strings!!) {
            result.add(RedditPostActions.Action.valueOf(StringUtils.asciiUppercase(s)))
        }

        return result
    }

    fun pref_menus_link_context_items(): EnumSet<LinkHandler.LinkAction> {
        val strings = getStringSet(
            string.pref_menus_link_context_items_key,
            R.array.pref_menus_link_context_items_return
        )

        val result = EnumSet.noneOf<LinkHandler.LinkAction>(LinkHandler.LinkAction::class.java)
        for (s in strings!!) {
            result.add(LinkHandler.LinkAction.valueOf(StringUtils.asciiUppercase(s)))
        }

        return result
    }

    fun pref_menus_subreddit_context_items(): EnumSet<SubredditAction> {
        val strings = getStringSet(
            string.pref_menus_subreddit_context_items_key,
            R.array.pref_menus_subreddit_context_items_return
        )

        val result = EnumSet.noneOf<SubredditAction>(
            SubredditAction::class.java
        )
        for (s in strings!!) {
            result.add(
                SubredditAction.valueOf(
                    StringUtils.asciiUppercase(
                        s
                    )
                )
            )
        }

        return result
    }

    fun pref_menus_mainmenu_useritems(): EnumSet<MainMenuUserItems> {
        val strings = getStringSet(
            string.pref_menus_mainmenu_useritems_key,
            R.array.pref_menus_mainmenu_useritems_items_default
        )

        val result = EnumSet.noneOf<MainMenuUserItems>(
            MainMenuUserItems::class.java
        )
        for (s in strings!!) {
            result.add(MainMenuUserItems.valueOf(StringUtils.asciiUppercase(s)))
        }

        return result
    }

    fun pref_menus_mainmenu_shortcutitems(): EnumSet<MainMenuShortcutItems> {
        val strings = getStringSet(
            string.pref_menus_mainmenu_shortcutitems_key,
            R.array.pref_menus_mainmenu_shortcutitems_items_default
        )

        val result = EnumSet.noneOf<MainMenuShortcutItems>(
            MainMenuShortcutItems::class.java
        )
        for (s in strings!!) {
            try {
                result.add(
                    MainMenuShortcutItems.valueOf(
                        StringUtils.asciiUppercase(s)
                    )
                )
            } catch (e: Exception) {
                Log.e("PrefsUtility", "Ignoring unknown constant " + s, e)
            }
        }

        return result
    }

    fun pref_menus_appbar_items(): EnumMap<AppbarItemsPref, Int> {
        val appbarItemsInfo: Array<AppbarItemInfo> = arrayOf(
            AppbarItemInfo(
                AppbarItemsPref.SORT,
                string.pref_menus_appbar_sort_key,
                MenuItem.SHOW_AS_ACTION_ALWAYS
            ),
            AppbarItemInfo(
                AppbarItemsPref.REFRESH,
                string.pref_menus_appbar_refresh_key,
                MenuItem.SHOW_AS_ACTION_ALWAYS
            ),
            AppbarItemInfo(
                AppbarItemsPref.PAST,
                string.pref_menus_appbar_past_key,
                MenuItem.SHOW_AS_ACTION_NEVER
            ),
            AppbarItemInfo(
                AppbarItemsPref.SUBMIT_POST,
                string.pref_menus_appbar_submit_post_key,
                MenuItem.SHOW_AS_ACTION_NEVER
            ),
            AppbarItemInfo(
                AppbarItemsPref.PIN,
                string.pref_menus_appbar_pin_key,
                MenuItem.SHOW_AS_ACTION_NEVER
            ),
            AppbarItemInfo(
                AppbarItemsPref.SUBSCRIBE,
                string.pref_menus_appbar_subscribe_key,
                MenuItem.SHOW_AS_ACTION_NEVER
            ),
            AppbarItemInfo(
                AppbarItemsPref.BLOCK,
                string.pref_menus_appbar_block_key,
                MenuItem.SHOW_AS_ACTION_NEVER
            ),
            AppbarItemInfo(
                AppbarItemsPref.SIDEBAR,
                string.pref_menus_appbar_sidebar_key,
                MenuItem.SHOW_AS_ACTION_NEVER
            ),
            AppbarItemInfo(
                AppbarItemsPref.ACCOUNTS,
                string.pref_menus_appbar_accounts_key,
                MenuItem.SHOW_AS_ACTION_NEVER
            ),
            AppbarItemInfo(
                AppbarItemsPref.THEME,
                string.pref_menus_appbar_theme_key,
                MenuItem.SHOW_AS_ACTION_NEVER
            ),
            AppbarItemInfo(
                AppbarItemsPref.SETTINGS,
                string.pref_menus_appbar_settings_key,
                MenuItem.SHOW_AS_ACTION_NEVER
            ),
            AppbarItemInfo(
                AppbarItemsPref.CLOSE_ALL,
                string.pref_menus_appbar_close_all_key,
                OptionsMenuUtility.DO_NOT_SHOW
            ),
            AppbarItemInfo(
                AppbarItemsPref.REPLY,
                string.pref_menus_appbar_reply_key,
                MenuItem.SHOW_AS_ACTION_NEVER
            ),
            AppbarItemInfo(
                AppbarItemsPref.SEARCH,
                string.pref_menus_appbar_search_key,
                MenuItem.SHOW_AS_ACTION_NEVER
            )
        )


        val appbarItemsPrefs = EnumMap<AppbarItemsPref, Int>(AppbarItemsPref::class.java)

        for (item in appbarItemsInfo) {
            try {
                appbarItemsPrefs.put(
                    item.itemPref, PrefsUtility.getString(
                        item.stringRes,
                        item.defaultValue.toString()
                    )!!.toInt()
                )
            } catch (e: NumberFormatException) {
                appbarItemsPrefs.put(item.itemPref, item.defaultValue)
            } catch (e: NullPointerException) {
                appbarItemsPrefs.put(item.itemPref, item.defaultValue)
            }
        }

        return appbarItemsPrefs
    }

    fun pref_menus_quick_account_switcher(): Boolean {
        return getBoolean(
            string.pref_menus_quick_account_switcher_key,
            true
        )
    }


    /**//////////////////////////// */ // pref_pinned_subreddits
    /**//////////////////////////// */
    fun pref_pinned_subreddits(): MutableList<SubredditCanonicalId> {
        return pref_subreddits_list(string.pref_pinned_subreddits_key)
    }

    fun pref_pinned_subreddits_add(
        context: Context,
        subreddit: SubredditCanonicalId
    ) {
        pref_subreddits_add(
            context,
            subreddit,
            string.pref_pinned_subreddits_key
        )

        quickToast(
            context, context.getApplicationContext().getString(
                string.pin_successful,
                subreddit.toString()
            )
        )
    }

    fun pref_pinned_subreddits_remove(
        context: Context,
        subreddit: SubredditCanonicalId
    ) {
        pref_subreddits_remove(
            context,
            subreddit,
            string.pref_pinned_subreddits_key
        )

        quickToast(
            context, context.getApplicationContext().getString(
                string.unpin_successful,
                subreddit.toString()
            )
        )
    }

    fun pref_pinned_subreddits_check(id: SubredditCanonicalId): Boolean {
        return pref_pinned_subreddits().contains(id)
    }

    /**//////////////////////////// */ // pref_blocked_subreddits
    /**//////////////////////////// */
    fun pref_blocked_subreddits(): MutableList<SubredditCanonicalId> {
        return pref_subreddits_list(string.pref_blocked_subreddits_key)
    }

    fun pref_blocked_subreddits_add(
        context: Context,
        subreddit: SubredditCanonicalId
    ) {
        pref_subreddits_add(
            context,
            subreddit,
            string.pref_blocked_subreddits_key
        )

        quickToast(context, string.block_done)
    }

    fun pref_blocked_subreddits_remove(
        context: Context,
        subreddit: SubredditCanonicalId
    ) {
        pref_subreddits_remove(
            context,
            subreddit,
            string.pref_blocked_subreddits_key
        )

        quickToast(context, string.unblock_done)
    }

    fun pref_blocked_subreddits_check(subreddit: SubredditCanonicalId): Boolean {
        return pref_blocked_subreddits().contains(subreddit)
    }

    /**//////////////////////////// */ // Shared pref_subreddits methods
    /**//////////////////////////// */
    private fun pref_subreddits_add(
        context: Context,
        subreddit: SubredditCanonicalId,
        prefId: Int
    ) {
        val value = getString(prefId, "")
        val list: ArrayList<String> = WritableHashSet.Companion.escapedStringToList(value)

        if (!list.contains(subreddit.toString())) {
            list.add(subreddit.toString())
            val result: String = WritableHashSet.Companion.listToEscapedString(list)
            sharedPrefs!!.edit().putString(context.getString(prefId), result).apply()
        }
    }

    private fun pref_subreddits_remove(
        context: Context,
        subreddit: SubredditCanonicalId,
        prefId: Int
    ) {
        val value = getString(prefId, "")
        val list: ArrayList<String> = WritableHashSet.Companion.escapedStringToList(value)

        val iterator = list.iterator()

        while (iterator.hasNext()) {
            val id = iterator.next()

            if (id == subreddit.toString()) {
                iterator.remove()
                break
            }
        }

        val resultStr: String = WritableHashSet.Companion.listToEscapedString(list)

        sharedPrefs!!.edit().putString(context.getString(prefId), resultStr).apply()
    }

    fun pref_subreddits_list(prefId: Int): MutableList<SubredditCanonicalId> {
        val value = getString(prefId, "")
        val list: ArrayList<String> = WritableHashSet.Companion.escapedStringToList(value)

        val result = ArrayList<SubredditCanonicalId>(list.size)

        try {
            for (str in list) {
                result.add(SubredditCanonicalId(str))
            }
        } catch (e: InvalidSubredditNameException) {
            throw RuntimeException(e)
        }

        return result
    }

    fun pref_accessibility_separate_body_text_lines(): Boolean {
        return getBoolean(
            string.pref_accessibility_separate_body_text_lines_key,
            true
        )
    }

    fun pref_accessibility_min_comment_height(): Int {
        try {
            return PrefsUtility.getString(
                org.quantumbadger.redreader.R.string.pref_accessibility_min_comment_height_key,
                "0"
            )!!.toInt()
        } catch (e: Throwable) {
            return 0
        }
    }

    fun pref_accessibility_say_comment_indent_level(): Boolean {
        return getBoolean(
            string.pref_accessibility_say_comment_indent_level_key,
            true
        )
    }

    fun behaviour_collapse_sticky_comments(): BehaviourCollapseStickyComments {
        return BehaviourCollapseStickyComments.valueOf(
            StringUtils.asciiUppercase(
                PrefsUtility.getString(
                    string.pref_behaviour_collapse_sticky_comments_key,
                    "ONLY_BOTS"
                )!!
            )
        )
    }

    fun pref_accessibility_concise_mode(): Boolean {
        return getBoolean(
            string.pref_accessibility_concise_mode_key,
            false
        )
    }

    fun pref_behaviour_keep_screen_awake(): Boolean {
        return getBoolean(
            string.pref_behaviour_keep_screen_awake_key,
            false
        )
    }

    fun pref_reddit_client_id_override(): String? {
        val value = getString(string.pref_reddit_client_id_override_key, null)

        if (value == null) {
            return null
        }

        val valueTrimmed = value.trim { it <= ' ' }

        if (valueTrimmed.isEmpty()) {
            return null
        }

        return valueTrimmed
    }

    fun pref_reddit_client_id_override_set(value: String?) {
        val trimmed = value?.trim { it <= ' ' }
        // putString with a null value removes the entry (framework semantics)
        sharedPrefs!!.edit()
            .putString(getPrefKey(string.pref_reddit_client_id_override_key), trimmed)
            .apply()
    }

    private const val REDDIT_USER_AGREEMENT_PREF = "accepted_reddit_user_agreement"
    @Suppress("PropertyName")
    private val REDDIT_USER_AGREEMENT_DECLINED = -1
    private const val REDDIT_USER_AGREEMENT_APRIL_2023 = 1
    @Suppress("PropertyName")
    private val REDDIT_USER_AGREEMENT_CURRENT = REDDIT_USER_AGREEMENT_APRIL_2023

    val isRedditUserAgreementAccepted: Boolean
        get() = (sharedPrefs!!.getInt(REDDIT_USER_AGREEMENT_PREF, 0)
                >= REDDIT_USER_AGREEMENT_CURRENT)

    val isRedditUserAgreementDeclined: Boolean
        get() = (sharedPrefs!!.getInt(REDDIT_USER_AGREEMENT_PREF, 0)
                == REDDIT_USER_AGREEMENT_DECLINED)

    @JvmStatic
    fun acceptRedditUserAgreement() {
        sharedPrefs!!
            .edit()
            .putInt(REDDIT_USER_AGREEMENT_PREF, REDDIT_USER_AGREEMENT_CURRENT)
            .apply()
    }

    fun declineRedditUserAgreement() {
        sharedPrefs!!
            .edit()
            .putInt(REDDIT_USER_AGREEMENT_PREF, REDDIT_USER_AGREEMENT_DECLINED)
            .apply()
    }

    /**//////////////////////////// */ // pref_appearance
    /**//////////////////////////// */ // pref_appearance_twopane
    enum class AppearanceTwopane {
        NEVER, AUTO, FORCE
    }

    enum class AppearanceNavbarColour {
        BLACK, WHITE, PRIMARY, PRIMARYDARK
    }

    enum class AppearanceStatusBarMode {
        ALWAYS_HIDE, HIDE_ON_MEDIA, NEVER_HIDE
    }

    enum class AppearancePostSubtitleItem {
        AUTHOR,
        FLAIR,
        SCORE,
        AGE,
        GOLD,
        CROSSPOST,
        SUBREDDIT,
        DOMAIN,
        STICKY,
        SPOILER,
        NSFW,
        UPVOTE_RATIO,
        COMMENTS
    }

    enum class AppearanceCommentHeaderItem {
        AUTHOR, FLAIR, SCORE, CONTROVERSIALITY, AGE, GOLD, SUBREDDIT
    }

    enum class CommentAgeMode {
        ABSOLUTE, RELATIVE_POST, RELATIVE_PARENT
    }

    enum class PostTapAction {
        LINK, COMMENTS, TITLE_COMMENTS
    }

    // pref_behaviour_imageview_mode
    enum class ImageViewMode(val downloadInApp: Boolean) {
        INTERNAL_OPENGL(true),
        INTERNAL_BROWSER(false),
        EXTERNAL_BROWSER(false)
    }

    // pref_behaviour_albumview_mode
    enum class AlbumViewMode {
        INTERNAL_LIST,
        INTERNAL_BROWSER,
        EXTERNAL_BROWSER
    }

    // pref_behaviour_gifview_mode
    enum class GifViewMode(val downloadInApp: Boolean) {
        INTERNAL_MOVIE(true),
        INTERNAL_LEGACY(true),
        INTERNAL_BROWSER(false),
        EXTERNAL_BROWSER(false)
    }

    // pref_behaviour_videoview_mode
    enum class VideoViewMode(val downloadInApp: Boolean) {
        INTERNAL_VIDEOVIEW(true),
        INTERNAL_BROWSER(false),
        EXTERNAL_BROWSER(false),
        EXTERNAL_APP_VLC(true)
    }

    // pref_behaviour_fling_post
    enum class PostFlingAction {
        UPVOTE,
        DOWNVOTE,
        SAVE,
        HIDE,
        COMMENTS,
        LINK,
        ACTION_MENU,
        BROWSER,
        BACK,
        REPORT,
        SAVE_IMAGE,
        GOTO_SUBREDDIT,
        SHARE,
        SHARE_COMMENTS,
        SHARE_IMAGE,
        COPY,
        USER_PROFILE,
        PROPERTIES,
        MARK_READ,
        DISABLED
    }

    enum class SelfpostAction {
        COLLAPSE, NOTHING
    }

    // pref_behaviour_fling_comment
    enum class CommentFlingAction {
        UPVOTE,
        DOWNVOTE,
        SAVE,
        REPORT,
        REPLY,
        CONTEXT,
        GO_TO_COMMENT,
        COMMENT_LINKS,
        SHARE,
        COPY_TEXT,
        COPY_URL,
        USER_PROFILE,
        COLLAPSE,
        ACTION_MENU,
        PROPERTIES,
        BACK,
        DISABLED
    }

    enum class CommentAction {
        COLLAPSE, ACTION_MENU, NOTHING
    }

    enum class PinnedSubredditSort {
        NAME, DATE
    }

    enum class BlockedSubredditSort {
        NAME, DATE
    }

    enum class SharingDomain(domain: String) {
        STANDARD_REDDIT("reddit.com"),
        SHORT_REDDIT("redd.it"),
        OLD_REDDIT("old.reddit.com"),
        NEW_REDDIT("new.reddit.com"),
        NP_REDDIT("np.reddit.com");

        val domain: String?

        init {
            this.domain = domain
        }
    }

    enum class PostCount {
        R25, R50, R100, ALL
    }

    enum class ScreenOrientation {
        AUTO, PORTRAIT, LANDSCAPE
    }

    enum class SaveLocation {
        PROMPT_EVERY_TIME, SYSTEM_DEFAULT
    }

    private class AppbarItemInfo(
        val itemPref: AppbarItemsPref,
        val stringRes: Int,
        val defaultValue: Int
    )

    enum class BehaviourCollapseStickyComments {
        ALWAYS, ONLY_BOTS, NEVER
    }
}
