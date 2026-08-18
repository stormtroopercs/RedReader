package org.quantumbadger.redreader.jsonwrap

class JsonLong(private val mValue: Long) : JsonValue() {
    override fun prettyPrint(indent: Int, sb: StringBuilder) {
        sb.append(mValue)
    }

    public override fun asString(): String {
        return mValue.toString()
    }

    public override fun asDouble(): Double {
        return mValue.toDouble()
    }

    public override fun asLong(): Long {
        return mValue
    }
}
