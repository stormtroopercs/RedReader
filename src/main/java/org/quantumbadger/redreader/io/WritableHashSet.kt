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
package org.quantumbadger.redreader.io

import org.quantumbadger.redreader.common.UnexpectedInternalStateException
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.common.time.TimestampUTC.Companion.fromUtcMs
import org.quantumbadger.redreader.io.WritableObject.WritableField
import org.quantumbadger.redreader.io.WritableObject.WritableObjectKey
import org.quantumbadger.redreader.io.WritableObject.WritableObjectTimestamp
import org.quantumbadger.redreader.io.WritableObject.WritableObjectVersion
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.Iterable
import kotlin.collections.MutableCollection
import kotlin.collections.MutableIterator

class WritableHashSet : WritableObject<String?>, Iterable<String?> {
    @Transient
    private var hashSet: HashSet<String>? = null

    @WritableField
    private var serialised: String? = null

    @WritableObjectKey
    private val key: String?

    @WritableObjectTimestamp
    private val timestamp: Long

    constructor(
        data: HashSet<String>?,
        timestamp: TimestampUTC,
        key: String?
    ) {
        this.hashSet = data
        this.timestamp = timestamp.toUtcMs()
        this.key = key
        serialised = Companion.listToEscapedString(hashSet!!)
    }

    private constructor(serializedData: String?, timestamp: Long, key: String?) {
        this.timestamp = timestamp
        this.key = key
        serialised = serializedData
    }

    constructor(creationData: WritableObject.CreationData) {
        this.timestamp = creationData.timestamp
        this.key = creationData.key
    }

    override fun toString(): String {
        throw UnexpectedInternalStateException(
            "Using toString() is the wrong way to serialise a WritableHashSet"
        )
    }

    fun serializeWithMetadata(): String {
        val result = ArrayList<String>(3)
        result.add(serialised!!)
        result.add(timestamp.toString())
        result.add(key!!)
        return listToEscapedString(result)
    }

    @Synchronized
    fun toHashset(): HashSet<String> {
        if (hashSet != null) {
            return hashSet!!
        }
        return (HashSet<String>(escapedStringToList(serialised)).also { hashSet = it })
    }

    override fun getKey(): String? {
        return key
    }

    override fun getTimestamp(): TimestampUTC {
        return fromUtcMs(timestamp)
    }

    override fun iterator(): MutableIterator<String?> {
        return toHashset().iterator()
    }

    companion object {
        @WritableObjectVersion
        var DB_VERSION: Int = 1

        fun unserializeWithMetadata(raw: String?): WritableHashSet {
            val data: ArrayList<String?> = escapedStringToList(raw)
            return WritableHashSet(data.get(0), data.get(1)!!.toLong(), data.get(2))
        }

        fun listToEscapedString(list: MutableCollection<String>): String {
            if (list.isEmpty()) {
                return ""
            }

            val sb = StringBuilder()

            for (str in list) {
                for (i in 0..<str.length) {
                    val c = str.get(i)

                    when (c) {
                        '\\' -> sb.append("\\\\")
                        ';' -> sb.append("\\;")
                        else -> sb.append(c)
                    }
                }

                sb.append(';')
            }

            return sb.toString()
        }

        fun escapedStringToList(str: String?): ArrayList<String?> {
            var str = str
            val result = ArrayList<String?>()

            if (str != null) {
                // Workaround to improve parsing of lists saved by older versions of the app

                if (!str.isEmpty() && !str.endsWith(";")) {
                    str += ";"
                }

                var isEscaped = false
                val sb = StringBuilder()

                for (i in 0..<str.length) {
                    val c = str.get(i)

                    if (c == ';' && !isEscaped) {
                        result.add(sb.toString())
                        sb.setLength(0)
                    } else if (c == '\\') {
                        if (isEscaped) {
                            sb.append('\\')
                        }
                    } else {
                        sb.append(c)
                    }

                    isEscaped = c == '\\' && !isEscaped
                }
            }

            return result
        }
    }
}
