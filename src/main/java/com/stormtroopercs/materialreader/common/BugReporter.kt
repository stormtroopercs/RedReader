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

package com.stormtroopercs.materialreader.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.stormtroopercs.materialreader.R
import com.stormtroopercs.materialreader.activities.MainActivityCompose
import com.stormtroopercs.materialreader.common.General.quickToast
import java.util.LinkedList

/**
 * App-wide global error collection + bug-report generation, extracted from
 * the companion of the (now-retired) BugReportActivity in the 40th increment —
 * it is referenced from cache / network / account / settings code all over the
 * app and had to outlive the Activity.
 *
 * [handleGlobalError] collects the error and opens the Compose bug-report
 * screen: it starts [MainActivityCompose] with the `bug_report` deep link
 * (Settings + BugReport), the in-app route the legacy activity used to open
 * on its own.
 */
object BugReporter {
    private val errors = ArrayList<RRError?>()

    @Synchronized
    fun addGlobalError(error: RRError?) {
        errors.add(error)
    }

    @Synchronized
    fun handleGlobalError(context: Context, text: String?) {
        handleGlobalError(context, RRError(text, null, true, RuntimeException()))
    }

    @Synchronized
    fun handleGlobalError(context: Context, t: Throwable?) {
        if (t != null) {
            Log.e("BugReporter", "Handling exception", t)
        }
        handleGlobalError(context, RRError(null, null, true, t))
    }

    @Synchronized
    fun handleGlobalError(context: Context, error: RRError?) {
        addGlobalError(error)
        AndroidCommon.UI_THREAD_HANDLER.post {
            val intent = Intent(context, MainActivityCompose::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra(MainActivityCompose.EXTRA_DEEP_LINK, MainActivityCompose.DEEP_LINK_BUG_REPORT)
            context.startActivity(intent)
        }
    }

    @Synchronized
    fun getErrors(): LinkedList<RRError> {
        val result = LinkedList<RRError>(errors)
        errors.clear()
        return result
    }

    fun sendBugReport(context: Context, error: RRError) {
        sendBugReport(context, listOf(error))
    }

    fun sendBugReport(context: Context, errors: Iterable<RRError>) {
        val sb = StringBuilder(1024)
        sb.append("Error report -- MaterialReader v")
            .append(Constants.version(context))
            .append("\r\n\r\n")

        sb.append("Manufacturer: ").append(Build.MANUFACTURER).append("\r\n")
        sb.append("Model: ").append(Build.MODEL).append("\r\n")
        sb.append("Product: ").append(Build.PRODUCT).append("\r\n")
        sb.append("Android release: ").append(Build.VERSION.RELEASE).append("\r\n")
        sb.append("Android SDK: ").append(Build.VERSION.SDK_INT).append("\r\n")

        for (error in errors) {
            sb.append("\r\n-------------------------------\r\n")
            if (error.title != null) {
                sb.append("Title: ").append(error.title).append("\r\n")
            }
            if (error.message != null) {
                sb.append("Message: ").append(error.message).append("\r\n")
            }
            if (error.httpStatus != null) {
                sb.append("HTTP Status: ").append(error.httpStatus).append("\r\n")
            }
            if (error.url != null) {
                sb.append("URL: ").append(error.url).append("\r\n")
            }
            if (error.debuggingContext != null) {
                sb.append("Debugging context: ").append(error.debuggingContext).append("\r\n")
            }
            if (error.responseString != null) {
                sb.append("Response: ").append(error.responseString).append("\r\n")
            }
            appendException(sb, error.t, 25)
        }

        val intent = Intent(Intent.ACTION_SENDTO)
        intent.putExtra(
            Intent.EXTRA_EMAIL,
            arrayOf<String>(
                "bugreports" + 64.toChar() + "redreader" + '.' + "org"
            )
        )
        intent.putExtra(Intent.EXTRA_SUBJECT, "Bug Report")
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString())

        val emailSelectorIntent = Intent(Intent.ACTION_SENDTO)
        emailSelectorIntent.setData(Uri.parse("mailto:"))
        intent.setSelector(emailSelectorIntent)

        try {
            context.startActivity(
                Intent.createChooser(
                    intent,
                    context.getApplicationContext().getString(R.string.bug_chooser_title)
                )
            )
        } catch (ex: ActivityNotFoundException) {
            quickToast(context, R.string.error_toast_no_email_apps)
        }
    }

    fun appendException(sb: StringBuilder, t: Throwable?, recurseLimit: Int) {
        if (t != null) {
            sb.append("Exception: ")
            sb.append(t.javaClass.getCanonicalName()).append("\r\n")
            sb.append(t.message).append("\r\n")
            for (elem in t.getStackTrace()) {
                sb.append("  ").append(elem.toString()).append("\r\n")
            }
            if (recurseLimit > 0 && t.cause != null) {
                sb.append("Caused by: ")
                appendException(sb, t.cause, recurseLimit - 1)
            }
        }
    }
}
