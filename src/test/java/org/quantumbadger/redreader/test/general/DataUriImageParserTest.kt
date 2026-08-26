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
package org.quantumbadger.redreader.test.general

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.quantumbadger.redreader.common.datastream.parseDataUri

/**
 * Tests for [parseDataUri] (RFC 2397 data URIs) — used to decode the account
 * picture Reddit returns as a base64 data URI in the user's `icon` field.
 */
class DataUriImageParserTest {
    // A valid 1x1 transparent PNG.
    private val pngBytes = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
    )

    private fun b64(bytes: ByteArray): String = java.util.Base64.getEncoder().encodeToString(bytes)

    @Test
    fun `base64 data uri with media type decodes`() {
        val parsed = parseDataUri("data:image/png;base64," + b64(pngBytes))

        assertNotNull(parsed)
        assertEquals("image/png", parsed!!.mimeType)
        assertArrayEquals(pngBytes, parsed.bytes)
    }

    @Test
    fun `base64 data uri without media type decodes`() {
        val parsed = parseDataUri("data:;base64," + b64(pngBytes))

        assertNotNull(parsed)
        assertNull(parsed!!.mimeType)
        assertArrayEquals(pngBytes, parsed.bytes)
    }

    @Test
    fun `base64 payload tolerates embedded newlines`() {
        val encoded = b64(pngBytes)
        val chunked = (0 until encoded.length step 16).joinToString("\n") {
            encoded.substring(it, minOf(it + 16, encoded.length))
        }

        val parsed = parseDataUri("data:image/png;base64,$chunked")

        assertNotNull(parsed)
        assertArrayEquals(pngBytes, parsed!!.bytes)
    }

    @Test
    fun `case-insensitive data prefix and base64 marker`() {
        val parsed = parseDataUri("DATA:image/png;BASE64," + b64(pngBytes))

        assertNotNull(parsed)
        assertArrayEquals(pngBytes, parsed!!.bytes)
    }

    @Test
    fun `non base64 payload is url decoded`() {
        val parsed = parseDataUri("data:text/plain,hello%20world")

        assertNotNull(parsed)
        assertEquals("text/plain", parsed!!.mimeType)
        assertEquals("hello world", parsed.bytes.decodeToString())
    }

    @Test
    fun `malformed uris return null`() {
        assertNull(parseDataUri("https://example.com/avatar.png"))
        assertNull(parseDataUri("data:image/png"))                    // no comma
        assertNull(parseDataUri("data:image/png;base64,!!!not-base64!!!"))
    }

    @Test
    fun `real reddit icon shape parses`() {
        // Shape as returned by the modern Reddit API (account picture, 100x100).
        val jpegHeader = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10
        )
        val icon = "data:image/jpeg;base64," + b64(jpegHeader)

        val parsed = parseDataUri(icon)

        assertNotNull(parsed)
        assertEquals("image/jpeg", parsed!!.mimeType)
        assertArrayEquals(jpegHeader, parsed.bytes)
    }
}
