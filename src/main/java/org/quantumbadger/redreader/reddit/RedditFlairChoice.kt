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
package org.quantumbadger.redreader.reddit

import android.os.Parcel
import android.os.Parcelable
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.jsonwrap.JsonArray
import org.quantumbadger.redreader.jsonwrap.JsonObject

class RedditFlairChoice private constructor(
    val text: String,
    val templateId: String
) : Parcelable {
    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(text)
        dest.writeString(templateId)
    }

    override fun toString(): String {
        return "RedditFlairChoice(" +
                "text='" + text + '\'' +
                ", templateId='" + templateId + '\'' +
                ')'
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }

        if (o !is RedditFlairChoice) {
            return false
        }

        val other = o
        return text == other.text && templateId == other.templateId
    }

    override fun hashCode(): Int {
        return text.hashCode() + 37 * templateId.hashCode()
    }

    companion object {
        fun fromJsonList(json: JsonArray): Optional<MutableList<RedditFlairChoice>> {
            val result = ArrayList<RedditFlairChoice?>(json.size())

            for (value in json) {
                val `object` = value.asObject()

                if (`object` == null) {
                    return Optional.Companion.empty<MutableList<RedditFlairChoice?>?>()
                }

                val choice: Optional<RedditFlairChoice> = fromJson(`object`)

                if (choice.isEmpty) {
                    return Optional.Companion.empty<MutableList<RedditFlairChoice?>?>()
                }

                result.add(choice.get())
            }

            return Optional.Companion.of<MutableList<RedditFlairChoice?>?>(result)
        }

        fun fromJson(
            json: JsonObject
        ): Optional<RedditFlairChoice> {
            val flairText = json.getString("flair_text")
            val flairTemplateId = json.getString("flair_template_id")

            if (flairText == null || flairTemplateId == null) {
                return Optional.Companion.empty<RedditFlairChoice>()
            }

            return Optional.Companion.of<RedditFlairChoice>(
                RedditFlairChoice(
                    flairText,
                    flairTemplateId
                )
            )
        }

        val CREATOR: Parcelable.Creator<RedditFlairChoice?> =
            object : Parcelable.Creator<RedditFlairChoice?> {
                override fun createFromParcel(`in`: Parcel): RedditFlairChoice {
                    val text = `in`.readString()
                    val templateId = `in`.readString()

                    return RedditFlairChoice(text!!, templateId!!)
                }

                override fun newArray(size: Int): Array<RedditFlairChoice?> {
                    return arrayOfNulls<RedditFlairChoice>(size)
                }
            }
    }
}
