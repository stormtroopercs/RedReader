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
import com.stormtroopercs.materialreader.common.HasUniqueId
import com.stormtroopercs.materialreader.common.ParcelHelper
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.common.time.TimestampUTC.Companion.fromUtcMs
import com.stormtroopercs.materialreader.io.WritableObject
import com.stormtroopercs.materialreader.io.WritableObject.WritableField
import com.stormtroopercs.materialreader.io.WritableObject.WritableObjectTimestamp
import com.stormtroopercs.materialreader.io.WritableObject.WritableObjectVersion
import com.stormtroopercs.materialreader.jsonwrap.JsonObject.JsonDeserializable
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

class RedditSubreddit : Parcelable, Comparable<RedditSubreddit>,
    WritableObject<SubredditCanonicalId>, JsonDeserializable, HasUniqueId {
    override val key: SubredditCanonicalId
        get() {
        try {
            return this.canonicalId
        } catch (e: InvalidSubredditNameException) {
            throw RuntimeException(
                String.format(
                    Locale.US,
                    "Cannot save subreddit '%s'",
                    url
                ), e
            )
        }
        }

    override val timestamp: TimestampUTC get() = fromUtcMs(downloadTime)

    @WritableField
    var header_img: String? = null

    @WritableField
    var header_title: String? = null

    @WritableField
    var description: String? = null

    @WritableField
    var description_html: String? = null
    var public_description_html: String? = null

    @WritableField
    var id: String? = null

    @WritableField
    var name: String = ""

    @WritableField
    var title: String? = null

    @WritableField
    var display_name: String? = null

    @WritableField
    var url: String? = null

    @WritableField
    var created: Long = 0

    @WritableField
    var created_utc: Long = 0

    @WritableField
    var accounts_active: Int? = null

    @WritableField
    var subscribers: Int? = null

    @WritableField
    var over18: Boolean? = null

    @WritableObjectTimestamp
    var downloadTime: Long = 0

    constructor(creationData: WritableObject.CreationData) : this() {
        downloadTime = creationData.timestamp
    }

    @get:Throws(InvalidSubredditNameException::class)
    val canonicalId: SubredditCanonicalId
        get() = SubredditCanonicalId(url!!)

    fun getUrl(): UriString {
        if (url != null) {
            return UriString(url!!)
        }

        return UriString("https://reddit.com/r/" + display_name)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeString(header_img)
        out.writeString(header_title)
        out.writeString(description)
        out.writeString(description_html)
        out.writeString(public_description_html)
        out.writeString(id)
        out.writeString(name)
        out.writeString(title)
        out.writeString(display_name)
        out.writeString(url)
        out.writeLong(created)
        out.writeLong(created_utc)
        out.writeInt((if (accounts_active == null) -1 else accounts_active)!!)
        out.writeInt((if (subscribers == null) -1 else subscribers)!!)
        ParcelHelper.writeNullableBoolean(out, over18)
    }

    constructor()

    constructor(url: String?, title: String?, isSortable: Boolean) {
        this.url = url
        this.title = title
    }

    constructor(parcel: Parcel) {
        header_img = parcel.readString()
        header_title = parcel.readString()
        description = parcel.readString()
        description_html = parcel.readString()
        public_description_html = parcel.readString()
        id = parcel.readString()
        name = parcel.readString() ?: ""
        title = parcel.readString()
        display_name = parcel.readString()
        url = parcel.readString()
        created = parcel.readLong()
        created_utc = parcel.readLong()

        accounts_active = parcel.readInt()
        subscribers = parcel.readInt()

        if (accounts_active!! < 0) {
            accounts_active = null
        }
        if (subscribers!! < 0) {
            subscribers = null
        }

        over18 = ParcelHelper.readNullableBoolean(parcel)
    }

    override fun compareTo(another: RedditSubreddit): Int {
        return display_name!!.compareTo(another.display_name!!, ignoreCase = true)
    }

    override val uniqueId: String get() = id!!

    companion object {
        @WritableObjectVersion
        @Suppress("PropertyName")
        var DB_VERSION: Int = 1

        private val NAME_PATTERN: Pattern = Pattern.compile(
            "((/)?r/)?([\\w\\+\\-\\.:]+)/?"
        )
        @Suppress("PropertyName")
        private val USER_PATTERN: Pattern = Pattern.compile(
            "/?(u/|user/)([\\w\\+\\-\\.:]+)/?"
        )

        @Throws(InvalidSubredditNameException::class)
        fun stripRPrefix(name: String): String? {
            val matcher: Matcher = NAME_PATTERN.matcher(name)
            if (matcher.matches()) {
                return matcher.group(3)
            } else {
                throw InvalidSubredditNameException(name)
            }
        }

        fun stripUserPrefix(name: String): String? {
            val matcher: Matcher = USER_PATTERN.matcher(name)
            if (matcher.matches()) {
                return matcher.group(2)
            } else {
                return null
            }
        }

        val CREATOR: Parcelable.Creator<RedditSubreddit?> =
            object : Parcelable.Creator<RedditSubreddit?> {
                override fun createFromParcel(`in`: Parcel): RedditSubreddit {
                    return RedditSubreddit(`in`)
                }

                override fun newArray(size: Int): Array<RedditSubreddit?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
