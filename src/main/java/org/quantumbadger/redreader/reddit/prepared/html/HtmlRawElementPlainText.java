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

import android.graphics.Typeface
import android.text.style.CharacterStyle
import android.text.style.ClickableSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElement

class HtmlRawElementPlainText(private val mText: String) : HtmlRawElement() {
    override fun getPlainText(stringBuilder: StringBuilder) {
        stringBuilder.append(mText)
    }

    override fun reduce(
        attributes: HtmlTextAttributes,
        activity: AppCompatActivity,
        destination: ArrayList<HtmlRawElement?>,
        linkButtons: ArrayList<LinkButtonDetails?>
    ) {
        var spans: ArrayList<CharacterStyle?>? = null

        if (attributes.bold > 0) {
            if (spans == null) {
                spans = ArrayList<CharacterStyle?>()
            }
            spans.add(StyleSpan(Typeface.BOLD))
        }

        if (attributes.italic > 0) {
            if (spans == null) {
                spans = ArrayList<CharacterStyle?>()
            }
            spans.add(StyleSpan(Typeface.ITALIC))
        }

        if (attributes.underline > 0) {
            if (spans == null) {
                spans = ArrayList<CharacterStyle?>()
            }
            spans.add(UnderlineSpan())
        }

        if (attributes.strikethrough > 0) {
            if (spans == null) {
                spans = ArrayList<CharacterStyle?>()
            }
            spans.add(StrikethroughSpan())
        }

        if (attributes.monospace > 0) {
            if (spans == null) {
                spans = ArrayList<CharacterStyle?>()
            }
            spans.add(TypefaceSpan("monospace"))
        }

        if (attributes.superscript > 0) {
            if (spans == null) {
                spans = ArrayList<CharacterStyle?>()
            }

            for (i in 0..<attributes.superscript) {
                spans.add(SuperscriptSpan())
                spans.add(RelativeSizeSpan(0.85f))
            }
        }

        if (attributes.extraLarge > 0) {
            if (spans == null) {
                spans = ArrayList<CharacterStyle?>()
            }
            spans.add(RelativeSizeSpan(1.6f))
        } else if (attributes.large > 0) {
            if (spans == null) {
                spans = ArrayList<CharacterStyle?>()
            }
            spans.add(RelativeSizeSpan(1.3f))
        }

        if (attributes.href != null) {
            if (spans == null) {
                spans = ArrayList<CharacterStyle?>()
            }

            val url = attributes.href

            spans.add(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    onLinkClicked(activity, url)
                }
            })
        }

        destination.add(HtmlRawElementStyledText(mText, spans))
    }

    override fun generate(
        activity: AppCompatActivity,
        destination: ArrayList<BodyElement?>
    ) {
        throw RuntimeException("Attempt to call generate() on reducible element")
    }
}
