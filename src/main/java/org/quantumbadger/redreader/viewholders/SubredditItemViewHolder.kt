package org.quantumbadger.redreader.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.apache.commons.text.StringEscapeUtils
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.RRThemeAttributes
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.reddit.SubredditDetails
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElement
import org.quantumbadger.redreader.reddit.prepared.html.HtmlReader
import org.quantumbadger.redreader.views.SubredditToolbar
import java.text.NumberFormat
import java.util.Locale

class SubredditItemViewHolder(
    parent: ViewGroup,
    private val mActivity: BaseActivity
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context)
        .inflate(R.layout.subreddit_item_view, parent, false)
) {
    private val mTheme: RRThemeAttributes
    private val mBodyFontScale: Float

    private val mPrimaryText: TextView
    private val mSubText: TextView
    private val mSupportingText: FrameLayout
    private val mActions: SubredditToolbar
    private val mGoButton: View

    init {
        mTheme = RRThemeAttributes(mActivity)
        mBodyFontScale = PrefsUtility.appearance_fontscale_bodytext()

        mPrimaryText = itemView.findViewById(R.id.subreddit_item_view_primary_text)
        mSubText = itemView.findViewById(R.id.subreddit_item_view_sub_text)
        mSupportingText = itemView.findViewById(R.id.subreddit_item_view_supporting_text)
        mActions = itemView.findViewById(R.id.subreddit_item_view_actions)
        mGoButton = itemView.findViewById(R.id.subreddit_item_view_go)
    }

    fun bind(subreddit: SubredditDetails) {
        mPrimaryText.text = subreddit.name

        val subtitle: String?
        if (subreddit.subscribers == null) {
            subtitle = null
        } else {
            subtitle = mActivity.getString(
                string.header_subscriber_count,
                NumberFormat.getNumberInstance(Locale.getDefault())
                    .format(subreddit.subscribers)
            )
        }

        if (subtitle == null) {
            mSubText.visibility = View.GONE
        } else {
            mSubText.visibility = View.VISIBLE
            mSubText.text = subtitle
        }

        mSupportingText.removeAllViews()

        val pubDesc = subreddit.publicDescriptionHtmlEscaped
        if (pubDesc != null && pubDesc.trim().isNotEmpty()) {
            val body: BodyElement = HtmlReader.parse(
                StringEscapeUtils.unescapeHtml4(pubDesc),
                mActivity
            )

            mSupportingText.visibility = View.VISIBLE

            mSupportingText.addView(
                body.generateView(
                    mActivity,
                    mTheme.rrCommentBodyCol,
                    13.0f * mBodyFontScale,
                    false
                )
            )
        } else {
            mSupportingText.visibility = View.GONE
        }

        mActions.bindSubreddit(subreddit, Optional.empty())

        mGoButton.setOnClickListener { v: View? -> onLinkClicked(mActivity, subreddit.url) }
    }
}
