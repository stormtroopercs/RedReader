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

object StringUtils {
    @JvmStatic
    fun removePrefix(
        input: String,
        prefix: String
    ): Optional<String?> {
        if (input.startsWith(prefix)) {
            return Optional.Companion.of<String?>(input.substring(prefix.length))
        } else {
            return Optional.Companion.empty<String?>()
        }
    }

    @JvmStatic
    fun asciiUppercase(input: String): String {
        val chars = input.toCharArray()

        for (i in chars.indices) {
            if (chars[i] >= 'a' && chars[i] <= 'z') {
                chars[i] -= 'a'.code
                chars[i] += 'A'.code
            }
        }

        return String(chars)
    }

    @JvmStatic
    fun asciiLowercase(input: String): String {
        val chars = input.toCharArray()

        for (i in chars.indices) {
            if (chars[i] >= 'A' && chars[i] <= 'Z') {
                chars[i] -= 'A'.code
                chars[i] += 'a'.code
            }
        }

        return String(chars)
    }

    fun join(
        elements: MutableCollection<*>,
        separator: CharSequence
    ): String {
        val result = StringBuilder()

        var first = true

        for (element in elements) {
            if (!first) {
                result.append(separator)
            }

            result.append(element.toString())
            first = false
        }

        return result.toString()
    }

    @JvmStatic
    fun isEmpty(value: CharSequence?): Boolean {
        return value == null || value.length == 0
    }

    fun fromUTF8(bytes: ByteArray): String {
        return String(bytes, General.CHARSET_UTF8)
    }
}
