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

package org.quantumbadger.redreader.common.time

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class TimeDuration(
	val value: Duration
) {
	fun toMs() = value.inWholeMilliseconds

	fun isLessThan(other: TimeDuration) = value < other.value
	fun isGreaterThan(other: TimeDuration) = value > other.value

	companion object {
		@JvmStatic
		fun ms(value: Long) = TimeDuration(value.milliseconds)

		@JvmStatic
		fun secs(value: Long) = TimeDuration(value.seconds)

		@JvmStatic
		fun minutes(value: Long) = TimeDuration(value.minutes)

		@JvmStatic
		fun hours(value: Long) = TimeDuration(value.hours)

		@JvmStatic
		fun days(value: Long) = TimeDuration(value.days)
	}
}
