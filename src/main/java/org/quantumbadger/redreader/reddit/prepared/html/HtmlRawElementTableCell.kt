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
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElement
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElementTableCell

class HtmlRawElementTableCell(private val mChild: HtmlRawElementBlock) : HtmlRawElement() {
    override fun getPlainText(stringBuilder: StringBuilder) {
        mChild.getPlainText(stringBuilder)
    }

    override fun reduce(
        activeAttributes: HtmlTextAttributes,
        activity: AppCompatActivity,
        destination: ArrayList<HtmlRawElement>,
        linkButtons: ArrayList<LinkButtonDetails?>
    ) {
        destination.add(
            HtmlRawElementTableCell(
                mChild.reduce(
                    activeAttributes,
                    activity
                )
            )
        )
    }

    override fun generate(
        activity: AppCompatActivity,
        destination: ArrayList<BodyElement>
    ) {
        val elements = ArrayList<BodyElement>()
        mChild.generate(activity, elements)

        destination.add(BodyElementTableCell(elements))
    }
}
