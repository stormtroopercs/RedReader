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
package com.stormtroopercs.materialreader.common

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicReference

class CachedValue<E>(
    private val mFactory: GenericFactory<E, RuntimeException>,
    private val mMaxAgeMs: Long
) {
    private class CacheEntry<E>(val value: E, val lastUpdateMs: Long)

    private val mEntry = AtomicReference<CacheEntry<E>?>()

    fun get(): E {
        val timeNow = SystemClock.uptimeMillis()

        val entry = mEntry.get()

        if (entry != null && timeNow - entry.lastUpdateMs < mMaxAgeMs) {
            return entry.value
        }

        val newValue: E = mFactory.create()
        mEntry.set(CacheEntry<E>(newValue, timeNow))
        return newValue
    }
}
