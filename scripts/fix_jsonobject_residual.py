#!/usr/bin/env python3
"""Finish the JsonObject de-null so the 3 jsonwrap base files are clean.

Faithful to original Java (JsonObject.java): `private final HashMap<String, JsonValue>
properties = new HashMap<>()`. The converter over-nulled it to
HashMap<String?, JsonValue?>, so after the supertype became
Iterable<MutableMap.MutableEntry<String, JsonValue>>, iterator() returned the
wrong entry type. De-null properties and fix the internal sites that fed it a
nullable key. Also drop the broken `Int.TYPE` reference (in Kotlin
`Int::class.java` IS the int primitive class, so the extra check is redundant).
"""
from pathlib import Path
root = Path(__file__).resolve().parent.parent / 'src/main/java/org/quantumbadger/redreader'

def apply(p, pairs):
    t = p.read_text(errors="replace")
    for old, new, exp in pairs:
        got = t.count(old)
        if got != exp:
            print(f"  [WARN] {p.name}: expected {exp} of {old[:55]!r} -> got {got}")
        t = t.replace(old, new)
    p.write_text(t)
    print(f"  {p.name}: done")

apply(root/"jsonwrap/JsonObject.kt", [
    # 1. de-null the backing map (faithful: original HashMap<String, JsonValue>)
    ("    private val properties = HashMap<String?, JsonValue?>()",
     "    private val properties = HashMap<String, JsonValue>()", 1),
    # 2. parser.currentName() is String? -> assert non-null (field name always present)
    ("            properties.put(fieldName, value)",
     "            properties.put(fieldName!!, value)", 1),
    # 3. get(name: String?) - guard the nullable key before indexing the non-null-key map
    ("    fun get(name: String?): JsonValue? {\n        return properties[name]\n    }",
     "    fun get(name: String?): JsonValue? {\n        if (name == null) {\n            return null\n        }\n\n        return properties[name]\n    }", 1),
    # 4. keys are non-null now -> plain toTypedArray()
    ("        val fieldNames = propertyKeySet.toTypedArray<String?>()",
     "        val fieldNames = propertyKeySet.toTypedArray()", 1),
    # 5. Int.TYPE is not valid Kotlin; Int::class.java already is the int primitive class
    ("                } else if (fieldType == Int::class.java || fieldType == Int.TYPE) {",
     "                } else if (fieldType == Int::class.java) {", 1),
])

print("DONE")
