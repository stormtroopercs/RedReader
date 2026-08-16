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
package org.quantumbadger.redreader.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.SessionChangeListener
import org.quantumbadger.redreader.activities.SessionChangeListener.SessionChangeType
import org.quantumbadger.redreader.cache.CacheEntry
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.common.BetterSSB
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.time.TimeDuration.Companion.minutes
import org.quantumbadger.redreader.common.time.TimeFormatHelper.format
import org.quantumbadger.redreader.viewholders.VH1Text
import java.util.UUID

class SessionListAdapter(
    private val context: Context,
    url: UriString?,
    private val current: UUID?,
    private val type: SessionChangeType?,
    private val fragment: AppCompatDialogFragment
) : HeaderRecyclerAdapter<RecyclerView.ViewHolder?>() {
    private val sessions: ArrayList<CacheEntry>
    private val rrIconRefresh: Drawable?

    init {
        sessions = ArrayList<CacheEntry>(
            CacheManager.Companion.getInstance(context)
                .getSessions(
                    url,
                    RedditAccountManager.Companion.getInstance(context)
                        .getDefaultAccount()
                )
        )

        val attr = context.obtainStyledAttributes(intArrayOf(R.attr.rrIconRefresh))
        rrIconRefresh = AppCompatResources.getDrawable(context, attr.getResourceId(0, 0))
        attr.recycle()
    }

    override fun onCreateHeaderItemViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.list_item_1_text, parent, false)
        return VH1Text(v)
    }

    override fun onCreateContentItemViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.list_item_1_text, parent, false)
        return VH1Text(v)
    }

    override fun onBindHeaderItemViewHolder(
        holder: RecyclerView.ViewHolder?,
        position: Int
    ) {
        val vh = holder as VH1Text
        vh.text.setText(context.getString(string.options_refresh))
        vh.text.setCompoundDrawablesWithIntrinsicBounds(rrIconRefresh, null, null, null)
        vh.itemView.setOnClickListener(View.OnClickListener { v: View? ->
            (context as SessionChangeListener).onSessionRefreshSelected(type)
            fragment.dismiss()
        })
    }

    override fun onBindContentItemViewHolder(
        holder: RecyclerView.ViewHolder?,
        position: Int
    ) {
        val vh = holder as VH1Text
        val session = sessions.get(position)
        val name = BetterSSB()

        if (session.timestamp.elapsed().isLessThan(minutes(2))) {
            name.append(
                format(
                    session.timestamp.elapsedPeriod(),
                    context,
                    string.time_ago,
                    2
                ),
                0
            )
        } else {
            name.append(session.timestamp.format(), 0)
        }

        if (session.session == current) {
            val attr = context.obtainStyledAttributes(intArrayOf(R.attr.rrListSubtitleCol))
            val col = attr.getColor(0, 0)
            attr.recycle()

            name.append(
                "  (" + context.getString(string.session_active) + ")",
                BetterSSB.Companion.FOREGROUND_COLOR or BetterSSB.Companion.SIZE,
                col,
                0,
                0.8f
            )
        }

        vh.text.setText(name.get())

        vh.itemView.setOnClickListener(View.OnClickListener { v: View? ->
            val ce = sessions.get(position)
            (context as SessionChangeListener).onSessionSelected(ce.session, type)
            fragment.dismiss()
        })
    }

    override val contentItemCount: Int get() = sessions.size
}
