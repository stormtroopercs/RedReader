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
package org.quantumbadger.redreader.common

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.common.AndroidCommon.runOnUiThread
import java.util.Objects
import java.util.concurrent.atomic.AtomicReference

object DialogUtils {
    fun showSearchDialog(
        context: Context,
        listener: OnSearchListener
    ) {
        showSearchDialog(context, string.action_search, listener)
    }

    fun showSearchDialog(
        context: Context,
        @StringRes titleRes: Int,
        listener: OnSearchListener
    ) {
        val alertBuilder = MaterialAlertDialogBuilder(context)
        val editTextRef = AtomicReference<EditText?>()

        alertBuilder.setView(R.layout.dialog_editbox)

        alertBuilder.setPositiveButton(
            string.action_search,
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                DialogUtils.performSearch(
                    editTextRef.get()!!,
                    listener
                )
            })

        alertBuilder.setNegativeButton(string.dialog_cancel, null)

        val alertDialog = alertBuilder.create()
        alertDialog.getWindow()!!
            .setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                        or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
            )
        alertDialog.show()

        val editText =             Objects.requireNonNull<TextInputEditText>(alertDialog.findViewById<TextInputEditText?>(R.id.editbox))

        val editTextLayout =             Objects.requireNonNull<TextInputLayout>(alertDialog.findViewById<TextInputLayout?>(R.id.editbox_layout))

        editTextRef.set(editText)

        val onEnter = OnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            performSearch(editText, listener)
            true
        }
        editText.setImeOptions(EditorInfo.IME_ACTION_SEARCH)
        editText.setOnEditorActionListener(onEnter)
        editText.requestFocus()

        editTextLayout.setHint(titleRes)
    }

    private fun performSearch(
        editText: EditText,
        listener: OnSearchListener
    ) {
        val query = editText.getText().toString().trim { it <= ' ' }
        if (StringUtils.isEmpty(query)) {
            listener.onSearch(null)
        } else {
            listener.onSearch(query)
        }
    }

    fun showDialogPositiveNegative(
        activity: AppCompatActivity,
        title: String,
        message: String,
        @StringRes positiveText: Int,
        @StringRes negativeText: Int,
        positiveAction: Runnable,
        negativeAction: Runnable
    ) {
        runOnUiThread(Runnable {
            MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(
                    positiveText,
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> positiveAction.run() })
                .setNegativeButton(
                    negativeText,
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> negativeAction.run() })
                .create()
                .show()
        })
    }

    fun showDialog(
        activity: Activity,
        title: String,
        message: String
    ) {
        runOnUiThread(Runnable {
            MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(
                    string.dialog_close,
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> })
                .create()
                .show()
        })
    }

    fun showDialog(
        activity: Activity,
        @StringRes title: Int,
        @StringRes message: Int
    ) {
        runOnUiThread(Runnable {
            MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(
                    string.dialog_close,
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> })
                .create()
                .show()
        })
    }

    fun interface OnSearchListener {
        fun onSearch(query: String?)
    }
}
