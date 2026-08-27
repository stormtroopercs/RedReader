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
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.quantumbadger.redreader.reddit.things

import android.os.Parcel
import android.os.Parcelable
import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.jsonwrap.JsonObject.JsonDeserializable

class SubredditCanonicalId(name: String) : Comparable<SubredditCanonicalId>, Parcelable,
    JsonDeserializable {
    private val mId: String

    init {
        var name = name
        name = StringUtils.asciiLowercase(name.trim { it <= ' ' })
        val userSr: String? = RedditSubreddit.Companion.stripUserPrefix(name)

        if (userSr != null) {
            mId = "/user/" + userSr
        } else {
            mId = "/r/" + RedditSubreddit.Companion.stripRPrefix(name)
        }
    }

    val displayNameLowercase: String
        get() {
            if (mId.startsWith("/user/")) {
                return mId
            }

            return mId.substring(3)
        }

    override fun toString(): String {
        return mId
    }

    override fun hashCode(): Int {
        return mId.hashCode()
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }

        if (obj !is SubredditCanonicalId) {
            return false
        }

        return obj.mId == mId
    }

    override fun compareTo(o: SubredditCanonicalId): Int {
        return mId.compareTo(o.mId)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(mId)
    }

    companion object {
        val CREATOR: Parcelable.Creator<SubredditCanonicalId?> =
            object : Parcelable.Creator<SubredditCanonicalId?> {
                override fun createFromParcel(`in`: Parcel): SubredditCanonicalId {
                    try {
                        return SubredditCanonicalId(`in`.readString()!!)
                    } catch (e: InvalidSubredditNameException) {
                        throw RuntimeException(e)
                    }
                }

                override fun newArray(size: Int): Array<SubredditCanonicalId?> {
                    return arrayOfNulls<SubredditCanonicalId>(size)
                }
            }
    }
}
