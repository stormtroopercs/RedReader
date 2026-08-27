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

import java.io.IOException
import java.util.Locale

object HexUtils {
    @JvmStatic
    fun toHex(input: ByteArray): String {
        val result = StringBuilder(input.size * 2)

        for (b in input) {
            result.append(String.format(Locale.US, "%02X", b))
        }

        return result.toString()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun fromHex(digit: Char): Int {
        if (digit >= '0' && digit <= '9') {
            return digit.code - '0'.code
        }

        if (digit >= 'A' && digit <= 'F') {
            return digit.code + 10 - 'A'.code
        }

        if (digit >= 'a' && digit <= 'f') {
            return digit.code + 10 - 'a'.code
        }

        throw IOException("Invalid hex digit '" + digit + "'")
    }

    @JvmStatic
    @Throws(IOException::class)
    fun fromHex(input: String): ByteArray {
        val inputTrimmed = input.trim { it <= ' ' }

        if (inputTrimmed.length % 2 != 0) {
            throw IOException("Hex string length is not even: '" + inputTrimmed + "'")
        }

        val chars = inputTrimmed.toCharArray()

        val result = ByteArray(chars.size / 2)

        for (i in result.indices) {
            result[i] = ((fromHex(chars[i * 2]) shl 4) or fromHex(chars[i * 2 + 1])).toByte()
        }

        return result
    }
}
