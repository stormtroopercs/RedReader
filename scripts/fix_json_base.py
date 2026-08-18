#!/usr/bin/env python3
"""De-null the Json wrapper base classes + Constants.getUriBuilder to the
faithful original-Java nullability (all @NonNull in Java).

Original Java (1d35f61e):
 - JsonValue.getAtPath -> Optional<JsonValue>
 - JsonValue.getObjectAtPath -> Optional<JsonObject>
 - JsonValue.getArrayAtPath -> Optional<JsonArray>
 - JsonValue.getStringAtPath -> Optional<String>
 - JsonValue.getAtPathInternal -> Optional<JsonValue>
 - JsonArray implements Iterable<JsonValue>
 - JsonObject implements Iterable<Map.Entry<String, JsonValue>>
 - Constants.getUriBuilder -> Uri.Builder (non-null)

The converter over-nulled the element/entry/return types, which made
JsonValue.get*AtPath().get() return nullable, breaking ~15 RedditAPI call
sites (and any other consumer of these base types).
"""
from pathlib import Path

root = Path(__file__).resolve().parent.parent / 'src/main/java/org/quantumbadger/redreader'

def apply(p, pairs):
    t = p.read_text(errors="replace")
    for old, new, exp in pairs:
        got = t.count(old)
        if got != exp:
            print(f"  [WARN] in {p.name}: expected {exp} of: {old[:50]!r} -> got {got}")
        t = t.replace(old, new)
    p.write_text(t)
    print(f"  {p.name}: done")

# ---- JsonValue.kt ----
print("=== jsonwrap/JsonValue.kt ===")
apply(root/"jsonwrap/JsonValue.kt", [
    ("    fun getAtPath(vararg keys: Any?): Optional<JsonValue?> {",
     "    fun getAtPath(vararg keys: Any?): Optional<JsonValue> {", 1),
    ("    fun getObjectAtPath(vararg keys: Any?): Optional<JsonObject?> {",
     "    fun getObjectAtPath(vararg keys: Any?): Optional<JsonObject> {", 1),
    ("    fun getArrayAtPath(vararg keys: Any?): Optional<JsonArray?> {",
     "    fun getArrayAtPath(vararg keys: Any?): Optional<JsonArray> {", 1),
    ("    fun getStringAtPath(vararg keys: Any?): Optional<String?> {",
     "    fun getStringAtPath(vararg keys: Any?): Optional<String> {", 1),
    ("    open fun getAtPathInternal(offset: Int, vararg keys: Any?): Optional<JsonValue?> {",
     "    open fun getAtPathInternal(offset: Int, vararg keys: Any?): Optional<JsonValue> {", 1),
    ("            return Optional.of<JsonValue?>(this)",
     "            return Optional.of(this)", 1),
])

# ---- JsonArray.kt ----
print("=== jsonwrap/JsonArray.kt ===")
apply(root/"jsonwrap/JsonArray.kt", [
    ("class JsonArray(parser: JsonParser) : JsonValue(), Iterable<JsonValue?> {",
     "class JsonArray(parser: JsonParser) : JsonValue(), Iterable<JsonValue> {", 1),
    ("    override fun iterator(): MutableIterator<JsonValue?> {",
     "    override fun iterator(): MutableIterator<JsonValue> {", 1),
    ("    override fun getAtPathInternal(offset: Int, vararg keys: Any?): Optional<JsonValue?> {",
     "    override fun getAtPathInternal(offset: Int, vararg keys: Any?): Optional<JsonValue> {", 1),
])

# ---- JsonObject.kt ----
print("=== jsonwrap/JsonObject.kt ===")
apply(root/"jsonwrap/JsonObject.kt", [
    ("class JsonObject(parser: JsonParser) : JsonValue(),\n    Iterable<Map.MutableEntry<String?, JsonValue?>?> {",
     "class JsonObject(parser: JsonParser) : JsonValue(),\n    Iterable<Map.MutableEntry<String, JsonValue>> {", 1),
    ("    override fun iterator(): MutableIterator<Map.MutableEntry<String?, JsonValue?>?> {",
     "    override fun iterator(): MutableIterator<Map.MutableEntry<String, JsonValue>> {", 1),
    ("    override fun getAtPathInternal(offset: Int, vararg keys: Any?): Optional<JsonValue?> {",
     "    override fun getAtPathInternal(offset: Int, vararg keys: Any?): Optional<JsonValue> {", 1),
])

# ---- Constants.kt ----
print("=== common/Constants.kt ===")
apply(root/"common/Constants.kt", [
    ("        fun getUriBuilder(path: String?): Uri.Builder? {",
     "        fun getUriBuilder(path: String): Uri.Builder {", 1),
])

print("DONE")
