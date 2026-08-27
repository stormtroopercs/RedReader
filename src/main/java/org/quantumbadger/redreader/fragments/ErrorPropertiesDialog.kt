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
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.quantumbadger.redreader.fragments

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.BugReporter
import org.quantumbadger.redreader.common.BugReporter.appendException
import org.quantumbadger.redreader.common.RRError
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorPropertiesDialog private constructor(private val mError: RRError) : PropertiesDialog() {
    private var mContext: AppCompatActivity?=null

    override fun interceptBuilder(builder: MaterialAlertDialogBuilder) {
        if ((mError.t !is UnknownHostException) && (mError.t !is SocketTimeoutException) && mError.reportable) {
            builder.setPositiveButton(
                string.button_error_send_report,
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    BugReporter.sendBugReport(
                        mContext!!,
                        mError
                    )
                })
        }
    }

    override fun getTitle(context: Context): String {
        return context.getString(string.props_error_title)
    }

    override fun prepare(
        context: BaseActivity,
        items: LinearLayout
    ) {
        mContext = context

        items.addView(
            propView(
                context,
                string.props_title,
                getArguments()!!.getString("title"),
                true
            )
        )
        items.addView(
            propView(
                context,
                "Message",
                getArguments()!!.getString("message"),
                false
            )
        )

        if (getArguments()!!.containsKey("httpStatus")) {
            items.addView(
                propView(
                    context,
                    "HTTP status",
                    getArguments()!!.getString("httpStatus"),
                    false
                )
            )
        }

        if (getArguments()!!.containsKey("url")) {
            items.addView(
                propView(
                    context,
                    "URL",
                    getArguments()!!.getString("url"),
                    false
                )
            )
        }

        if (getArguments()!!.containsKey("t")) {
            items.addView(
                propView(
                    context,
                    "Exception",
                    getArguments()!!.getString("t"),
                    false
                )
            )
        }

        if (getArguments()!!.containsKey("response")) {
            items.addView(
                propView(
                    context,
                    "Response",
                    getArguments()!!.getString("response"),
                    false
                )
            )
        }
    }

    companion object {
        fun newInstance(error: RRError): ErrorPropertiesDialog {
            val dialog = ErrorPropertiesDialog(error)

            val args = Bundle()

            args.putString("title", error.title)
            args.putString("message", error.message)

            if (error.t != null) {
                val sb = StringBuilder(1024)
                appendException(sb, error.t, 10)
                args.putString("t", sb.toString())
            }

            if (error.httpStatus != null) {
                args.putString("httpStatus", error.httpStatus.toString())
            }

            if (error.url != null) {
                args.putString("url", error.url.value)
            }

            if (error.responseString != null) {
                args.putString("response", error.responseString)
            }

            dialog.setArguments(args)

            return dialog
        }
    }
}
