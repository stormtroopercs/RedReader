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

object ParcelUtils {
    fun writeNullableBoolean(
        parcel: Parcel,
        value: Boolean?
    ) {
        if (value == null) {
            parcel.writeInt(0)
        } else if (value) {
            parcel.writeInt(1)
        } else {
            parcel.writeInt(-1)
        }
    }

    fun readNullableBoolean(parcel: Parcel): Boolean? {
        val value = parcel.readInt()

        when (value) {
            -1 -> return false
            0 -> return null
            1 -> return true
        }

        throw RuntimeException("Invalid value " + value)
    }

    fun writeNullableInt(
        parcel: Parcel,
        value: Int?
    ) {
        if (value == null) {
            parcel.writeInt(0)
        } else {
            parcel.writeInt(1)
            parcel.writeInt(value)
        }
    }

    fun readNullableInt(parcel: Parcel): Int? {
        val present = parcel.readInt()

        if (present == 1) {
            return parcel.readInt()
        } else {
            return null
        }
    }

    fun writeNullableLong(
        parcel: Parcel,
        value: Long?
    ) {
        if (value == null) {
            parcel.writeLong(0)
        } else {
            parcel.writeLong(1)
            parcel.writeLong(value)
        }
    }

    fun readNullableLong(parcel: Parcel): Long? {
        val present = parcel.readLong()

        if (present == 1L) {
            return parcel.readLong()
        } else {
            return null
        }
    }
}
