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
import android.content.SharedPreferences
import android.util.Log
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

class SharedPrefsWrapper internal constructor(private val mPrefs: SharedPreferences) {
    inner class Editor @SuppressLint("CommitPrefEdits") private constructor() {
        private val mEditor: SharedPreferences.Editor

        init {
            mEditor = mPrefs.edit()
        }

        fun putString(
            key: String,
            value: String?
        ): Editor {
            mEditor.putString(key, value)
            return this
        }

        fun putFloat(
            key: String,
            value: Float?
        ): Editor {
            mEditor.putString(key, if (value == null) null else value.toString())
            return this
        }

        fun putInt(
            key: String,
            value: Int
        ): Editor {
            mEditor.putInt(key, value)
            return this
        }

        fun putLong(
            key: String,
            value: Long
        ): Editor {
            mEditor.putLong(key, value)
            return this
        }

        fun putBoolean(
            key: String,
            value: Boolean
        ): Editor {
            mEditor.putBoolean(key, value)
            return this
        }

        fun putStringSet(
            key: String,
            value: MutableSet<String?>?
        ): Editor {
            mEditor.putStringSet(key, value)
            return this
        }

        fun apply() {
            // Take read lock as we aren't doing an atomic restore
            Locker(mRestoreLock.readLock()).use { ignored ->
                mEditor.apply()
            }
        }
    }

    fun interface OnSharedPreferenceChangeListener {
        fun onSharedPreferenceChanged(
            sharedPreferences: SharedPrefsWrapper,
            key: String
        )
    }

    private val mRestoreLock: ReadWriteLock = ReentrantReadWriteLock()

    private val mListenerWrappers =         HashMap<OnSharedPreferenceChangeListener?, SharedPreferences.OnSharedPreferenceChangeListener?>()

    fun registerOnSharedPreferenceChangeListener(
        listener: OnSharedPreferenceChangeListener
    ) {
        val spListener =             SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences?, key: String? ->
                listener.onSharedPreferenceChanged(
                    this,
                    key!!
                )
            }

        mPrefs.registerOnSharedPreferenceChangeListener(spListener)

        mListenerWrappers.put(listener, spListener)
    }

    fun unregisterOnSharedPreferenceChangeListener(
        listener: OnSharedPreferenceChangeListener?
    ) {
        val spListener = mListenerWrappers.remove(listener)

        if (spListener != null) {
            mPrefs.unregisterOnSharedPreferenceChangeListener(spListener)
        }
    }

    fun contains(key: String): Boolean {
        Locker(mRestoreLock.readLock()).use { ignored ->
            return mPrefs.contains(key)
        }
    }

    val allClone: MutableMap<String?, *>
        get() {
            Locker(mRestoreLock.readLock()).use { ignored ->
                return HashMap<String?, Any?>(mPrefs.getAll())
            }
        }

    fun getString(
        key: String,
        defValue: String?
    ): String? {
        Locker(mRestoreLock.readLock()).use { ignored ->
            return mPrefs.getString(key, defValue)
        }
    }

    fun getInt(
        key: String,
        defValue: Int
    ): Int {
        try {
            Locker(mRestoreLock.readLock()).use { ignored ->
                return mPrefs.getInt(key, defValue)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get pref", e)
            return defValue
        }
    }

    fun getLong(
        key: String,
        defValue: Long
    ): Long {
        try {
            Locker(mRestoreLock.readLock()).use { ignored ->
                return mPrefs.getLong(key, defValue)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get pref", e)
            return defValue
        }
    }

    fun getStringSet(
        key: String,
        defValues: MutableSet<String?>?
    ): MutableSet<String?>? {
        Locker(mRestoreLock.readLock()).use { ignored ->
            return mPrefs.getStringSet(key, defValues)
        }
    }

    fun getBoolean(
        key: String,
        defValue: Boolean
    ): Boolean {
        Locker(mRestoreLock.readLock()).use { ignored ->
            return mPrefs.getBoolean(key, defValue)
        }
    }

    fun edit(): Editor {
        return SharedPrefsWrapper.Editor()
    }

    fun performActionWithWriteLock(action: Consumer<SharedPreferences?>) {
        Log.i(TAG, "Acquiring write lock")

        Locker(mRestoreLock.writeLock()).use { ignored ->
            Log.i(TAG, "Write lock acquired, performing action...")
            action.consume(mPrefs)
        }
    }

    companion object {
        private const val TAG = "SharedPrefsWrapper"
    }
}
