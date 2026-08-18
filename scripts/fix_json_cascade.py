#!/usr/bin/env python3
"""Fix the mechanical cascades inside the Json base files after the
Optional/iterator de-null, so the base files themselves compile.

JsonValue.kt / JsonArray.kt / JsonObject.kt:
 - internal Optional.empty<X?>( )  -> Optional.empty<X>( )
 - Optional.ofNullable<X?>(...)-> Optional.ofNullable<X>(...)
 - Optional.of<JsonValue?>(this) -> Optional.of(this)
 - Map.MutableEntry (unresolvable) -> MutableMap.MutableEntry
 - drop java.lang.Double/Float/Long imports (they shadow Kotlin primitives,
   making getLong/getDouble return java.lang.Long? / java.lang.Double?), and
   use fully-qualified java.lang.X.TYPE for the reflective field-type checks.
"""
from pathlib import Path
root = Path(__file__).resolve().parent.parent / 'src/main/java/org/quantumbadger/redreader'

def apply(p, pairs):
    t = p.read_text(errors="replace")
    for old, new, exp in pairs:
        got = t.count(old)
        if got != exp:
            print(f"  [WARN] {p.name}: expected {exp} of: {old[:55]!r} -> got {got}")
        t = t.replace(old, new)
    p.write_text(t)
    print(f"  {p.name}: done")

print("=== jsonwrap/JsonValue.kt ===")
apply(root/"jsonwrap/JsonValue.kt", [
    ("            return Optional.empty<JsonObject?>()", "            return Optional.empty<JsonObject>()", 1),
    ("        return Optional.ofNullable<JsonObject?>(result.get().asObject())",
     "        return Optional.ofNullable<JsonObject>(result.get().asObject())", 1),
    ("            return Optional.empty<JsonArray?>()", "            return Optional.empty<JsonArray>()", 1),
    ("        return Optional.ofNullable<JsonArray?>(result.get().asArray())",
     "        return Optional.ofNullable<JsonArray>(result.get().asArray())", 1),
    ("            return Optional.empty<String?>()", "            return Optional.empty<String>()", 1),
    ("        return Optional.ofNullable<String?>(result.get().asString())",
     "        return Optional.ofNullable<String>(result.get().asString())", 1),
    ("        return Optional.empty<JsonValue?>()", "        return Optional.empty<JsonValue>()", 1),
])

print("=== jsonwrap/JsonArray.kt ===")
apply(root/"jsonwrap/JsonArray.kt", [
    ("            return Optional.of<JsonValue?>(this)", "            return Optional.of(this)", 1),
    ("            return Optional.empty<JsonValue?>()", "            return Optional.empty<JsonValue>()", 3),
])

print("=== jsonwrap/JsonObject.kt ===")
apply(root/"jsonwrap/JsonObject.kt", [
    ("import org.quantumbadger.redreader.common.Optional\nimport java.lang.Double\nimport java.lang.Float\nimport java.lang.Long\nimport java.lang.reflect.InvocationTargetException",
     "import org.quantumbadger.redreader.common.Optional\nimport java.lang.reflect.InvocationTargetException", 1),
    ("    Iterable<Map.MutableEntry<String, JsonValue>> {",
     "    Iterable<MutableMap.MutableEntry<String, JsonValue>> {", 1),
    ("    override fun iterator(): MutableIterator<Map.MutableEntry<String, JsonValue>> {",
     "    override fun iterator(): MutableIterator<MutableMap.MutableEntry<String, JsonValue>> {", 1),
    ("                if (fieldType == Long::class.java || fieldType == Long.TYPE) {",
     "                if (fieldType == Long::class.java || fieldType == java.lang.Long.TYPE) {", 1),
    ("                } else if (fieldType == Double::class.java || fieldType == Double.TYPE) {",
     "                } else if (fieldType == Double::class.java || fieldType == java.lang.Double.TYPE) {", 1),
    ("                } else if (fieldType == Float::class.java || fieldType == Float.TYPE) {",
     "                } else if (fieldType == Float::class.java || fieldType == java.lang.Float.TYPE) {", 1),
    ("            return Optional.of<JsonValue?>(this)", "            return Optional.of(this)", 1),
    ("            return Optional.empty<JsonValue?>()", "            return Optional.empty<JsonValue>()", 1),
])

print("DONE")
