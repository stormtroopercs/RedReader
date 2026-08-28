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

import android.content.Context
import android.os.Bundle
import android.widget.LinearLayout
import com.stormtroopercs.materialreader.R.string
import com.stormtroopercs.materialreader.activities.BaseActivity
import com.stormtroopercs.materialreader.common.General.dpToPixels
import com.stormtroopercs.materialreader.reddit.prepared.markdown.MarkdownParser
import com.stormtroopercs.materialreader.common.General

class MarkdownPreviewDialog : PropertiesDialog() {
    override fun getTitle(context: Context): String {
        return context.getString(string.comment_reply_preview)
    }

    override fun prepare(
        activity: BaseActivity,
        items: LinearLayout
    ) {
        val parsedGen = MarkdownParser.parse(
            getArguments()!!.getString("markdown")!!
                .toCharArray()
        )

        val parsed = parsedGen.buildView(activity, null, 14f, false)

        val paddingPx = dpToPixels(activity, 10f)
        parsed.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

        items.addView(parsed)
    }

    companion object {
        fun newInstance(markdown: String?): MarkdownPreviewDialog {
            val dialog = MarkdownPreviewDialog()

            val args = Bundle(1)
            args.putString("markdown", markdown)
            dialog.setArguments(args)

            return dialog
        }
    }
}
