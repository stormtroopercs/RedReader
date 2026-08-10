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
package org.quantumbadger.redreader.viewholders

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.RecyclerView
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.RRThemeAttributes
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElementLinkButton
import org.quantumbadger.redreader.reddit.prepared.html.HtmlRawElement.LinkButtonDetails

/**
 * A view holder for a three line, text and icon list item, which can have link buttons.
 */
class VH3TextIcon(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val textHoldingLayout: LinearLayout?

    val text: TextView?
    val text2: TextView?
    val text3: TextView?
    val icon: ImageView?
    val extra: LinearLayoutCompat

    var bindingId: Long = 0

    init {
        textHoldingLayout = itemView.findViewById<LinearLayout?>(R.id.recycler_text_layout)

        text = itemView.findViewById<TextView?>(R.id.recycler_item_text)
        text2 = itemView.findViewById<TextView?>(R.id.recycler_item_2_text)
        text3 = itemView.findViewById<TextView?>(R.id.recycler_item_3_text)
        icon = itemView.findViewById<ImageView?>(R.id.recycler_item_icon)
        extra = itemView.findViewById<LinearLayoutCompat>(R.id.recycler_item_extra)
    }

    fun removeExtras() {
        extra.removeAllViews()
    }

    fun addLinkButton(activity: BaseActivity, url: UriString) {
        val linkButton = BodyElementLinkButton(LinkButtonDetails(url.value, url))

        val linkButtonView =             linkButton.generateView(
                activity,
                RRThemeAttributes(activity.getApplicationContext()).rrCommentBodyCol,
                13.0f,
                true
            )

        extra.addView(linkButtonView)
    }
}
