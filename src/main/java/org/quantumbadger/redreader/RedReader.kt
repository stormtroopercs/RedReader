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
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.Fonts
import org.quantumbadger.redreader.common.GlobalConfig
import org.quantumbadger.redreader.common.GlobalExceptionHandler
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.compose.prefs.ComposePrefsSingleton
import javax.inject.Inject

/**
 * Hilt-managed application class for RedReader.
 * Replaces manual singleton patterns with Hilt dependency injection.
 */
@HiltAndroidApp
class RedReader : Application() {

    companion object {
        const val TAG = "RedReader"

        @EntryPoint
        @InstallIn(SingletonComponent::class)
        interface CacheManagerEntryPoint {
            fun cacheManager(): CacheManager
        }
    }

    private lateinit var packageInfo: AndroidCommon.PackageInfo

    @Inject
    lateinit var cacheManager: CacheManager

    override fun onCreate() {
        super.onCreate()

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

        object : Thread() {
            override fun run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                CacheManager.getInstance(this@RedReader).pruneTemp()
                CacheManager.getInstance(this@RedReader).pruneCache()
            }
        }.start()
    }
}
