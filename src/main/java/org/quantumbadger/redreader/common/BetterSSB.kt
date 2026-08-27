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

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SuperscriptSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import org.quantumbadger.redreader.common.LinkHandler.computeAllLinks
import java.util.Observable

class BetterSSB : Observable() {
    private val sb: SpannableStringBuilder

    init {
        this.sb = SpannableStringBuilder()
    }

    fun append(str: String, flags: Int, url: String?) {
        append(str, flags, 0, 0, 1f, url)
    }

    @JvmOverloads
    fun append(
        str: String,
        flags: Int,
        foregroundCol: Int = 0,
        backgroundCol: Int = 0,
        scale: Float = 1f,
        url: String?=null
    ) {
        val strStart = sb.length
        sb.append(str)
        val strEnd = sb.length

        if ((flags and BOLD) != 0) {
            sb.setSpan(
                StyleSpan(Typeface.BOLD),
                strStart,
                strEnd,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        if ((flags and ITALIC) != 0) {
            sb.setSpan(
                StyleSpan(Typeface.ITALIC),
                strStart,
                strEnd,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        if ((flags and UNDERLINE) != 0) {
            sb.setSpan(
                UnderlineSpan(),
                strStart,
                strEnd,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        if ((flags and STRIKETHROUGH) != 0) {
            sb.setSpan(
                StrikethroughSpan(),
                strStart,
                strEnd,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        if ((flags and FOREGROUND_COLOR) != 0) {
            sb.setSpan(
                ForegroundColorSpan(foregroundCol),
                strStart,
                strEnd,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        if ((flags and BACKGROUND_COLOR) != 0) {
            sb.setSpan(
                BackgroundColorSpan(backgroundCol),
                strStart,
                strEnd,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        if ((flags and SIZE) != 0) {
            sb.setSpan(
                RelativeSizeSpan(scale),
                strStart,
                strEnd,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        if ((flags and SUPERSCRIPT) != 0) {
            sb.setSpan(
                SuperscriptSpan(),
                strStart,
                strEnd,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        if (url != null) {
            sb.setSpan(
                URLSpan(url),
                strStart,
                strEnd,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }
    }

    fun linkify() {
        val asText = sb.toString()
        val links: HashSet<UriString> = computeAllLinks(asText)

        for (uri in links) {
            val link = uri.value

            var index = -1

            while (index < asText.length
                && (asText.indexOf(link, index + 1).also { index = it }) >= 0
            ) {
                if (sb.getSpans<URLSpan?>(
                        index,
                        index + link.length,
                        URLSpan::class.java
                    ).size < 1
                ) {
                    sb.setSpan(
                        URLSpan(link),
                        index,
                        index + link.length,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    }

    fun append(text: CharSequence?) {
        this.sb.append(text)
        this.setChanged()
        this.notifyObservers(this.sb)
    }

    fun replace(start: Int, end: Int, text: CharSequence?) {
        this.sb.replace(start, end, text)
        this.setChanged()
        this.notifyObservers(this.sb)
    }

    fun replace(
        textToBeReplaced: CharSequence,
        replacement: Any
    ) {
        val textStartIndex = TextUtils.indexOf(this.sb, textToBeReplaced)

        this.sb.setSpan(
            replacement,
            textStartIndex,
            textStartIndex + textToBeReplaced.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )

        this.setChanged()
        this.notifyObservers(this.sb)
    }

    val isEmpty: Boolean
        get() = this.sb.length == 0

    fun setSpan(what: Any?, start: Int, end: Int, flag: Int) {
        this.sb.setSpan(what, start, end, flag)

        this.setChanged()
        this.notifyObservers(this.sb)
    }

    fun get(): SpannableStringBuilder {
        return sb
    }

    companion object {
        const val BOLD: Int = 1
        @Suppress("PropertyName")
        val ITALIC: Int = 1 shl 1
        @Suppress("PropertyName")
        val UNDERLINE: Int = 1 shl 2
        @Suppress("PropertyName")
        val STRIKETHROUGH: Int = 1 shl 3
        @Suppress("PropertyName")
        val FOREGROUND_COLOR: Int = 1 shl 4
        @Suppress("PropertyName")
        val BACKGROUND_COLOR: Int = 1 shl 5
        @Suppress("PropertyName")
        val SIZE: Int = 1 shl 6
        @Suppress("PropertyName")
        val SUPERSCRIPT: Int = 1 shl 7

        const val NBSP: Char = '\u00A0'
    }
}
