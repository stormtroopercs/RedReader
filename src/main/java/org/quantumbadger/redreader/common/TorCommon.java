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

import org.quantumbadger.redreader.cache.CacheDownload
import org.quantumbadger.redreader.common.General.checkThisIsUIThread
import org.quantumbadger.redreader.http.HTTPBackend.Companion.backend
import java.util.concurrent.atomic.AtomicBoolean

object TorCommon {
    private val sIsTorEnabled = AtomicBoolean(false)

    fun updateTorStatus() {
        checkThisIsUIThread()

        val torEnabled = PrefsUtility.network_tor()
        val torChanged = (torEnabled != isTorEnabled)

        sIsTorEnabled.set(torEnabled)

        if (torChanged) {
            backend.recreateHttpBackend()
            CacheDownload.Companion.resetUserCredentialsOnNextRequest()
        }
    }

    val isTorEnabled: Boolean
        get() = sIsTorEnabled.get()
}
