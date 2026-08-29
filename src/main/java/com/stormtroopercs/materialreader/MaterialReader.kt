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

package com.stormtroopercs.materialreader

import android.app.Application
import android.content.Context
import android.os.Process
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.cache.CacheManager
import com.stormtroopercs.materialreader.common.AndroidCommon
import com.stormtroopercs.materialreader.common.Fonts
import com.stormtroopercs.materialreader.common.GlobalConfig
import com.stormtroopercs.materialreader.common.GlobalExceptionHandler
import com.stormtroopercs.materialreader.common.PrefsUtility
import com.stormtroopercs.materialreader.compose.prefs.ComposePrefsSingleton
import com.stormtroopercs.materialreader.io.RedditChangeDataIO
import com.stormtroopercs.materialreader.navigation.FeedPreferences
import com.stormtroopercs.materialreader.reddit.api.RedditOAuth
import com.stormtroopercs.materialreader.reddit.prepared.RedditChangeDataManager
import javax.inject.Inject

/**
 * Hilt-managed application class for MaterialReader.
 * Replaces manual singleton patterns with Hilt dependency injection.
 */
@HiltAndroidApp
class MaterialReader : Application() {

    companion object {
        const val TAG = "MaterialReader"

        @Volatile
        private var instance: MaterialReader? = null

        /**
         * Legacy accessor for converted pre-Hilt call sites that still use
         * the static `MaterialReader.getInstance(context)` form.
         */
        fun getInstance(context: Context): MaterialReader {
            return instance ?: synchronized(this) {
                instance ?: throw IllegalStateException(
                    "MaterialReader not initialized by Application.onCreate()"
                )
            }
        }

        @EntryPoint
        @InstallIn(SingletonComponent::class)
        interface CacheManagerEntryPoint {
            fun cacheManager(): CacheManager

            fun redditAccountManager(): RedditAccountManager
        }
    }

    lateinit var packageInfo: AndroidCommon.PackageInfo

    @Inject
    lateinit var cacheManager: CacheManager

    @Inject
    lateinit var redditAccountManager: RedditAccountManager

    override fun onCreate() {
        super.onCreate()
        instance = this

        Log.i(TAG, "Application created.")

        packageInfo = AndroidCommon.getPackageInfo(this)

        GlobalExceptionHandler.init(this)
        PrefsUtility.init(this)
        PrefsUtility.applyLanguageSetting()
        ComposePrefsSingleton.init(this)
        FeedPreferences.init(this)
        Fonts.onAppCreate(assets)

        // Initialise the Reddit OAuth client ID. RedditOAuth.init() reads the built-in
        // client ID (falls back to RedReader's public ID when no local reddit_auth.txt
        // is present) so the app can authenticate out of the box.
        RedditOAuth.init(this)

        // Note: Network initialization moved to Hilt modules
        // OkHttpClient and HTTPBackend are now provided via NetworkModule

        Log.i(TAG, "Config: " + GlobalConfig.appName + " (" + GlobalConfig.appBuildType + ")")

        // Wire CacheManager static instance for legacy call sites
        CacheManager.setInstance(cacheManager)
        RedditAccountManager.setInstance(redditAccountManager)

        object : Thread() {
            override fun run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                CacheManager.getInstance(this@MaterialReader).pruneTemp()
                CacheManager.getInstance(this@MaterialReader).pruneCache()
            }
        }.start()

        object : Thread() {
            override fun run() {
                RedditChangeDataIO.getInstance(this@MaterialReader).runInitialReadInThisThread()
                RedditChangeDataManager.pruneAllUsersDefaultMaxAge()
            }
        }.start()
    }
}
