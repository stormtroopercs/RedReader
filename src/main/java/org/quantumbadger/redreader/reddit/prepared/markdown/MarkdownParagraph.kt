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

import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.view.View
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.common.LinkHandler.onLinkLongClicked
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.UriString.Companion.from
import org.quantumbadger.redreader.reddit.prepared.markdown.MarkdownParser.MarkdownParagraphType
import org.quantumbadger.redreader.views.LinkifiedTextView

// TODO number links
class MarkdownParagraph(
    val raw: CharArrSubstring?,
    val parent: MarkdownParagraph?,
    val type: MarkdownParagraphType,
    val tokens: IntArray?,
    val level: Int,
    val number: Int
) {
    val spanned: Spanned?
    val links: MutableList<Link>

    class Link(val title: String, val subtitle: String?, private val url: UriString) {
        fun onClicked(activity: BaseActivity) {
            onLinkClicked(activity, url, false)
        }

        fun onLongClicked(activity: BaseActivity) {
            onLinkLongClicked(activity, url)
        }
    }

    init {
        links = ArrayList<Link>()
        spanned = internalGenerateSpanned()

        if (tokens == null && raw != null) {
            raw.replaceUnicodeSpaces()
        }
    }

    private fun internalGenerateSpanned(): Spanned? {
        if (type == MarkdownParagraphType.CODE
            || type == MarkdownParagraphType.HLINE
        ) {
            return null
        }

        if (tokens == null) {
            return SpannableString(raw.toString())
        }

        val builder = SpannableStringBuilder()

        var boldStart = -1
        var italicStart = -1
        var strikeStart = -1
        var linkStart = -1
        var caretStart = -1
        var parentOpenCount = 0
        var parentCloseCount = 0

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]

            when (token) {
                MarkdownTokenizer.TOKEN_ASTERISK, MarkdownTokenizer.TOKEN_UNDERSCORE -> if (italicStart < 0) {
                    italicStart = builder.length
                } else {
                    builder.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        italicStart,
                        builder.length,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                    italicStart = -1
                }

                MarkdownTokenizer.TOKEN_ASTERISK_DOUBLE, MarkdownTokenizer.TOKEN_UNDERSCORE_DOUBLE -> if (boldStart < 0) {
                    boldStart = builder.length
                } else {
                    builder.setSpan(
                        StyleSpan(Typeface.BOLD),
                        boldStart,
                        builder.length,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                    boldStart = -1
                }

                MarkdownTokenizer.TOKEN_TILDE_DOUBLE -> if (strikeStart == -1) {
                    strikeStart = builder.length
                } else {
                    builder.setSpan(
                        StrikethroughSpan(),
                        strikeStart,
                        builder.length,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                    strikeStart = -1
                }

                MarkdownTokenizer.TOKEN_GRAVE -> {
                    val codeStart = builder.length

                    while (tokens[++i] != MarkdownTokenizer.TOKEN_GRAVE) {
                        builder.append(tokens[i].toChar())
                    }

                    builder.setSpan(
                        TypefaceSpan("monospace"),
                        codeStart,
                        builder.length,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                }

                MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN -> linkStart = builder.length
                MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE -> {
                    val urlStart: Int = indexOf(
                        tokens,
                        MarkdownTokenizer.TOKEN_PAREN_OPEN,
                        i + 1
                    )
                    val urlEnd: Int = indexOf(
                        tokens,
                        MarkdownTokenizer.TOKEN_PAREN_CLOSE,
                        urlStart + 1
                    )

                    val urlBuilder = StringBuilder(urlEnd - urlStart)

                    var j = urlStart + 1
                    while (j < urlEnd) {
                        urlBuilder.append(tokens[j].toChar())
                        j++
                    }

                    val linkText = builder.subSequence(
                        linkStart,
                        builder.length
                    ).toString()
                    val url = urlBuilder.toString()

                    if (url.startsWith("/spoiler")) {
                        builder.delete(linkStart, builder.length)
                        builder.append("[Spoiler]")

                        val spoilerUriBuilder = Uri.parse("rr://msg/")
                            .buildUpon()
                        spoilerUriBuilder.appendQueryParameter("title", "Spoiler")
                        spoilerUriBuilder.appendQueryParameter("message", linkText)

                        links.add(
                            Link(
                                "Spoiler",
                                null,
                                from(spoilerUriBuilder)
                            )
                        )
                    } else if (url.length > 3 && url.get(2) == ' ' && (url.get(0) == '#' || url.get(
                            0
                        ) == '/')
                    ) {
                        val subtitle: String
                        when (url.get(1)) {
                            'b' -> subtitle = "Spoiler: Book"
                            'g' -> subtitle = "Spoiler: Speculation"
                            's' -> subtitle = "Spoiler"
                            else -> subtitle = "Spoiler"
                        }

                        val spoilerUriBuilder = Uri.parse("rr://msg/")
                            .buildUpon()
                        spoilerUriBuilder.appendQueryParameter("title", subtitle)
                        spoilerUriBuilder.appendQueryParameter(
                            "message",
                            url.substring(3)
                        )

                        links.add(
                            Link(
                                linkText,
                                subtitle,
                                from(spoilerUriBuilder)
                            )
                        )
                    } else {
                        links.add(Link(linkText, url, UriString(url)))
                    }

                    // TODO
                    //builder.insert(linkStart, "[NUMBER HERE]");
                    val span: ClickableSpan = object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            val activity = (widget as LinkifiedTextView)
                                .activity
                            onLinkClicked(activity, UriString(url))
                        }
                    }

                    builder.setSpan(
                        span,
                        linkStart,
                        builder.length,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                    )

                    i = urlEnd
                }

                MarkdownTokenizer.TOKEN_CARET -> if (caretStart < 0) {
                    caretStart = builder.length
                } else {
                    builder.append(' ')
                }

                ' '.code -> {
                    builder.append(' ')

                    if (caretStart >= 0 && parentOpenCount == parentCloseCount) {
                        builder.setSpan(
                            SuperscriptSpan(),
                            caretStart,
                            builder.length,
                            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                        )
                        builder.setSpan(
                            RelativeSizeSpan(0.6f),
                            caretStart,
                            builder.length,
                            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                        )
                        caretStart = -1
                    }
                }

                '('.code -> if (caretStart >= 0) {
                    parentOpenCount++
                    if (caretStart != builder.length) {
                        builder.append('(')
                    }
                } else {
                    parentOpenCount = 0
                    builder.append('(')
                }

                ')'.code -> if (caretStart >= 0) {
                    parentCloseCount++
                    if (parentOpenCount != parentCloseCount) {
                        builder.append(')')
                    } else {
                        builder.setSpan(
                            SuperscriptSpan(),
                            caretStart,
                            builder.length,
                            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                        )
                        builder.setSpan(
                            RelativeSizeSpan(0.6f),
                            caretStart,
                            builder.length,
                            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                        )
                        caretStart = -1
                    }
                } else {
                    parentCloseCount = 0
                    builder.append(')')
                }

                else -> builder.append(token.toChar())
            }
            i++
        }

        if (caretStart >= 0) {
            builder.setSpan(
                SuperscriptSpan(),
                caretStart,
                builder.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                RelativeSizeSpan(0.6f),
                caretStart,
                builder.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        if (type == MarkdownParagraphType.HEADER) {
            while (builder.length > 0 && builder.get(builder.length - 1) == '#') {
                builder.delete(builder.length - 1, builder.length)
            }
        }

        return builder
    }

    val isEmpty: Boolean
        get() {
            if (type == MarkdownParagraphType.HLINE) {
                return false
            }
            if (type == MarkdownParagraphType.EMPTY) {
                return true
            }

            if (tokens == null) {
                return raw!!.countSpacesAtStart() == raw.length
            } else {
                for (token in tokens) {
                    if (!MarkdownTokenizer.isUnicodeWhitespace(token)) {
                        return false
                    }
                }
                return true
            }
        }

    companion object {
        private fun indexOf(
            haystack: IntArray,
            needle: Int,
            startPos: Int
        ): Int {
            for (i in startPos..<haystack.size) {
                if (haystack[i] == needle) {
                    return i
                }
            }
            return -1
        }
    }
}
