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

package org.quantumbadger.redreader.jsonwrap

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import org.quantumbadger.redreader.common.Optional
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier

class JsonObject(parser: JsonParser) : JsonValue(),
    Iterable<MutableMap.MutableEntry<String, JsonValue>> {
    interface JsonDeserializable

    private val properties = HashMap<String, JsonValue>()

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
            val value: JsonValue = JsonValue.parse(parser)

            properties.put(fieldName!!, value)
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
    public override fun <E : JsonDeserializable> asObject(clazz: Class<E>): E {
        val obj = clazz.getConstructor().newInstance()
        populateObject(obj)
        return obj
    }

    fun get(name: String?): JsonValue? {
        if (name == null) {
            return null
        }

        return properties[name]
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
    fun <E : JsonDeserializable> getObject(
        id: String,
        clazz: Class<E>
    ): E? {
        val value = get(id)

        if (value == null) {
            return null
        }

        return value.asObject<E>(clazz)
    }

    fun getArray(id: String): JsonArray? {
        val value = get(id)

        if (value == null) {
            return null
        }

        return value.asArray()
    }

    override fun prettyPrint(indent: Int, sb: StringBuilder) {
        sb.append('{')

        val propertyKeySet = properties.keys
        val fieldNames = propertyKeySet.toTypedArray()

        for (prop in fieldNames.indices) {
            if (prop != 0) {
                sb.append(',')
            }
            sb.append('\n')
            for (i in 0 until indent + 1) {
                sb.append("   ")
            }
            sb.append("\"")
                .append(fieldNames[prop]!!.replace("\\", "\\\\").replace("\"", "\\\""))
                .append("\": ")
            properties[fieldNames[prop]]!!.prettyPrint(indent + 1, sb)
        }

        sb.append('\n')
        for (i in 0 until indent) {
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
        // Walk declared fields (Kotlin `var`s are private backing fields, so
        // `javaClass.fields` — public only — would never match them), up the
        // class hierarchy. Static fields are skipped (e.g. the `Companion`
        // field Kotlin adds to every class with a companion object).
        val objectFields = ArrayList<java.lang.reflect.Field>()

        var clazz: Class<*>? = o.javaClass
        while (clazz != null && clazz != Any::class.java) {
            for (field in clazz.declaredFields) {
                if ((field.modifiers and Modifier.STATIC) != 0) {
                    continue
                }

                objectFields.add(field)
            }

            clazz = clazz.superclass
        }

        try {
            for (objectField in objectFields) {
                if ((objectField.modifiers and Modifier.TRANSIENT) != 0) {
                    continue
                }

                val jsonValue: JsonValue?

                if (properties.containsKey(objectField.name)) {
                    jsonValue = properties[objectField.name]
                } else if (objectField.name.startsWith("_json_")) {
                    jsonValue = properties[
                        objectField.name
                            .substring("_json_".length)
                    ]
                } else {
                    jsonValue = null
                }

                if (jsonValue == null) {
                    continue
                }

                objectField.isAccessible = true

                val fieldType = objectField.type

                // Kotlin nullable (`T?`) fields box to java.lang.T — accept
                // both the primitive (non-null `T`) and the boxed form.
                if (fieldType == Long::class.java || fieldType == java.lang.Long.TYPE || fieldType == java.lang.Long::class.java) {
                    objectField[o] = jsonValue.asLong()
                } else if (fieldType == Double::class.java || fieldType == java.lang.Double.TYPE || fieldType == java.lang.Double::class.java) {
                    objectField[o] = jsonValue.asDouble()
                } else if (fieldType == Int::class.java || fieldType == java.lang.Integer::class.java) {
                    objectField[o] =                         if (jsonValue.asLong() == null) null else jsonValue.asLong()!!.toInt()
                } else if (fieldType == Float::class.java || fieldType == java.lang.Float.TYPE || fieldType == java.lang.Float::class.java) {
                    objectField[o] =                         if (jsonValue.asDouble() == null) null else jsonValue.asDouble()!!.toFloat()
                } else if (fieldType == Boolean::class.java || fieldType == java.lang.Boolean.TYPE || fieldType == java.lang.Boolean::class.java) {
                    objectField[o] = jsonValue.asBoolean()
                } else if (fieldType == String::class.java) {
                    objectField[o] = jsonValue.asString()
                } else if (fieldType == JsonArray::class.java) {
                    objectField[o] = jsonValue.asArray()
                } else if (fieldType == JsonObject::class.java) {
                    objectField[o] = jsonValue.asObject()
                } else if (fieldType == JsonValue::class.java) {
                    objectField[o] = jsonValue
                } else if (JsonDeserializable::class.java.isAssignableFrom(fieldType)) {
                    objectField[o] = jsonValue.asObject(
                        fieldType as Class<out JsonDeserializable>
                    )
                } else {
                    throw RuntimeException(
                        "Cannot handle field type "
                                + fieldType.canonicalName
                    )
                }
            }
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }
    }

    override fun iterator(): MutableIterator<MutableMap.MutableEntry<String, JsonValue>> {
        return properties.entries.iterator()
    }

    override fun getAtPathInternal(offset: Int, vararg keys: Any?): Optional<JsonValue> {
        if (offset == keys.size) {
            return Optional.of(this)
        }

        val next = properties[keys[offset].toString()]

        if (next == null) {
            return Optional.empty<JsonValue>()
        }

        return next.getAtPathInternal(offset + 1, *keys)
    }
}
