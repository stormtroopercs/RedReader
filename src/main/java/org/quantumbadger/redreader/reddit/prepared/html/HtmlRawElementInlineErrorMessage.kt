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

import android.graphics.Color
import android.text.style.BackgroundColorSpan
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan

object HtmlRawElementInlineErrorMessage : HtmlRawElement() {
    fun create(text: String): HtmlRawElementStyledText {
        val spans = ArrayList<CharacterStyle?>()
        spans.add(BackgroundColorSpan(Color.RED))
        spans.add(ForegroundColorSpan(Color.WHITE))

        return HtmlRawElementStyledText(text, spans)
    }

    fun appendError(
        text: String,
        element: HtmlRawElement
    ): HtmlRawElementTagPassthrough {
        val children = ArrayList<HtmlRawElement?>()

        children.add(element)
        children.add(create(text))

        return HtmlRawElementTagPassthrough(children)
    }
}
