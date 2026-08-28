/*******************************************************************************
 * This file is part of MaterialReader.
 *
 * MaterialReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MaterialReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MaterialReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.stormtroopercs.materialreader.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.R
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.stormtroopercs.materialreader.R.string
import com.stormtroopercs.materialreader.activities.BaseActivity
import com.stormtroopercs.materialreader.common.General
import com.stormtroopercs.materialreader.common.General.dpToPixels
import kotlin.concurrent.Volatile

abstract class PropertiesDialog : AppCompatDialogFragment() {
    protected var colorPrimary: Int = 0
    protected var rrCommentBodyCol: Int = 0

    // Workaround for HoloEverywhere bug?
    @Volatile
    private var alreadyCreated = false

    protected abstract fun getTitle(context : Context): String?

    protected abstract fun prepare(
        context: BaseActivity,
        items: LinearLayout
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        if (alreadyCreated) {
            return getDialog()!!
        }
        alreadyCreated = true

        val activity = getActivity() as BaseActivity?

        val attr = activity!!.obtainStyledAttributes(
            intArrayOf(
                R.attr.colorPrimary,
                com.stormtroopercs.materialreader.R.attr.rrMainTextCol
            )
        )

        colorPrimary = attr.getColor(0, 0)
        rrCommentBodyCol = attr.getColor(1, 0)

        attr.recycle()

        val builder = MaterialAlertDialogBuilder(activity)

        val items = LinearLayout(activity)
        items.setOrientation(LinearLayout.VERTICAL)

        val hPaddingPx = General.dpToPixels(activity, 12f)
        items.setPadding(hPaddingPx, 0, hPaddingPx, 0)

        prepare(activity, items)
        builder.setTitle(getTitle(activity))

        val sv = ScrollView(activity)
        sv.addView(items)
        builder.setView(sv)

        builder.setNeutralButton(string.dialog_close, null)

        interceptBuilder(builder)

        return builder.create()
    }

    protected open fun interceptBuilder(builder: MaterialAlertDialogBuilder) {
        // Do nothing by default
    }

    protected fun propView(
        context: Context,
        titleRes: Int,
        textRes: Int,
        firstInList: Boolean
    ): LinearLayout {
        return propView(
            context,
            context.getString(titleRes),
            getString(textRes),
            firstInList
        )
    }

    protected fun propView(
        context: Context,
        titleRes: Int,
        text: CharSequence?,
        firstInList: Boolean
    ): LinearLayout {
        return propView(context, context.getString(titleRes), text, firstInList)
    }

    // TODO xml?
    protected fun propView(
        context: Context,
        title: String?,
        text: CharSequence?,
        firstInList: Boolean
    ): LinearLayout {
        val paddingPixels = dpToPixels(context, 12f)

        val prop = LinearLayout(context)
        prop.setOrientation(LinearLayout.VERTICAL)

        val titleView: TextView = MaterialTextView(context)
        titleView.setText(title)
        titleView.setTextColor(colorPrimary)
        titleView.setTextSize(14.0f)
        titleView.setPadding(paddingPixels, paddingPixels, paddingPixels, 0)
        prop.addView(titleView)

        val textView: TextView = MaterialTextView(context)
        textView.setText(if (text == null) "<null>" else text)
        textView.setTextColor(rrCommentBodyCol)
        textView.setTextSize(16.0f)
        textView.setPadding(paddingPixels, 0, paddingPixels, 0)
        textView.setTextIsSelectable(true)
        textView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
        prop.addView(textView)

        prop.setContentDescription(title + "\n" + text)

        return prop
    }
}
