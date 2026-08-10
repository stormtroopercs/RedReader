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

import org.apache.commons.text.StringEscapeUtils
import kotlin.math.min

object MarkdownTokenizer {
    // TODO support double graves
    @JvmField
    val TOKEN_UNDERSCORE: Int = -1
    val TOKEN_UNDERSCORE_DOUBLE: Int = -2
    @JvmField
    val TOKEN_ASTERISK: Int = -3
    @JvmField
    val TOKEN_ASTERISK_DOUBLE: Int = -4
    val TOKEN_TILDE_DOUBLE: Int = -5
    @JvmField
    val TOKEN_CARET: Int = -6
    val TOKEN_GRAVE: Int = -7
    @JvmField
    val TOKEN_BRACKET_SQUARE_OPEN: Int = -8
    @JvmField
    val TOKEN_BRACKET_SQUARE_CLOSE: Int = -9
    @JvmField
    val TOKEN_PAREN_OPEN: Int = -10
    @JvmField
    val TOKEN_PAREN_CLOSE: Int = -11
    val TOKEN_UNICODE_OPEN: Int = -12
    val TOKEN_UNICODE_CLOSE: Int = -13

    private val reverseLookup: Array<CharArray> = arrayOfNulls<CharArray>(20)

    private val linkPrefixes = arrayOf<CharArray?>(
        "http://".toCharArray(),
        "https://".toCharArray(),
        "www.".toCharArray()
    )

    private val linkPrefixes_reddit = arrayOf<CharArray?>(
        "/r/".toCharArray(),
        "r/".toCharArray(),
        "/u/".toCharArray(),
        "u/".toCharArray(),
        "/user/".toCharArray()
    )

    private val unicodeWhitespace = HashSet<Int?>()

    init {
        reverseLookup[20 + TOKEN_UNDERSCORE] = charArrayOf('_')
        reverseLookup[20 + TOKEN_UNDERSCORE_DOUBLE] = charArrayOf('_', '_')
        reverseLookup[20 + TOKEN_ASTERISK] = charArrayOf('*')
        reverseLookup[20 + TOKEN_ASTERISK_DOUBLE] = charArrayOf('*', '*')
        reverseLookup[20 + TOKEN_TILDE_DOUBLE] = charArrayOf('~', '~')
        reverseLookup[20 + TOKEN_CARET] = charArrayOf('^')
        reverseLookup[20 + TOKEN_GRAVE] = charArrayOf('`')
        reverseLookup[20 + TOKEN_BRACKET_SQUARE_OPEN] = charArrayOf('[')
        reverseLookup[20 + TOKEN_BRACKET_SQUARE_CLOSE] = charArrayOf(']')
        reverseLookup[20 + TOKEN_PAREN_OPEN] = charArrayOf('(')
        reverseLookup[20 + TOKEN_PAREN_CLOSE] = charArrayOf(')')
        reverseLookup[20 + TOKEN_UNICODE_OPEN] = charArrayOf('&')
        reverseLookup[20 + TOKEN_UNICODE_CLOSE] = charArrayOf(';')

        unicodeWhitespace.add(0x0009)
        unicodeWhitespace.add(0x000B)
        unicodeWhitespace.add(0x00A0)
        unicodeWhitespace.add(0x1680)
        unicodeWhitespace.add(0x2000)
        unicodeWhitespace.add(0x2001)
        unicodeWhitespace.add(0x2002)
        unicodeWhitespace.add(0x2003)
        unicodeWhitespace.add(0x2004)
        unicodeWhitespace.add(0x2005)
        unicodeWhitespace.add(0x2006)
        unicodeWhitespace.add(0x2007)
        unicodeWhitespace.add(0x2008)
        unicodeWhitespace.add(0x2009)
        unicodeWhitespace.add(0x200A)
        unicodeWhitespace.add(0x202F)
        unicodeWhitespace.add(0x205F)
        unicodeWhitespace.add(0x3000)
    }

    fun isUnicodeWhitespace(codepoint: Int): Boolean {
        return unicodeWhitespace.contains(codepoint)
    }

    @JvmStatic
    fun tokenize(input: CharArrSubstring): IntArrayLengthPair {
        val tmp1 = IntArrayLengthPair(input.length * 3)
        val tmp2 = IntArrayLengthPair(input.length * 3)

        tmp1.pos = input.length
        for (i in 0..<input.length) {
            tmp1.data[i] = input.charAt(i).code
        }

        // Markdown is evil.
        naiveTokenize(tmp1, tmp2)
        clean(tmp2, tmp1)
        linkify(tmp1, tmp2)
        clean(tmp2, tmp1)

        return tmp1
    }

    private fun linkify(
        input: IntArrayLengthPair,
        output: IntArrayLengthPair
    ) {
        if (input.data.size > output.data.size * 3) {
            throw RuntimeException()
        }
        output.clear()

        var inBrackets = 0
        var lastCharOk = true

        var i = 0
        while (i < input.pos) {
            val token = input.data[i]

            when (token) {
                TOKEN_BRACKET_SQUARE_OPEN, TOKEN_PAREN_OPEN -> {
                    output.data[output.pos++] = token
                    inBrackets++
                    lastCharOk = true
                }

                TOKEN_BRACKET_SQUARE_CLOSE, TOKEN_PAREN_CLOSE -> {
                    output.data[output.pos++] = token
                    inBrackets--
                    lastCharOk = true
                }

                ' ' -> {
                    output.data[output.pos++] = ' '.code
                    lastCharOk = true
                }

                'h', 'w' -> {
                    if (inBrackets == 0 && lastCharOk) {
                        val linkStartType =
                            getLinkStartType(input.data, i, input.pos)
                        if (linkStartType >= 0) {
                            // Greedily read to space, or <>, or etc

                            val linkStartPos = i
                            val linkPrefixEndPos =
                                linkPrefixes[linkStartType]!!.size + linkStartPos
                            var linkEndPos = linkPrefixEndPos

                            var hasOpeningParen = false

                            while (linkEndPos < input.pos) {
                                val lToken = input.data[linkEndPos]

                                val isValidChar =
                                    lToken != ' '.code && lToken != '<'.code && lToken != '>'.code && lToken != TOKEN_GRAVE && lToken != TOKEN_BRACKET_SQUARE_OPEN && lToken != TOKEN_BRACKET_SQUARE_CLOSE

                                if (lToken == '('.code) {
                                    hasOpeningParen = true
                                }

                                if (isValidChar) {
                                    linkEndPos++
                                } else {
                                    break
                                }
                            }

                            // discard many final chars if they are '.', ',', '?', ';' etc
                            // THEN, discard single final char if it is '\'', '"', etc
                            while (input.data[linkEndPos - 1] == '.'.code || input.data[linkEndPos - 1] == ','.code || input.data[linkEndPos - 1] == '?'.code || input.data[linkEndPos - 1] == ';'.code) {
                                linkEndPos--
                            }

                            if (input.data[linkEndPos - 1] == '"'.code) {
                                linkEndPos--
                            }

                            if (input.data[linkEndPos - 1] == '\''.code) {
                                linkEndPos--
                            }

                            if (!hasOpeningParen && input.data[linkEndPos - 1] == ')'.code) {
                                linkEndPos--
                            }

                            if (linkEndPos - linkPrefixEndPos >= 2) {
                                val reverted =
                                    revert(input.data, linkStartPos, linkEndPos)

                                output.data[output.pos++] = TOKEN_BRACKET_SQUARE_OPEN
                                output.append(reverted)
                                output.data[output.pos++] = TOKEN_BRACKET_SQUARE_CLOSE
                                output.data[output.pos++] = TOKEN_PAREN_OPEN
                                output.append(reverted)
                                output.data[output.pos++] = TOKEN_PAREN_CLOSE

                                i = linkEndPos - 1
                            } else {
                                output.data[output.pos++] = token
                            }
                        } else {
                            output.data[output.pos++] = token
                        }
                    } else {
                        output.data[output.pos++] = token
                    }

                    lastCharOk = false
                }

                'r', 'u', '/' -> {
                    if (inBrackets == 0 && lastCharOk) {
                        val linkStartType =
                            getRedditLinkStartType(input.data, i, input.pos)
                        if (linkStartType >= 0) {
                            val linkStartPos = i
                            val linkPrefixEndPos =
                                (linkPrefixes_reddit[linkStartType]!!.size
                                        + linkStartPos)
                            var linkEndPos = linkPrefixEndPos

                            while (linkEndPos < input.pos) {
                                val lToken = input.data[linkEndPos]

                                val isValidChar =
                                    (lToken >= 'a'.code && lToken <= 'z'.code)
                                            || (lToken >= 'A'.code && lToken <= 'Z'.code)
                                            || (lToken >= '0'.code && lToken <= '9'.code)
                                            || lToken == '_'.code || lToken == TOKEN_UNDERSCORE || lToken == TOKEN_UNDERSCORE_DOUBLE || lToken == '+'.code || lToken == '-'.code

                                if (isValidChar) {
                                    linkEndPos++
                                } else {
                                    break
                                }
                            }

                            if (linkEndPos - linkPrefixEndPos > 2) {
                                val reverted =
                                    revert(input.data, linkStartPos, linkEndPos)

                                output.data[output.pos++] = TOKEN_BRACKET_SQUARE_OPEN
                                output.append(reverted)
                                output.data[output.pos++] = TOKEN_BRACKET_SQUARE_CLOSE
                                output.data[output.pos++] = TOKEN_PAREN_OPEN
                                output.append(reverted)
                                output.data[output.pos++] = TOKEN_PAREN_CLOSE

                                i = linkEndPos - 1
                            } else {
                                output.data[output.pos++] = token
                            }
                        } else {
                            output.data[output.pos++] = token
                        }
                    } else {
                        output.data[output.pos++] = token
                    }

                    lastCharOk = false
                }

                else -> {
                    // TODO test this against reddits impl
                    lastCharOk = token < 0 || (!Character.isLetterOrDigit(token))
                    output.data[output.pos++] = token
                }
            }
            i++
        }
    }

    @JvmStatic
    fun clean(
        input: IntArrayLengthPair,
        output: IntArrayLengthPair
    ) {
        // TODO use single byte array, flags

        val toRevert = BooleanArray(input.pos)
        val toDelete = BooleanArray(input.pos)

        var openingUnderscore = -1
        var openingUnderscoreDouble = -1
        var openingAsterisk = -1
        var openingAsteriskDouble = -1
        var openingTildeDouble = -1

        var lastBracketSquareOpen = -1

        run {
            var i = 0
            while (i < input.pos) {
                val c = input.data[i]

                val beforeASpace = i + 1 < input.pos && input.data[i + 1] == ' '.code
                val afterASpace = i > 0 && input.data[i - 1] == ' '.code

                when (c) {
                    MarkdownTokenizer.TOKEN_UNDERSCORE -> if (openingUnderscore < 0) {
                        // Opening underscore
                        if (beforeASpace) {
                            toRevert[i] = true
                        } else {
                            openingUnderscore = i
                        }
                    } else {
                        // Closing underscore
                        if (afterASpace) {
                            toRevert[i] = true
                        } else {
                            openingUnderscore = -1
                        }
                    }

                    MarkdownTokenizer.TOKEN_UNDERSCORE_DOUBLE -> if (i != 0 && openingUnderscoreDouble == i - 1) {
                        toRevert[openingUnderscoreDouble] = true
                        toRevert[i] = true
                        openingUnderscoreDouble = -1
                    } else {
                        if (openingUnderscoreDouble < 0) {
                            // Opening double underscore
                            if (beforeASpace) {
                                toRevert[i] = true
                            } else {
                                openingUnderscoreDouble = i
                            }
                        } else {
                            // Closing double underscore
                            if (afterASpace) {
                                toRevert[i] = true
                            } else {
                                openingUnderscoreDouble = -1
                            }
                        }
                    }

                    MarkdownTokenizer.TOKEN_ASTERISK -> if (openingAsterisk < 0) {
                        // Opening asterisk
                        if (beforeASpace) {
                            toRevert[i] = true
                        } else {
                            openingAsterisk = i
                        }
                    } else {
                        // Closing asterisk
                        if (afterASpace) {
                            toRevert[i] = true
                        } else {
                            openingAsterisk = -1
                        }
                    }

                    MarkdownTokenizer.TOKEN_ASTERISK_DOUBLE -> if (i != 0 && openingAsteriskDouble == i - 1) {
                        toRevert[openingAsteriskDouble] = true
                        toRevert[i] = true
                        openingAsteriskDouble = -1
                    } else {
                        if (openingAsteriskDouble < 0) {
                            // Opening double asterisk
                            if (beforeASpace) {
                                toRevert[i] = true
                            } else {
                                openingAsteriskDouble = i
                            }
                        } else {
                            // Closing double asterisk
                            if (afterASpace) {
                                toRevert[i] = true
                            } else {
                                openingAsteriskDouble = -1
                            }
                        }
                    }

                    MarkdownTokenizer.TOKEN_TILDE_DOUBLE -> if (i != 0 && openingTildeDouble == i - 1) {
                        toRevert[openingTildeDouble] = true
                        toRevert[i] = true
                        openingTildeDouble = -1
                    } else {
                        if (openingTildeDouble < 0) {
                            // Opening double tilde
                            if (beforeASpace) {
                                toRevert[i] = true
                            } else {
                                openingTildeDouble = i
                            }
                        } else {
                            // Closing double tilde
                            if (afterASpace) {
                                toRevert[i] = true
                            } else {
                                openingTildeDouble = -1
                            }
                        }
                    }

                    MarkdownTokenizer.TOKEN_GRAVE -> {
                        val openingGrave = i
                        val closingGrave =
                            MarkdownTokenizer.indexOf(
                                input.data,
                                MarkdownTokenizer.TOKEN_GRAVE,
                                i + 1,
                                input.pos
                            )

                        if (closingGrave < 0) {
                            toRevert[i] = true
                        } else {
                            var j = openingGrave + 1
                            while (j < closingGrave) {
                                if (input.data[j] < 0) {
                                    toRevert[j] = true
                                }
                                j++
                            }

                            i = closingGrave
                        }
                    }

                    MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN -> if (lastBracketSquareOpen < 0) {
                        // Attempt to parse link text with well-bracketed square brackets

                        val closingSquareBracket = MarkdownTokenizer.findCloseWellBracketed(
                            input.data,
                            MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN,
                            MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE,
                            i,
                            input.pos
                        )

                        if (closingSquareBracket > i) {
                            val parenOpenPos = MarkdownTokenizer.indexOf(
                                input.data,
                                MarkdownTokenizer.TOKEN_PAREN_OPEN,
                                closingSquareBracket + 1,
                                input.pos
                            )

                            if (parenOpenPos > closingSquareBracket
                                && MarkdownTokenizer.isSpaces(
                                    input.data,
                                    closingSquareBracket + 1,
                                    parenOpenPos
                                )
                            ) {
                                lastBracketSquareOpen = i

                                var j = i + 1
                                while (j < closingSquareBracket) {
                                    if (input.data[j] == MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN) {
                                        input.data[j] = '['.code
                                    } else if (input.data[j]
                                        == MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE
                                    ) {
                                        input.data[j] = ']'.code
                                    }
                                    j++
                                }
                            } else {
                                toRevert[i] = true
                            }
                        } else {
                            toRevert[i] = true
                        }
                    } else {
                        toRevert[lastBracketSquareOpen] = true
                        lastBracketSquareOpen = i
                    }

                    MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE -> {
                        if (lastBracketSquareOpen < 0) {
                            toRevert[i] = true
                        } else {
                            val lastBracketSquareClose = i

                            val parenOpenPos = MarkdownTokenizer.indexOf(
                                input.data, MarkdownTokenizer.TOKEN_PAREN_OPEN,
                                lastBracketSquareClose + 1, input.pos
                            )

                            var linkParseSuccess = false

                            if (parenOpenPos >= 0) {
                                if (MarkdownTokenizer.isSpaces(
                                        input.data,
                                        lastBracketSquareClose + 1,
                                        parenOpenPos
                                    )
                                ) {
                                    val parenClosePos =
                                        MarkdownTokenizer.findParenClosePos(input, parenOpenPos + 1)

                                    if (parenClosePos >= 0) {
                                        linkParseSuccess = true

                                        run {
                                            var j = lastBracketSquareOpen + 1
                                            while (j < lastBracketSquareClose
                                            ) {
                                                if (input.data[j] == MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN
                                                    || (input.data[j]
                                                            == MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE)
                                                ) {
                                                    toRevert[j] = true
                                                }
                                                j++
                                            }
                                        }

                                        run {
                                            var j = lastBracketSquareClose + 1
                                            while (j < parenOpenPos
                                            ) {
                                                toDelete[j] = true
                                                j++
                                            }
                                        }

                                        run {
                                            var j = parenOpenPos + 1
                                            while (j < parenClosePos
                                            ) {
                                                if (input.data[j] < 0) {
                                                    toRevert[j] = true
                                                } else if (input.data[j] == ' '.code
                                                    && input.data[j - 1] == ' '.code
                                                ) {
                                                    toDelete[j] = true
                                                }
                                                j++
                                            }
                                        }

                                        run {
                                            var j = parenOpenPos + 1
                                            while (input.data[j] == ' '.code
                                            ) {
                                                toDelete[j] = true
                                                j++
                                            }
                                        }

                                        var j = parenClosePos - 1
                                        while (input.data[j] == ' '.code
                                        ) {
                                            toDelete[j] = true
                                            j--
                                        }

                                        i = parenClosePos
                                    }
                                }
                            }

                            if (!linkParseSuccess) {
                                toRevert[lastBracketSquareOpen] = true
                                toRevert[lastBracketSquareClose] = true
                            }
                        }

                        lastBracketSquareOpen = -1
                    }

                    MarkdownTokenizer.TOKEN_PAREN_OPEN, MarkdownTokenizer.TOKEN_PAREN_CLOSE, MarkdownTokenizer.TOKEN_UNICODE_CLOSE -> toRevert[i] =
                        true

                    MarkdownTokenizer.TOKEN_UNICODE_OPEN -> {
                        val openingUnicode = i
                        val closingUnicode =
                            MarkdownTokenizer.indexOf(
                                input.data, MarkdownTokenizer.TOKEN_UNICODE_CLOSE, i + 1,
                                min(input.pos, i + 20)
                            )

                        if (closingUnicode < 0) {
                            toRevert[i] = true
                        } else if (input.data[i + 1] == '#'.code) {
                            if (input.data[i + 2] == 'x'.code && MarkdownTokenizer.isHexDigits(
                                    input.data,
                                    openingUnicode + 3,
                                    closingUnicode
                                )
                            ) {
                                val codePoint = MarkdownTokenizer.getHex(
                                    input.data,
                                    openingUnicode + 3,
                                    closingUnicode
                                )

                                if (MarkdownTokenizer.unicodeWhitespace.contains(codePoint)) {
                                    input.data[openingUnicode] = ' '.code
                                } else {
                                    input.data[openingUnicode] = codePoint
                                }

                                var j = openingUnicode + 1
                                while (j <= closingUnicode) {
                                    toDelete[j] = true
                                    j++
                                }

                                i = closingUnicode
                            } else if (MarkdownTokenizer.isDigits(
                                    input.data,
                                    openingUnicode + 2,
                                    closingUnicode
                                )
                            ) {
                                val codePoint = MarkdownTokenizer.getDecimal(
                                    input.data,
                                    openingUnicode + 2,
                                    closingUnicode
                                )

                                if (MarkdownTokenizer.unicodeWhitespace.contains(codePoint)) {
                                    input.data[openingUnicode] = ' '.code
                                } else {
                                    input.data[openingUnicode] = codePoint
                                }

                                var j = openingUnicode + 1
                                while (j <= closingUnicode) {
                                    toDelete[j] = true
                                    j++
                                }

                                i = closingUnicode
                            } else {
                                toRevert[i] = true
                            }
                        } else {
                            var codePoint: Int? = null

                            try {
                                val name = String(
                                    input.data,
                                    openingUnicode + 1,
                                    closingUnicode - openingUnicode - 1
                                )

                                val result =
                                    StringEscapeUtils.unescapeHtml4("&" + name + ";")

                                if (result.length == 1) {
                                    codePoint = result.get(0).code
                                } else if (name.equals("apos", ignoreCase = true)) {
                                    codePoint = '\''.code
                                } else if (name.equals("nsub", ignoreCase = true)) {
                                    codePoint = '⊄'.code
                                }
                            } catch (ignore: Throwable) {
                                // Ignore this
                            }

                            if (codePoint != null) {
                                if (MarkdownTokenizer.unicodeWhitespace.contains(codePoint)) {
                                    input.data[openingUnicode] = ' '.code
                                } else {
                                    input.data[openingUnicode] = codePoint
                                }

                                var j = openingUnicode + 1
                                while (j <= closingUnicode) {
                                    toDelete[j] = true
                                    j++
                                }

                                i = closingUnicode
                            } else {
                                toRevert[i] = true
                            }
                        }
                    }

                    MarkdownTokenizer.TOKEN_CARET -> if (input.pos <= i + 1 || input.data[i + 1] == ' '.code) {
                        toRevert[i] = true
                    }

                    ' ' -> if (i < 1 || input.data[i - 1] == ' '.code) {
                        toDelete[i] = true
                    }

                }
                i++
            }
        }

        if (openingUnderscore >= 0) {
            toRevert[openingUnderscore] = true
        }
        if (openingUnderscoreDouble >= 0) {
            toRevert[openingUnderscoreDouble] = true
        }
        if (openingAsterisk >= 0) {
            toRevert[openingAsterisk] = true
        }
        if (openingAsteriskDouble >= 0) {
            toRevert[openingAsteriskDouble] = true
        }
        if (openingTildeDouble >= 0) {
            toRevert[openingTildeDouble] = true
        }
        if (lastBracketSquareOpen >= 0) {
            toRevert[lastBracketSquareOpen] = true
        }

        var j = input.pos - 1
        while (j >= 0 && input.data[j] == ' '.code) {
            toDelete[j] = true
            j--
        }

        output.clear()

        for (i in 0..<input.pos) {
            if (toDelete[i]) {
                continue
            }

            if (toRevert[i]) {
                val revertTo = reverseLookup[20 + input.data[i]]
                output.append(revertTo)
            } else {
                output.data[output.pos++] = input.data[i]
            }
        }
    }

    private fun findParenClosePos(tokens: IntArrayLengthPair, startPos: Int): Int {
        var i = startPos
        while (i < tokens.pos) {
            when (tokens.data[i]) {
                TOKEN_PAREN_CLOSE -> return i

                '"' -> {
                    i = indexOfIgnoreEscaped(tokens, '"'.code, i + 1)
                    if (i < 0) {
                        return -1
                    }
                }
            }
            i++
        }

        return -1
    }

    private fun indexOfIgnoreEscaped(
        haystack: IntArrayLengthPair,
        needle: Int,
        startPos: Int
    ): Int {
        var i = startPos
        while (i < haystack.pos) {
            if (haystack.data[i] == '\\'.code) {
                i++
            } else if (haystack.data[i] == needle) {
                return i
            }
            i++
        }
        return -1
    }

    @JvmStatic
    fun naiveTokenize(
        input: IntArrayLengthPair,
        output: IntArrayLengthPair
    ) {
        output.clear()

        var i = 0
        while (i < input.pos) {
            val c = input.data[i]

            when (c) {
                '*' -> if (i < input.pos - 1 && input.data[i + 1] == '*'.code) {
                    i++
                    output.data[output.pos++] = TOKEN_ASTERISK_DOUBLE
                } else {
                    output.data[output.pos++] = TOKEN_ASTERISK
                }

                '_' -> if (i < input.pos - 1 && input.data[i + 1] == '_'.code) {
                    i++
                    output.data[output.pos++] = TOKEN_UNDERSCORE_DOUBLE
                } else {
                    if ((i < input.pos - 1 && input.data[i + 1] == ' '.code)
                        || (i > 0 && input.data[i - 1] == ' '.code)
                        || (i == 0) || (i == input.pos - 1)
                    ) {
                        output.data[output.pos++] = TOKEN_UNDERSCORE
                    } else {
                        output.data[output.pos++] = c
                    }
                }

                '~' -> if (i < input.pos - 1 && input.data[i + 1] == '~'.code) {
                    i++
                    output.data[output.pos++] = TOKEN_TILDE_DOUBLE
                } else {
                    output.data[output.pos++] = '~'.code
                }

                '^' -> output.data[output.pos++] = TOKEN_CARET
                '`' -> output.data[output.pos++] = TOKEN_GRAVE
                '[' -> output.data[output.pos++] = TOKEN_BRACKET_SQUARE_OPEN
                ']' -> output.data[output.pos++] = TOKEN_BRACKET_SQUARE_CLOSE
                '(' -> output.data[output.pos++] = TOKEN_PAREN_OPEN
                ')' -> output.data[output.pos++] = TOKEN_PAREN_CLOSE
                '&' -> output.data[output.pos++] = TOKEN_UNICODE_OPEN
                ';' -> output.data[output.pos++] = TOKEN_UNICODE_CLOSE
                '\\' -> if (i < input.pos - 1) {
                    output.data[output.pos++] = input.data[++i]
                } else {
                    output.data[output.pos++] = '\\'.code
                }

                '\t', '\r', '\f', '\n' -> output.data[output.pos++] = ' '.code
                else -> output.data[output.pos++] = c
            }
            i++
        }
    }

    private fun indexOf(
        haystack: IntArray,
        needle: Int,
        startInclusive: Int,
        endExclusive: Int
    ): Int {
        for (i in startInclusive..<endExclusive) {
            if (haystack[i] == needle) {
                return i
            }
        }
        return -1
    }

    @JvmStatic
    fun findCloseWellBracketed(
        haystack: IntArray,
        openBracket: Int,
        closeBracket: Int,
        startInclusive: Int,
        endExclusive: Int
    ): Int {
        if (haystack[startInclusive] != openBracket) {
            throw RuntimeException("Internal markdown parser error")
        }

        var b = 1

        for (i in startInclusive + 1..<endExclusive) {
            if (haystack[i] == openBracket) {
                b++
            } else if (haystack[i] == closeBracket) {
                b--
            }

            if (b == 0) {
                return i
            }
        }

        return -1
    }

    private fun isSpaces(
        haystack: IntArray,
        startInclusive: Int,
        endExclusive: Int
    ): Boolean {
        for (i in startInclusive..<endExclusive) {
            if (haystack[i] != ' '.code) {
                return false
            }
        }
        return true
    }

    private fun isDigits(
        haystack: IntArray,
        startInclusive: Int,
        endExclusive: Int
    ): Boolean {
        for (i in startInclusive..<endExclusive) {
            if (haystack[i] < '0'.code || haystack[i] > '9'.code) {
                return false
            }
        }
        return true
    }

    private fun isHexDigits(
        haystack: IntArray,
        startInclusive: Int,
        endExclusive: Int
    ): Boolean {
        for (i in startInclusive..<endExclusive) {
            val c = haystack[i]
            if ((c < '0'.code || c > '9'.code) && (c < 'a'.code || c > 'f'.code) && (c < 'A'.code || c > 'F'.code)) {
                return false
            }
        }
        return true
    }

    private fun getDecimal(
        chars: IntArray,
        startInclusive: Int,
        endExclusive: Int
    ): Int {
        var result = 0
        for (i in startInclusive..<endExclusive) {
            result *= 10
            result += chars[i] - '0'.code
        }
        return result
    }

    private fun fromHex(ch: Int): Int {
        if (ch >= '0'.code && ch <= '9'.code) {
            return ch - '0'.code
        }
        if (ch >= 'a'.code && ch <= 'f'.code) {
            return 10 + ch - 'a'.code
        }
        return 10 + ch - 'A'.code
    }

    private fun getHex(
        chars: IntArray,
        startInclusive: Int,
        endExclusive: Int
    ): Int {
        var result = 0
        for (i in startInclusive..<endExclusive) {
            result *= 16
            result += fromHex(chars[i])
        }
        return result
    }

    private fun equals(
        haystack: IntArray,
        needle: CharArray,
        startInclusive: Int
    ): Boolean {
        for (i in needle.indices) {
            if (haystack[startInclusive + i] != needle[i].code) {
                return false
            }
        }
        return true
    }

    private fun getLinkStartType(
        haystack: IntArray,
        startInclusive: Int,
        endExclusive: Int
    ): Int {
        val maxLen = endExclusive - startInclusive
        for (type in linkPrefixes.indices) {
            if (linkPrefixes[type]!!.size <= maxLen && MarkdownTokenizer.equals(
                    haystack,
                    linkPrefixes[type]!!,
                    startInclusive
                )
            ) {
                return type
            }
        }
        return -1
    }

    private fun getRedditLinkStartType(
        haystack: IntArray,
        startInclusive: Int,
        endExclusive: Int
    ): Int {
        val maxLen = endExclusive - startInclusive
        for (type in linkPrefixes_reddit.indices) {
            if (linkPrefixes_reddit[type]!!.size <= maxLen && MarkdownTokenizer.equals(
                    haystack,
                    linkPrefixes_reddit[type]!!,
                    startInclusive
                )
            ) {
                return type
            }
        }
        return -1
    }

    // TODO avoid generating new array
    private fun revert(
        tokens: IntArray,
        startInclusive: Int,
        endExclusive: Int
    ): IntArray {
        var outputLen = 0

        for (i in startInclusive..<endExclusive) {
            val token = tokens[i]
            if (token < 0) {
                outputLen += reverseLookup[20 + token].size
            } else {
                outputLen++
            }
        }

        val result = IntArray(outputLen)
        var resultPos = 0

        for (i in startInclusive..<endExclusive) {
            val token = tokens[i]
            if (token < 0) {
                for (c in reverseLookup[20 + token]) {
                    result[resultPos++] = c.code
                }
            } else {
                result[resultPos++] = token
            }
        }

        return result
    }
}
