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
package org.quantumbadger.redreader.reddit.things

import org.quantumbadger.redreader.jsonwrap.JsonObject
import org.quantumbadger.redreader.jsonwrap.JsonObject.JsonDeserializable
import java.lang.reflect.InvocationTargetException

class RedditThing : JsonDeserializable {
    enum class Kind {
        POST, USER, COMMENT, MESSAGE, SUBREDDIT, MORE_COMMENTS, LISTING
    }

    var kind: String?=null
    var data: JsonObject?=null

    fun getKind(): Kind {
        val result: Kind = kinds.get(this.kind)!!

        if (result == null) {
            throw RuntimeException("Unknown thing type: " + this.kind)
        }

        return result
    }

    @Throws(
        InstantiationException::class,
        IllegalAccessException::class,
        NoSuchMethodException::class,
        InvocationTargetException::class
    )
    fun asSubreddit(): RedditSubreddit {
        return data!!.asObject<RedditSubreddit>(RedditSubreddit::class.java)
    }

    @Throws(
        InstantiationException::class,
        IllegalAccessException::class,
        NoSuchMethodException::class,
        InvocationTargetException::class
    )
    fun asUser(): RedditUser {
        return data!!.asObject<RedditUser>(RedditUser::class.java)
    }

    companion object {
        const val KIND_USER: String = "t2"

        private val kinds: MutableMap<String?, Kind>

        init {
            kinds = HashMap<String?, Kind>()
            kinds.put("t1", Kind.COMMENT)
            kinds.put(KIND_USER, Kind.USER)
            kinds.put("t3", Kind.POST)
            kinds.put("t4", Kind.MESSAGE)
            kinds.put("t5", Kind.SUBREDDIT)
            kinds.put("more", Kind.MORE_COMMENTS)
            kinds.put("Listing", Kind.LISTING)
        }
    }
}
