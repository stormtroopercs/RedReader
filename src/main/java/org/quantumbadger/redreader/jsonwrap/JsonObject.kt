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

class JsonObject(parser: JsonParser) : JsonValue(),
    Iterable<Map.MutableEntry<String?, JsonValue?>?> {
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
            val value: JsonValue = JsonValue.parse(parser)

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
    public override fun <E : JsonDeserializable?> asObject(clazz: Class<E>): E {
        val obj = clazz.getConstructor().newInstance()
        populateObject(obj)
        return obj
    }

    fun get(name: String?): JsonValue? {
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
        val objectFields = o.javaClass.fields

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

                if (fieldType == Long::class.java || fieldType == Long.TYPE) {
                    objectField[o] = jsonValue.asLong()
                } else if (fieldType == Double::class.java || fieldType == Double.TYPE) {
                    objectField[o] = jsonValue.asDouble()
                } else if (fieldType == Int::class.java || fieldType == Int.TYPE) {
                    objectField[o] =                         if (jsonValue.asLong() == null) null else jsonValue.asLong()!!.toInt()
                } else if (fieldType == Float::class.java || fieldType == Float.TYPE) {
                    objectField[o] =                         if (jsonValue.asDouble() == null) null else jsonValue.asDouble()!!.toFloat()
                } else if (fieldType == Boolean::class.java || fieldType == java.lang.Boolean.TYPE) {
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

    override fun iterator(): MutableIterator<Map.MutableEntry<String?, JsonValue?>?> {
        return properties.entries.iterator()
    }

    protected override fun getAtPathInternal(offset: Int, vararg keys: Any?): Optional<JsonValue?> {
        if (offset == keys.size) {
            return Optional.of<JsonValue?>(this)
        }

        val next = properties[keys[offset].toString()]

        if (next == null) {
            return Optional.empty<JsonValue?>()
        }

        return next.getAtPathInternal(offset + 1, *keys)
    }
}
