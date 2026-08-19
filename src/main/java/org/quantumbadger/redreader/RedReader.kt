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

package org.quantumbadger.redreader

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
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.Fonts
import org.quantumbadger.redreader.common.GlobalConfig
import org.quantumbadger.redreader.common.GlobalExceptionHandler
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.compose.prefs.ComposePrefsSingleton
import org.quantumbadger.redreader.io.RedditChangeDataIO
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager
import javax.inject.Inject

/**
 * Hilt-managed application class for RedReader.
 * Replaces manual singleton patterns with Hilt dependency injection.
 */
@HiltAndroidApp
class RedReader : Application() {

    companion object {
        const val TAG = "RedReader"

        @Volatile
        private var instance: RedReader? = null

        /**
         * Legacy accessor for converted pre-Hilt call sites that still use
         * the static `RedReader.getInstance(context)` form.
         */
        fun getInstance(context: Context): RedReader {
            return instance ?: synchronized(this) {
                instance ?: throw IllegalStateException(
                    "RedReader not initialized by Application.onCreate()"
                )
            }
        }

        @EntryPoint
        @InstallIn(SingletonComponent::class)
        interface CacheManagerEntryPoint {
            fun cacheManager(): CacheManager
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
        Fonts.onAppCreate(assets)

        // Note: Network initialization moved to Hilt modules
        // OkHttpClient and HTTPBackend are now provided via NetworkModule

        Log.i(TAG, "Config: " + GlobalConfig.appName + " (" + GlobalConfig.appBuildType + ")")

        // Wire CacheManager static instance for legacy call sites
        CacheManager.setInstance(cacheManager)
        RedditAccountManager.setInstance(redditAccountManager)

        object : Thread() {
            override fun run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                CacheManager.getInstance(this@RedReader).pruneTemp()
                CacheManager.getInstance(this@RedReader).pruneCache()
            }
        }.start()

        object : Thread() {
            override fun run() {
                RedditChangeDataIO.getInstance(this@RedReader).runInitialReadInThisThread()
                RedditChangeDataManager.pruneAllUsersDefaultMaxAge()
            }
        }.start()
    }
}
