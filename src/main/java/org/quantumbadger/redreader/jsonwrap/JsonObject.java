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

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import org.quantumbadger.redreader.common.Optional
import java.lang.Double
import java.lang.Float
import java.lang.Long
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import kotlin.Any
import kotlin.Boolean
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.RuntimeException
import kotlin.String
import kotlin.Throws
import kotlin.also
import kotlin.toString

class JsonObject(parser: JsonParser) : JsonValue(),
    Iterable<MutableMap.MutableEntry<String?, JsonValue?>?> {
    interface JsonDeserializable

    private val properties = HashMap<String?, JsonValue?>()

    init {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            throw JsonParseException(
                parser,
                "Expecting object start, got " + parser.currentToken(),
                parser.currentLocation()
            )
        }

        parser.nextToken()

        var jt: JsonToken

        while ((parser.currentToken().also { jt = it }) != JsonToken.END_OBJECT) {
            if (jt != JsonToken.FIELD_NAME) {
                throw JsonParseException(
                    parser, "Expecting field name, got " + jt.name,
                    parser.currentLocation()
                )
            }

            val fieldName = parser.currentName()

            parser.nextToken()
            val value: JsonValue = JsonValue.Companion.parse(parser)

            properties.put(fieldName, value)
        }

        parser.nextToken()
    }

    val isEmpty: Boolean
        get() = properties.isEmpty()

    public override fun asObject(): JsonObject {
        return this
    }

    @Throws(
        InstantiationException::class,
        IllegalAccessException::class,
        NoSuchMethodException::class,
        InvocationTargetException::class
    )
    public override fun <E : JsonDeserializable?> asObject(clazz: Class<E?>): E {
        val obj = clazz.getConstructor().newInstance()
        populateObject(obj)
        return obj
    }

    fun get(name: String?): JsonValue? {
        return properties.get(name)
    }

    fun getString(id: String): String? {
        val value = get(id)

        if (value == null) {
            return null
        }

        return value.asString()
    }

    fun getLong(id: String): Long? {
        val value = get(id)

        if (value == null) {
            return null
        }

        return value.asLong()
    }

    fun getDouble(id: String): Double? {
        val value = get(id)

        if (value == null) {
            return null
        }

        return value.asDouble()
    }

    fun getBoolean(id: String): Boolean? {
        val value = get(id)

        if (value == null) {
            return null
        }

        return value.asBoolean()
    }

    fun getObject(id: String): JsonObject? {
        val value = get(id)

        if (value == null) {
            return null
        }

        return value.asObject()
    }

    @Throws(
        InstantiationException::class,
        IllegalAccessException::class,
        NoSuchMethodException::class,
        InvocationTargetException::class
    )
    fun <E : JsonDeserializable?> getObject(
        id: String,
        clazz: Class<E?>?
    ): E? {
        val value = get(id)

        if (value == null) {
            return null
        }

        return value.asObject<E?>(clazz)
    }

    fun getArray(id: String): JsonArray? {
        val value = get(id)

        if (value == null) {
            return null
        }

        return value.asArray()
    }

    protected override fun prettyPrint(indent: Int, sb: StringBuilder) {
        sb.append('{')

        val propertyKeySet = properties.keys
        val fieldNames = propertyKeySet.toTypedArray<String?>()

        for (prop in fieldNames.indices) {
            if (prop != 0) {
                sb.append(',')
            }
            sb.append('\n')
            for (i in 0..<indent + 1) {
                sb.append("   ")
            }
            sb.append("\"")
                .append(fieldNames[prop]!!.replace("\\", "\\\\").replace("\"", "\\\""))
                .append("\": ")
            properties.get(fieldNames[prop])!!.prettyPrint(indent + 1, sb)
        }

        sb.append('\n')
        for (i in 0..<indent) {
            sb.append("   ")
        }
        sb.append('}')
    }

    @Throws(
        IllegalArgumentException::class,
        InstantiationException::class,
        NoSuchMethodException::class,
        InvocationTargetException::class
    )
    fun populateObject(o: Any) {
        val objectFields = o.javaClass.getFields()

        try {
            for (objectField in objectFields) {
                if ((objectField.getModifiers() and Modifier.TRANSIENT) != 0) {
                    continue
                }

                val `val`: JsonValue?

                if (properties.containsKey(objectField.getName())) {
                    `val` = properties.get(objectField.getName())
                } else if (objectField.getName().startsWith("_json_")) {
                    `val` = properties.get(
                        objectField.getName()
                            .substring("_json_".length)
                    )
                } else {
                    `val` = null
                }

                if (`val` == null) {
                    continue
                }

                objectField.setAccessible(true)

                val fieldType = objectField.getType()

                if (fieldType == Long::class.java || fieldType == Long.TYPE) {
                    objectField.set(o, `val`.asLong())
                } else if (fieldType == Double::class.java || fieldType == Double.TYPE) {
                    objectField.set(o, `val`.asDouble())
                } else if (fieldType == Int::class.java || fieldType == Integer.TYPE) {
                    objectField.set(
                        o,
                        if (`val`.asLong() == null) null else `val`.asLong()!!.toInt()
                    )
                } else if (fieldType == Float::class.java || fieldType == Float.TYPE) {
                    objectField.set(
                        o,
                        if (`val`.asDouble() == null) null else `val`.asDouble()!!.toFloat()
                    )
                } else if (fieldType == Boolean::class.java || fieldType == java.lang.Boolean.TYPE) {
                    objectField.set(o, `val`.asBoolean())
                } else if (fieldType == String::class.java) {
                    objectField.set(o, `val`.asString())
                } else if (fieldType == JsonArray::class.java) {
                    objectField.set(o, `val`.asArray())
                } else if (fieldType == JsonObject::class.java) {
                    objectField.set(o, `val`.asObject())
                } else if (fieldType == JsonValue::class.java) {
                    objectField.set(o, `val`)
                } else if (JsonDeserializable::class.java.isAssignableFrom(fieldType)) {
                    objectField.set(
                        o, `val`.asObject(
                            fieldType as Class<out JsonDeserializable?>
                        )
                    )
                } else {
                    throw RuntimeException(
                        "Cannot handle field type "
                                + fieldType.getCanonicalName()
                    )
                }
            }
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }
    }

    override fun iterator(): MutableIterator<MutableMap.MutableEntry<String?, JsonValue?>?> {
        return properties.entries.iterator()
    }

    protected override fun getAtPathInternal(offset: Int, vararg keys: Any?): Optional<JsonValue?> {
        if (offset == keys.size) {
            return Optional.Companion.of<JsonValue?>(this)
        }

        val next = properties.get(keys[offset].toString())

        if (next == null) {
            return Optional.Companion.empty<JsonValue?>()
        }

        return next.getAtPathInternal(offset + 1, *keys)
    }
}
