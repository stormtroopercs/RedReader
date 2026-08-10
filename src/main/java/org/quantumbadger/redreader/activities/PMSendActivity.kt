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
package org.quantumbadger.redreader.activities

import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.RedReader.Companion.getInstance
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.BugReportActivity.Companion.handleGlobalError
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.General.quickToast
import org.quantumbadger.redreader.common.General.safeDismissDialog
import org.quantumbadger.redreader.common.General.showResultDialog
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.fragments.AccountListDialog.Companion.show
import org.quantumbadger.redreader.fragments.MarkdownPreviewDialog
import org.quantumbadger.redreader.fragments.ReportDialog.Companion.show
import org.quantumbadger.redreader.reddit.APIResponseHandler.ActionResponseHandler
import org.quantumbadger.redreader.reddit.RedditAPI

class PMSendActivity : ViewsBaseActivity() {
    private var usernameSpinner: Spinner?=null
    private var recipientEdit: EditText?=null
    private var subjectEdit: EditText?=null
    private var textEdit: EditText?=null

    private var mSendSuccess = false

    protected override fun onCreate(savedInstanceState: Bundle?) {
        PrefsUtility.applyTheme(this)

        super.onCreate(savedInstanceState)

        setTitle(string.pm_send_actionbar)

        val layout = getLayoutInflater().inflate(R.layout.pm_send, null) as LinearLayout

        usernameSpinner = layout.findViewById<Spinner>(R.id.pm_send_username)
        recipientEdit = layout.findViewById<EditText>(R.id.pm_send_recipient)
        subjectEdit = layout.findViewById<EditText>(R.id.pm_send_subject)
        textEdit = layout.findViewById<EditText?>(R.id.pm_send_text)

        val initialRecipient: String?
        val initialSubject: String?
        val initialText: String?

        if (savedInstanceState != null
            && savedInstanceState.containsKey(SAVED_STATE_TEXT)
        ) {
            initialRecipient = savedInstanceState.getString(SAVED_STATE_RECIPIENT)
            initialSubject = savedInstanceState.getString(SAVED_STATE_SUBJECT)
            initialText = savedInstanceState.getString(SAVED_STATE_TEXT)
        } else {
            val intent = getIntent()

            if (intent != null && intent.hasExtra(EXTRA_RECIPIENT)) {
                initialRecipient = intent.getStringExtra(EXTRA_RECIPIENT)
            } else {
                initialRecipient = lastRecipient
            }

            if (intent != null && intent.hasExtra(EXTRA_SUBJECT)) {
                initialSubject = intent.getStringExtra(EXTRA_SUBJECT)
            } else {
                initialSubject = lastSubject
            }

            if (intent != null && intent.hasExtra(EXTRA_TEXT)) {
                initialText = intent.getStringExtra(EXTRA_TEXT)
            } else {
                initialText = lastText
            }
        }

        if (initialRecipient != null) {
            recipientEdit!!.setText(initialRecipient)
        }

        if (initialSubject != null) {
            subjectEdit!!.setText(initialSubject)
        }

        if (initialText != null) {
            textEdit!!.setText(initialText)
        }

        val accounts: ArrayList<RedditAccount> = RedditAccountManager.Companion.getInstance(this)
            .getAccounts()
        val usernames = ArrayList<String?>()

        for (account in accounts) {
            if (!account.isAnonymous) {
                usernames.add(account.username)
            }
        }

        if (usernames.isEmpty()) {
            quickToast(this, getString(string.error_toast_notloggedin))
            finish()
        }

        usernameSpinner!!.setAdapter(
            ArrayAdapter<String?>(
                this,
                android.R.layout.simple_list_item_1,
                usernames
            )
        )

        val sv = ScrollView(this)
        sv.addView(layout)
        setBaseActivityListing(sv)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SAVED_STATE_RECIPIENT, recipientEdit!!.getText().toString())
        outState.putString(SAVED_STATE_SUBJECT, subjectEdit!!.getText().toString())
        outState.putString(SAVED_STATE_TEXT, textEdit!!.getText().toString())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val send = menu.add(string.comment_reply_send)
        send.setIcon(R.drawable.ic_action_send_dark)
        send.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        menu.add(string.comment_reply_preview)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getTitle() == getString(string.comment_reply_send)) {
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle(getString(string.comment_reply_submitting_title))
            progressDialog.setMessage(getString(string.comment_reply_submitting_message))
            progressDialog.setIndeterminate(true)
            progressDialog.setCancelable(true)
            progressDialog.setCanceledOnTouchOutside(false)

            progressDialog.setOnCancelListener(DialogInterface.OnCancelListener { dialogInterface: DialogInterface? ->
                quickToast(this, getString(string.comment_reply_oncancel))
                safeDismissDialog(progressDialog)
            })

            progressDialog.setOnKeyListener(DialogInterface.OnKeyListener { dialogInterface: DialogInterface?, keyCode: Int, keyEvent: KeyEvent? ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    quickToast(this, getString(string.comment_reply_oncancel))
                    safeDismissDialog(progressDialog)
                }
                true
            })

            val handler: ActionResponseHandler=object : ActionResponseHandler(this) {
                override fun onSuccess() {
                    AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                        safeDismissDialog(progressDialog)
                        mSendSuccess = true

                        lastText = null
                        lastRecipient = null
                        lastSubject = null

                        quickToast(
                            this@PMSendActivity,
                            getString(string.pm_send_done)
                        )
                        finish()
                    })
                }

                protected override fun onCallbackException(t: Throwable?) {
                    handleGlobalError(this@PMSendActivity, t)
                }

                protected override fun onFailure(error: RRError) {
                    showResultDialog(this@PMSendActivity, error)
                    safeDismissDialog(progressDialog)
                }
            }

            val cm: CacheManager = CacheManager.Companion.getInstance(this)

            val accounts: ArrayList<RedditAccount> = RedditAccountManager.Companion.getInstance(
                this
            ).getAccounts()
            var selectedAccount: RedditAccount?=null

            for (account in accounts) {
                if (!account.isAnonymous
                    && account.username.equals(
                        usernameSpinner!!.getSelectedItem() as String?, ignoreCase = true
                    )
                ) {
                    selectedAccount = account
                    break
                }
            }

            if (selectedAccount == null) {
                throw RuntimeException("Selected account no longer present")
            }

            RedditAPI.compose(
                cm,
                handler,
                selectedAccount,
                recipientEdit!!.getText().toString(),
                subjectEdit!!.getText().toString(),
                textEdit!!.getText().toString(),
                this
            )

            progressDialog.show()
        } else if (item.getTitle() == getString(string.comment_reply_preview)) {
            MarkdownPreviewDialog.Companion.newInstance(textEdit!!.getText().toString())
                .show(getSupportFragmentManager(), "MarkdownPreviewDialog")
        }

        return true
    }

    protected override fun onDestroy() {
        super.onDestroy()

        if (!mSendSuccess && textEdit != null) {
            lastRecipient = recipientEdit!!.getText().toString()
            lastSubject = subjectEdit!!.getText().toString()
            lastText = textEdit!!.getText().toString()
        }
    }

    companion object {
        const val EXTRA_RECIPIENT: String = "recipient"
        const val EXTRA_SUBJECT: String = "subject"
        const val EXTRA_TEXT: String = "text"

        private const val SAVED_STATE_RECIPIENT = "recipient"
        private const val SAVED_STATE_TEXT = "pm_text"
        private const val SAVED_STATE_SUBJECT = "pm_subject"

        private var lastText: String?=null
        private var lastRecipient: String?=null
        private var lastSubject: String?=null
    }
}
