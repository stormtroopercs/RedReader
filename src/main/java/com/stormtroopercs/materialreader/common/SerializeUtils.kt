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
package com.stormtroopercs.materialreader.common

import com.github.luben.zstd.Zstd
import com.github.luben.zstd.ZstdInputStream
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

object SerializeUtils {
    private const val COMPRESSED_FILE_VERSION = 1

    @Suppress("PropertyName")
    private val COMPRESSED_FILE_USER_HEADER =         "MaterialReader compressed data\r\n".toByteArray(General.CHARSET_UTF8)

    private fun isInvalidHashKey(value: Any?): Boolean {
        if (value == null) {
            return false
        }

        return !(value is String
                || value is Byte
                || value is Char
                || value is Short
                || value is Int
                || value is Long
                || value is Boolean)
    }

    @JvmStatic
    @Throws(IOException::class, UnhandledTypeException::class)
    fun serialize(
        destination: DataOutputStream,
        value: Any?
    ) {
        if (value == null) {
            destination.writeByte(DataType.NULL.constant.toInt())
        } else if (value is Byte) {
            destination.writeByte(DataType.BYTE.constant.toInt())
            destination.writeByte(value.toInt())
        } else if (value is Char) {
            destination.writeByte(DataType.CHAR.constant.toInt())
            destination.writeChar(value.code)
        } else if (value is Short) {
            destination.writeByte(DataType.SHORT.constant.toInt())
            destination.writeShort(value.toInt())
        } else if (value is Int) {
            destination.writeByte(DataType.INT.constant.toInt())
            destination.writeInt(value)
        } else if (value is Long) {
            destination.writeByte(DataType.LONG.constant.toInt())
            destination.writeLong(value)
        } else if (value is Float) {
            destination.writeByte(DataType.FLOAT.constant.toInt())
            destination.writeFloat(value)
        } else if (value is Double) {
            destination.writeByte(DataType.DOUBLE.constant.toInt())
            destination.writeDouble(value)
        } else if (value is MutableSet<*>) {
            val set = value

            destination.writeByte(DataType.SET.constant.toInt())
            destination.writeInt(set.size)

            for (obj in set) {
                if (isInvalidHashKey(obj)) {
                    throw UnhandledTypeException(
                        "Invalid set entry type: " + value.javaClass.getCanonicalName()
                    )
                }

                serialize(destination, obj)
            }
        } else if (value is MutableList<*>) {
            val list = value

            destination.writeByte(DataType.LIST.constant.toInt())
            destination.writeInt(list.size)

            for (obj in list) {
                serialize(destination, obj)
            }
        } else if (value is MutableMap<*, *>) {
            val map = value

            destination.writeByte(DataType.MAP.constant.toInt())
            destination.writeInt(map.size)

            for (entry in map.entries) {
                if (isInvalidHashKey(entry.key)) {
                    throw UnhandledTypeException(
                        "Invalid map key type: " + value.javaClass.getCanonicalName()
                    )
                }

                serialize(destination, entry.key)
                serialize(destination, entry.value)
            }
        } else if (value is String) {
            destination.writeByte(DataType.STRING.constant.toInt())

            val bytes = value.toByteArray(General.CHARSET_UTF8)
            destination.writeInt(bytes.size)
            destination.write(bytes)
        } else if (value is Boolean) {
            destination.writeByte(DataType.BOOLEAN.constant.toInt())
            destination.writeBoolean(value)
        } else {
            throw UnhandledTypeException(
                "Unhandled type: " + value.javaClass.getCanonicalName()
            )
        }
    }

    @JvmStatic
    @Throws(IOException::class, UnhandledTypeException::class)
    fun deserialize(source: DataInputStream): Any? {
        val type: DataType = DataType.Companion.fromConstant(source.readByte())

        when (type) {
            DataType.NULL -> return null
            DataType.BYTE -> return source.readByte()
            DataType.CHAR -> return source.readChar()
            DataType.SHORT -> return source.readShort()
            DataType.INT -> return source.readInt()
            DataType.LONG -> return source.readLong()
            DataType.FLOAT -> return source.readFloat()
            DataType.DOUBLE -> return source.readDouble()
            DataType.BOOLEAN -> return source.readBoolean()

            DataType.SET -> {
                val count = source.readInt()
                val result = HashSet<Any?>(count)

                var i = 0
                while (i < count) {
                    result.add(deserialize(source))
                    i++
                }

                return result
            }

            DataType.LIST -> {
                val count = source.readInt()
                val result = ArrayList<Any?>(count)

                var i = 0
                while (i < count) {
                    result.add(deserialize(source))
                    i++
                }

                return result
            }

            DataType.MAP -> {
                val count = source.readInt()
                val result = HashMap<Any?, Any?>(count)

                var i = 0
                while (i < count) {
                    val key = deserialize(source)
                    val value = deserialize(source)
                    result.put(key, value)
                    i++
                }

                return result
            }

            DataType.STRING -> {
                val byteCount = source.readInt()
                val bytes = ByteArray(byteCount)
                source.readFully(bytes)

                return String(bytes, General.CHARSET_UTF8)
            }
        }

        throw UnhandledTypeException(
            "Unhandled deserialize type: " + type
        )
    }

    @Throws(IOException::class, UnhandledTypeException::class)
    fun serializeCompressed(
        destination: DataOutputStream,
        value: Any?
    ) {
        val data = ByteArrayOutputStream()
        serialize(DataOutputStream(data), value)

        val uncompressedBytes = data.toByteArray()

        val maxDestSize = Zstd.compressBound(uncompressedBytes.size.toLong())

        if (maxDestSize > Int.MAX_VALUE) {
            throw IOException("Max output size is greater than MAX_INT")
        }

        val compressedBytes = ByteArray(maxDestSize.toInt())

        val compressedSize = Zstd.compressByteArray(
            compressedBytes,
            0,
            compressedBytes.size,
            uncompressedBytes,
            0,
            uncompressedBytes.size,
            3
        ).toInt()

        destination.write(COMPRESSED_FILE_USER_HEADER)
        destination.writeInt(COMPRESSED_FILE_VERSION)

        destination.writeInt(compressedSize)
        destination.write(compressedBytes, 0, compressedSize)
    }

    @Throws(IOException::class, UnhandledTypeException::class)
    fun deserializeCompressed(source: DataInputStream): Any? {
        val userHeader = ByteArray(COMPRESSED_FILE_USER_HEADER.size)
        source.readFully(userHeader)

        if (!userHeader.contentEquals(COMPRESSED_FILE_USER_HEADER)) {
            throw IOException("Invalid user header")
        }

        val version = source.readInt()
        val compressedBytesLength = source.readInt()

        if (version != COMPRESSED_FILE_VERSION) {
            throw IOException("Unsupported version " + version)
        }

        val compressedData = ByteArray(compressedBytesLength)
        source.readFully(compressedData)

        return deserialize(
            DataInputStream(
                BufferedInputStream(
                    ZstdInputStream(ByteArrayInputStream(compressedData))
                )
            )
        )
    }

    private enum class DataType(constant: Int) {
        NULL(0),
        BYTE(1),
        CHAR(2),
        SHORT(3),
        INT(4),
        LONG(5),
        FLOAT(6),
        DOUBLE(7),
        SET(8),
        LIST(9),
        MAP(10),
        STRING(11),
        BOOLEAN(12);

        val constant: Byte

        init {
            this.constant = constant.toByte()
        }

        companion object {
            @Throws(UnhandledTypeException::class)
            fun fromConstant(value: Byte): DataType {
                if (value < 0 || value >= entries.size) {
                    throw UnhandledTypeException("Unknown type constant " + value.toInt())
                }

                return entries[value.toInt()]
            }
        }
    }

    class UnhandledTypeException(message: String?) : Exception(message)
}
