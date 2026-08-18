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

import android.util.Log
import org.quantumbadger.redreader.BuildConfig
import org.quantumbadger.redreader.R
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import org.quantumbadger.redreader.activities.BugReportActivity
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.receivers.NewMessageChecker
import org.quantumbadger.redreader.receivers.announcements.AnnouncementDownloader

object PrefsBackup {
    private const val TAG = "PrefsBackup"

    @Suppress("PropertyName")
    private val MAGIC_HEADER = "RedReader preferences backup\r\n".toByteArray(General.CHARSET_UTF8)

    private const val FIELD_TYPE = "type"
    private const val FIELD_FILE_VERSION = "file_version"
    private const val FIELD_VERSION_CODE = "version_code"
    private const val FIELD_VERSION_NAME = "version_name"
    private const val FIELD_IS_ALPHA = "is_alpha"
    private const val FIELD_TIMESTAMP_UTC = "timestamp_utc"
    private const val FIELD_PREFS = "prefs"

    private const val FILE_TYPE = "redreader_prefs_backup"
    private const val FILE_VERSION = 1

    @Suppress("PropertyName")
    private val IGNORED_PREFS = HashSet<String?>()

    init {
        IGNORED_PREFS.add(AnnouncementDownloader.PREF_KEY_LAST_READ_ID)
        IGNORED_PREFS.add(AnnouncementDownloader.PREF_KEY_PAYLOAD_STORAGE_HEX)
        IGNORED_PREFS.add(NewMessageChecker.Companion.PREFS_SAVED_MESSAGE_ID)
        IGNORED_PREFS.add(NewMessageChecker.Companion.PREFS_SAVED_MESSAGE_TIMESTAMP)
        IGNORED_PREFS.add(FeatureFlagHandler.PREF_LAST_VERSION)
        IGNORED_PREFS.add(FeatureFlagHandler.PREF_FIRST_RUN_MESSAGE_SHOWN)
        IGNORED_PREFS.add(PrefsUtility.PREF_LANGUAGE_SETTING_MIGRATED)
    }

    fun backup(
        activity: AppCompatActivity,
        destination: BackupDestination,
        onSuccess: Runnable
    ) {
        val prefs = General.getSharedPrefs(activity)

        Thread(Runnable {
            val prefMap: HashMap<String?, *> = HashMap<String?, Any?>(prefs.allClone)
            for (ignoredPref in IGNORED_PREFS) {
                prefMap.remove(ignoredPref)
            }

            val map = HashMap<String?, Any?>()

            map.put(FIELD_TYPE, FILE_TYPE)
            map.put(FIELD_FILE_VERSION, FILE_VERSION)
            map.put(FIELD_VERSION_CODE, BuildConfig.VERSION_CODE)
            map.put(FIELD_VERSION_NAME, BuildConfig.VERSION_NAME)
            map.put(FIELD_IS_ALPHA, General.isAlpha)
            map.put(FIELD_TIMESTAMP_UTC, TimestampUTC.now().toUtcMs())
            map.put(FIELD_PREFS, prefMap)

            val bytes = ByteArrayOutputStream()

            try {
                val dos = DataOutputStream(bytes)
                dos.write(MAGIC_HEADER)
                SerializeUtils.serialize(dos, map)
                dos.flush()
            } catch (e: UnhandledTypeException) {
                BugReportActivity.handleGlobalError(activity, e)
                return@Runnable
            } catch (e: IOException) {
                BugReportActivity.handleGlobalError(activity, e)
                return@Runnable
            }

            try {
                destination.openOutputStream().use { outputStream ->
                    outputStream.write(bytes.toByteArray())
                    outputStream.flush()
                }
            } catch (e: IOException) {
                General.showResultDialog(
                    activity,
                    RRError(
                        activity.getString(R.string.error_unexpected_storage_title),
                        activity.getString(R.string.error_unexpected_storage_message),
                        true,
                        e
                    )
                )

                return@Runnable
            }
            onSuccess.run()
        }).start()
    }

    fun restore(
        activity: AppCompatActivity,
        source: BackupSource,
        onSuccess: Runnable
    ) {
        Thread(Runnable {
            try {
                DataInputStream(
                    BufferedInputStream(
                        source.openInputStream()
                    )
                ).use { dis ->
                    val magicHeader = ByteArray(MAGIC_HEADER.size)
                    dis.readFully(magicHeader)

                    if (!MAGIC_HEADER.contentEquals(magicHeader)) {
                        DialogUtils.showDialog(
                            activity,
                            R.string.restore_preferences_error_invalid_file_title,
                            R.string.restore_preferences_error_invalid_file_contents_message
                        )

                        return@Runnable
                    }

                    val root: MapReader

                    run {
                        val rootObj = SerializeUtils.deserialize(dis)
                        if (rootObj == null) {
                            throw IOException("Expecting Map, got null")
                        }

                        if (rootObj !is MutableMap<*, *>) {
                            throw IOException(
                                "Expecting Map, got "
                                        + rootObj.javaClass.getCanonicalName()
                            )
                        }
                        root = MapReader(rootObj as MutableMap<Any?, Any?>)
                    }

                    val type = root.getRequiredString(FIELD_TYPE)
                    val fileVersion = root.getRequiredInt(FIELD_FILE_VERSION)
                    val versionCode = root.getRequiredInt(FIELD_VERSION_CODE)
                    val versionName = root.getRequiredString(FIELD_VERSION_NAME)
                    val isAlpha = root.getRequiredBoolean(FIELD_IS_ALPHA)
                    val timestampUtc = root.getRequiredLong(FIELD_TIMESTAMP_UTC)
                    val restorePrefs = root.getRequiredMap(FIELD_PREFS)

                    Log.i(
                        TAG, ("Backup loaded: type="
                                + type
                                + ", fileVersion="
                                + fileVersion
                                + ", versionCode="
                                + versionCode
                                + ", versionName="
                                + versionName
                                + ", isAlpha="
                                + isAlpha
                                + ", timestampUtc="
                                + timestampUtc)
                    )

                    if (type != FILE_TYPE) {
                        DialogUtils.showDialog(
                            activity,
                            R.string.restore_preferences_error_invalid_file_title,
                            R.string.restore_preferences_error_invalid_file_contents_message
                        )

                        return@Runnable
                    }

                    if (fileVersion > FILE_VERSION) {
                        DialogUtils.showDialog(
                            activity,
                            R.string.restore_preferences_error_invalid_file_title,
                            R.string.restore_preferences_error_invalid_file_version_message
                        )

                        return@Runnable
                    }

                    val doRestore = Runnable {
                        Log.i(TAG, "Restoring " + restorePrefs.size + " value(s)")
                        General.getSharedPrefs(activity)
                            .performActionWithWriteLock(Consumer { sharedPrefs: SharedPreferences? ->
                                val keysToRemove: HashSet<String?> = HashSet<String?>(sharedPrefs.getAll().keys)
                                for (ignoredPref in IGNORED_PREFS) {
                                    keysToRemove.remove(ignoredPref)
                                }

                                Log.i(TAG, "Existing preference count: " + restorePrefs.size)

                                val editor: SharedPreferences.Editor = sharedPrefs.edit()

                                for (entry in restorePrefs.entries) {
                                    if (entry.key !is String) {
                                        Log.e(
                                            TAG, ("Skipping entry of type "
                                                    + entry.key!!.javaClass.getCanonicalName()
                                                    + " ("
                                                    + entry.key.toString()
                                                    + ")")
                                        )

                                        continue
                                    }

                                    val key = entry.key as String?
                                    val value: Any = entry.value!!

                                    if (IGNORED_PREFS.contains(key)) {
                                        Log.i(TAG, "Ignoring pref '" + key + "'")
                                        continue
                                    }

                                    Log.i(TAG, "Restoring '" + key + "'")

                                    keysToRemove.remove(key)

                                    if (value is String) {
                                        editor.putString(key, value)
                                    } else if (value is Int) {
                                        editor.putInt(key, value)
                                    } else if (value is MutableSet<*>) {
                                        editor.putStringSet(key, value as MutableSet<String>)
                                    } else if (value is Boolean) {
                                        editor.putBoolean(key, value)
                                    } else if (value is Long) {
                                        editor.putLong(key, value)
                                    } else if (value is Float) {
                                        editor.putFloat(key, value)
                                    } else {
                                        throw RuntimeException(
                                            "Unexpected type: "
                                                    + value.javaClass.getCanonicalName()
                                        )
                                    }
                                }

                                Log.i(TAG, "Removing " + keysToRemove.size + " old values")

                                for (key in keysToRemove) {
                                    Log.i(TAG, "Removing '" + key + "'")
                                    editor.remove(key)
                                }

                                Log.i(TAG, "All restored, committing...")

                                editor.apply()

                                Log.i(TAG, "Handling feature flag upgrades...")

                                FeatureFlagHandler.handleUpgrade(activity)
                                Log.i(TAG, "Restore complete")
                            })
                        onSuccess.run()
                    }
                    if (versionCode > BuildConfig.VERSION_CODE) {
                        DialogUtils.showDialogPositiveNegative(
                            activity,
                            activity.getString(
                                R.string.restore_preferences_error_version_warning_title
                            ),
                            activity.getString(
                                R.string.restore_preferences_error_version_warning_message
                            ),
                            R.string.button_continue_anyway,
                            R.string.button_cancel,
                            doRestore,
                            Runnable {})
                    } else {
                        doRestore.run()
                    }
                }
            } catch (e: IOException) {
                General.showResultDialog(
                    activity, RRError(
                        activity.getString(
                            R.string.restore_preferences_error_invalid_file_title
                        ),
                        activity.getString(
                            R.string.restore_preferences_error_invalid_file_contents_message
                        ),
                        true,
                        e
                    )
                )
            } catch (e: UnhandledTypeException) {
                General.showResultDialog(
                    activity, RRError(
                        activity.getString(
                            R.string.restore_preferences_error_invalid_file_title
                        ),
                        activity.getString(
                            R.string.restore_preferences_error_invalid_file_contents_message
                        ),
                        true,
                        e
                    )
                )
            }
        }).start()
    }

    interface BackupDestination {
        @Throws(IOException::class)
        fun openOutputStream(): OutputStream
    }

    interface BackupSource {
        @Throws(IOException::class)
        fun openInputStream(): InputStream
    }

    private class MapReader(private val mMap: MutableMap<*, *>) {
        @Throws(IOException::class)
        fun getRequired(key: Any): Any {
            val result: Any = mMap.get(key)!!

            if (result == null) {
                throw IOException("Missing field: '" + key + "'")
            }

            return result
        }

        @Throws(IOException::class)
        fun getRequiredString(key: Any): String {
            val result = getRequired(key)

            if (result !is String) {
                throw IOException(
                    ("Expecting string for key '"
                            + key
                            + "', got "
                            + result.javaClass.getCanonicalName())
                )
            }

            return result
        }

        @Throws(IOException::class)
        fun getRequiredMap(key: Any): MutableMap<*, *> {
            val result = getRequired(key)

            if (result !is MutableMap<*, *>) {
                throw IOException(
                    ("Expecting map for key '"
                            + key
                            + "', got "
                            + result.javaClass.getCanonicalName())
                )
            }

            return result
        }

        @Throws(IOException::class)
        fun getRequiredInt(key: Any): Int {
            val result = getRequired(key)

            if (result !is Int) {
                throw IOException(
                    ("Expecting integer for key '"
                            + key
                            + "', got "
                            + result.javaClass.getCanonicalName())
                )
            }

            return result
        }

        @Throws(IOException::class)
        fun getRequiredLong(key: Any): Long {
            val result = getRequired(key)

            if (result !is Long) {
                throw IOException(
                    ("Expecting long for key '"
                            + key
                            + "', got "
                            + result.javaClass.getCanonicalName())
                )
            }

            return result
        }

        @Throws(IOException::class)
        fun getRequiredBoolean(key: Any): Boolean {
            val result = getRequired(key)

            if (result !is Boolean) {
                throw IOException(
                    ("Expecting boolean for key '"
                            + key
                            + "', got "
                            + result.javaClass.getCanonicalName())
                )
            }

            return result
        }
    }
}
