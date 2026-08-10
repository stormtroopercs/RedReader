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
package org.quantumbadger.redreader.receivers.announcements

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class Payload {
    private val mStrings = HashMap<String?, String?>()
    private val mLongs = HashMap<String?, Long?>()
    private val mBooleans = HashMap<String?, Boolean?>()

    fun setString(key: String, value: String) {
        mStrings.put(key, value)
    }

    fun setLong(key: String, value: Long) {
        mLongs.put(key, value)
    }

    fun setBoolean(key: String, value: Boolean) {
        mBooleans.put(key, value)
    }

    fun getString(key: String): String? {
        return mStrings.get(key)
    }

    fun getLong(key: String): Long? {
        return mLongs.get(key)
    }

    fun getBoolean(key: String): Boolean? {
        return mBooleans.get(key)
    }

    fun toBytes(): ByteArray {
        val result = ByteArrayOutputStream()
        val dos = DataOutputStream(result)

        try {
            for (entry in mStrings.entries) {
                if (entry.value == null) {
                    continue
                }
                dos.writeByte(HEADER_ENTRY_STRING.toInt())
                dos.writeUTF(entry.key)
                dos.writeUTF(entry.value)
            }

            for (entry in mLongs.entries) {
                if (entry.value == null) {
                    continue
                }

                dos.writeByte(HEADER_ENTRY_LONG.toInt())
                dos.writeUTF(entry.key)
                dos.writeLong(entry.value!!)
            }

            for (entry in mBooleans.entries) {
                if (entry.value == null) {
                    continue
                }

                dos.writeByte(HEADER_ENTRY_BOOLEAN.toInt())
                dos.writeUTF(entry.key)
                dos.writeBoolean(entry.value!!)
            }

            dos.writeByte(HEADER_EOF.toInt())

            dos.flush()
            dos.close()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        return result.toByteArray()
    }

    companion object {
        private const val HEADER_EOF: Byte = 0
        private const val HEADER_ENTRY_STRING: Byte = 1
        private const val HEADER_ENTRY_LONG: Byte = 2
        private const val HEADER_ENTRY_BOOLEAN: Byte = 3

        @JvmStatic
        @Throws(IOException::class)
        fun fromBytes(data: ByteArray): Payload {
            DataInputStream(ByteArrayInputStream(data)).use { dis ->
                val result = Payload()
                while (true) {
                    val header = dis.readByte()

                    when (header) {
                        HEADER_EOF -> return result

                        HEADER_ENTRY_STRING -> result.setString(dis.readUTF(), dis.readUTF())
                        HEADER_ENTRY_LONG -> result.setLong(dis.readUTF(), dis.readLong())
                        HEADER_ENTRY_BOOLEAN -> result.setBoolean(dis.readUTF(), dis.readBoolean())
                        else -> throw IOException("Unknown entry header " + header)
                    }
                }
            }
        }
    }
}
