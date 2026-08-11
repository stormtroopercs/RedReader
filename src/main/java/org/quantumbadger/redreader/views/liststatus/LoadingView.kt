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
package org.quantumbadger.redreader.views.liststatus

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.widget.LinearLayout
import android.widget.TextView
import org.quantumbadger.redreader.R.string
import java.util.Locale

class LoadingView(
    context: Context?,
    initialText: String,
    progressBarEnabled: Boolean,
    indeterminate: Boolean
) : StatusListItemView(context) {
    private val textView: TextView?

    private val loadingHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (textView != null) {
                textView.setText((msg.obj as String).uppercase(Locale.getDefault()))
            }

            if (msg.what == LOADING_DONE) {
                hideNoAnim()
            }
        }
    }

    fun setIndeterminate(textRes: Int) {
        sendMessage(getContext().getString(textRes), LOADING_INDETERMINATE)
    }

    fun setProgress(textRes: Int, fraction: Float) {
        sendMessage(getContext().getString(textRes), Math.round(fraction * 100))
    }

    fun setDone(textRes: Int) {
        sendMessage(getContext().getString(textRes), LOADING_DONE)
    }

    private fun sendMessage(text: String?, what: Int) {
        val msg = Message.obtain()
        msg.obj = text
        msg.what = what
        loadingHandler.sendMessage(msg)
    }

    @JvmOverloads
    constructor(
        context: Context,
        initialTextRes: Int = string.download_waiting,
        progressBarEnabled: Boolean = true,
        indeterminate: Boolean = true
    ) : this(
        context,
        context.getString(initialTextRes),
        progressBarEnabled,
        indeterminate
    )

    init {
        val layout = LinearLayout(context)
        layout.setOrientation(LinearLayout.VERTICAL)

        textView = TextView(context)
        textView.setText(initialText.uppercase(Locale.getDefault()))
        textView.setTextSize(13.0f)
        textView.setPadding(
            (15 * dpScale).toInt(),
            (10 * dpScale).toInt(),
            (10 * dpScale).toInt(),
            (10 * dpScale).toInt()
        )
        textView.setSingleLine(true)
        textView.setEllipsize(TextUtils.TruncateAt.END)
        layout.addView(textView)

        setContents(layout)
    }

    companion object {
        @Suppress("PropertyName")
        private val LOADING_INDETERMINATE = -1
        @Suppress("PropertyName")
        private val LOADING_DONE = -2
    }
}
