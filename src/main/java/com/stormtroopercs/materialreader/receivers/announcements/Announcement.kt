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
package com.stormtroopercs.materialreader.receivers.announcements

import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.common.time.TimeDuration
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.common.time.TimestampUTC.Companion.fromUtcMs
import com.stormtroopercs.materialreader.common.time.TimestampUTC.Companion.now
import java.io.IOException

class Announcement private constructor(
	@JvmField val id: String,
	@JvmField val title: String,
	@JvmField val message: String?,
	@JvmField val url: UriString,
	@JvmField val showUntil: TimestampUTC,
) {
	val isExpired: Boolean
		get() = showUntil.hasPassed()

	fun toPayload(): Payload {
		val result = Payload()

		result.setString(ENTRY_ID, id)
		result.setString(ENTRY_TITLE, title)

		if (message != null) {
			result.setString(ENTRY_MESSAGE, message)
		}

		result.setString(ENTRY_URL, url.value)
		result.setLong(ENTRY_SHOW_UNTIL, showUntil.toUtcMs())

		return result
	}

	companion object {
		private const val ENTRY_ID = "i"
		private const val ENTRY_TITLE = "t"
		private const val ENTRY_MESSAGE = "m"
		private const val ENTRY_URL = "u"
		private const val ENTRY_SHOW_UNTIL = "until"

		@JvmStatic
		fun create(
			id: String,
			title: String,
			message: String?,
			url: UriString,
			duration: TimeDuration,
		): Announcement = Announcement(
			id,
			title,
			message,
			url,
			now().add(duration),
		)

		@JvmStatic
		@Throws(IOException::class)
		fun fromPayload(payload: Payload): Announcement {
			var id = payload.getString(ENTRY_ID)
			val title = payload.getString(ENTRY_TITLE)
			val message = payload.getString(ENTRY_MESSAGE)
			val url = payload.getString(ENTRY_URL)
			val showUntil = payload.getLong(ENTRY_SHOW_UNTIL)

			if (title == null || url == null || showUntil == null) {
				throw IOException("Required entry missing")
			}

			if (id == null) {
				id = url
			}

			return Announcement(
				id,
				title,
				message,
				UriString(url),
				fromUtcMs(showUntil),
			)
		}
	}
}
