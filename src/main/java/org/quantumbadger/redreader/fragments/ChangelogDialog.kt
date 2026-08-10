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
package org.quantumbadger.redreader.fragments

import android.content.Context
import android.widget.LinearLayout
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.ChangelogManager

class ChangelogDialog : PropertiesDialog() {
    override fun getTitle(context: Context): String {
        return context.getString(string.title_changelog)
    }

    override fun prepare(
        context: BaseActivity,
        items: LinearLayout
    ) {
        ChangelogManager.generateViews(context, items, false)
    }

    companion object {
        fun newInstance(): ChangelogDialog {
            return ChangelogDialog()
        }
    }
}
