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
import android.widget.EditText
import android.widget.ScrollView
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.General.quickToast
import org.quantumbadger.redreader.common.General.safeDismissDialog
import org.quantumbadger.redreader.common.General.showResultDialog
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.fragments.MarkdownPreviewDialog
import org.quantumbadger.redreader.reddit.APIResponseHandler.ActionResponseHandler
import org.quantumbadger.redreader.reddit.RedditAPI
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import org.quantumbadger.redreader.common.General

class CommentEditActivity : ViewsBaseActivity() {
    private var textEdit: EditText?=null

    private var commentIdAndType: RedditIdAndType?=null
    private var isSelfPost = false

    protected override fun onCreate(savedInstanceState: Bundle?) {
        PrefsUtility.applyTheme(this)

        super.onCreate(savedInstanceState)

        if (getIntent() != null && getIntent().hasExtra("isSelfPost")
            && getIntent().getBooleanExtra("isSelfPost", false)
        ) {
            setTitle(R.string.edit_post_actionbar)
            isSelfPost = true
        } else {
            setTitle(R.string.edit_comment_actionbar)
        }
        textEdit = getLayoutInflater().inflate(R.layout.comment_edit, null) as EditText

        if (getIntent() != null && getIntent().hasExtra("commentIdAndType")) {
            commentIdAndType = IntentCompat.getParcelableExtra<RedditIdAndType?>(
                getIntent(),
                "commentIdAndType",
                RedditIdAndType::class.java
            )
            textEdit!!.setText(getIntent().getStringExtra("commentText"))
        } else if (savedInstanceState != null && savedInstanceState.containsKey(
                "commentIdAndType"
            )
        ) {
            textEdit!!.setText(savedInstanceState.getString("commentText"))
            commentIdAndType = BundleCompat.getParcelable<RedditIdAndType?>(
                savedInstanceState,
                "commentIdAndType",
                RedditIdAndType::class.java
            )
        }

        val sv = ScrollView(this)
        sv.addView(textEdit)
        setBaseActivityListing(sv)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("commentText", textEdit!!.getText().toString())
        outState.putParcelable("commentIdAndType", commentIdAndType)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val send = menu.add(R.string.comment_edit_save)
        send.setIcon(R.drawable.ic_action_save_dark)
        send.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        menu.add(R.string.comment_reply_preview)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getTitle() == getString(R.string.comment_edit_save)) {
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle(getString(R.string.comment_reply_submitting_title))
            progressDialog.setMessage(getString(R.string.comment_reply_submitting_message))
            progressDialog.setIndeterminate(true)
            progressDialog.setCancelable(true)
            progressDialog.setCanceledOnTouchOutside(false)

            progressDialog.setOnCancelListener(DialogInterface.OnCancelListener { dialogInterface: DialogInterface? ->
                quickToast(this, R.string.comment_reply_oncancel)
                safeDismissDialog(progressDialog)
            })

            progressDialog.setOnKeyListener(DialogInterface.OnKeyListener { dialogInterface: DialogInterface?, keyCode: Int, keyEvent: KeyEvent? ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    quickToast(this, R.string.comment_reply_oncancel)
                    safeDismissDialog(progressDialog)
                }
                true
            })

            val handler: ActionResponseHandler=object : ActionResponseHandler(this) {
                override fun onSuccess() {
                    AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                        safeDismissDialog(progressDialog)
                        if (isSelfPost) {
                            quickToast(
                                this@CommentEditActivity,
                                R.string.post_edit_done
                            )
                        } else {
                            quickToast(
                                this@CommentEditActivity,
                                R.string.comment_edit_done
                            )
                        }
                        finish()
                    })
                }

                override fun onCallbackException(t : Throwable) {
                    BugReportActivity.Companion.handleGlobalError(this@CommentEditActivity, t)
                }

                override fun onFailure(error: RRError) {
                    showResultDialog(this@CommentEditActivity, error)
                    safeDismissDialog(progressDialog)
                }
            }

            val cm = CacheManager.getInstance(this@CommentEditActivity)
            val selectedAccount = RedditAccountManager.getInstance(this@CommentEditActivity)
                .getDefaultAccount()

            RedditAPI.editComment(
                cm,
                handler,
                selectedAccount,
                commentIdAndType,
                textEdit!!.getText().toString(),
                this
            )

            progressDialog.show()
        } else if (item.getTitle() == getString(R.string.comment_reply_preview)) {
            MarkdownPreviewDialog.newInstance(textEdit!!.getText().toString())
                .show(getSupportFragmentManager(), "MarkdownPreviewDialog")
        }

        return true
    }
}