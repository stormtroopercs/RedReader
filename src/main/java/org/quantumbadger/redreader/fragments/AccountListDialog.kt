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

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.account.RedditAccountChangeListener
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.adapters.AccountListAdapter
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.AndroidCommon.promptForNotificationPermission
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.RunnableOnce
import org.quantumbadger.redreader.reddit.api.RedditOAuth
import kotlin.concurrent.Volatile

class AccountListDialog private constructor() : AppCompatDialogFragment(),
    RedditAccountChangeListener {
    private var mActivity: AppCompatActivity?=null

    // Workaround for HoloEverywhere bug?
    @Volatile
    private var alreadyCreated = false

    private var rv: RecyclerView?=null

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent
    ) {
        if (requestCode == 123 && requestCode == resultCode && data.hasExtra("url")) {
            val uri = Uri.parse(data.getStringExtra("url"))
            RedditOAuth.completeLogin(mActivity!!, uri, RunnableOnce.DO_NOTHING)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        if (alreadyCreated) {
            return getDialog()!!
        }
        alreadyCreated = true

        mActivity = getActivity() as AppCompatActivity?

        val builder = MaterialAlertDialogBuilder(mActivity!!)
        builder.setTitle(mActivity!!.getString(R.string.options_accounts_long))

        rv = RecyclerView(mActivity!!)

        rv!!.setLayoutManager(LinearLayoutManager(mActivity))
        rv!!.setAdapter(AccountListAdapter(mActivity!!, this))
        rv!!.setHasFixedSize(true)

        val paddingPx = General.dpToPixels(mActivity!!, 16f)
        rv!!.setPadding(paddingPx, paddingPx, paddingPx, 0)

        RedditAccountManager.getInstance(mActivity).addUpdateListener(this)

        builder.setNeutralButton(mActivity!!.getString(R.string.dialog_close), null)

        builder.setView(rv)
        return builder.create()
    }

    override fun onRedditAccountChanged() {
        AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
            rv!!.setAdapter(AccountListAdapter(mActivity!!, this))
            if (mActivity is BaseActivity) {
                promptForNotificationPermission(mActivity as BaseActivity, null)
            }
        })
    }

    companion object {
        @JvmStatic
        fun show(activity: AppCompatActivity) {
            AccountListDialog().show(
                activity.getSupportFragmentManager(),
                null
            )
        }
    }
}
