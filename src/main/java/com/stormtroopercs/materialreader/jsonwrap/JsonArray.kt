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

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.stormtroopercs.materialreader.common.Consumer
import com.stormtroopercs.materialreader.common.Optional
import com.stormtroopercs.materialreader.jsonwrap.JsonObject.JsonDeserializable
import java.lang.reflect.InvocationTargetException

class JsonArray(parser: JsonParser) :
	JsonValue(),
	Iterable<JsonValue> {
	private val mContents = ArrayList<JsonValue>(16)

	init {
		if (parser.currentToken() != JsonToken.START_ARRAY) {
			throw JsonParseException(
				parser,
				"Expecting array start, got " + parser.currentToken(),
				parser.currentLocation(),
			)
		}

		parser.nextToken()

		while (parser.currentToken() != JsonToken.END_ARRAY) {
			mContents.add(JsonValue.parse(parser))
		}

		parser.nextToken()
	}

	public override fun asArray(): JsonArray = this

	fun get(id: Int): JsonValue = mContents[id]

	fun getString(id: Int): String? = get(id).asString()

	fun getLong(id: Int): Long? = get(id).asLong()

	fun getDouble(id: Int): Double? = get(id).asDouble()

	fun getBoolean(id: Int): Boolean? = get(id).asBoolean()

	fun getObject(id: Int): JsonObject? = get(id).asObject()

	@Throws(
		InstantiationException::class,
		IllegalAccessException::class,
		NoSuchMethodException::class,
		InvocationTargetException::class,
	)
	fun <E : JsonDeserializable> getObject(
		id: Int,
		clazz: Class<E>,
	): E? = get(id).asObject<E>(clazz)

	fun getArray(id: Int): JsonArray? = get(id).asArray()

	override fun iterator(): MutableIterator<JsonValue> = mContents.iterator()

	override fun prettyPrint(indent: Int, sb: StringBuilder) {
		sb.append('[')

		for (item in mContents.indices) {
			if (item != 0) {
				sb.append(',')
			}
			sb.append('\n')
			for (i in 0 until indent + 1) {
				sb.append("   ")
			}
			mContents[item].prettyPrint(indent + 1, sb)
		}

		sb.append('\n')
		for (i in 0 until indent) {
			sb.append("   ")
		}
		sb.append(']')
	}

	fun size(): Int = mContents.size

	fun forEachObject(consumer: Consumer<JsonObject?>) {
		for (value in mContents) {
			consumer.consume(value.asObject())
		}
	}

	override fun getAtPathInternal(offset: Int, vararg keys: Any?): Optional<JsonValue> {
		if (offset == keys.size) {
			return Optional.of(this)
		}

		if (keys[offset] !is Int) {
			return Optional.empty<JsonValue>()
		}

		val key = keys[offset] as Int

		if (key < 0 || key >= mContents.size) {
			return Optional.empty<JsonValue>()
		}

		return mContents[key].getAtPathInternal(offset + 1, *keys)
	}
}
