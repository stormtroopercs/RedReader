package org.quantumbadger.redreader.jsonwrap

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.jsonwrap.JsonObject.JsonDeserializable
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.InvocationTargetException

abstract class JsonValue {
    open fun asObject(): JsonObject? {
        return null
    }

    @Throws(
        InstantiationException::class,
        IllegalAccessException::class,
        NoSuchMethodException::class,
        InvocationTargetException::class
    )
    open fun <E : JsonDeserializable?> asObject(clazz: Class<E?>?): E? {
        return null
    }

    open fun asArray(): JsonArray? {
        return null
    }

    open fun asBoolean(): Boolean? {
        return null
    }

    open fun asString(): String? {
        return null
    }

    open fun asDouble(): Double? {
        return null
    }

    open fun asLong(): Long? {
        return null
    }

    override fun toString(): String {
        val sb = StringBuilder()
        prettyPrint(0, sb)
        return sb.toString()
    }

    abstract fun prettyPrint(indent: Int, sb: StringBuilder?)

    fun getAtPath(vararg keys: Any?): Optional<JsonValue?> {
        return getAtPathInternal(0, *keys)
    }

    fun getObjectAtPath(vararg keys: Any?): Optional<JsonObject?> {
        val result = getAtPath(*keys)

        if (result.isEmpty) {
            return Optional.empty<JsonObject?>()
        }

        return Optional.ofNullable<JsonObject?>(result.get().asObject())
    }

    fun getArrayAtPath(vararg keys: Any?): Optional<JsonArray?> {
        val result = getAtPath(*keys)

        if (result.isEmpty) {
            return Optional.empty<JsonArray?>()
        }

        return Optional.ofNullable<JsonArray?>(result.get().asArray())
    }

    fun getStringAtPath(vararg keys: Any?): Optional<String?> {
        val result = getAtPath(*keys)

        if (result.isEmpty) {
            return Optional.empty<String?>()
        }

        return Optional.ofNullable<String?>(result.get().asString())
    }

    open fun getAtPathInternal(offset: Int, vararg keys: Any?): Optional<JsonValue?> {
        if (offset == keys.size) {
            return Optional.of<JsonValue?>(this)
        }

        return Optional.empty<JsonValue?>()
    }

    companion object {
        @Throws(IOException::class)
        fun parse(source: InputStream?): JsonValue {
            return parse(JsonFactory().createParser(source))
        }

        @Throws(IOException::class)
        fun parse(parser: JsonParser): JsonValue {
            if (parser.currentToken() == null) {
                parser.nextToken()
            }

            if (parser.currentToken() == null) {
                throw IOException("Invalid input: no JSON tokens available")
            }

            return when (parser.currentToken()) {
                JsonToken.START_OBJECT -> JsonObject(parser)
                JsonToken.START_ARRAY -> JsonArray(parser)
                JsonToken.VALUE_FALSE -> {
                    parser.nextToken()
                    JsonBoolean.FALSE
                }
                JsonToken.VALUE_TRUE -> {
                    parser.nextToken()
                    JsonBoolean.TRUE
                }
                JsonToken.VALUE_NULL -> {
                    parser.nextToken()
                    JsonNull.INSTANCE
                }
                JsonToken.VALUE_STRING -> {
                    val result = JsonString(parser.valueAsString)
                    parser.nextToken()
                    result
                }
                JsonToken.VALUE_NUMBER_FLOAT -> {
                    val result = JsonDouble(parser.valueAsDouble)
                    parser.nextToken()
                    result
                }
                JsonToken.VALUE_NUMBER_INT -> {
                    val result = JsonLong(parser.valueAsLong)
                    parser.nextToken()
                    result
                }
                else -> throw JsonParseException(
                    parser,
                    "Expecting an object, literal, or array, got: " + parser.currentToken(),
                    parser.currentLocation()
                )
            }
        }
    }
}
