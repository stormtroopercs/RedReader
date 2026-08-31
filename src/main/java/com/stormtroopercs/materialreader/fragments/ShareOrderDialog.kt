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
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.BundleCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stormtroopercs.materialreader.R.string
import com.stormtroopercs.materialreader.adapters.ShareOrderAdapter
import com.stormtroopercs.materialreader.adapters.ShareOrderCallbackListener
import com.stormtroopercs.materialreader.common.General
import com.stormtroopercs.materialreader.common.PrefsUtility
import com.stormtroopercs.materialreader.common.StringUtils
import java.util.Arrays
import java.util.LinkedList

class ShareOrderDialog : AppCompatDialogFragment(), ShareOrderCallbackListener {
    private var packageManager: PackageManager?=null
    private var shareIntent: Intent?=null
    private var orderedAppList: MutableList<ResolveInfo>? = null
    private var context: Context?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = getContext()
        packageManager = getActivity()!!.getPackageManager()
        shareIntent =             BundleCompat.getParcelable<Intent?>(requireArguments(), "intent", Intent::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        orderedAppList = prioritizeTopApps(
            packageManager!!.queryIntentActivities(
                shareIntent!!,
                0
            )
        )

        val builder = MaterialAlertDialogBuilder(context!!)
        builder.setTitle(
            context!!.getString(
                string.pref_behaviour_sharing_share_dialog_dialogtitle
            )
        )
        val listView = ListView(context)
        builder.setView(listView)
        listView.setAdapter(ShareOrderAdapter(context!!, orderedAppList!!, this))

        return builder.create()
    }

    private fun prioritizeTopApps(unorderedList: MutableList<ResolveInfo>): MutableList<ResolveInfo> {
        if (unorderedList.isEmpty()) {
            General.quickToast(context!!, string.error_toast_no_share_app_installed)
            dismiss()
        }

        // Make a copy of the list since the original is not modifiable
        val orderedList = LinkedList<ResolveInfo>(unorderedList)

        val prioritizedAppNames =             Arrays.asList<String?>(
                *PrefsUtility.pref_behaviour_sharing_dialog_data_get()!!
                    .split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            )
        val prioritizedApps = arrayOfNulls<ResolveInfo>(prioritizedAppNames.size)

        // get the ResolveInfos for the available prioritized Apps and save them in order
        var count = 0
        val iterator: MutableIterator<ResolveInfo> = orderedList.iterator()
        while (iterator.hasNext()) {
            val currentApp = iterator.next()
            val currentAppName = currentApp.activityInfo.name
            if (prioritizedAppNames.contains(currentAppName)) {
                prioritizedApps[prioritizedAppNames.indexOf(currentAppName)] = currentApp
                iterator.remove()
                // Exit early if all apps matched
                if (++count >= prioritizedAppNames.size) {
                    break
                }
            }
        }

        // Combine the two lists in order, respecting unavailable apps (null values in the Array)
        for (i in prioritizedApps.indices.reversed()) {
            if (prioritizedApps[i] != null) {
                orderedList.addFirst(prioritizedApps[i]!!)
            }
        }

        return orderedList
    }

    override fun onSelectedIntent(position: Int) {
        val info = orderedAppList!!.get(position).activityInfo
        persistPriority(info)
        shareIntent!!.addCategory(Intent.CATEGORY_LAUNCHER)
        shareIntent!!.setClassName(info.applicationInfo.packageName, info.name)
        shareIntent!!.setFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        )
        startActivity(shareIntent!!)
    }

    private fun persistPriority(selectedApplication: ActivityInfo) {
        val priorityAppList =             LinkedList<String?>(
                Arrays.asList<String?>(
                    *PrefsUtility.pref_behaviour_sharing_dialog_data_get()!!
                        .split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                )
            )
        priorityAppList.remove(selectedApplication.name)
        priorityAppList.add(0, selectedApplication.name)
        if (priorityAppList.size > amountOfPrioritizedApps) {
            priorityAppList.removeLast()
        }

        PrefsUtility.pref_behaviour_sharing_dialog_data_set(
            context!!,
            StringUtils.join(priorityAppList, ";")
        )
    }

    companion object {
        private const val amountOfPrioritizedApps = 3
        fun newInstance(shareIntent: Intent?): ShareOrderDialog {
            val dialog = ShareOrderDialog()

            val args = Bundle(1)
            args.putParcelable("intent", shareIntent)
            dialog.setArguments(args)

            return dialog
        }
    }
}
