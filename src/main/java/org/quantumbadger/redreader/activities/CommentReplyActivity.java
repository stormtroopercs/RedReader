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
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.RedReader.Companion.getInstance
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.BugReportActivity.Companion.handleGlobalError
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.Consumer
import org.quantumbadger.redreader.common.DialogUtils
import org.quantumbadger.redreader.common.General.quickToast
import org.quantumbadger.redreader.common.General.safeDismissDialog
import org.quantumbadger.redreader.common.General.showResultDialog
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.common.UriString.Companion.from
import org.quantumbadger.redreader.fragments.AccountListDialog.Companion.show
import org.quantumbadger.redreader.fragments.MarkdownPreviewDialog
import org.quantumbadger.redreader.fragments.ReportDialog.Companion.show
import org.quantumbadger.redreader.reddit.APIResponseHandler.ActionResponseHandler
import org.quantumbadger.redreader.reddit.APIResponseHandler.SubmitResponseHandler
import org.quantumbadger.redreader.reddit.RedditAPI
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import java.util.Objects

class CommentReplyActivity : ViewsBaseActivity() {
    private enum class ParentType {
        MESSAGE, COMMENT_OR_POST
    }

    private var usernameSpinner: Spinner? = null
    private var textEdit: EditText? = null
    private var inboxReplies: CheckBox? = null
    private var parentIdAndType: RedditIdAndType? = null

    private var mParentType: ParentType? = null

    private var mDraftReset = false
    protected override fun onCreate(savedInstanceState: Bundle?) {
        PrefsUtility.applyTheme(this)

        super.onCreate(savedInstanceState)

        val intent = getIntent()

        if (intent != null && intent.hasExtra(PARENT_TYPE)
            && PARENT_TYPE_MESSAGE == intent.getStringExtra(PARENT_TYPE)
        ) {
            mParentType = ParentType.MESSAGE
            setTitle(string.submit_pmreply_actionbar)
        } else {
            mParentType = ParentType.COMMENT_OR_POST
            setTitle(string.submit_comment_actionbar)
        }

        val layout = getLayoutInflater().inflate(R.layout.comment_reply, null) as LinearLayout

        usernameSpinner = layout.findViewById<Spinner>(R.id.comment_reply_username)
        inboxReplies = layout.findViewById<CheckBox>(R.id.comment_reply_inbox)
        textEdit = layout.findViewById<EditText?>(R.id.comment_reply_text)

        val uploadPicture = layout.findViewById<Button>(R.id.comment_reply_picture)

        uploadPicture.setOnClickListener(View.OnClickListener { v: View? -> uploadPicture() })

        if (mParentType == ParentType.COMMENT_OR_POST) {
            inboxReplies!!.setVisibility(View.VISIBLE)
        }

        if (intent != null && intent.hasExtra(PARENT_ID_AND_TYPE_KEY)) {
            parentIdAndType = Objects.requireNonNull<RedditIdAndType?>(
                IntentCompat.getParcelableExtra<RedditIdAndType?>(
                    intent,
                    PARENT_ID_AND_TYPE_KEY,
                    RedditIdAndType::class.java
                )
            )
        } else if (savedInstanceState != null
            && savedInstanceState.containsKey(PARENT_ID_AND_TYPE_KEY)
        ) {
            parentIdAndType = Objects.requireNonNull<RedditIdAndType?>(
                BundleCompat.getParcelable<RedditIdAndType?>(
                    savedInstanceState,
                    PARENT_ID_AND_TYPE_KEY,
                    RedditIdAndType::class.java
                )
            )
        } else {
            throw RuntimeException("No parent ID in CommentReplyActivity")
        }

        val existingCommentText: String?

        if (savedInstanceState != null
            && savedInstanceState.containsKey(COMMENT_TEXT_KEY)
        ) {
            existingCommentText = savedInstanceState.getString(COMMENT_TEXT_KEY)
        } else if (lastText != null && parentIdAndType == lastParentIdAndType) {
            existingCommentText = lastText
        } else {
            existingCommentText = null
        }

        if (existingCommentText != null) {
            textEdit!!.setText(existingCommentText)
        }

        if (intent != null && intent.hasExtra(PARENT_MARKDOWN_KEY)) {
            val parentMarkdown = layout.findViewById<TextView>(R.id.comment_parent_text)
            parentMarkdown.setText(intent.getStringExtra(PARENT_MARKDOWN_KEY))
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
        outState.putString(COMMENT_TEXT_KEY, textEdit!!.getText().toString())
        outState.putParcelable(PARENT_ID_AND_TYPE_KEY, parentIdAndType)
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

            val handler
                    : SubmitResponseHandler = object : SubmitResponseHandler(this) {
                override fun onSubmitErrors(errors: ArrayList<String?>) {
                    AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                        val errorsJoined = StringUtils.join(errors, " ")
                        DialogUtils.showDialog(
                            this@CommentReplyActivity,
                            getString(string.error_comment_submit_title),
                            errorsJoined
                        )
                        safeDismissDialog(progressDialog)
                    })
                }

                override fun onSuccess(
                    redirectUrl: Optional<String?>,
                    thingId: Optional<String?>
                ) {
                    AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                        safeDismissDialog(progressDialog)
                        if (mParentType == ParentType.MESSAGE) {
                            quickToast(
                                this@CommentReplyActivity,
                                getString(string.pm_reply_done)
                            )
                        } else {
                            quickToast(
                                this@CommentReplyActivity,
                                getString(string.comment_reply_done_norefresh)
                            )
                        }

                        mDraftReset = true
                        lastText = null
                        lastParentIdAndType = null

                        redirectUrl.ifPresent(Consumer { url: String? ->
                            onLinkClicked(
                                this@CommentReplyActivity,
                                from(
                                    Uri.parse(url)
                                        .buildUpon()
                                        .appendQueryParameter("context", "1")
                                        .build()
                                )
                            )
                        })
                        finish()
                    })
                }

                protected override fun onCallbackException(t: Throwable?) {
                    handleGlobalError(this@CommentReplyActivity, t)
                }

                protected override fun onFailure(error: RRError) {
                    showResultDialog(this@CommentReplyActivity, error)
                    safeDismissDialog(progressDialog)
                }
            }

            val inboxHandler
                    : ActionResponseHandler = object : ActionResponseHandler(this) {
                override fun onSuccess() {
                    // Do nothing (result expected)
                }

                protected override fun onCallbackException(t: Throwable?) {
                    handleGlobalError(this@CommentReplyActivity, t)
                }

                protected override fun onFailure(error: RRError) {
                    Toast.makeText(
                        context,
                        getString(string.disable_replies_to_infobox_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            val cm: CacheManager = CacheManager.Companion.getInstance(this)

            val accounts: ArrayList<RedditAccount> = RedditAccountManager.Companion.getInstance(
                this
            ).getAccounts()
            var selectedAccount: RedditAccount? = null

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
            val sendRepliesToInbox: Boolean
            if (mParentType == ParentType.COMMENT_OR_POST) {
                sendRepliesToInbox = inboxReplies!!.isChecked()
            } else {
                sendRepliesToInbox = true
            }
            RedditAPI.comment(
                cm,
                handler,
                inboxHandler,
                selectedAccount!!,
                parentIdAndType!!,
                textEdit!!.getText().toString(),
                sendRepliesToInbox,
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

        if (textEdit != null && !mDraftReset) {
            lastText = textEdit!!.getText().toString()
            lastParentIdAndType = parentIdAndType
        }
    }

    private fun uploadPicture() {
        val intent = Intent(this, ImgurUploadActivity::class.java)
        startActivityForResultWithCallback(
            intent,
            BaseActivity.ActivityResultCallback { resultCode: Int, data: Intent? ->
                if (resultCode == 0 && data != null) {
                    val uploadedImageUrl = data.getData()
                    if (uploadedImageUrl != null) {
                        // set the picture into textedit as a link: [Picture](PictureURL)
                        val existingText = textEdit!!.getText().toString()
                        val picturePretext = getString(string.comment_picture_pretext)
                        val linkText =
                            "[" + picturePretext + "](" + uploadedImageUrl + ")"
                        val combinedText = existingText + " " + linkText
                        textEdit!!.setText(combinedText)
                    }
                }
            })
    }

    companion object {
        private var lastText: String? = null
        private var lastParentIdAndType: RedditIdAndType? = null

        const val PARENT_TYPE: String = "parentType"
        const val PARENT_TYPE_MESSAGE: String = "parentTypeMessage"

        const val PARENT_ID_AND_TYPE_KEY: String = "parentIdAndType"
        const val PARENT_MARKDOWN_KEY: String = "parent_markdown"
        private const val COMMENT_TEXT_KEY = "comment_text"
    }
}
