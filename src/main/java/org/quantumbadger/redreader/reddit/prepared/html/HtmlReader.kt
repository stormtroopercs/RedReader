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
package org.quantumbadger.redreader.reddit.prepared.html

import androidx.appcompat.app.AppCompatActivity
import org.apache.commons.text.StringEscapeUtils
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.reddit.prepared.bodytext.BlockType
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElement
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElementRRError
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElementVerticalSequence

class HtmlReader(val html: String) {
    enum class TokenType {
        TAG_START,
        TAG_END,
        TAG_START_AND_END,
        TEXT,
        EOF
    }

    class Token {
        val type: TokenType
        val text: String
        val href: String?
        val cssClass: String?
        val title: String?
        val src: UriString?

        constructor(
            type: TokenType,
            text: String,
            href: String?,
            cssClass: String?,
            title: String?
        ) {
            this.type = type
            this.text = text
            this.href = href
            this.cssClass = cssClass
            this.title = title
            this.src = null
        }

        constructor(
            type: TokenType,
            text: String,
            href: String?,
            cssClass: String?,
            title: String?,
            src: UriString?
        ) {
            this.type = type
            this.text = text
            this.href = href
            this.cssClass = cssClass
            this.title = title
            this.src = src
        }

        override fun toString(): String {
            return type.name + "(" + text + ")"
        }

        companion object {
            val EOF: Token = Token(TokenType.EOF, "", null, null, null)
        }
    }

    var pos: Int = 0
        private set

    private var mPreformattedTextPending = false

    @Throws(MalformedHtmlException::class)
    private fun readName(): String {
        val result = StringBuilder(16)

        try {
            while (isNameChar(html.get(this.pos))) {
                result.append(html.get(this.pos))
                this.pos++
            }
        } catch (e: IndexOutOfBoundsException) {
            throw MalformedHtmlException(
                "Reached EOF while reading name",
                this.html,
                this.pos,
                e
            )
        }

        if (result.length == 0) {
            throw MalformedHtmlException("Got zero-length name", this.html, this.pos)
        }

        return result.toString()
    }

    private fun readAndUnescapeUntil(endChar: Char): String {
        val result = StringBuilder(64)

        while (this.pos < html.length && html.get(this.pos) != endChar) {
            result.append(html.get(this.pos))
            this.pos++
        }

        return StringEscapeUtils.unescapeHtml4(result.toString())
    }

    private fun tryAccept(c: Char): Boolean {
        if (this.pos < html.length && html.get(this.pos) == c) {
            this.pos++
            return true
        }

        return false
    }

    @Throws(MalformedHtmlException::class)
    private fun accept(c: Char) {
        try {
            if (html.get(this.pos) != c) {
                throw MalformedHtmlException("Expecting " + c, this.html, this.pos)
            }
        } catch (e: IndexOutOfBoundsException) {
            throw MalformedHtmlException("Unexpected EOF", this.html, this.pos, e)
        }

        this.pos++
    }

    private fun skipWhitespace() {
        while (this.pos < html.length && isWhitespace(html.get(this.pos))) {
            this.pos++
        }
    }

    private fun skipNewlines() {
        while (this.pos < html.length && html.get(this.pos) == '\n') {
            this.pos++
        }
    }

    @Throws(MalformedHtmlException::class)
    fun readNext(): Token {
        try {
            mainLoop@ while (true) {
                skipNewlines()

                if (this.pos >= html.length) {
                    // End of data
                    return Token.Companion.EOF
                }

                if (html.get(this.pos) == '<') {
                    this.pos++
                    skipWhitespace()

                    var type: TokenType?

                    if (html.get(this.pos) == '!') {
                        // Comment

                        this.pos++
                        accept('-')
                        accept('-')

                        while (true) {
                            if (html.get(this.pos) == '-' && html.get(this.pos + 1) == '-' && html.get(
                                    this.pos + 2
                                ) == '>'
                            ) {
                                this.pos += 3
                                continue@mainLoop
                            } else {
                                this.pos++
                            }
                        }
                    }

                    if (html.get(this.pos) == '/') {
                        type = TokenType.TAG_END
                        this.pos++
                        skipWhitespace()
                    } else {
                        type = TokenType.TAG_START
                    }

                    val tagName = readName()
                    var href: String?=null
                    var cssClass: String?=null
                    var title: String?=null
                    var src: UriString?=null

                    if (tagName.equals("pre", ignoreCase = true)) {
                        mPreformattedTextPending = true
                    }

                    skipWhitespace()

                    while (html.get(this.pos) != '>') {
                        if (tryAccept('/')) {
                            skipWhitespace()
                            accept('>')
                            return Token(
                                TokenType.TAG_START_AND_END,
                                tagName,
                                href,
                                cssClass,
                                title
                            )
                        }

                        val propertyName = readName()

                        if (tryAccept('=')) {
                            accept('"')
                            val value = readAndUnescapeUntil('"')
                            accept('"')
                            skipWhitespace()

                            if (propertyName.equals("href", ignoreCase = true)) {
                                href = value
                            } else if (propertyName.equals("class", ignoreCase = true)) {
                                cssClass = value
                            } else if (propertyName.equals("title", ignoreCase = true)) {
                                title = value
                            } else if (propertyName.equals("src", ignoreCase = true)) {
                                src = UriString(value)
                            }
                        }
                    }

                    accept('>')

                    // Reddit doesn't provide an end tag with their img tags for some reason
                    // Need this to show multiple concurrent images correctly
                    if (tagName == "img") {
                        type = TokenType.TAG_START_AND_END
                    }

                    return Token(type, tagName, href, cssClass, title, src)
                } else {
                    if (mPreformattedTextPending) {
                        mPreformattedTextPending = false

                        var preformattedText = readAndUnescapeUntil('<')

                        if (preformattedText.endsWith("\n")) {
                            preformattedText = preformattedText.substring(
                                0,
                                preformattedText.length - 1
                            )
                        }

                        return Token(
                            TokenType.TEXT,
                            preformattedText,
                            null,
                            null,
                            null
                        )
                    }

                    // Raw text
                    return Token(
                        TokenType.TEXT,
                        normaliseWhitespace(readAndUnescapeUntil('<')),
                        null,
                        null,
                        null
                    )
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            throw MalformedHtmlException("Unexpected EOF", this.html, this.pos, e)
        }
    }

    companion object {
        private fun normaliseWhitespace(html: String): String {
            val result = StringBuilder(html.length)

            var lastCharWasWhitespace = false

            for (i in 0..<html.length) {
                val c = html.get(i)

                if (c != '\n' && c != '\r') {
                    if (isWhitespace(c)) {
                        if (!lastCharWasWhitespace) {
                            result.append(" ")
                            lastCharWasWhitespace = true
                        }
                    } else {
                        lastCharWasWhitespace = false
                        result.append(c)
                    }
                }
            }

            return result.toString()
        }

        private fun isWhitespace(c: Char): Boolean {
            return c == ' ' || c == '\t' || c == '\r' || c == '\n'
        }

        private fun isNameChar(c: Char): Boolean {
            when (c) {
                0, ' ', '\'', '"', '>', '/', '=' -> return false

                else -> return true
            }
        }

        fun parse(
            html: String?,
            activity: AppCompatActivity
        ): BodyElement {
            var html = html
            if (html == null) {
                html = ""
            }

            val applicationContext = activity.getApplicationContext()

            try {
                val reader = HtmlReaderPeekable(HtmlReader(html))

                var rootElement: HtmlRawElement?

                if (reader.peek().type == TokenType.EOF) {
                    // Empty comment
                    rootElement = HtmlRawElementPlainText("")
                } else {
                    rootElement = HtmlRawElement.Companion.readFrom(reader)
                }

                if (rootElement !is HtmlRawElementBlock) {
                    rootElement = HtmlRawElementBlock(BlockType.NORMAL_TEXT, rootElement)
                }

                val reduced = rootElement.reduce(
                    HtmlTextAttributes(),
                    activity
                )

                val generated = ArrayList<BodyElement?>()

                reduced.generate(activity, generated)

                return BodyElementVerticalSequence(generated)
            } catch (e: MalformedHtmlException) {
                return BodyElementRRError(
                    RRError(
                        applicationContext.getString(string.error_title_malformed_html),
                        applicationContext.getString(string.error_message_malformed_html),
                        true,
                        e
                    )
                )
            } catch (e: Exception) {
                return BodyElementRRError(
                    RRError(
                        applicationContext.getString(string.error_parse_title),
                        applicationContext.getString(string.error_parse_message),
                        true,
                        e
                    )
                )
            }
        }
    }
}
