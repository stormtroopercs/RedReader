package org.quantumbadger.redreader.jsonwrap

class JsonDouble(private val mValue: Double) : JsonValue() {
    protected override fun prettyPrint(indent: Int, sb: StringBuilder) {
        sb.append(mValue)
    }

    public override fun asString(): String {
        return mValue.toString()
    }

    public override fun asDouble(): Double {
        return mValue
    }

    public override fun asLong(): Long {
        return Math.round(mValue)
    }
}
