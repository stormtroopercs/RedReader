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
package com.stormtroopercs.materialreader.common.datastream

import java.net.URLDecoder
import java.util.Base64

/**
 * The decoded payload of an [RFC 2397](https://datatracker.ietf.org/doc/html/rfc2397)
 * data URI: the media type from the URI's metadata (or null when the URI has
 * none) plus the underlying bytes.
 */
data class DataUriImage(
	val mimeType: String?,
	val bytes: ByteArray,
)

/**
 * Parses an RFC 2397 data URI (`data:[<mediatype>][;base64],<data>`) into its
 * decoded payload. Returns null when the string is not a data URI or its
 * payload cannot be decoded.
 *
 * Used to display user avatars: the modern Reddit API returns the account
 * picture as a base64 data URI in the user's `icon` field, which cannot go
 * through the URL-based [com.stormtroopercs.materialreader.compose.net.fetchImage]
 * pipeline.
 */
fun parseDataUri(dataUri: String): DataUriImage? {
	val prefix = "data:"
	if (!dataUri.startsWith(prefix, ignoreCase = true)) {
		return null
	}

	val comma = dataUri.indexOf(',')
	if (comma < 0) {
		return null
	}

	val metadata = dataUri.substring(prefix.length, comma).trim()
	val payload = dataUri.substring(comma + 1)

	val mimeType = metadata
		.split(';')
		.firstOrNull()
		?.trim()
		?.takeIf { it.isNotEmpty() }

	return if (metadata.contains("base64", ignoreCase = true)) {
		// MIME decoder: lenient about stray newlines in the payload.
		val bytes = try {
			Base64.getMimeDecoder().decode(payload)
		} catch (e: IllegalArgumentException) {
			return null
		}
		DataUriImage(mimeType, bytes)
	} else {
		try {
			DataUriImage(mimeType, URLDecoder.decode(payload, "UTF-8").toByteArray())
		} catch (e: IllegalArgumentException) {
			null
		}
	}
}
