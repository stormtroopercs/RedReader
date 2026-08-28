/*******************************************************************************
 * This file is part of MaterialReader.
 *
 * MaterialReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MaterialReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MaterialReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.stormtroopercs.materialreader.fragments

import android.content.Context
import android.os.Bundle
import android.widget.LinearLayout
import androidx.core.os.BundleCompat
import com.stormtroopercs.materialreader.R.string
import com.stormtroopercs.materialreader.activities.BaseActivity
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.reddit.things.RedditUser
import java.util.Objects

class UserPropertiesDialog : PropertiesDialog() {
    override fun getTitle(context : Context): String {
        return Objects.requireNonNull<RedditUser>(
            BundleCompat.getParcelable<RedditUser?>(
                requireArguments(),
                "user",
                RedditUser::class.java
            )
        ).name!!
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
                    if (user.is_friend == true) string.general_true else string.general_false,
                    false
                )
            )
        }

        if (user.is_gold != null) {
            items.addView(
                propView(
                    context,
                    string.userprofile_isgold,
                    if (user.is_gold == true) string.general_true else string.general_false,
                    false
                )
            )
        }

        if (user.is_mod != null) {
            items.addView(
                propView(
                    context,
                    string.userprofile_moderator,
                    if (user.is_mod == true) string.general_true else string.general_false,
                    false
                )
            )
        }

        if (user.is_employee != null) {
            items.addView(
                propView(
                    context,
                    string.userprofile_tag_admin,
                    if (user.is_employee == true) string.general_true else string.general_false,
                    false
                )
            )
        }

        if (user.is_suspended != null) {
            items.addView(
                propView(
                    context,
                    string.userprofile_tag_suspended,
                    if (user.is_suspended == true) string.general_true else string.general_false,
                    false
                )
            )
        }

        if (user.is_blocked != null) {
            items.addView(
                propView(
                    context,
                    string.userprofile_tag_blocked,
                    if (user.is_blocked == true) string.general_true else string.general_false,
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
