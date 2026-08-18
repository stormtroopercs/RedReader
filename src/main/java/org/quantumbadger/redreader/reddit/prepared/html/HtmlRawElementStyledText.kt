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

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.CharacterStyle
import androidx.appcompat.app.AppCompatActivity
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElement

class HtmlRawElementStyledText(
    private val mText: String,
    private val mSpans: ArrayList<CharacterStyle?>?
) : HtmlRawElement() {
    override fun getPlainText(stringBuilder: StringBuilder) {
        stringBuilder.append(mText)
    }

    fun writeTo(ssb: SpannableStringBuilder) {
        val textStart = ssb.length
        ssb.append(mText)
        val textEnd = ssb.length

        if (mSpans != null) {
            for (span in mSpans) {
                ssb.setSpan(span, textStart, textEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }
    }

    override fun reduce(
        activeAttributes: HtmlTextAttributes,
        activity: AppCompatActivity,
        destination: ArrayList<HtmlRawElement>,
        linkButtons: ArrayList<LinkButtonDetails?>
    ) {
        destination.add(this)
    }

    override fun generate(
        activity: AppCompatActivity,
        destination: ArrayList<BodyElement>
    ) {
        throw RuntimeException(
            "Attempt to call generate() on styled text: should be inside a block"
        )
    }
}
