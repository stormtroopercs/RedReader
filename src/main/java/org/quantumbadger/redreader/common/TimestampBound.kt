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
package org.quantumbadger.redreader.common

import org.quantumbadger.redreader.common.time.TimeDuration
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.common.time.TimestampUTC.Companion.now

abstract class TimestampBound {
    abstract fun verifyTimestamp(timestamp : TimestampUTC): Boolean

    class MoreRecentThanBound(private val minTimestamp: TimestampUTC) : TimestampBound() {
        override fun verifyTimestamp(timestamp: TimestampUTC): Boolean {
            return timestamp.isGreaterThan(minTimestamp)
        }
    }

    companion object {
        val ANY: TimestampBound = object : TimestampBound() {
            override fun verifyTimestamp(timestamp : TimestampUTC): Boolean {
                return true
            }
        }
        @Suppress("PropertyName")
    val NONE: TimestampBound = object : TimestampBound() {
            override fun verifyTimestamp(timestamp : TimestampUTC): Boolean {
                return false
            }
        }

        fun notOlderThan(age: TimeDuration): MoreRecentThanBound {
            return MoreRecentThanBound(now().subtract(age))
        }
    }
}
