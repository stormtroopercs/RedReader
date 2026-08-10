package org.quantumbadger.redreader.jsonwrap

import org.apache.commons.text.StringEscapeUtils
import org.quantumbadger.redreader.common.StringUtils

class JsonString(private val mValue: String) : JsonValue() {
    protected override fun prettyPrint(indent: Int, sb: StringBuilder) {
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

    public override fun asString(): String {
        return mValue
    }

    public override fun asDouble(): Double? {
        return try {
            mValue.toDouble()
        } catch (e: NumberFormatException) {
            null
        }
    }

    public override fun asLong(): Long? {
        return try {
            mValue.toLong()
        } catch (e: NumberFormatException) {
            null
        }
    }
}
