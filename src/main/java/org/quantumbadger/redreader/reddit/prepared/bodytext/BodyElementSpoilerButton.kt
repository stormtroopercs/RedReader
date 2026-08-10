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

import android.content.DialogInterface
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.General.dpToPixels

class BodyElementSpoilerButton(
    activity: AppCompatActivity,
    private val mSpoilerText: BodyElement
) : BodyElementBaseButton(
    activity.getApplicationContext().getString(
        string.spoiler
    ), null, false
) {
    protected override fun generateOnClickListener(
        activity: BaseActivity,
        textColor: Int?,
        textSize: Float?,
        showLinkButtons: Boolean
    ): View.OnClickListener {
        return View.OnClickListener { button: View? ->
            val scrollView = ScrollView(activity)
            val view = mSpoilerText.generateView(
                activity,
                textColor,
                textSize,
                true
            )

            scrollView.addView(view)

            val layoutParams: MarginLayoutParams=view.getLayoutParams() as FrameLayout.LayoutParams

            val marginPx = dpToPixels(activity, 14f)
            layoutParams.setMargins(marginPx, marginPx, marginPx, marginPx)

            val builder = MaterialAlertDialogBuilder(activity)
            builder.setView(scrollView)

            builder.setNeutralButton(
                string.dialog_close,
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> })

            val alert = builder.create()
            alert.show()
        }
    }

    protected override fun generateOnLongClickListener(
        activity: BaseActivity,
        textColor: Int?,
        textSize: Float?,
        showLinkButtons: Boolean
    ): OnLongClickListener? {
        return null
    }
}
