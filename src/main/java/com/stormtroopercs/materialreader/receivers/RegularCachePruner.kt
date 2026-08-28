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
package com.stormtroopercs.materialreader.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.stormtroopercs.materialreader.cache.CacheManager
import com.stormtroopercs.materialreader.reddit.prepared.RedditChangeDataManager

class RegularCachePruner : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.i("RegularCachePruner", "Pruning cache...")

        object : Thread() {
            override fun run() {
                RedditChangeDataManager.Companion.pruneAllUsersDefaultMaxAge()
                CacheManager.Companion.getInstance(context).pruneCache()
            }
        }.start()
    }
}
