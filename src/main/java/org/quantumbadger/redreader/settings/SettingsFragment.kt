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
package org.quantumbadger.redreader.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import org.quantumbadger.redreader.BuildConfig
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.FileUtils
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.LinkHandler
import org.quantumbadger.redreader.common.time.TimeDuration
import java.io.File
import java.lang.Boolean
import java.util.EnumMap
import java.util.Locale
import java.util.Objects
import kotlin.Any
import kotlin.CharSequence
import kotlin.Exception
import kotlin.Int
import kotlin.IntArray
import kotlin.Long
import kotlin.RuntimeException
import kotlin.String
import kotlin.arrayOf
import kotlin.arrayOfNulls
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.MutableList
import kotlin.collections.indices
import kotlin.collections.plus
import kotlin.collections.toTypedArray
import kotlin.intArrayOf
import kotlin.plus
import kotlin.run
import kotlin.sequences.plus
import kotlin.text.endsWith
import kotlin.text.format
import kotlin.text.plus
import kotlin.text.substring

class SettingsFragment : PreferenceFragmentCompat() {
    @StringRes
    private var mTitle = 0

    override fun onResume() {
        super.onResume()

        val activity: FragmentActivity?=getActivity()

        if (activity != null) {
            activity.setTitle(mTitle)
        }
    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        val context: Context?=getActivity()

        val panel = requireArguments().getString("panel")
        val resource: Int

        try {
            resource = xml::class.java.getDeclaredField("prefs_" + panel).getInt(null)

            if ("root" == panel) {
                mTitle = R.string.options_settings
            } else {
                mTitle = string::class.java.getDeclaredField("prefs_category_" + panel)
                    .getInt(null)
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        // On Android 13+ the user may have changed the app's language in the
        // system settings, so sync the language preference before displaying it
        PrefsUtility.applyLanguageSetting()

        addPreferencesFromResource(resource)

        val listPrefsToUpdate = intArrayOf(
            R.string.pref_appearance_twopane_key,
            R.string.pref_behaviour_self_post_tap_actions_key,
            R.string.pref_behaviour_fling_post_left_key,
            R.string.pref_behaviour_fling_post_right_key,
            R.string.pref_behaviour_fling_comment_left_key,
            R.string.pref_behaviour_fling_comment_right_key,
            R.string.pref_appearance_theme_key,
            R.string.pref_appearance_navbar_color_key,
            R.string.pref_cache_maxage_listing_key,
            R.string.pref_cache_maxage_thumb_key,
            R.string.pref_cache_maxage_image_key,
            R.string.pref_cache_maxage_entry_key,
            R.string.pref_appearance_fontscale_global_key,
            R.string.pref_appearance_fontscale_posts_key,
            R.string.pref_appearance_fontscale_post_subtitles_key,
            R.string.pref_appearance_fontscale_post_header_titles_key,
            R.string.pref_appearance_fontscale_post_header_subtitles_key,
            R.string.pref_appearance_fontscale_comment_headers_key,
            R.string.pref_appearance_fontscale_bodytext_key,
            R.string.pref_appearance_fontscale_linkbuttons_key,
            R.string.pref_behaviour_actions_comment_tap_key,
            R.string.pref_behaviour_actions_comment_longclick_key,
            R.string.pref_behaviour_commentsort_key,
            R.string.pref_behaviour_user_commentsort_key,
            R.string.pref_behaviour_postsort_key,
            R.string.pref_behaviour_user_postsort_key,
            R.string.pref_behaviour_multi_postsort_key,
            R.string.pref_appearance_langforce_key,
            R.string.pref_behaviour_postcount_key,
            R.string.pref_behaviour_bezel_toolbar_swipezone_key,
            R.string.pref_behaviour_imageview_mode_key,
            R.string.pref_behaviour_albumview_mode_key,
            R.string.pref_behaviour_gifview_mode_key,
            R.string.pref_behaviour_videoview_mode_key,
            R.string.pref_behaviour_screenorientation_key,
            R.string.pref_behaviour_gallery_swipe_length_key,
            R.string.pref_behaviour_pinned_subredditsort_key,
            R.string.pref_behaviour_blocked_subredditsort_key,
            R.string.pref_behaviour_save_location_key,
            R.string.pref_cache_rerequest_postlist_age_key,
            R.string.pref_appearance_thumbnails_show_list_key,
            R.string.pref_cache_precache_images_list_key,
            R.string.pref_cache_precache_comments_list_key,
            R.string.pref_menus_appbar_sort_key,
            R.string.pref_menus_appbar_refresh_key,
            R.string.pref_menus_appbar_past_key,
            R.string.pref_menus_appbar_submit_post_key,
            R.string.pref_menus_appbar_pin_key,
            R.string.pref_menus_appbar_block_key,
            R.string.pref_menus_appbar_subscribe_key,
            R.string.pref_menus_appbar_sidebar_key,
            R.string.pref_menus_appbar_accounts_key,
            R.string.pref_menus_appbar_theme_key,
            R.string.pref_menus_appbar_settings_key,
            R.string.pref_menus_appbar_close_all_key,
            R.string.pref_menus_appbar_reply_key,
            R.string.pref_menus_appbar_search_key,
            R.string.pref_appearance_post_age_units_key,
            R.string.pref_appearance_post_header_age_units_key,
            R.string.pref_appearance_comment_age_units_key,
            R.string.pref_appearance_comment_age_mode_key,
            R.string.pref_appearance_inbox_age_units_key,
            R.string.pref_images_thumbnail_size_key,
            R.string.pref_images_inline_image_previews_key,
            R.string.pref_images_high_res_thumbnails_key,
            R.string.pref_accessibility_min_comment_height_key,
            R.string.pref_appearance_android_status_key,
            R.string.pref_behaviour_collapse_sticky_comments_key,
            R.string.pref_behaviour_sharing_domain_key,
            R.string.pref_behaviour_post_tap_action_key
        )

        val editTextPrefsToUpdate = intArrayOf(
            R.string.pref_behaviour_comment_min_key,
            R.string.pref_reddit_client_id_override_key
        )

        for (pref in listPrefsToUpdate) {
            val listPreference: ListPreference?
            ListPreference > findPreference<Preference?>(getString(pref))

            if (listPreference == null) {
                continue
            }

            run {
                val index = listPreference.findIndexOfValue(listPreference.getValue())
                if (index < 0) {
                    continue
                }
                listPreference.setSummary(listPreference.getEntries()[index])
            }

            listPreference.setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                val index = listPreference.findIndexOfValue(newValue as String?)
                listPreference.setSummary(listPreference.getEntries()[index])
                true
            })
        }

        for (pref in editTextPrefsToUpdate) {
            val editTextPreference: EditTextPreference?
            EditTextPreference > findPreference<Preference?>(getString(pref))

            if (editTextPreference == null) {
                continue
            }

            editTextPreference.setSummary(editTextPreference.getText())

            editTextPreference.setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                if (newValue != null) {
                    editTextPreference.setSummary(newValue.toString())
                } else {
                    editTextPreference.setSummary("(null)")
                }
                true
            })
        }

        run {
            val notifPref: CheckBoxPreference?
            CheckBoxPreference > findPreference<Preference?>(getString(R.string.pref_behaviour_notifications_key))
            if (notifPref != null) {
                notifPref.setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                    val activity: Activity?=getActivity()
                    if (activity is BaseActivity) {
                        // Delay this because the preference hasn't taken effect yet
                        AndroidCommon.UI_THREAD_HANDLER.postDelayed(Runnable {
                            AndroidCommon.promptForNotificationPermission(
                                activity as BaseActivity,
                                Runnable {
                                    notifPref.setChecked(false)
                                }
                            )
                        }, 300)
                    }
                    true
                })
            }
        }


        val testNotificationPref: Preference?
        Preference > findPreference<Preference?>(getString(R.string.pref_developer_test_notification_key))
        val versionPref: Preference?
        Preference > findPreference<Preference?>(getString(R.string.pref_about_version_key))
        val changelogPref: Preference?
        Preference > findPreference<Preference?>(getString(R.string.pref_about_changelog_key))
        val torPref: Preference?
        Preference > findPreference<Preference?>(getString(R.string.pref_network_tor_key))
        val licensePref: Preference?
        Preference > findPreference<Preference?>(getString(R.string.pref_about_license_key))
        val githubPref: Preference?
        Preference > findPreference<Preference?>(getString(R.string.pref_about_github_key))
        val backupPreferencesPref: Preference?
        Preference > findPreference<Preference?>(getString(R.string.pref_item_backup_preferences_key))
        val restorePreferencesPref: Preference?
        Preference > findPreference<Preference?>(getString(R.string.pref_item_restore_preferences_key))

        if (testNotificationPref != null) {
            testNotificationPref.setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference? ->
                Log.i("SettingsFragment", "Showing test notification")
                NewMessageChecker.Companion.createNotification(
                    "Test notification title",
                    "Test notification message",
                    context
                )
                true
            })
        }

        if (versionPref != null) {
            versionPref.setSummary(
                RedReader.getInstance(context).packageInfo.versionName
            )
        }

        if (changelogPref != null) {
            changelogPref.setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference? ->
                val intent: Intent = Intent(context, ChangelogActivity::class.java)
                context!!.startActivity(intent)
                true
            })
        }

        if (licensePref != null) {
            licensePref.setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference? ->
                HtmlViewActivity.Companion.showAsset(context, "license.html")
                true
            })
        }

        if (githubPref != null) {
            githubPref.setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference? ->
                val activity: BaseActivity?=getActivity() as BaseActivity?
                if (activity == null) {
                    return@setOnPreferenceClickListener true
                }

                LinkHandler.onLinkClicked(
                    activity,
                    UriString(getString(R.string.pref_about_github_url))
                )
                true
            })
        }

        if (torPref != null) {
            torPref.setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->

                // Run this after the preference has actually changed
                AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                    TorCommon.updateTorStatus()
                    if (TorCommon.isTorEnabled()
                        != (Boolean.TRUE == newValue)
                    ) {
                        throw RuntimeException(
                            "Tor not correctly enabled after preference change"
                        )
                    }
                })
                true
            })
        }

        if (backupPreferencesPref != null) {
            backupPreferencesPref.setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference? ->
                val activity: BaseActivity?=getActivity() as BaseActivity?
                if (activity == null) {
                    return@setOnPreferenceClickListener true
                }

                val utc: TimestampUTC = TimestampUTC.now()
                val filename: String=(utc.formatFilenameSafe()
                        + ".rr_prefs_backup")

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .setType("application/vnd.redreader.prefsbackup")
                    .putExtra(Intent.EXTRA_TITLE, filename)
                    .addCategory(Intent.CATEGORY_OPENABLE)

                try {
                    activity.startActivityForResultWithCallback(
                        intent,
                        BaseActivity.ActivityResultCallback { resultCode: Int, data: Intent? ->
                            if (data == null || data.getData() == null) {
                                return@startActivityForResultWithCallback
                            }
                            val contentResolver: ContentResolver=activity.getContentResolver()
                            PrefsBackup.backup(
                                activity,
                                BackupDestination { contentResolver.openOutputStream(data.getData()) },
                                Runnable {
                                    General.quickToast(
                                        context!!,
                                        R.string.backup_preferences_success
                                    )
                                })
                        })
                } catch (e: ActivityNotFoundException) {
                    DialogUtils.showDialog(
                        activity,
                        R.string.error_no_file_manager_title,
                        R.string.error_no_file_manager_message
                    )
                }
                true
            })
        }

        if (restorePreferencesPref != null) {
            restorePreferencesPref.setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference? ->
                val activity = getActivity() as SettingsActivity?
                if (activity == null) {
                    return@setOnPreferenceClickListener true
                }

                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .setType("*/*")
                    .addCategory(Intent.CATEGORY_OPENABLE)

                try {
                    activity.startActivityForResultWithCallback(
                        intent,
                        BaseActivity.ActivityResultCallback { resultCode: Int, data: Intent? ->
                            if (data == null || data.getData() == null) {
                                return@startActivityForResultWithCallback
                            }
                            val contentResolver: ContentResolver=activity.getContentResolver()
                            PrefsBackup.restore(
                                activity,
                                BackupSource { contentResolver.openInputStream(data.getData()) },
                                Runnable {
                                    General.quickToast(
                                        context!!,
                                        R.string.restore_preferences_success
                                    )
                                })
                        })
                } catch (e: ActivityNotFoundException) {
                    DialogUtils.showDialog(
                        activity,
                        R.string.error_no_file_manager_title,
                        R.string.error_no_file_manager_message
                    )
                }
                true
            })
        }

        val cacheLocationPref: Preference?
        Preference > findPreference<Preference?>(getString(R.string.pref_cache_location_key))
        if (cacheLocationPref != null) {
            cacheLocationPref.setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference? ->
                showChooseStorageLocationDialog()
                true
            })
            updateStorageLocationText(PrefsUtility.pref_cache_location(context))
        }

        //This disables the "Show NSFW thumbnails" setting when Show thumbnails is set to Never
        //Based off https://stackoverflow.com/a/4137963
        run {
            val thumbnailPref: ListPreference?
            ListPreference > findPreference<Preference?>(
                getString(R.string.pref_appearance_thumbnails_show_list_key)
            )
            val thumbnailNsfwPref: Preference
            Preference > findPreference<Preference?>(getString(R.string.pref_appearance_thumbnails_nsfw_show_key))
            val thumbnailSpoilerPref: Preference
            Preference > findPreference<Preference?>(getString(R.string.pref_appearance_thumbnails_spoiler_show_key))
            if (thumbnailPref != null) {
                thumbnailPref.setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                    val index = thumbnailPref.findIndexOfValue(newValue as String?)
                    thumbnailPref.setSummary(thumbnailPref.getEntries()[index])
                    thumbnailNsfwPref.setEnabled(newValue != "never")
                    thumbnailSpoilerPref.setEnabled(newValue != "never")
                    true
                })
            }
        }

        run {
            val inlineImagesPref: ListPreference?
            ListPreference > findPreference<Preference?>(
                getString(R.string.pref_images_inline_image_previews_key)
            )
            val inlineImagesNsfwPref: Preference
            Preference > findPreference<Preference?>(getString(R.string.pref_images_inline_image_previews_nsfw_key))
            val inlineImagesSpoilerPref: Preference
            Preference > findPreference<Preference?>(getString(R.string.pref_images_inline_image_previews_spoiler_key))
            if (inlineImagesPref != null) {
                inlineImagesPref.setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                    val index = inlineImagesPref.findIndexOfValue(newValue as String?)
                    inlineImagesPref.setSummary(inlineImagesPref.getEntries()[index])
                    inlineImagesNsfwPref.setEnabled(newValue != "never")
                    inlineImagesSpoilerPref.setEnabled(newValue != "never")
                    true
                })
            }
        }

        run {
            val sharingDomainPref: ListPreference?
            ListPreference > findPreference<Preference?>(
                getString(R.string.pref_behaviour_sharing_domain_key)
            )
            val shareAsPermalinkPref: Preference
            Preference > findPreference<Preference?>(
                getString(R.string.pref_behaviour_share_permalink_key)
            )
            if (sharingDomainPref != null) {
                sharingDomainPref.setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                    val index = sharingDomainPref.findIndexOfValue(newValue as String?)
                    sharingDomainPref.setSummary(sharingDomainPref.getEntries()[index])
                    shareAsPermalinkPref.setEnabled(newValue != "short_reddit")
                    true
                })
            }
        }

        run {
            val hideOnScrollPref: CheckBoxPreference?
            CheckBoxPreference > findPreference<Preference?>(
                getString(
                    R.string.pref_appearance_hide_toolbar_on_scroll_key
                )
            )

            val toolbarAtBottomPref: Preference?
            Preference > findPreference<Preference?>(
                getString(
                    R.string.pref_appearance_bottom_toolbar_key
                )
            )

            val twoPanePref: Preference?
            Preference > findPreference<Preference?>(
                getString(
                    R.string.pref_appearance_twopane_key
                )
            )
            if (hideOnScrollPref != null || twoPanePref != null || toolbarAtBottomPref != null) {
                if (!(hideOnScrollPref != null && twoPanePref != null && toolbarAtBottomPref != null)) {
                    BugReportActivity.handleGlobalError(
                        context, RuntimeException(
                            "Not all preferences present"
                        )
                    )
                    return
                }

                val update = Runnable {
                    if (General.isTablet(context!!)) {
                        hideOnScrollPref.setEnabled(false)
                        hideOnScrollPref.setSummary(
                            R.string.pref_appearance_not_possible_in_tablet_mode
                        )
                        toolbarAtBottomPref.setEnabled(true)
                    } else {
                        hideOnScrollPref.setEnabled(true)
                        hideOnScrollPref.setSummary(null)
                        toolbarAtBottomPref.setEnabled(!hideOnScrollPref.isChecked())
                    }
                }

                update.run()

                for (pref in arrayOf<Preference>(twoPanePref, hideOnScrollPref)) {
                    val existingListener = pref.getOnPreferenceChangeListener()

                    pref.setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->

                        // Post this after the preference has been updated
                        AndroidCommon.UI_THREAD_HANDLER.post(update)
                        if (existingListener != null) {
                            return@setOnPreferenceChangeListener existingListener.onPreferenceChange(
                                preference!!,
                                newValue
                            )
                        } else {
                            return@setOnPreferenceChangeListener true
                        }
                    })
                }
            }
        }

        val cacheClearPref: Preference?
        Preference > findPreference<Preference?>(getString(R.string.pref_cache_clear_key))

        if (cacheClearPref != null) {
            cacheClearPref.setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference? ->
                showCacheClearDialog()
                true
            })
        }

        val categoryPrefix = "prefs_category_"

        for (i in 0..<getPreferenceScreen().getPreferenceCount()) {
            val pref = getPreferenceScreen().getPreference(i)

            val key = pref.getKey()

            if (key != null && key.startsWith(categoryPrefix)) {
                pref.setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference? ->
                    (getActivity() as SettingsActivity).onPanelSelected(
                        key.replace(categoryPrefix, "")
                    )
                    true
                })
            }
        }
    }

    //Based on https://stackoverflow.com/a/55724743
    override fun setPreferenceScreen(preferenceScreen: PreferenceScreen?) {
        if (preferenceScreen != null) {
            configureAllPrefsAppearance(preferenceScreen)
        }

        super.setPreferenceScreen(preferenceScreen)
    }

    private fun configureAllPrefsAppearance(prefGroup: PreferenceGroup) {
        for (i in 0..<prefGroup.getPreferenceCount()) {
            val pref = prefGroup.getPreference(i)

            pref.setSingleLineTitle(false)
            pref.setIconSpaceReserved(false)

            if (pref is PreferenceGroup) {
                configureAllPrefsAppearance(pref)
            }
        }
    }

    private fun showChooseStorageLocationDialog() {
        val context: Context?=getActivity()

        val currentStorage: String=PrefsUtility.pref_cache_location(context)

        val checkPaths: MutableList<File?> = CacheManager.Companion.getCacheDirs(context)

        val folders: MutableList<File?> = ArrayList<File?>(checkPaths.size)

        val choices: MutableList<CharSequence?> = ArrayList<CharSequence?>(checkPaths.size)
        var selectedIndex = 0

        for (i in checkPaths.indices) {
            val dir = checkPaths.get(i)
            if (dir == null || !dir.exists() || !dir.canRead() || !dir.canWrite()) {
                continue
            }
            folders.add(dir)
            if (currentStorage == dir.getAbsolutePath()) {
                selectedIndex = i
            }

            var path = dir.getAbsolutePath()
            val bytes = FileUtils.getFreeSpaceAvailable(path)
            val freeSpace = General.addUnits(bytes)
            if (!path.endsWith("/")) {
                path += "/"
            }
            val appCachePostfix: String = BuildConfig.APPLICATION_ID + "/cache/"
            if (path.endsWith("Android/data/" + appCachePostfix)) {
                path = path.substring(0, path.length - appCachePostfix.length - 14)
            } else if (path.endsWith(appCachePostfix)) {
                path = path.substring(0, path.length - appCachePostfix.length - 1)
            }
            choices.add(
                Html.fromHtml(
                    "<small>" + path +
                            " [" + freeSpace + "]</small>"
                )
            )
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.pref_cache_location_title)
            .setSingleChoiceItems(
                choices.toTypedArray<CharSequence?>(),
                selectedIndex,
                DialogInterface.OnClickListener { dialog: DialogInterface?, i: Int ->
                    dialog.dismiss()
                    val path = folders.get(i)!!.getAbsolutePath()
                    PrefsUtility.pref_cache_location(context, path)
                    updateStorageLocationText(path)
                })
            .setNegativeButton(
                R.string.dialog_close,
                DialogInterface.OnClickListener { dialog: DialogInterface?, i: Int -> dialog.dismiss() })
            .create()
            .show()
    }

    private fun updateStorageLocationText(path: String?) {
        Preference > findPreference<Preference?>(getString(R.string.pref_cache_location_key)).setSummary(
            path
        )
    }

    private enum class CacheType(
        val plainStringRes: Int,
        val dataUsageStringRes: Int,
        val fileTypes: IntArray
    ) {
        LISTINGS(
            R.string.cache_clear_dialog_listings,
            R.string.cache_clear_dialog_listings_data,
            intArrayOf(
                Constants.FileType.POST_LIST,
                Constants.FileType.COMMENT_LIST,
                Constants.FileType.SUBREDDIT_LIST,
                Constants.FileType.SUBREDDIT_ABOUT,
                Constants.FileType.USER_ABOUT,
                Constants.FileType.INBOX_LIST
            )
        ),
        THUMBNAILS(
            R.string.cache_clear_dialog_thumbnails,
            R.string.cache_clear_dialog_thumbnails_data,
            intArrayOf(Constants.FileType.THUMBNAIL)
        ),
        IMAGES(
            R.string.cache_clear_dialog_images,
            R.string.cache_clear_dialog_images_data,
            intArrayOf(
                Constants.FileType.IMAGE,
                Constants.FileType.IMAGE_INFO,
                Constants.FileType.CAPTCHA,
                Constants.FileType.INLINE_IMAGE_PREVIEW
            )
        ),
        FLAGS(
            R.string.cache_clear_dialog_flags,
            R.string.cache_clear_dialog_flags,
            intArrayOf()
        )
    }

    private fun showCacheClearDialog() {
        val context: Context?=getActivity()
        val cacheManager: CacheManager = CacheManager.Companion.getInstance(context)

        val cachesToClear = EnumMap<CacheType?, kotlin.Boolean?>(CacheType::class.java)
        val cacheItemStrings = arrayOfNulls<String>(CacheType.entries.size)

        for (cacheType in CacheType.entries) {
            cachesToClear.put(cacheType, false)
            cacheItemStrings[cacheType.ordinal] = getString(cacheType.plainStringRes)
        }

        val cacheDialog: AlertDialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.pref_cache_clear_title)
            .setMultiChoiceItems(
                cacheItemStrings, null,
                OnMultiChoiceClickListener { dialog: DialogInterface?, which: Int, isChecked: kotlin.Boolean ->  //Subtract 1, since progressBar gets put at position 0.
                    cachesToClear.put(CacheType.entries[which - 1], isChecked)
                })
            .setPositiveButton(
                R.string.dialog_clear,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int ->
                    object : Thread() {
                        override fun run() {
                            cacheManager.pruneCache(
                                cachesToClear.get(CacheType.LISTINGS),
                                cachesToClear.get(CacheType.THUMBNAILS),
                                cachesToClear.get(CacheType.IMAGES)
                            )

                            if (Objects.requireNonNull<kotlin.Boolean?>(cachesToClear.get(CacheType.FLAGS))) {
                                RedditChangeDataManager.Companion.pruneAllUsersWhereOlderThan(
                                    TimeDuration.ms(0)
                                )
                            }
                        }
                    }.start()
                })
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()

        val progressBar = ProgressBar(
            context,
            null,
            android.R.attr.progressBarStyleHorizontal
        )
        progressBar.setIndeterminate(true)
        progressBar.setContentDescription(getString(R.string.cache_clear_dialog_loading))

        cacheDialog.getListView().addHeaderView(progressBar, null, false)
        cacheDialog.show()

        object : Thread() {
            override fun run() {
                val fileTypeDataUsages: HashMap<Int?, Long?> = cacheManager.getCacheDataUsages()

                for (cacheType in CacheType.entries) {
                    if (cacheType.fileTypes.size >= 1) {
                        /*If the CacheType has files managed by the CacheManager,
						add up the data usage from each fileType the cacheType encompasses
						and format it into its data-usage string.*/
                        var cacheTypeDataUsage: Long = 0

                        for (fileTypeDataUsage
                        in fileTypeDataUsages.entries) {
                            for (fileType in cacheType.fileTypes) {
                                if (fileType == fileTypeDataUsage.key) {
                                    cacheTypeDataUsage += fileTypeDataUsage.value!!
                                }
                            }
                        }

                        val finalCacheTypeDataUsage = cacheTypeDataUsage
                        AndroidCommon.runOnUiThread(Runnable {
                            val cacheItemView = cacheDialog.getListView()
                                .getChildAt(cacheType.ordinal + 1) as TextView
                            cacheItemView.setText(
                                String.format(
                                    Locale.US,
                                    context!!.getApplicationContext().getString(
                                        cacheType.dataUsageStringRes
                                    ),
                                    General.addUnits(finalCacheTypeDataUsage)
                                )
                            )
                        })
                    }
                }

                AndroidCommon.runOnUiThread(Runnable {
                    //It might look better to just make this invisible once it's done, but that
                    //causes strange issues when using directional navigation with TalkBack.
                    progressBar.setIndeterminate(false)
                    progressBar.setProgress(progressBar.getMax())
                    progressBar.setContentDescription(
                        context!!.getApplicationContext().getString(
                            R.string.cache_clear_dialog_loaded
                        )
                    )
                })
            }
        }.start()
    }
}
