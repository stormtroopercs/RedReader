package org.quantumbadger.redreader.jsonwrap

class JsonNull private constructor() : JsonValue() {
    protected override fun prettyPrint(indent: Int, sb: StringBuilder) {
        sb.append("null")
    }

    companion object {
        val INSTANCE: JsonNull = JsonNull()
    }
}
