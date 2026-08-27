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
package org.quantumbadger.redreader.http

import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.General.readWholeStream
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import org.quantumbadger.redreader.jsonwrap.JsonValue
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

class FailedRequestBody {
    private var mBytes: Optional<ByteArray>
    private var mString: Optional<String>
    private var mJson: Optional<JsonValue>
    private var mAttemptedParse = false

    constructor(bytes: ByteArray) {
        mBytes = Optional.Companion.of<ByteArray>(bytes)
        mString = Optional.Companion.empty<String>()
        mJson = Optional.Companion.empty<JsonValue>()
    }

    constructor(value: String) {
        mBytes = Optional.Companion.empty<ByteArray>()
        mString = Optional.Companion.of<String>(value)
        mJson = Optional.Companion.empty<JsonValue>()
    }

    constructor(value: JsonValue) {
        mBytes = Optional.Companion.empty<ByteArray>()
        mString = Optional.Companion.empty<String>()
        mJson = Optional.Companion.of<JsonValue>(value)
    }

    @Synchronized
    override fun toString(): String {
        if (!mString.isPresent) {
            if (mBytes.isPresent) {
                mString = Optional.Companion.of<String>(String(mBytes.get(), General.CHARSET_UTF8))
            } else if (mJson.isPresent) {
                mString = Optional.Companion.of<String>(mJson.toString())
            } else {
                throw RuntimeException("No data present")
            }
        }

        return mString.get()
    }

    @Synchronized
    fun toBytes(): ByteArray {
        if (!mBytes.isPresent) {
            mBytes = Optional.Companion.of<ByteArray>(toString().toByteArray(General.CHARSET_UTF8))
        }

        return mBytes.get()
    }

    @Synchronized
    fun toJson(): Optional<JsonValue> {
        if (!mJson.isPresent && !mAttemptedParse) {
            mAttemptedParse = true

            try {
                mJson = Optional.Companion.of<JsonValue>(
                    JsonValue.Companion.parse(
                        ByteArrayInputStream(toBytes())
                    )
                )
            } catch (e: IOException) {
                // Ignore this
            }
        }

        return mJson
    }

    companion object {
        fun from(
            `is`: InputStream
        ): Optional<FailedRequestBody> {
            try {
                return Optional.Companion.of<FailedRequestBody>(
                    FailedRequestBody(
                        readWholeStream(
                            `is`
                        )
                    )
                )
            } catch (e: IOException) {
                return Optional.Companion.empty<FailedRequestBody>()
            }
        }

        fun from(
            `is`: GenericFactory<SeekableInputStream, IOException>
        ): Optional<FailedRequestBody> {
            try {
                return from(`is`.create())
            } catch (e: IOException) {
                return Optional.Companion.empty<FailedRequestBody>()
            }
        }
    }
}
