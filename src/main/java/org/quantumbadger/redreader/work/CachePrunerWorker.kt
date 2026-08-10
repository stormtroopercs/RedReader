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

package org.quantumbadger.redreader.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager

/**
 * WorkManager Worker that prunes the cache and Reddit change data.
 * Replaces the existing RegularCachePruner BroadcastReceiver + AlarmManager pattern.
 */
@HiltWorker
class CachePrunerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "CachePrunerWorker"

    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Pruning cache...")
            RedditChangeDataManager.pruneAllUsersDefaultMaxAge()
            CacheManager.getInstance(applicationContext).pruneCache()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error pruning cache", e)
            Result.retry()
        }
    }
}
