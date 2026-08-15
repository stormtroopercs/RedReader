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

import android.annotation.SuppressLint
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.account.RedditAccountChangeListener
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.SessionChangeListener.SessionChangeType
import org.quantumbadger.redreader.adapters.SessionListAdapter
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.UriString
import java.util.UUID
import kotlin.concurrent.Volatile

class SessionListDialog : AppCompatDialogFragment(), RedditAccountChangeListener {
    private var url: UriString?=null
    private var current: UUID?=null
    private var type: SessionChangeType?=null

    private var rv: RecyclerView?=null

    // Workaround for HoloEverywhere bug?
    @Volatile
    private var alreadyCreated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        url = UriString(getArguments()!!.getString("url")!!)

        if (getArguments()!!.containsKey("current")) {
            current = UUID.fromString(getArguments()!!.getString("current"))
        } else {
            current = null
        }

        type = SessionChangeType.valueOf(
            getArguments()!!.getString(
                "type"
            )!!
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        if (alreadyCreated) {
            return getDialog()!!
        }
        alreadyCreated = true

        val context = getContext()

        val builder = MaterialAlertDialogBuilder(context!!)
        builder.setTitle(context.getString(string.options_past))

        rv = RecyclerView(context)
        builder.setView(rv)

        rv!!.setLayoutManager(LinearLayoutManager(context))
        rv!!.setAdapter(SessionListAdapter(context, url, current, type, this))
        rv!!.setHasFixedSize(true)

        RedditAccountManager.Companion.getInstance(context).addUpdateListener(this)

        builder.setNeutralButton(context.getString(string.dialog_close), null)

        return builder.create()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onRedditAccountChanged() {
        AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
            rv!!.getAdapter()!!.notifyDataSetChanged()
        })
    }

    companion object {
        fun newInstance(
            url: Uri,
            current: UUID?,
            type: SessionChangeType
        ): SessionListDialog {
            val dialog = SessionListDialog()

            val args = Bundle(3)
            args.putString("url", url.toString())
            if (current != null) {
                args.putString("current", current.toString())
            }
            args.putString("type", type.name)
            dialog.setArguments(args)

            return dialog
        }
    }
}
