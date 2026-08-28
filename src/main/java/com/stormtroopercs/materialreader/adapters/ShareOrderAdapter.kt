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
package com.stormtroopercs.materialreader.adapters

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import com.stormtroopercs.materialreader.R

class ShareOrderAdapter(
    private val context: Context,
    private val appList: MutableList<ResolveInfo>,
    private val fragment: AppCompatDialogFragment
) : BaseAdapter() {
    private val packageManager: PackageManager

    init {
        this.packageManager = context.getPackageManager()
    }

    override fun getCount(): Int {
        return appList.size
    }

    override fun getItem(position: Int): Any? {
        return appList.get(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        var rowView: View?=null
        if (inflater != null) {
            rowView = inflater.inflate(R.layout.list_item_share_dialog, parent, false)
            val label = rowView.findViewById<TextView>(R.id.list_item_share_dialog_text)
            label.setText(appList.get(position)!!.loadLabel(packageManager).toString())
            val icon = rowView.findViewById<ImageView>(R.id.list_item_share_dialog_icon)
            icon.setImageDrawable(appList.get(position)!!.loadIcon(packageManager))
            val divider = rowView.findViewById<View>(R.id.list_item_share_dialog_divider)
            divider.setVisibility(View.INVISIBLE)

            rowView.setOnClickListener(View.OnClickListener { v: View? ->
                (fragment as ShareOrderCallbackListener).onSelectedIntent(position)
                fragment.dismiss()
            })
        }

        return rowView
    }
}
