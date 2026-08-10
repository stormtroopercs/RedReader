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
package org.quantumbadger.redreader.reddit.prepared.bodytext

import android.view.View
import android.view.View.OnLongClickListener
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.common.LinkHandler.onLinkLongClicked
import org.quantumbadger.redreader.reddit.prepared.html.HtmlRawElement.LinkButtonDetails

class BodyElementLinkButton(private val mDetails: LinkButtonDetails) : BodyElementBaseButton(
    mDetails.getButtonTitle(), mDetails.getButtonSubtitle(), true
) {
    protected override fun generateOnClickListener(
        activity: BaseActivity,
        textColor: Int?,
        textSize: Float?,
        showLinkButtons: Boolean
    ): View.OnClickListener {
        return View.OnClickListener { button: View? ->
            onLinkClicked(
                activity,
                mDetails.url,
                false
            )
        }
    }

    protected override fun generateOnLongClickListener(
        activity: BaseActivity,
        textColor: Int?,
        textSize: Float?,
        showLinkButtons: Boolean
    ): OnLongClickListener? {
        return OnLongClickListener { button: View? ->
            onLinkLongClicked(activity, mDetails.url)
            true
        }
    }
}
