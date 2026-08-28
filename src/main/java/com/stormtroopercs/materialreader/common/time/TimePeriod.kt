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

import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import java.util.Locale
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
data class TimePeriod(
	val start: TimestampUTC,
	val end: TimestampUTC
) {
	private data class PeriodSegment(
			val suffixSingular: (TimeStrings) -> String,
			val suffixPlural: (TimeStrings) -> String,
			val value: (DateTimePeriod) -> Int
	)

	companion object {
		private const val NBSP = '\u00A0'

		private val segments = listOf(
				PeriodSegment(
						suffixSingular = {s -> s.year},
						suffixPlural = {s -> s.years},
						value = {p -> p.years}
				),
				PeriodSegment(
						suffixSingular = {s -> s.month},
						suffixPlural = {s -> s.months},
						value = {p -> p.months}
				),
				PeriodSegment(
						suffixSingular = {s -> s.day},
						suffixPlural = {s -> s.days},
						value = {p -> p.days}
				),
				PeriodSegment(
						suffixSingular = {s -> s.hour},
						suffixPlural = {s -> s.hours},
						value = {p -> p.hours}
				),
				PeriodSegment(
						suffixSingular = {s -> s.min},
						suffixPlural = {s -> s.mins},
						value = {p -> p.minutes}
				),
				PeriodSegment(
						suffixSingular = {s -> s.sec},
						suffixPlural = {s -> s.secs},
						value = {p -> p.seconds}
				),
				PeriodSegment(
						suffixSingular = {s -> s.ms},
						suffixPlural = {s -> s.ms},
						value = {p -> p.nanoseconds / 1000000}
				)
		)
	}

	fun format(
		strings: TimeStrings,
		unitsToDisplay: Int
	): String {
		val period = start.value.periodUntil(end.value, TimeZone.currentSystemDefault())

		val separator = ", "

		val startIndex = segments
				.indexOfFirst { it.value(period) != 0 }
				.takeUnless { it < 0 } ?: (segments.size - 1)

		return segments.drop(startIndex).take(unitsToDisplay).joinToString(separator) {

			val value = it.value(period);

			String.format(
					Locale.getDefault(),
					"%d%c%s",
					value,
					NBSP,
					(if (value == 1) it.suffixSingular else it.suffixPlural)(strings))
		}
	}

	fun asDuration() = TimeDuration(end.value - start.value)
}
