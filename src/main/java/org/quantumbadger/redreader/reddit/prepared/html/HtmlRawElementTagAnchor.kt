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

import org.quantumbadger.redreader.common.UriString

class HtmlRawElementTagAnchor(
    children: ArrayList<HtmlRawElement>,
    private val mHref: UriString
) : HtmlRawElementTagAttributeChange(children) {
    override fun onLinkButtons(linkButtons: ArrayList<LinkButtonDetails>) {
        val text = plainText.trim { it <= ' ' }

        linkButtons.add(
            LinkButtonDetails(
                if (text.isEmpty()) null else text,
                mHref
            )
        )
    }

    override fun onStart(activeAttributes: HtmlTextAttributes) {
        activeAttributes.href = mHref
    }

    override fun onEnd(activeAttributes: HtmlTextAttributes) {
        activeAttributes.href = null
    }
}
