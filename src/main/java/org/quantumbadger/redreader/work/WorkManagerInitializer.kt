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

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkManager configuration class for RedReader.
 * Manages WorkerFactory initialization and WorkManager setup.
 */
@Singleton
class WorkManagerInitializer @Inject constructor(
    @ApplicationContext private val application: Application,
    private val hiltWorkerFactory: HiltWorkerFactory
) : Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() {
        return Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        }

    companion object {
        const val NEW_MESSAGE_CHECKER_WORK_NAME = "new_message_checker"
        const val CACHE_PRUNER_WORK_NAME = "cache_pruner"
    }

    /**
     * Enqueues the new message checker work.
     * Runs periodically every 30 minutes (matching the existing AlarmManager behavior).
     */
    fun enqueueNewMessageChecker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<NewMessageWorker>(
            30, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(application).enqueueUniquePeriodicWork(
            NEW_MESSAGE_CHECKER_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Enqueues the cache pruner work.
     * Runs periodically every hour (matching the existing AlarmManager behavior).
     */
    fun enqueueCachePruner() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<CachePrunerWorker>(
            60, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(application).enqueueUniquePeriodicWork(
            CACHE_PRUNER_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Enqueues all periodic background work.
     */
    fun enqueueAllPeriodicWork() {
        enqueueNewMessageChecker()
        enqueueCachePruner()
    }
}
