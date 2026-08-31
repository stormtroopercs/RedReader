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
package com.stormtroopercs.materialreader.reddit.things

import android.os.Parcel
import android.os.Parcelable
import com.stormtroopercs.materialreader.common.ParcelUtils
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.jsonwrap.JsonObject.JsonDeserializable
import org.apache.commons.text.StringEscapeUtils

class RedditUser :
	Parcelable,
	JsonDeserializable {
	var comment_karma: Int? = null
	var link_karma: Int? = null

	var created: Long? = null
	var created_utc: Long? = null

	var has_mail: Boolean? = null
	var has_mod_mail: Boolean? = null
	var is_friend: Boolean? = null
	var is_gold: Boolean? = null
	var is_mod: Boolean? = null
	var is_suspended: Boolean? = null
	var over_18: Boolean? = null
	var is_blocked: Boolean? = null

	var id: String? = null
	var name: String? = null
	var icon_img: String? = null

	// Modern Reddit API: the account picture is `icon` (a base64 data URI,
	// e.g. `data:image/jpeg;base64,...`), with `icon_size` giving its square
	// dimension in px. `icon_img` is the legacy field (a plain URL); keep it
	// as the fallback so old API shapes still resolve.
	var icon: String? = null
	var icon_size: Int? = null

	var is_employee: Boolean? = null

	override fun describeContents(): Int = 0

	constructor()

	// one of the many reasons why the Android API is awful
	private constructor(`in`: Parcel) {
		comment_karma = `in`.readInt()
		link_karma = `in`.readInt()

		created = `in`.readLong()
		created_utc = `in`.readLong()

		val inHasMail = `in`.readInt()
		if (inHasMail == 0) {
			has_mail = null
		} else {
			has_mail = inHasMail == 1
		}

		val inHasModMail = `in`.readInt()
		if (inHasModMail == 0) {
			has_mod_mail = null
		} else {
			has_mod_mail = inHasModMail == 1
		}

		is_friend = `in`.readInt() == 1
		is_gold = `in`.readInt() == 1
		is_mod = `in`.readInt() == 1
		over_18 = `in`.readInt() == 1
		is_blocked = `in`.readInt() == 1

		id = `in`.readString()
		name = `in`.readString()
		icon_img = `in`.readString()

		icon = `in`.readString()
		icon_size = `in`.readInt()
		if (icon_size == 0) {
			icon_size = null
		}

		is_employee = ParcelUtils.readNullableBoolean(`in`)
	}

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeInt(comment_karma!!)
		parcel.writeInt(link_karma!!)

		parcel.writeLong(created!!)
		parcel.writeLong(created_utc!!)

		if (has_mail == null) {
			parcel.writeInt(0)
		} else {
			parcel.writeInt(if (has_mail == true) 1 else -1)
		}

		if (has_mod_mail == null) {
			parcel.writeInt(0)
		} else {
			parcel.writeInt(if (has_mod_mail == true) 1 else -1)
		}

		parcel.writeInt(if (is_friend == true) 1 else 0)
		parcel.writeInt(if (is_gold == true) 1 else 0)
		parcel.writeInt(if (is_mod == true) 1 else 0)
		parcel.writeInt(if (over_18 == true) 1 else 0)
		parcel.writeInt(if (is_blocked == true) 1 else 0)

		parcel.writeString(id)
		parcel.writeString(name)
		parcel.writeString(icon_img)

		parcel.writeString(icon)
		parcel.writeInt(icon_size ?: 0)

		ParcelUtils.writeNullableBoolean(parcel, is_employee)
	}

	val iconUrl: UriString?
		get() {
			// Prefer the modern `icon` (data URI); fall back to the legacy
			// `icon_img` (plain URL).
			val raw = icon?.takeIf { it.isNotEmpty() } ?: icon_img
			if (raw == null || raw.isEmpty()) {
				return null
			} else {
				return UriString(StringEscapeUtils.unescapeHtml4(raw))
			}
		}

	fun fullname(): String = String.format("%s_%s", RedditThing.Companion.KIND_USER, id)

	companion object {
		val CREATOR: Parcelable.Creator<RedditUser?> =
			object : Parcelable.Creator<RedditUser?> {
				override fun createFromParcel(`in`: Parcel): RedditUser = RedditUser(`in`)

				override fun newArray(size: Int): Array<RedditUser?> = arrayOfNulls<RedditUser>(size)
			}
	}
}
