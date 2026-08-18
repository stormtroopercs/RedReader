package org.quantumbadger.redreader.jsonwrap

class JsonBoolean private constructor(private val mValue: Boolean) : JsonValue() {
    override fun prettyPrint(indent: Int, sb: StringBuilder) {
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
