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
package org.quantumbadger.redreader.fragments.postsubmit

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.activities.BugReportActivity
import org.quantumbadger.redreader.activities.ImgurUploadActivity
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.AndroidCommon.runOnUiThread
import org.quantumbadger.redreader.common.Consumer
import org.quantumbadger.redreader.common.DialogUtils
import org.quantumbadger.redreader.common.General.findViewById
import org.quantumbadger.redreader.common.General.quickToast
import org.quantumbadger.redreader.common.General.safeDismissDialog
import org.quantumbadger.redreader.common.General.showResultDialog
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.UriString.Companion.fromNullable
import org.quantumbadger.redreader.fragments.AccountListDialog.Companion.show
import org.quantumbadger.redreader.fragments.MarkdownPreviewDialog
import org.quantumbadger.redreader.fragments.ReportDialog.Companion.show
import org.quantumbadger.redreader.reddit.APIResponseHandler.SubmitResponseHandler
import org.quantumbadger.redreader.reddit.RedditAPI
import org.quantumbadger.redreader.reddit.RedditAPI.FlairSelectorResponseHandler
import org.quantumbadger.redreader.reddit.RedditFlairChoice
import org.quantumbadger.redreader.reddit.things.SubredditCanonicalId
import java.util.Locale
import java.util.Objects
import org.quantumbadger.redreader.common.General

class PostSubmitContentFragment : Fragment() {
    class Args(
        val username: String,
        val subreddit: SubredditCanonicalId,
        val url: String?
    ) {
        fun toBundle(): Bundle {
            val result = Bundle(3)
            result.putString(KEY_USER, username)
            result.putParcelable(KEY_SUBREDDIT, subreddit)
            result.putString(KEY_URL, url)

            return result
        }

        companion object {
            private const val KEY_USER = "user"
            private const val KEY_SUBREDDIT = "subreddit"
            private const val KEY_URL = "url"

            fun fromBundle(bundle: Bundle): Args {
                return Args(
                    Objects.requireNonNull<String?>(bundle.getString(KEY_USER)),
                    Objects.requireNonNull<SubredditCanonicalId?>(
                        BundleCompat.getParcelable<SubredditCanonicalId?>(
                            bundle,
                            KEY_SUBREDDIT,
                            SubredditCanonicalId::class.java
                        )
                    ),
                    bundle.getString(KEY_URL)
                )
            }
        }
    }

    interface Listener {
        fun onContentFragmentSubmissionSuccess(redirectUrl: UriString?)
        fun onContentFragmentSubredditDoesNotExist()
        fun onContentFragmentSubredditPermissionDenied()
        fun onContentFragmentFlairRequestError(error: RRError)
    }

    private var mDraftReset = false

    private var mActive = true

    private var mContext: Context?=null

    private var mSelectedAccount: RedditAccount?=null
    private var mSelectedSubreddit: SubredditCanonicalId?=null

    private var mLoadingSpinnerView: View?=null
    private var mMainControls: View?=null

    private var mTypeSpinner: MaterialAutoCompleteTextView?=null
    private var mFlairSpinner: MaterialAutoCompleteTextView?=null
    private var mFlairSpinnerLayout: TextInputLayout?=null
    private var mTitleEdit: TextInputEditText?=null

    private var mTextEditBodyText: TextInputEditText?=null
    private var mTextEditLayoutBodyText: TextInputLayout?=null

    private var mTextEditBodyUrl: TextInputEditText?=null
    private var mTextEditLayoutBodyUrl: TextInputLayout?=null

    private var mSendRepliesToInboxCheckbox: CheckBox?=null
    private var mMarkAsNsfwCheckbox: CheckBox?=null
    private var mMarkAsSpoilerCheckbox: CheckBox?=null

    private val mFlairIds = HashMap<String?, String?>()

    override fun onResume() {
        super.onResume()

        val activity = getActivity()

        if (activity != null) {
            activity.setTitle(string.submit_post_actionbar)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mActive = false

        // Store information for draft
        if (mTitleEdit != null && !mDraftReset) {
            lastType = mTypeSpinner!!.getText().toString()
            lastTitle = mTitleEdit!!.getText().toString()
            lastBodyText = mTextEditBodyText!!.getText().toString()
            lastBodyUrl = mTextEditBodyUrl!!.getText().toString()
            lastInbox = mSendRepliesToInboxCheckbox!!.isChecked()
            lastNsfw = mMarkAsNsfwCheckbox!!.isChecked()
            lastSpoiler = mMarkAsSpoilerCheckbox!!.isChecked()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        mContext = Objects.requireNonNull<ViewGroup?>(container).getContext()

        val accountManager: RedditAccountManager =             RedditAccountManager.Companion.getInstance(mContext)

        val args: Args = Args.Companion.fromBundle(requireArguments())

        mSelectedAccount = accountManager.getAccount(args.username)

        if (mSelectedAccount == null) {
            BugReportActivity.handleGlobalError(
                mContext!!, RuntimeException(
                    "Selected account is not in the account manager"
                )
            )

            return null
        }

        mSelectedSubreddit = args.subreddit

        val root = inflater.inflate(R.layout.post_submit, container, false)

        mMainControls = Objects.requireNonNull<View>(
            root.findViewById<View?>(R.id.post_submit_main_controls)
        )

        mLoadingSpinnerView = Objects.requireNonNull<View>(
            findViewById<View?>(root, R.id.post_submit_loading_spinner_view)
        )

        mTypeSpinner = Objects.requireNonNull<MaterialAutoCompleteTextView>(
            root.findViewById<MaterialAutoCompleteTextView?>(
                R.id.post_submit_type
            )
        )
        mFlairSpinner = Objects.requireNonNull<MaterialAutoCompleteTextView>(
            root.findViewById<MaterialAutoCompleteTextView?>(
                R.id.post_submit_flair
            )
        )
        mFlairSpinnerLayout = Objects.requireNonNull<TextInputLayout>(
            root.findViewById<TextInputLayout?>(R.id.post_submit_flair_layout)
        )
        mTitleEdit = Objects.requireNonNull<TextInputEditText?>(
            root.findViewById<TextInputEditText?>(
                R.id.post_submit_title
            )
        )

        mTextEditBodyText = Objects.requireNonNull<TextInputEditText>(
            root.findViewById<TextInputEditText?>(
                R.id.post_submit_body_text
            )
        )
        mTextEditLayoutBodyText = Objects.requireNonNull<TextInputLayout>(
            root.findViewById<TextInputLayout?>(R.id.post_submit_body_text_layout)
        )

        mTextEditBodyUrl = Objects.requireNonNull<TextInputEditText>(
            root.findViewById<TextInputEditText?>(
                R.id.post_submit_body_url
            )
        )
        mTextEditLayoutBodyUrl = Objects.requireNonNull<TextInputLayout>(
            root.findViewById<TextInputLayout?>(R.id.post_submit_body_url_layout)
        )

        mSendRepliesToInboxCheckbox = Objects.requireNonNull<CheckBox>(
            root.findViewById<CheckBox?>(R.id.post_submit_send_replies_to_inbox)
        )

        mMarkAsNsfwCheckbox = Objects.requireNonNull<CheckBox>(
            root.findViewById<CheckBox?>(R.id.post_submit_mark_nsfw)
        )

        mMarkAsSpoilerCheckbox = Objects.requireNonNull<CheckBox>(
            root.findViewById<CheckBox?>(R.id.post_submit_mark_spoiler)
        )

        val heading = root.findViewById<TextView>(R.id.post_submit_heading)

        heading.setText(
            String.format(
                Locale.US,
                getString(string.post_submit_heading),
                args.subreddit,
                args.username
            )
        )

        mTypeSpinner!!.setText(POST_TYPE_LINK)

        AndroidCommon.setAutoCompleteTextViewItemsNoFilter(mTypeSpinner!!, POST_TYPES)

        if (args.url != null) {
            mTextEditBodyUrl!!.setText(args.url)
        }

        // Fetch information from draft if a draft exists
        if (args.url == null && lastTitle != null) {
            mTypeSpinner!!.setText(lastType)
            mTitleEdit!!.setText(lastTitle)
            mTextEditBodyText!!.setText(lastBodyText)
            mTextEditBodyUrl!!.setText(lastBodyUrl)
            mSendRepliesToInboxCheckbox!!.setChecked(lastInbox)
            mMarkAsSpoilerCheckbox!!.setChecked(lastSpoiler)
            mMarkAsNsfwCheckbox!!.setChecked(lastNsfw)
        }

        setHint()

        AndroidCommon.onTextChanged(mTypeSpinner!!, Runnable { this.setHint() })

        requestSubredditDetails()

        return root
    }

    private fun disableFlairSpinner(@StringRes message: Int) {
        val appContext = mContext!!.getApplicationContext()

        mFlairSpinner!!.setAdapter(null)
        mFlairSpinner!!.setText(appContext.getString(message))

        mFlairSpinnerLayout!!.setEnabled(false)

        mFlairSpinner!!.setEnabled(false)
        mFlairSpinner!!.setAlpha(0.5f)
    }

    private fun enableFlairSpinner(choices: MutableCollection<RedditFlairChoice>) {
        val appContext = mContext!!.getApplicationContext()

        mFlairSpinner!!.setEnabled(true)
        mFlairSpinner!!.setAlpha(1.0f)

        val choiceStrings = ArrayList<String?>(choices.size + 1)
        mFlairIds.clear()

        val noneSelected = appContext.getString(string.post_submit_flair_none_selected)

        choiceStrings.add(noneSelected)

        for (choice in choices) {
            choiceStrings.add(choice.text)
            mFlairIds.put(choice.text, choice.templateId)
        }

        AndroidCommon.setAutoCompleteTextViewItemsNoFilter(mFlairSpinner!!, choiceStrings)

        mFlairSpinner!!.setText(noneSelected)
    }

    private fun setHint() {
        val selected: Any = mTypeSpinner!!.getText().toString()

        if (selected == POST_TYPE_LINK || selected == POST_TYPE_IMGUR) {
            mTextEditLayoutBodyText!!.setVisibility(View.GONE)
            mTextEditLayoutBodyUrl!!.setVisibility(View.VISIBLE)
        } else if (selected == POST_TYPE_SELF) {
            mTextEditLayoutBodyText!!.setVisibility(View.VISIBLE)
            mTextEditLayoutBodyUrl!!.setVisibility(View.GONE)
        } else {
            throw RuntimeException("Unknown selection " + selected)
        }

        if (selected == POST_TYPE_IMGUR) {
            mTypeSpinner!!.setSelection(0) // Link

            val activity = getActivity()

            if (activity == null) {
                return
            }

            val intent = Intent(activity, ImgurUploadActivity::class.java)

            (activity as BaseActivity).startActivityForResultWithCallback(
                intent,
                BaseActivity.ActivityResultCallback { resultCode: Int, data: Intent? ->
                    if (data != null && data.getData() != null) {
                        mTextEditBodyUrl!!.setText(data.getData().toString())
                    }
                })
        }
    }

    private fun requestSubredditDetails() {
        RedditAPI.flairSelectorForNewLink(
            mContext!!,
            CacheManager.Companion.getInstance(mContext),
            mSelectedAccount!!,
            mSelectedSubreddit!!,
            object : FlairSelectorResponseHandler {
                override fun onSuccess(choices: MutableCollection<RedditFlairChoice>) {
                    runOnUiThread(Runnable {
                        if (!mActive) {
                            return@Runnable
                        }
                        mLoadingSpinnerView!!.setVisibility(View.GONE)
                        mMainControls!!.setVisibility(View.VISIBLE)
                        if (choices.isEmpty()) {
                            disableFlairSpinner(string.post_submit_flair_none_available)
                        } else {
                            enableFlairSpinner(choices)
                        }
                    })
                }

                override fun onSubredditDoesNotExist() {
                    runOnUiThread(Runnable {
                        if (!mActive) {
                            return@Runnable
                        }
                        ifActivityNotNull(Consumer { obj -> obj.onContentFragmentSubredditDoesNotExist() })
                    })
                }

                override fun onSubredditPermissionDenied() {
                    runOnUiThread(Runnable {
                        if (!mActive) {
                            return@Runnable
                        }
                        ifActivityNotNull(Consumer { obj -> obj.onContentFragmentSubredditPermissionDenied() })
                    })
                }

                override fun onFailure(error: RRError) {
                    runOnUiThread(Runnable {
                        if (!mActive) {
                            return@Runnable
                        }
                        ifActivityNotNull(Consumer { listener: Listener ->
                            listener.onContentFragmentFlairRequestError(error)
                        })
                    })
                }
            })
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater
    ) {
        val send = menu.add(string.comment_reply_send)
        send.setIcon(R.drawable.ic_action_send_dark)
        send.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        menu.add(string.comment_reply_preview)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val isSelfPost = mTypeSpinner!!.getText().toString() == POST_TYPE_SELF

        val bodyText = if (isSelfPost)
            mTextEditBodyText!!.getText().toString()
        else
            mTextEditBodyUrl!!.getText().toString()

        if (item.getTitle() == getString(string.comment_reply_send)) {
            var subreddit = mSelectedSubreddit!!.displayNameLowercase
            val postTitle = mTitleEdit!!.getText().toString()

            if (postTitle.isEmpty()) {
                Toast.makeText(mContext, string.submit_post_title_empty, Toast.LENGTH_SHORT)
                    .show()
                mTitleEdit!!.requestFocus()
            } else if (bodyText.isEmpty() && !isSelfPost) {
                Toast.makeText(mContext, string.submit_post_url_empty, Toast.LENGTH_SHORT)
                    .show()
                mTextEditBodyUrl!!.requestFocus()
            } else {
                val activity = getActivity()

                if (activity == null) {
                    Log.e(TAG, "Activity was null when sending")
                    return true
                }

                val progressDialog = ProgressDialog(mContext)
                progressDialog.setTitle(getString(string.comment_reply_submitting_title))
                progressDialog.setMessage(getString(string.comment_reply_submitting_message))
                progressDialog.setIndeterminate(true)
                progressDialog.setCancelable(true)
                progressDialog.setCanceledOnTouchOutside(false)

                progressDialog.setOnCancelListener(DialogInterface.OnCancelListener { dialogInterface: DialogInterface? ->
                    quickToast(mContext, getString(string.comment_reply_oncancel))
                    safeDismissDialog(progressDialog)
                })

                progressDialog.setOnKeyListener(DialogInterface.OnKeyListener { dialogInterface: DialogInterface?, keyCode: Int, keyEvent: KeyEvent? ->
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        quickToast(mContext, getString(string.comment_reply_oncancel))
                        safeDismissDialog(progressDialog)
                    }
                    true
                })

                val cm: CacheManager?=CacheManager.Companion.getInstance(mContext)

                val handler: SubmitResponseHandler=object : SubmitResponseHandler(
                    activity as AppCompatActivity
                ) {
                    override fun onSubmitErrors(errors: ArrayList<String?>) {
                        val activity = getActivity()

                        if (activity != null) {
                            val errorsJoined = StringUtils.join(errors, " ")

                            DialogUtils.showDialog(
                                activity,
                                activity.getString(string.error_post_submit_title),
                                errorsJoined
                            )
                        }

                        safeDismissDialog(progressDialog)
                    }

                    override fun onSuccess(
                        redirectUrl: Optional<String>,
                        thingId: Optional<String>
                    ) {
                        AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                            safeDismissDialog(progressDialog)
                            quickToast(
                                mContext,
                                getString(string.post_submit_done)
                            )

                            resetDraft()

                            val activity = getActivity()
                            if (activity != null) {
                                (activity as Listener).onContentFragmentSubmissionSuccess(
                                    fromNullable(redirectUrl.orElseNull())
                                )
                            }
                        })
                    }

                    override fun onCallbackException(t : Throwable) {
                        BugReportActivity.handleGlobalError(mContext!!, t)
                    }

                    override fun onFailure(error: RRError) {
                        val activity = getActivity()

                        if (activity != null) {
                            showResultDialog(activity as AppCompatActivity, error)
                        }

                        safeDismissDialog(progressDialog)
                    }
                }

                while (subreddit.startsWith("/")) {
                    subreddit = subreddit.substring(1)
                }
                while (subreddit.startsWith("r/")) {
                    subreddit = subreddit.substring(2)
                }
                while (subreddit.endsWith("/")) {
                    subreddit = subreddit.substring(0, subreddit.length - 1)
                }

                val sendRepliesToInbox = mSendRepliesToInboxCheckbox!!.isChecked()
                val markAsNsfw = mMarkAsNsfwCheckbox!!.isChecked()
                val markAsSpoiler = mMarkAsSpoilerCheckbox!!.isChecked()

                val flairId = mFlairIds.get(mFlairSpinner!!.getText().toString())

                RedditAPI.submit(
                    cm,
                    handler,
                    mSelectedAccount,
                    isSelfPost,
                    subreddit,
                    postTitle,
                    bodyText,
                    sendRepliesToInbox,
                    markAsNsfw,
                    markAsSpoiler,
                    flairId,
                    mContext
                )

                progressDialog.show()
            }
            return true
        } else if (item.getTitle() == getString(string.comment_reply_preview)) {
            MarkdownPreviewDialog.Companion.newInstance(bodyText)
                .show(getParentFragmentManager(), null)
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    private fun resetDraft() {
        mDraftReset = true
        lastType = null
        lastTitle = null
        lastBodyText = null
        lastBodyUrl = null
        lastInbox = true
        lastNsfw = false
        lastSpoiler = false
    }

    private fun ifActivityNotNull(action: Consumer<Listener>) {
        val activity = getActivity()

        if (activity != null) {
            action.consume(activity as Listener)
        }
    }

    companion object {
        private const val TAG = "PostSubmitContentFrag"

        private const val POST_TYPE_LINK = "Link"
        private const val POST_TYPE_SELF = "Text"
        private const val POST_TYPE_IMGUR = "Upload to Imgur"

        @Suppress("PropertyName")
        private val POST_TYPES = arrayOf<String?>(POST_TYPE_LINK, POST_TYPE_SELF, POST_TYPE_IMGUR)

        private var lastType: String?=null
        private var lastTitle: String?=null
        private var lastBodyText: String?=null
        private var lastBodyUrl: String?=null
        private var lastNsfw = false
        private var lastSpoiler = false
        private var lastInbox = false
    }
}