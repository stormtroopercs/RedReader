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
import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.reddit.prepared.bodytext.BlockType
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElement
import java.util.Objects

abstract class HtmlRawElement {
    // TODO potential improvements:
    //		- Profile performance
    //		- Test left/right swiping interaction with table scrollview
    class LinkButtonDetails(
        val name: String?,
        val url: UriString
    ) {
        val buttonTitle: String
            get() {
                if (name == null || name.isEmpty()) {
                    return url.value
                } else {
                    return name
                }
            }

        val buttonSubtitle: String?
            get() {
                if (name == null || name.isEmpty()) {
                    return null
                } else {
                    return url.value
                }
            }
    }

    val plainText: String
        get() {
            val sb = StringBuilder()
            getPlainText(sb)
            return sb.toString()
        }

    abstract fun getPlainText(stringBuilder: StringBuilder)


    abstract fun reduce(
        activeAttributes: HtmlTextAttributes,
        activity: AppCompatActivity,
        destination: ArrayList<HtmlRawElement>,
        linkButtons: ArrayList<LinkButtonDetails>
    )

    abstract fun generate(
        activity: AppCompatActivity,
        destination: ArrayList<BodyElement>
    )

    companion object {
        @Throws(MalformedHtmlException::class)
        fun readFrom(reader: HtmlReaderPeekable): HtmlRawElement {
            val startToken = reader.peek()
            reader.advance()

            val children = ArrayList<HtmlRawElement>()

            if (startToken.type == HtmlReader.TokenType.TAG_START_AND_END) {
                when (startToken.text) {
                    "hr" -> return HtmlRawElementTagHorizontalRule()

                    "br" -> return HtmlRawElementBreak()

                    "img" -> return HtmlRawElementImg(
                        children,
                        if (startToken.title == null || startToken.title.isEmpty())
                            "emote"
                        else
                            startToken.title,
                        startToken.src!!
                    )

                    else -> return HtmlRawElementInlineErrorMessage.create(
                        "Error: Unexpected tag <" + startToken.text + "/>"
                    )
                }
            } else if (startToken.type == HtmlReader.TokenType.TAG_START) {
                while (reader.peek().type != HtmlReader.TokenType.TAG_END
                    && reader.peek().type != HtmlReader.TokenType.EOF
                ) {
                    children.add(readFrom(reader))
                }

                run {
                    val endToken = reader.peek()
                    // Reddit sometimes doesn't close tags properly :'(
                    if (endToken.text.equals(startToken.text, ignoreCase = true)) {
                        reader.advance()
                    }
                }

                val result: HtmlRawElement

                when (StringUtils.asciiLowercase(startToken.text)) {
                    "code" -> result = HtmlRawElementTagCode(children)
                    "del" -> result = HtmlRawElementTagDel(children)
                    "em" -> result = HtmlRawElementTagEmphasis(children)
                    "div" -> result = HtmlRawElementBlock(
                        BlockType.VERTICAL_SEQUENCE,
                        children
                    )

                    "h1" -> result = HtmlRawElementBlock(
                        BlockType.HEADER,
                        HtmlRawElementTagH1(children)
                    )

                    "h2" -> result = HtmlRawElementBlock(
                        BlockType.HEADER,
                        HtmlRawElementTagH2(children)
                    )

                    "h3" -> result = HtmlRawElementBlock(
                        BlockType.HEADER,
                        HtmlRawElementTagH3(children)
                    )

                    "h4" -> result = HtmlRawElementBlock(
                        BlockType.HEADER,
                        HtmlRawElementTagH4(children)
                    )

                    "h5" -> result = HtmlRawElementBlock(
                        BlockType.HEADER,
                        HtmlRawElementTagH5(children)
                    )

                    "h6" -> result = HtmlRawElementBlock(
                        BlockType.HEADER,
                        HtmlRawElementTagH6(children)
                    )

                    "strong" -> result = HtmlRawElementTagStrong(children)
                    "p" -> result = HtmlRawElementBlock(BlockType.NORMAL_TEXT, children)
                    "th", "td" -> result = HtmlRawElementTableCell(
                        HtmlRawElementBlock(
                            BlockType.TABLE_CELL,
                            children
                        )
                    )

                    "sup" -> result = HtmlRawElementTagSuperscript(children)
                    "a" -> {
                        val href = Objects.requireNonNull<String>(startToken.href)

                        if (href.startsWith("/spoiler")) {
                            // Old spoiler syntax
                            result = HtmlRawElementSpoiler(
                                HtmlRawElementBlock(
                                    BlockType.BUTTON,
                                    children
                                )
                            )
                        } else if (href.length == 2 && (href.get(0) == '#' || href.get(0) == '/')
                            && startToken.title != null
                        ) {
                            // Another old spoiler syntax

                            children.add(
                                HtmlRawElementSpoiler(
                                    HtmlRawElementBlock(
                                        BlockType.NORMAL_TEXT,
                                        HtmlRawElementPlainText(startToken.title)
                                    )
                                )
                            )

                            result = HtmlRawElementTagPassthrough(children)
                        } else if (href.startsWith("#")) {
                            // Probably an emote: pass through the text, but don't make a link
                            result = HtmlRawElementTagPassthrough(children)
                        } else {
                            result = HtmlRawElementTagAnchor(children, UriString(href))
                        }
                    }

                    "pre" -> result = HtmlRawElementBlock(
                        BlockType.CODE_BLOCK,
                        HtmlRawElementTagCode(children)
                    )

                    "ul" -> result = HtmlRawElementBulletList(children)
                    "ol" -> result = HtmlRawElementNumberedList(children)
                    "li" -> result = HtmlRawElementBlock(BlockType.LIST_ELEMENT, children)
                    "blockquote" -> result = HtmlRawElementQuote(
                        HtmlRawElementBlock(
                            BlockType.QUOTE,
                            children
                        )
                    )

                    "span" -> if ("md-spoiler-text".equals(
                            startToken.cssClass,
                            ignoreCase = true
                        )
                    ) {
                        result = HtmlRawElementSpoiler(
                            HtmlRawElementBlock(
                                BlockType.BUTTON,
                                children
                            )
                        )
                    } else {
                        result = HtmlRawElementTagPassthrough(children)
                    }

                    "thead" -> result = HtmlRawElementTagStrong(children)
                    "tbody" -> result = HtmlRawElementTagPassthrough(children)
                    "table" -> result = HtmlRawElementTable(children)
                    "tr" -> result = HtmlRawElementTableRow(children)
                    "emote" -> {
                        val src = Objects.requireNonNull<UriString>(startToken.src)
                        result = HtmlRawElementImg(
                            children,
                            if (startToken.title == null || startToken.title.isEmpty())
                                "emote"
                            else
                                startToken.title,
                            src
                        )
                    }

                    else -> return HtmlRawElementInlineErrorMessage.appendError(
                        "Error: Unexpected tag start <" + startToken.text + ">",
                        HtmlRawElementBlock(BlockType.NORMAL_TEXT, children)
                    )
                }

                return result
            } else if (startToken.type == HtmlReader.TokenType.TEXT) {
                return HtmlRawElementPlainText(startToken.text)
            } else if (startToken.type == HtmlReader.TokenType.EOF) {
                throw MalformedHtmlException(
                    "Unexpected EOF",
                    reader.html,
                    reader.pos
                )
            } else {
                return HtmlRawElementInlineErrorMessage.create(
                    "Error: Unexpected token type " + startToken.type
                )
            }
        }
    }
}
