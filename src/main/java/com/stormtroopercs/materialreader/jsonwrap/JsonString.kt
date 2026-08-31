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

package com.stormtroopercs.materialreader.jsonwrap

import com.stormtroopercs.materialreader.common.StringUtils
import org.apache.commons.text.StringEscapeUtils

class JsonString(private val mValue: String) : JsonValue() {
	override fun prettyPrint(indent: Int, sb: StringBuilder) {
		sb.append('"').append(StringEscapeUtils.escapeJson(mValue)).append('"')
	}

	public override fun asBoolean(): Boolean? {
		val lowercase = StringUtils.asciiLowercase(mValue)

		when (lowercase) {
			"true", "t", "1" -> return true
			"false", "f", "0" -> return false
		}

		return null
	}

	public override fun asString(): String = mValue

	public override fun asDouble(): Double? = try {
		mValue.toDouble()
	} catch (e: NumberFormatException) {
		null
	}

	public override fun asLong(): Long? = try {
		mValue.toLong()
	} catch (e: NumberFormatException) {
		null
	}
}
