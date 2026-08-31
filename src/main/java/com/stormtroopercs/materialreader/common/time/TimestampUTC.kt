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

package com.stormtroopercs.materialreader.common.time

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class TimestampUTC(
	val value: Instant,
) : Comparable<TimestampUTC> {
	companion object {

		@JvmStatic
		fun now() = TimestampUTC(Clock.System.now())

		@JvmStatic
		fun fromUtcMs(value: Long) = TimestampUTC(Instant.fromEpochMilliseconds(value))

		@JvmStatic
		fun fromUtcSecs(value: Long) = TimestampUTC(Instant.fromEpochSeconds(value, 0))

		@JvmField
		val ZERO = TimestampUTC(Instant.fromEpochMilliseconds(0))

		@JvmStatic
		fun oldest(a: TimestampUTC, b: TimestampUTC) = if (a.value < b.value) a else b
	}

	fun toUtcMs() = value.toEpochMilliseconds()

	fun toUtcSecs() = value.epochSeconds

	fun elapsed() = TimePeriod(this, now()).asDuration()

	fun elapsedPeriod() = TimePeriod(this, now())

	fun elapsedPeriodSince(start: TimestampUTC) = TimePeriod(start, this)

	fun format() = localDateTime().run {
		String.format(
			Locale.US,
			"%d-%02d-%02d %02d:%02d",
			year,
			month.number,
			day,
			hour,
			minute,
		)
	}

	fun formatFilenameSafe() = localDateTime().run {
		String.format(
			Locale.US,
			"%d_%02d_%02d__%02d_%02d_%02d",
			year,
			month.number,
			day,
			hour,
			minute,
			second,
		)
	}

	private fun localDateTime() = value.toLocalDateTime(TimeZone.currentSystemDefault())

	fun isLessThan(other: TimestampUTC) = value < other.value
	fun isGreaterThan(other: TimestampUTC) = value > other.value

	fun add(duration: TimeDuration) = TimestampUTC(value + duration.value)
	fun subtract(duration: TimeDuration) = TimestampUTC(value - duration.value)

	override fun compareTo(other: TimestampUTC) = value.compareTo(other.value)

	fun hasPassed() = now().isGreaterThan(this)
}
