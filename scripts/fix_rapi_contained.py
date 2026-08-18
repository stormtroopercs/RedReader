#!/usr/bin/env python3
"""Contained faithful de-null for RedditAPI residuals (subset 1).
Matches original Java (@NonNull / non-null). Self-contained to
RedditAPI.kt, RedditFlairChoice.kt, APIResponseHandler.kt.
"""
from pathlib import Path
root = Path(__file__).resolve().parent.parent / 'src/main/java/org/quantumbadger/redreader'

def apply(p, pairs):
    t = p.read_text(errors="replace")
    for old, new, exp in pairs:
        got = t.count(old)
        flag = "ok" if got == exp else f"WARN(got={got})"
        print(f"  [{flag}] {old[:58]!r}")
        t = t.replace(old, new)
    p.write_text(t)

print("=== RedditFlairChoice.kt (fromJsonList + fromJson) ===")
apply(root/"reddit/RedditFlairChoice.kt", [
    ("fun fromJsonList(json: JsonArray): Optional<MutableList<RedditFlairChoice?>?>",
     "fun fromJsonList(json: JsonArray): Optional<MutableList<RedditFlairChoice>>", 1),
    ("val result = ArrayList<RedditFlairChoice?>(json.size())",
     "val result = ArrayList<RedditFlairChoice>(json.size())", 1),
    ("return Optional.Companion.empty<MutableList<RedditFlairChoice?>?>()",
     "return Optional.Companion.empty<MutableList<RedditFlairChoice>>()", 2),
    ("val choice: Optional<RedditFlairChoice?> = fromJson(`object`)",
     "val choice: Optional<RedditFlairChoice> = fromJson(`object`)", 1),
    ("return Optional.Companion.of<MutableList<RedditFlairChoice?>?>(result)",
     "return Optional.Companion.of<MutableList<RedditFlairChoice>>(result)", 1),
    ("): Optional<RedditFlairChoice?> {",
     "): Optional<RedditFlairChoice> {", 1),
    ("return Optional.Companion.empty<RedditFlairChoice?>()",
     "return Optional.Companion.empty<RedditFlairChoice>()", 1),
    ("return Optional.Companion.of<RedditFlairChoice?>(",
     "return Optional.Companion.of<RedditFlairChoice>(", 1),
])

print("=== APIResponseHandler.kt (SubmitResponseHandler.onSuccess) ===")
apply(root/"reddit/APIResponseHandler.kt", [
    ("redirectUrl: Optional<String?>,\n            thingId: Optional<String?>",
     "redirectUrl: Optional<String>,\n            thingId: Optional<String>", 1),
])

print("=== RedditAPI.kt (contained) ===")
apply(root/"reddit/RedditAPI.kt", [
    # L118 / L125: redundant requireNonNull on already null-checked receiver -> !!
    ("Objects.requireNonNull<JsonObject?>(result.asObject()).isEmpty",
     "result.asObject()!!.isEmpty", 1),
    ("Objects.requireNonNull<String?>(result.asString()) == \"{}\"",
     "result.asString()!! == \"{}\"", 1),
    # FlairSelectorResponseHandler interface (L1092) + two empty lists
    ("fun onSuccess(choices: MutableCollection<RedditFlairChoice?>)",
     "fun onSuccess(choices: MutableCollection<RedditFlairChoice>)", 1),
    ("responseHandler.onSuccess(mutableListOf<RedditFlairChoice?>())",
     "responseHandler.onSuccess(mutableListOf<RedditFlairChoice>())", 2),
    # postFields: 12 LinkedList decls + both createPostRequest params
    ("val postFields = LinkedList<PostField?>()", "val postFields = LinkedList<PostField>()", 12),
    ("postFields: MutableList<PostField?>,\n        context: Context,\n        handler: CacheRequestJSONParser.Listener",
     "postFields: MutableList<PostField>,\n        context: Context,\n        handler: CacheRequestJSONParser.Listener", 1),
    ("postFields: MutableList<PostField?>,\n        context: Context,\n        callbacks: CacheRequestCallbacks",
     "postFields: MutableList<PostField>,\n        context: Context,\n        callbacks: CacheRequestCallbacks", 1),
    # submit onSuccess anon (L293/294)
    ("redirectUrl: Optional<String?>,\n                        thingId: Optional<String?>",
     "redirectUrl: Optional<String>,\n                        thingId: Optional<String>", 1),
    # subreddit-list: anon handler + 3 handler params + class + ArrayLists + 3 after
    ("object : ValueResponseHandler<SubredditListResponse?>(context) {",
     "object : ValueResponseHandler<SubredditListResponse>(context) {", 1),
    ("handler: ValueResponseHandler<SubredditListResponse?>",
     "handler: ValueResponseHandler<SubredditListResponse>", 3),
    ("val subreddits: ArrayList<RedditSubreddit?>,\n        val after: Optional<String?>",
     "val subreddits: ArrayList<RedditSubreddit>,\n        val after: Optional<String>", 1),
    ("val output = ArrayList<RedditSubreddit?>()", "val output = ArrayList<RedditSubreddit>()", 1),
    ("handler: ValueResponseHandler<ArrayList<RedditSubreddit?>?>",
     "handler: ValueResponseHandler<ArrayList<RedditSubreddit>>", 2),
    ("ArrayList<RedditSubreddit?>(128)", "ArrayList<RedditSubreddit>(128)", 1),
    ("results: ArrayList<RedditSubreddit?>", "results: ArrayList<RedditSubreddit>", 1),
    ("after: Optional<String?>", "after: Optional<String>", 3),
    # after.apply lambdas (3)
    ("after.apply(FunctionOneArgNoReturn { value: String? ->",
     "after.apply(FunctionOneArgNoReturn { value: String ->", 3),
])
print("DONE")
