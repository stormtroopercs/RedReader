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

import android.os.Parcel
import com.stormtroopercs.materialreader.image.ImageInfo
import com.stormtroopercs.materialreader.image.ImageInfo.HasAudio

object ParcelHelper {
    fun readBoolean(`in`: Parcel): Boolean {
        return `in`.readByte().toInt() == 1
    }

    fun readNullableString(`in`: Parcel): String? {
        val isNull = readBoolean(`in`)
        if (isNull) {
            return null
        }

        return `in`.readString()
    }

    fun readNullableImageInfoMediaType(`in`: Parcel): ImageInfo.MediaType? {
        val isNull = readBoolean(`in`)
        if (isNull) {
            return null
        }

        return ImageInfo.MediaType.valueOf(`in`.readString()!!)
    }

    fun readImageInfoHasAudio(`in`: Parcel): HasAudio {
        return HasAudio.valueOf(`in`.readString()!!)
    }

    fun writeNullableEnum(
        parcel: Parcel,
        value: Enum<*>?
    ) {
        if (value == null) {
            writeBoolean(parcel, false)
        } else {
            writeBoolean(parcel, true)
            parcel.writeString(value.name)
        }
    }

    fun writeNonNullEnum(parcel: Parcel, value: Enum<*>) {
        parcel.writeString(value.name)
    }

    fun readNullableInt(`in`: Parcel): Int? {
        val isNull = readBoolean(`in`)
        if (isNull) {
            return null
        }

        return `in`.readInt()
    }

    fun readNullableLong(`in`: Parcel): Long? {
        val isNull = readBoolean(`in`)
        if (isNull) {
            return null
        }

        return `in`.readLong()
    }

    fun readNullableBoolean(`in`: Parcel): Boolean? {
        val isNull = readBoolean(`in`)
        if (isNull) {
            return null
        }

        return readBoolean(`in`)
    }

    fun writeBoolean(parcel: Parcel, b: Boolean) {
        parcel.writeByte((if (b) 1 else 0).toByte())
    }

    fun writeNullableString(parcel: Parcel, value: String?) {
        if (value == null) {
            writeBoolean(parcel, false)
        } else {
            writeBoolean(parcel, true)
            parcel.writeString(value)
        }
    }

    fun writeNullableLong(parcel: Parcel, value: Long?) {
        if (value == null) {
            writeBoolean(parcel, false)
        } else {
            writeBoolean(parcel, true)
            parcel.writeLong(value)
        }
    }

    fun writeNullableBoolean(parcel: Parcel, value: Boolean?) {
        if (value == null) {
            writeBoolean(parcel, false)
        } else {
            writeBoolean(parcel, true)
            writeBoolean(parcel, value)
        }
    }
}
