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
package org.quantumbadger.redreader.jsonwrap

class JsonBoolean private constructor(private val mValue: Boolean) : JsonValue() {
    protected override fun prettyPrint(indent: Int, sb: StringBuilder) {
        sb.append(if (mValue) "true" else "false")
    }

    public override fun asBoolean(): Boolean {
        return mValue
    }

    companion object {
        val TRUE: JsonBoolean = JsonBoolean(true)
        val FALSE: JsonBoolean = JsonBoolean(false)
    }
}
