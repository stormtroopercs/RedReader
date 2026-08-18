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

abstract class HtmlRawElementTagAttributeChange(private val mChildren: ArrayList<HtmlRawElement>) :
    HtmlRawElementTag() {
    protected open fun onLinkButtons(linkButtons: ArrayList<LinkButtonDetails?>) {
        // Add nothing by default
    }

    protected abstract fun onStart(activeAttributes: HtmlTextAttributes)

    protected abstract fun onEnd(activeAttributes: HtmlTextAttributes)

    override fun getPlainText(stringBuilder: StringBuilder) {
        for (element in mChildren) {
            element.getPlainText(stringBuilder)
        }
    }

    override fun reduce(
        activeAttributes: HtmlTextAttributes,
        activity: AppCompatActivity,
        destination: ArrayList<HtmlRawElement>,
        linkButtons: ArrayList<LinkButtonDetails?>
    ) {
        onStart(activeAttributes)

        try {
            for (child in mChildren) {
                child.reduce(activeAttributes, activity, destination, linkButtons)
            }
        } finally {
            onEnd(activeAttributes)
        }

        onLinkButtons(linkButtons)
    }

    override fun generate(
        activity: AppCompatActivity,
        destination: ArrayList<BodyElement>
    ) {
        throw RuntimeException("Attempt to call generate() on reducible element")
    }
}
