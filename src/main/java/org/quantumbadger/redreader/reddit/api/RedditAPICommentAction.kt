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
package org.quantumbadger.redreader.reddit.api

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccount.isAnonymous
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.CommentEditActivity
import org.quantumbadger.redreader.activities.CommentReplyActivity
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.common.General.quickToast
import org.quantumbadger.redreader.common.General.showMustBeLoggedInDialog
import org.quantumbadger.redreader.common.General.showResultDialog
import org.quantumbadger.redreader.common.LinkHandler.getPreferredRedditUriString
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.common.LinkHandler.shareText
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.UriString.Companion.from
import org.quantumbadger.redreader.common.time.TimestampUTC.Companion.now
import org.quantumbadger.redreader.fragments.AccountListDialog.Companion.show
import org.quantumbadger.redreader.fragments.CommentListingFragment
import org.quantumbadger.redreader.fragments.CommentPropertiesDialog
import org.quantumbadger.redreader.fragments.ReportDialog.Companion.show
import org.quantumbadger.redreader.reddit.APIResponseHandler.ActionResponseHandler
import org.quantumbadger.redreader.reddit.RedditAPI
import org.quantumbadger.redreader.reddit.RedditAPI.RedditAction
import org.quantumbadger.redreader.reddit.kthings.RedditComment
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager
import org.quantumbadger.redreader.reddit.prepared.RedditRenderableComment
import org.quantumbadger.redreader.reddit.url.UserProfileURL
import org.quantumbadger.redreader.views.RedditCommentView
import java.util.Locale
import org.quantumbadger.redreader.common.General

object RedditAPICommentAction {
    fun showActionMenu(
        activity: AppCompatActivity,
        commentListingFragment: CommentListingFragment?,
        comment: RedditRenderableComment,
        commentView: RedditCommentView,
        changeDataManager: RedditChangeDataManager,
        isPostLocked: Boolean
    ) {
        val itemPref = PrefsUtility.pref_menus_comment_context_items()

        if (itemPref.isEmpty()) {
            return
        }

        // These will be false for comments in the inbox. There seems to be no way around this,
        // unless we do a lot of work to download the associated post and check there.
        val isArchived = comment.parsedComment.rawComment.archived
        val isCommentLocked = comment.parsedComment.rawComment.locked
        val canModerate = comment.parsedComment.rawComment.can_mod_post

        val user: RedditAccount =             RedditAccountManager.Companion.getInstance(activity).getDefaultAccount()

        val menu: ArrayList<RCVMenuItem?> = ArrayList<RCVMenuItem?>()

        if (!user.isAnonymous) {
            if (!isArchived) {
                if (itemPref.contains(RedditCommentAction.UPVOTE)) {
                    if (!changeDataManager.isUpvoted(comment.idAndType)) {
                        menu.add(
                            RCVMenuItem(
                                activity,
                                string.action_upvote,
                                RedditCommentAction.UPVOTE
                            )
                        )
                    } else {
                        menu.add(
                            RCVMenuItem(
                                activity,
                                string.action_upvote_remove,
                                RedditCommentAction.UNVOTE
                            )
                        )
                    }
                }

                if (itemPref.contains(RedditCommentAction.DOWNVOTE)) {
                    if (!changeDataManager.isDownvoted(comment.idAndType)) {
                        menu.add(
                            RCVMenuItem(
                                activity,
                                string.action_downvote,
                                RedditCommentAction.DOWNVOTE
                            )
                        )
                    } else {
                        menu.add(
                            RCVMenuItem(
                                activity,
                                string.action_downvote_remove,
                                RedditCommentAction.UNVOTE
                            )
                        )
                    }
                }
            }

            if (itemPref.contains(RedditCommentAction.SAVE)) {
                if (changeDataManager.isSaved(comment.idAndType)) {
                    menu.add(
                        RCVMenuItem(
                            activity,
                            string.action_unsave,
                            RedditCommentAction.UNSAVE
                        )
                    )
                } else {
                    menu.add(
                        RCVMenuItem(
                            activity,
                            string.action_save,
                            RedditCommentAction.SAVE
                        )
                    )
                }
            }

            if (itemPref.contains(RedditCommentAction.REPORT)) {
                menu.add(
                    RCVMenuItem(
                        activity,
                        string.action_report,
                        RedditCommentAction.REPORT
                    )
                )
            }

            if (itemPref.contains(RedditCommentAction.REPLY)
                && !isArchived && !((isCommentLocked || isPostLocked) && !canModerate)
            ) {
                menu.add(
                    RCVMenuItem(
                        activity,
                        string.action_reply,
                        RedditCommentAction.REPLY
                    )
                )
            }

            if (user.username.equals(
                    comment.parsedComment
                        .rawComment.author!!.decoded, ignoreCase = true
                )
            ) {
                if (itemPref.contains(RedditCommentAction.EDIT) && !isArchived) {
                    menu.add(
                        RCVMenuItem(
                            activity,
                            string.action_edit,
                            RedditCommentAction.EDIT
                        )
                    )
                }

                if (itemPref.contains(RedditCommentAction.DELETE)) {
                    menu.add(
                        RCVMenuItem(
                            activity,
                            string.action_delete,
                            RedditCommentAction.DELETE
                        )
                    )
                }
            }
        }

        if (itemPref.contains(RedditCommentAction.EXTERNAL)) {
            menu.add(
                RCVMenuItem(
                    activity,
                    string.action_external,
                    RedditCommentAction.EXTERNAL
                )
            )
        }

        if (itemPref.contains(RedditCommentAction.CONTEXT)) {
            menu.add(
                RCVMenuItem(
                    activity,
                    string.action_comment_context,
                    RedditCommentAction.CONTEXT
                )
            )
        }

        if (itemPref.contains(RedditCommentAction.GO_TO_COMMENT)) {
            menu.add(
                RCVMenuItem(
                    activity,
                    string.action_comment_go_to,
                    RedditCommentAction.GO_TO_COMMENT
                )
            )
        }

        if (itemPref.contains(RedditCommentAction.COMMENT_LINKS)) {
            menu.add(
                RCVMenuItem(
                    activity,
                    string.action_comment_links,
                    RedditCommentAction.COMMENT_LINKS
                )
            )
        }

        if (itemPref.contains(RedditCommentAction.COLLAPSE) && commentListingFragment != null) {
            menu.add(
                RCVMenuItem(
                    activity,
                    string.action_collapse,
                    RedditCommentAction.COLLAPSE
                )
            )
        }

        if (itemPref.contains(RedditCommentAction.SHARE)) {
            menu.add(
                RCVMenuItem(
                    activity,
                    string.action_share,
                    RedditCommentAction.SHARE
                )
            )
        }

        if (itemPref.contains(RedditCommentAction.COPY_TEXT)) {
            menu.add(
                RCVMenuItem(
                    activity,
                    string.action_copy_text,
                    RedditCommentAction.COPY_TEXT
                )
            )
        }

        if (itemPref.contains(RedditCommentAction.COPY_URL)) {
            menu.add(
                RCVMenuItem(
                    activity,
                    string.action_copy_link,
                    RedditCommentAction.COPY_URL
                )
            )
        }

        if (itemPref.contains(RedditCommentAction.USER_PROFILE)) {
            menu.add(
                RCVMenuItem(
                    activity,
                    string.action_user_profile,
                    RedditCommentAction.USER_PROFILE
                )
            )
        }

        if (itemPref.contains(RedditCommentAction.PROPERTIES)) {
            menu.add(
                RCVMenuItem(
                    activity,
                    string.action_properties,
                    RedditCommentAction.PROPERTIES
                )
            )
        }

        val menuText = arrayOfNulls<String>(menu.size)

        for (i in menuText.indices) {
            menuText[i] = menu.get(i)!!.title
        }

        val builder = MaterialAlertDialogBuilder(activity)

        builder.setItems(
            menuText,
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                onActionMenuItemSelected(
                    comment,
                    commentView,
                    activity,
                    commentListingFragment,
                    menu.get(which)!!.action,
                    changeDataManager
                )
            })

        val alert = builder.create()
        alert.setCanceledOnTouchOutside(true)
        alert.show()
    }

    fun onActionMenuItemSelected(
        renderableComment: RedditRenderableComment,
        commentView: RedditCommentView,
        activity: AppCompatActivity,
        commentListingFragment: CommentListingFragment?,
        action: RedditCommentAction,
        changeDataManager: RedditChangeDataManager
    ) {
        val comment =             renderableComment.parsedComment.rawComment

        val postLocked =             commentListingFragment != null && commentListingFragment.post != null && commentListingFragment.post.isLocked

        when (action) {
            RedditCommentAction.UPVOTE -> action(
                activity,
                comment,
                RedditAPI.ACTION_UPVOTE,
                changeDataManager
            )

            RedditCommentAction.DOWNVOTE -> action(
                activity,
                comment,
                RedditAPI.ACTION_DOWNVOTE,
                changeDataManager
            )

            RedditCommentAction.UNVOTE -> action(
                activity,
                comment,
                RedditAPI.ACTION_UNVOTE,
                changeDataManager
            )

            RedditCommentAction.SAVE -> action(
                activity,
                comment,
                RedditAPI.ACTION_SAVE,
                changeDataManager
            )

            RedditCommentAction.UNSAVE -> action(
                activity,
                comment,
                RedditAPI.ACTION_UNSAVE,
                changeDataManager
            )

            RedditCommentAction.REPORT -> if (RedditAccountManager.Companion.getInstance(activity)
                    .getDefaultAccount().isAnonymous
            ) {
                showMustBeLoggedInDialog(activity)
            } else if (comment.subreddit != null) {
                show(
                    activity,
                    comment.idAndType,
                    comment.subreddit.decoded,
                    true
                )
            } else {
                quickToast(activity, string.error_unknown_title)
            }

            RedditCommentAction.REPLY -> {
                if (comment.archived) {
                    quickToast(activity, string.error_archived_reply, Toast.LENGTH_SHORT)
                    break
                } else if ((comment.locked || postLocked) && !comment.can_mod_post) {
                    quickToast(activity, string.error_locked_reply, Toast.LENGTH_SHORT)
                    break
                }

                val intent = Intent(activity, CommentReplyActivity::class.java)
                intent.putExtra(
                    CommentReplyActivity.Companion.PARENT_ID_AND_TYPE_KEY,
                    comment.idAndType
                )
                intent.putExtra(
                    CommentReplyActivity.Companion.PARENT_MARKDOWN_KEY,
                    comment.body!!.decoded
                )
                activity.startActivity(intent)
            }

            RedditCommentAction.EDIT -> {
                val intent = Intent(activity, CommentEditActivity::class.java)
                intent.putExtra("commentIdAndType", comment.idAndType)
                intent.putExtra(
                    "commentText",
                    comment.body!!.decoded
                )
                activity.startActivity(intent)
            }

            RedditCommentAction.DELETE -> {
                MaterialAlertDialogBuilder(activity)
                    .setTitle(string.accounts_delete)
                    .setMessage(string.delete_confirm)
                    .setPositiveButton(
                        string.action_delete,
                        DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                            action(
                                activity,
                                comment,
                                RedditAPI.ACTION_DELETE,
                                changeDataManager
                            )
                        })
                    .setNegativeButton(string.dialog_cancel, null)
                    .show()
            }

            RedditCommentAction.EXTERNAL -> {
                try {
                    val url = comment.getContextUrl().context(null).generateNonJsonUri().toString()

                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setData(Uri.parse(url))
                    activity.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    quickToast(
                        activity,
                        string.action_not_handled_by_installed_app_toast
                    )
                }
            }

            RedditCommentAction.COMMENT_LINKS -> {
                val linksInComment: MutableList<String?> = comment.computeAllLinksString()

                if (linksInComment.isEmpty()) {
                    quickToast(activity, string.error_toast_no_urls_in_comment)
                } else {
                    val linksArr =                         linksInComment.toTypedArray<String?>()

                    val builder = MaterialAlertDialogBuilder(activity)

                    builder.setItems(
                        linksArr,
                        DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                            onLinkClicked(activity, UriString(linksArr[which]!!), false)
                            dialog!!.dismiss()
                        })

                    val alert = builder.create()
                    alert.setTitle(string.action_comment_links)
                    alert.setCanceledOnTouchOutside(true)
                    alert.show()
                }
            }

            RedditCommentAction.SHARE -> {
                var body = ""
                var subject: String?=null

                if (PrefsUtility.pref_behaviour_sharing_include_desc()) {
                    subject = String.format(
                        Locale.US,
                        activity.getText(string.share_comment_by_on_reddit)
                            .toString(),
                        comment.author!!.decoded
                    )
                }

                // TODO this currently just dumps the markdown (only if sharing text is enabled)
                if (PrefsUtility.pref_behaviour_sharing_share_text()) {
                    body = (comment.body!!.decoded
                            + "\r\n\r\n")
                }

                body += getPreferredRedditUriString(
                    from(comment.getContextUrl().generateNonJsonUri())
                )

                shareText(activity, subject, body)
            }

            RedditCommentAction.COPY_TEXT -> {
                val clipboardManager =                     activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                // TODO this currently just dumps the markdown
                if (clipboardManager != null) {
                    val data = ClipData.newPlainText(
                        null,
                        comment.body!!.decoded
                    )
                    clipboardManager.setPrimaryClip(data)

                    quickToast(
                        activity.getApplicationContext(),
                        string.comment_text_copied_to_clipboard
                    )
                }
            }

            RedditCommentAction.COPY_URL -> {
                val clipboardManager =                     activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                if (clipboardManager != null) {
                    val data = ClipData.newPlainText(
                        null,
                        comment.getContextUrl().context(null).generateNonJsonUri().toString()
                    )
                    clipboardManager.setPrimaryClip(data)

                    quickToast(
                        activity.getApplicationContext(),
                        string.comment_link_copied_to_clipboard
                    )
                }
            }

            RedditCommentAction.COLLAPSE -> {
                commentListingFragment!!.handleCommentVisibilityToggle(commentView)
            }

            RedditCommentAction.USER_PROFILE -> onLinkClicked(
                activity,
                UserProfileURL(comment.author!!.decoded).toUriString()
            )

            RedditCommentAction.PROPERTIES -> CommentPropertiesDialog.Companion.newInstance(comment)
                .show(activity.getSupportFragmentManager(), null)

            RedditCommentAction.GO_TO_COMMENT -> {
                onLinkClicked(
                    activity,
                    comment.getContextUrl().context(null).toUriString()
                )
            }

            RedditCommentAction.CONTEXT -> {
                onLinkClicked(activity, comment.getContextUrl().toUriString())
            }

            RedditCommentAction.ACTION_MENU -> showActionMenu(
                activity,
                commentListingFragment,
                renderableComment,
                commentView,
                changeDataManager,
                postLocked
            )

            RedditCommentAction.BACK -> activity.onBackPressedDispatcher.onBackPressed()
        }
    }

    fun action(
        activity: AppCompatActivity,
        comment: RedditComment,
        @RedditAction action: Int,
        changeDataManager: RedditChangeDataManager
    ) {
        val user: RedditAccount =             RedditAccountManager.Companion.getInstance(activity).getDefaultAccount()

        if (user.isAnonymous) {
            showMustBeLoggedInDialog(activity)
            return
        }

        val wasUpvoted = changeDataManager.isUpvoted(comment.idAndType)
        val wasDownvoted = changeDataManager.isUpvoted(comment.idAndType)

        when (action) {
            RedditAPI.ACTION_DOWNVOTE -> if (!comment.archived) {
                changeDataManager.markDownvoted(
                    now(),
                    comment.idAndType
                )
            }

            RedditAPI.ACTION_UNVOTE -> if (!comment.archived) {
                changeDataManager.markUnvoted(
                    now(),
                    comment.idAndType
                )
            }

            RedditAPI.ACTION_UPVOTE -> if (!comment.archived) {
                changeDataManager.markUpvoted(
                    now(),
                    comment.idAndType
                )
            }

            RedditAPI.ACTION_SAVE -> changeDataManager.markSaved(
                now(),
                comment.idAndType,
                true
            )

            RedditAPI.ACTION_UNSAVE -> changeDataManager.markSaved(
                now(),
                comment.idAndType,
                false
            )

            RedditAPI.ACTION_DELETE -> {}
            RedditAPI.ACTION_HIDE, RedditAPI.ACTION_UNHIDE -> {}
        }

        val vote = ((action == RedditAPI.ACTION_DOWNVOTE
                ) or (action == RedditAPI.ACTION_UPVOTE
                ) or (action == RedditAPI.ACTION_UNVOTE))

        if (comment.archived && vote) {
            Toast.makeText(activity, string.error_archived_vote, Toast.LENGTH_SHORT)
                .show()
            return
        }

        RedditAPI.action(
            CacheManager.Companion.getInstance(activity),
            object : ActionResponseHandler(activity) {
                protected override fun onCallbackException(t : Throwable) {
                    throw RuntimeException(t)
                }

                protected override fun onFailure(error: RRError) {
                    revertOnFailure()
                    showResultDialog(activity, error)
                }

                override fun onSuccess() {
                    if (action == RedditAPI.ACTION_DELETE) {
                        quickToast(context, string.delete_success)
                    }
                }

                fun revertOnFailure() {
                    when (action) {
                        RedditAPI.ACTION_DOWNVOTE, RedditAPI.ACTION_UNVOTE, RedditAPI.ACTION_UPVOTE -> {
                            if (wasUpvoted) {
                                changeDataManager.markUpvoted(
                                    now(),
                                    comment.idAndType
                                )
                            } else if (wasDownvoted) {
                                changeDataManager.markDownvoted(
                                    now(),
                                    comment.idAndType
                                )
                            } else {
                                changeDataManager.markUnvoted(
                                    now(),
                                    comment.idAndType
                                )
                            }
                            changeDataManager.markSaved(
                                now(),
                                comment.idAndType,
                                false
                            )
                        }

                        RedditAPI.ACTION_SAVE -> changeDataManager.markSaved(
                            now(),
                            comment.idAndType,
                            false
                        )

                        RedditAPI.ACTION_UNSAVE -> changeDataManager.markSaved(
                            now(),
                            comment.idAndType,
                            true
                        )

                        RedditAPI.ACTION_DELETE -> {}
                        RedditAPI.ACTION_HIDE, RedditAPI.ACTION_UNHIDE -> {}
                    }
                }
            }, user, comment.idAndType, action, activity
        )
    }

    enum class RedditCommentAction {
        UPVOTE,
        UNVOTE,
        DOWNVOTE,
        SAVE,
        UNSAVE,
        REPORT,
        SHARE,
        COPY_TEXT,
        COPY_URL,
        REPLY,
        USER_PROFILE,
        COMMENT_LINKS,
        COLLAPSE,
        EDIT,
        DELETE,
        EXTERNAL,
        PROPERTIES,
        CONTEXT,
        GO_TO_COMMENT,
        ACTION_MENU,
        BACK
    }

    private class RCVMenuItem(
        context: Context,
        titleRes: Int,
        val action: RedditCommentAction
    ) {
        val title: String

        init {
            this.title = context.getString(titleRes)
        }
    }
}
