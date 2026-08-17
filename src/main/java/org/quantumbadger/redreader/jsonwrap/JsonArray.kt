package org.quantumbadger.redreader.jsonwrap

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import org.quantumbadger.redreader.common.Consumer
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.jsonwrap.JsonObject.JsonDeserializable
import java.lang.reflect.InvocationTargetException

class JsonArray(parser: JsonParser) : JsonValue(), Iterable<JsonValue?> {
    private val mContents = ArrayList<JsonValue>(16)

    init {
        if (parser.currentToken() != JsonToken.START_ARRAY) {
            throw JsonParseException(
                parser,
                "Expecting array start, got " + parser.currentToken(),
                parser.currentLocation()
            )
        }

        parser.nextToken()

        while (parser.currentToken() != JsonToken.END_ARRAY) {
            mContents.add(JsonValue.parse(parser))
        }

        parser.nextToken()
    }

    public override fun asArray(): JsonArray {
        return this
    }

    fun get(id: Int): JsonValue {
        return mContents[id]
    }

    fun getString(id: Int): String? {
        return get(id).asString()
    }

    fun getLong(id: Int): Long? {
        return get(id).asLong()
    }

    fun getDouble(id: Int): Double? {
        return get(id).asDouble()
    }

    fun getBoolean(id: Int): Boolean? {
        return get(id).asBoolean()
    }

    fun getObject(id: Int): JsonObject? {
        return get(id).asObject()
    }

    @Throws(
        InstantiationException::class,
        IllegalAccessException::class,
        NoSuchMethodException::class,
        InvocationTargetException::class
    )
    fun <E : JsonDeserializable> getObject(
        id: Int,
        clazz: Class<E>
    ): E? {
        return get(id).asObject<E>(clazz)
    }

    fun getArray(id: Int): JsonArray? {
        return get(id).asArray()
    }

    override fun iterator(): MutableIterator<JsonValue?> {
        return mContents.iterator()
    }

    protected override fun prettyPrint(indent: Int, sb: StringBuilder) {
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

    fun size(): Int {
        return mContents.size
    }

    fun forEachObject(consumer: Consumer<JsonObject?>) {
        for (value in mContents) {
            consumer.consume(value.asObject())
        }
    }

    protected override fun getAtPathInternal(offset: Int, vararg keys: Any?): Optional<JsonValue?> {
        if (offset == keys.size) {
            return Optional.of<JsonValue?>(this)
        }

        if (keys[offset] !is Int) {
            return Optional.empty<JsonValue?>()
        }

        val key = keys[offset] as Int

        if (key < 0 || key >= mContents.size) {
            return Optional.empty<JsonValue?>()
        }

        val next = mContents[key]

        if (next == null) {
            return Optional.empty<JsonValue?>()
        }

        return next.getAtPathInternal(offset + 1, *keys)
    }
}
