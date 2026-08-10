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
package org.quantumbadger.redreader.fragments

import android.content.Context
import android.os.Bundle
import android.widget.LinearLayout
import androidx.core.os.BundleCompat
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.reddit.things.RedditUser
import java.util.Objects

class UserPropertiesDialog : PropertiesDialog() {
    override fun getTitle(context: Context?): String {
        return Objects.requireNonNull<RedditUser?>(
            BundleCompat.getParcelable<RedditUser?>(
                requireArguments(),
                "user",
                RedditUser::class.java
            )
        ).name
    }

    override fun prepare(
        context: BaseActivity,
        items: LinearLayout
    ) {
        val user = Objects.requireNonNull<RedditUser>(
            BundleCompat.getParcelable<RedditUser?>(
                requireArguments(),
                "user",
                RedditUser::class.java
            )
        )

        items.addView(
            propView(
                context,
                string.props_id,
                user.id,
                true
            )
        )

        if (user.created_utc != null) {
            items.addView(
                propView(
                    context,
                    string.userprofile_created,
                    TimestampUTC.fromUtcSecs(user.created_utc!!).format(),
                    false
                )
            )
        }

        if (user.link_karma != null) {
            items.addView(
                propView(
                    context,
                    string.karma_link,
                    user.link_karma.toString(),
                    false
                )
            )
        }

        if (user.comment_karma != null) {
            items.addView(
                propView(
                    context,
                    string.karma_comment,
                    user.comment_karma.toString(),
                    false
                )
            )
        }

        if (user.is_friend != null) {
            items.addView(
                propView(
                    context,
                    string.userprofile_isfriend,
                    if (user.is_friend) string.general_true else string.general_false,
                    false
                )
            )
        }

        if (user.is_gold != null) {
            items.addView(
                propView(
                    context,
                    string.userprofile_isgold,
                    if (user.is_gold) string.general_true else string.general_false,
                    false
                )
            )
        }

        if (user.is_mod != null) {
            items.addView(
                propView(
                    context,
                    string.userprofile_moderator,
                    if (user.is_mod) string.general_true else string.general_false,
                    false
                )
            )
        }

        if (user.is_employee != null) {
            items.addView(
                propView(
                    context,
                    string.userprofile_tag_admin,
                    if (user.is_employee) string.general_true else string.general_false,
                    false
                )
            )
        }

        if (user.is_suspended != null) {
            items.addView(
                propView(
                    context,
                    string.userprofile_tag_suspended,
                    if (user.is_suspended) string.general_true else string.general_false,
                    false
                )
            )
        }

        if (user.is_blocked != null) {
            items.addView(
                propView(
                    context,
                    string.userprofile_tag_blocked,
                    if (user.is_blocked) string.general_true else string.general_false,
                    false
                )
            )
        }

        if (user.icon_img != null) {
            items.addView(
                propView(
                    context,
                    string.userprofile_avatar,
                    user.icon_img,
                    false
                )
            )
        }
    }

    companion object {
        fun newInstance(user: RedditUser?): UserPropertiesDialog {
            val pp = UserPropertiesDialog()

            val args = Bundle()
            args.putParcelable("user", user)
            pp.setArguments(args)

            return pp
        }
    }
}
