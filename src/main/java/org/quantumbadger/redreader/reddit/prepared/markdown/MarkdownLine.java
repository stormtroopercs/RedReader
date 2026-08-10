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

import org.quantumbadger.redreader.reddit.prepared.markdown.MarkdownParser.MarkdownParagraphType

class MarkdownLine internal constructor(
    val src: CharArrSubstring,
    val type: MarkdownParagraphType?,
    val spacesAtStart: Int,
    val spacesAtEnd: Int,
    val prefixLength: Int,
    val level: Int,
    val number: Int
) {
    fun rejoin(toAppend: MarkdownLine): MarkdownLine {
        src.arr[src.start + src.length] = ' '
        return MarkdownLine(
            src.rejoin(toAppend.src),
            type,
            spacesAtStart,
            toAppend.spacesAtEnd,
            prefixLength,
            level,
            number
        )
    }

    fun tokenize(parent: MarkdownParagraph?): MarkdownParagraph {
        val cleanedSrc =
            if (prefixLength == 0) src else src.substring(prefixLength)

        if (type != MarkdownParagraphType.CODE
            && type != MarkdownParagraphType.HLINE
        ) {
            if (this.isPlainText) {
                return MarkdownParagraph(
                    cleanedSrc,
                    parent,
                    type,
                    null,
                    level,
                    number
                )
            } else {
                val tokens = MarkdownTokenizer.tokenize(cleanedSrc)
                return MarkdownParagraph(
                    cleanedSrc,
                    parent,
                    type,
                    tokens.substringAsArray(0),
                    level,
                    number
                )
            }
        } else {
            return MarkdownParagraph(cleanedSrc, parent, type, null, level, number)
        }
    }

    private val isPlainText: Boolean
        get() {
            for (i in prefixLength..<src.length) {
                when (src.arr[i + src.start]) {
                    '*', '_', '^', '`', '\\', '[', '~', '#', '&' -> return false

                    '/' -> if (src.equalAt(i + 1, "u/") || src.equalAt(i + 1, "r/")) {
                        return false
                    }

                    'h' -> if (src.equalAt(i + 1, "ttp://") || src.equalAt(i + 1, "ttps://")) {
                        return false
                    }

                    'w' -> if (src.equalAt(i + 1, "ww.")) {
                        return false
                    }

                    'r', 'u' -> if (src.length > i + 1 && src.arr[src.start + i + 1] == '/') {
                        return false
                    }

                    else -> {}
                }
            }

            return true
        }

    companion object {
        fun generate(src: CharArrSubstring): MarkdownLine {
            val spacesAtStart = src.countSpacesAtStart()
            val spacesAtEnd = src.countSpacesAtEnd()

            if (spacesAtStart == src.length) {
                // New paragraph
                return MarkdownLine(
                    null,
                    MarkdownParagraphType.EMPTY,
                    0,
                    0,
                    0,
                    0,
                    0
                )
            }

            if (spacesAtStart >= 4) {
                return MarkdownLine(
                    src,
                    MarkdownParagraphType.CODE,
                    spacesAtStart,
                    spacesAtEnd,
                    4,
                    0,
                    0
                )
            }

            val firstNonSpaceChar = src.charAt(spacesAtStart)

            when (firstNonSpaceChar) {
                '>' -> {
                    val level = src.countPrefixLevelIgnoringSpaces('>')
                    val prefixLen = src.countPrefixLengthIgnoringSpaces('>')

                    return MarkdownLine(
                        src,
                        MarkdownParagraphType.QUOTE,
                        spacesAtStart,
                        spacesAtEnd,
                        prefixLen,
                        level,
                        0
                    )
                }

                '-', '*' -> {
                    if (src.length > spacesAtStart + 1
                        && src.charAt(spacesAtStart + 1) == ' '
                    ) {
                        return MarkdownLine(
                            src,
                            MarkdownParagraphType.BULLET,
                            spacesAtStart,
                            spacesAtEnd,
                            spacesAtStart + 2,
                            if (spacesAtStart == 0) 0 else 1,
                            0
                        )
                    } else if (src.length >= 3 && src.isRepeatingChar(
                            '*',
                            spacesAtStart,
                            src.length - spacesAtEnd
                        )
                    ) {
                        return MarkdownLine(
                            src,
                            MarkdownParagraphType.HLINE,
                            0,
                            0,
                            0,
                            0,
                            0
                        )
                    } else {
                        return MarkdownLine(
                            src,
                            MarkdownParagraphType.TEXT,
                            spacesAtStart,
                            spacesAtEnd,
                            spacesAtStart,
                            0,
                            0
                        )
                    }
                }

                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                    val num = src.readInteger(spacesAtStart)

                    if (src.length > spacesAtStart + num.length + 2 && src.charAt(spacesAtStart + num.length) == '.' && src.charAt(
                            spacesAtStart + num.length + 1
                        ) == ' '
                    ) {
                        return MarkdownLine(
                            src,
                            MarkdownParagraphType.NUMBERED,
                            spacesAtStart,
                            spacesAtEnd,
                            spacesAtStart + num.length + 2,
                            if (spacesAtStart == 0) 0 else 1,
                            num.toString().toInt()
                        )
                    } else {
                        return MarkdownLine(
                            src,
                            MarkdownParagraphType.TEXT,
                            spacesAtStart,
                            spacesAtEnd,
                            spacesAtStart,
                            0,
                            0
                        )
                    }
                }

                '#' ->                // TODO prefix and suffix length
                    return MarkdownLine(
                        src,
                        MarkdownParagraphType.HEADER,
                        spacesAtStart,
                        spacesAtEnd,
                        src.countPrefixLengthIgnoringSpaces('#'),
                        0,
                        0
                    )

                else -> return MarkdownLine(
                    src,
                    MarkdownParagraphType.TEXT,
                    spacesAtStart,
                    spacesAtEnd,
                    spacesAtStart,
                    0,
                    0
                )
            }
        }
    }
}
