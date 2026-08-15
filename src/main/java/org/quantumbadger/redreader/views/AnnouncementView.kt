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
package org.quantumbadger.redreader.views

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.receivers.announcements.Announcement
import org.quantumbadger.redreader.receivers.announcements.AnnouncementDownloader

class AnnouncementView(
    activity: AppCompatActivity,
    announcement: Announcement
) : FrameLayout(activity) {
    init {
        LayoutInflater.from(activity)
            .inflate(R.layout.announcement_view, this, true)

        val textTitle: TextView = findViewById(R.id.announcement_view_title)
        val textMessage: TextView = findViewById(R.id.announcement_view_message)

        val buttonView: Button = findViewById(R.id.announcement_view_button_view)
        val buttonDismiss: Button = findViewById(R.id.announcement_view_button_dismiss)

        textTitle.setText(announcement.title)

        if (announcement.message == null) {
            textMessage.setVisibility(GONE)
        } else {
            textMessage.setText(announcement.message)
        }

        buttonView.setOnClickListener(OnClickListener { v: View? ->
            onLinkClicked(activity, announcement.url)
            RRAnimationShrinkHeight(this).start()
            AnnouncementDownloader.markAsRead(activity, announcement)
        })

        buttonDismiss.setOnClickListener(OnClickListener { v: View? ->
            RRAnimationShrinkHeight(this).start()
            AnnouncementDownloader.markAsRead(activity, announcement)
        })
    }
}
