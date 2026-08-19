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
package org.quantumbadger.redreader.reddit.prepared.markdown

import java.util.LinkedList

class CharArrSubstring internal constructor(val arr: CharArray, val start: Int, val length: Int) {
    fun rejoin(toAppend: CharArrSubstring): CharArrSubstring {
        if (toAppend.start - 1 != start + length) {
            throw RuntimeException(
                "Internal error: attempt to join non-consecutive substrings"
            )
        }

        return CharArrSubstring(arr, start, length + 1 + toAppend.length)
    }

    fun countSpacesAtStart(): Int {
        for (i in 0..<length) {
            if (arr[start + i] != ' ') {
                return i
            }
        }
        return length
    }

    fun countSpacesAtEnd(): Int {
        for (i in 0..<length) {
            if (arr[start + length - 1 - i] != ' ') {
                return i
            }
        }
        return length
    }

    fun charAt(index: Int): Char {
        return arr[start + index]
    }

    fun countPrefixLengthIgnoringSpaces(c: Char): Int {
        for (i in 0..<length) {
            if (arr[start + i] != ' ' && arr[start + i] != c) {
                return i
            }
        }
        return length
    }

    fun countPrefixLevelIgnoringSpaces(c: Char): Int {
        var level = 0
        for (i in 0..<length) {
            if (arr[start + i] != ' ' && arr[start + i] != c) {
                return level
            } else if (arr[start + i] == c) {
                level++ // TODO tidy up
            }
        }
        return length
    }

    fun left(chars: Int): CharArrSubstring {
        return CharArrSubstring(arr, start, chars)
    }

    fun substring(start: Int): CharArrSubstring {
        return CharArrSubstring(arr, this.start + start, length - start)
    }

    fun substring(start: Int, len: Int): CharArrSubstring {
        return CharArrSubstring(arr, this.start + start, len)
    }

    fun readInteger(start: Int): CharArrSubstring {
        for (i in start..<length) {
            val c = arr[this.start + i]
            if (c < '0' || c > '9') {
                return CharArrSubstring(arr, this.start + start, i - start)
            }
        }
        return CharArrSubstring(arr, this.start + start, length - start)
    }

    override fun toString(): String {
        return String(arr, start, length)
    }

    fun isRepeatingChar(c: Char, start: Int, len: Int): Boolean {
        for (i in 0..<len) {
            if (arr[i + start + this.start] != c) {
                return false
            }
        }
        return true
    }

    fun equalAt(position: Int, needle: String): Boolean {
        if (length < position + needle.length) {
            return false
        }

        for (i in 0..<needle.length) {
            if (needle.get(i) != arr[start + position + i]) {
                return false
            }
        }

        return true
    }

    fun replaceUnicodeSpaces() {
        for (i in 0..<length) {
            if (MarkdownTokenizer.isUnicodeWhitespace(arr[start + i].code)) {
                arr[start + i] = ' '
            }
        }
    }

    companion object {
        @JvmStatic
        fun generate(src: CharArray): CharArrSubstring {
            return CharArrSubstring(src, 0, src.size)
        }

        fun generateFromLines(src: CharArray): Array<CharArrSubstring> {
            var curPos = 0

            val result = LinkedList<CharArrSubstring>()

            var nextLinebreak: Int

            while ((indexOfLinebreak(src, curPos).also { nextLinebreak = it }) != -1) {
                result.add(CharArrSubstring(src, curPos, nextLinebreak - curPos))
                curPos = nextLinebreak + 1
            }

            result.add(CharArrSubstring(src, curPos, src.size - curPos))

            return result.toTypedArray()
        }

        private fun indexOfLinebreak(raw: CharArray, startPos: Int): Int {
            for (i in startPos..<raw.size) {
                if (raw[i] == '\n') {
                    return i
                }
            }
            return -1
        }
    }
}
