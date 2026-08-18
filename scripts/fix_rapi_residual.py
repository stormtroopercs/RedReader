#!/usr/bin/env python3
"""Finish the RedditAPI 12 residuals — all faithful de-nulls matching the
original Java (@NonNull) and the already-de-nulled Json base / handler
interfaces.

Group 1 (local to RedditAPI.kt, no external cascade):
 - L118  Objects.requireNonNull<JsonObject?>(result.asObject())  -> <JsonObject>
        (asObject() already null-checked on L117)
 - L125  Objects.requireNonNull<String?>(result.asString())      -> <String>
 - L293/294  SubmitResponseHandler onSuccess(Optional<String?>, Optional<String?>)
             -> (Optional<String>, Optional<String>)  (base is now non-null)
 - L297  Consumer lambda `commentFullname: String?` -> `String`
 - L478  RequestResponseHandler<RedditSubreddit?, RRError?> -> <RedditSubreddit, RRError>
 - L789  ValueResponseHandler<SubredditListResponse?> -> <SubredditListResponse>
 - L860  SubredditListResponse.after  Optional<String?> -> Optional<String>
 - L1139/1141  (these call .orElse() which now returns Optional<String>;
                the param they feed is de-nulled in Group 2)
 - L1166/1167  SubredditListResponse(subreddits: ArrayList<RedditSubreddit?>,
                after: Optional<String?>) -> (ArrayList<RedditSubreddit>,
                Optional<String>)
 - L487/510  postFields: LinkedList<PostField?> -> LinkedList<PostField>
             (2 sites: L487 in subscriptionActionUri, L510 in subscriptionPrepareActionUri)
 - L175  choices.get() returns MutableList<RedditFlairChoice?>? -> needs !!
 - L1006  PostFields(postFields) where postFields is LinkedList<PostField?>
          -> de-nulled to LinkedList<PostField> above

Group 2 (getSubreddit chain de-null — original Java @NonNull):
 - RedditSubredditManager.getSubreddit(handler: RequestResponseHandler<RedditSubreddit?, RRError?>)
   -> <RedditSubreddit, RRError>
 - RedditSubredditManager.getSubreddit(subredditCanonicalId: SubredditCanonicalId?)
   -> SubredditCanonicalId
 - PostListingFragment.getSubreddit call site: pass non-null subreddit
   (subredditPostListURL.subreddit!!)

Group 3 (requestSubredditList chain — original Java @NonNull):
 - requestSubredditList handler: ValueResponseHandler<SubredditListResponse?> -> <SubredditListResponse>
 - subscribedSubreddits / popularSubreddits / searchSubreddits: after: Optional<String?> -> Optional<String>
 - L684/726/775  after.apply(FunctionOneArgNoReturn { value: String? -> ... }) -> String
 - L751/759/767/769/851/1166  ArrayList<RedditSubreddit?> -> ArrayList<RedditSubreddit>
"""
from pathlib import Path

root = Path(__file__).resolve().parent.parent / 'src/main/java/org/quantumbadger/redreader'

def apply(p, pairs):
    t = p.read_text(errors="replace")
    ok = True
    for old, new, exp in pairs:
        got = t.count(old)
        if got != exp:
            print(f"  [WARN got={got} exp={exp}] {old[:80]!r}")
            ok = False
        else:
            t = t.replace(old, new)
    p.write_text(t)
    return ok

print("=== RedditAPI.kt (local) ===")
apply(root/"reddit/RedditAPI.kt", [
    # L118: requireNonNull<JsonObject?> -> <JsonObject>
    ("Objects.requireNonNull<JsonObject?>(result.asObject())",
     "Objects.requireNonNull<JsonObject>(result.asObject()!!)", 1),
    # L125: requireNonNull<String?> -> <String>
    ("Objects.requireNonNull<String?>(result.asString())",
     "Objects.requireNonNull<String>(result.asString()!!)", 1),
    # L293/294: SubmitResponseHandler onSuccess params
    ("redirectUrl: Optional<String?>,\n                        thingId: Optional<String?>",
     "redirectUrl: Optional<String>,\n                        thingId: Optional<String>", 1),
    # L297: Consumer lambda param
    ("Consumer { commentFullname: String? ->",
     "Consumer { commentFullname: String ->", 1),
    # L478: RequestResponseHandler type args
    ("object : RequestResponseHandler<RedditSubreddit?, RRError?> {",
     "object : RequestResponseHandler<RedditSubreddit, RRError> {", 1),
    # L789: ValueResponseHandler type arg
    ("object : ValueResponseHandler<SubredditListResponse?>(context) {",
     "object : ValueResponseHandler<SubredditListResponse>(context) {", 1),
    # L1166/1167: SubredditListResponse class
    ("val subreddits: ArrayList<RedditSubreddit?>,\n        val after: Optional<String?>",
     "val subreddits: ArrayList<RedditSubreddit>,\n        val after: Optional<String>", 1),
    # L487: postFields in subscriptionActionUri
    ("val postFields = LinkedList<PostField?>()\n\n                    postFields.add(PostField(\"sr\", subreddit.name))",
     "val postFields = LinkedList<PostField>()\n\n                    postFields.add(PostField(\"sr\", subreddit.name))", 1),
    # L510: subscriptionPrepareActionUri param
    ("postFields: LinkedList<PostField?>\n    ): UriString {",
     "postFields: LinkedList<PostField>\n    ): UriString {", 1),
    # L175: choices.get() nullable -> !!
    ("responseHandler.onSuccess(choices.get())",
     "responseHandler.onSuccess(choices.get()!!)", 1),
    # L674/711/768: after: Optional<String?> -> Optional<String> (3 sites)
    ("after: Optional<String?>", "after: Optional<String>", 3),
    # L684/726/775: FunctionOneArgNoReturn lambda param (3 sites)
    ("after.apply(FunctionOneArgNoReturn { value: String? ->",
     "after.apply(FunctionOneArgNoReturn { value: String ->", 3),
    # L751/767: handler type args
    ("handler: ValueResponseHandler<ArrayList<RedditSubreddit?>?>",
     "handler: ValueResponseHandler<ArrayList<RedditSubreddit>>", 2),
    # L759: results init
    ("ArrayList<RedditSubreddit?>(128)",
     "ArrayList<RedditSubreddit>(128)", 1),
    # L769: results param
    ("results: ArrayList<RedditSubreddit?>",
     "results: ArrayList<RedditSubreddit>", 1),
    # L851: output init
    ("val output = ArrayList<RedditSubreddit?>()",
     "val output = ArrayList<RedditSubreddit>()", 1),
    # L855: asSubreddit() result -> add to ArrayList<RedditSubreddit>
    ("val subreddit = redditThing!!.asSubreddit()\n                                output.add(subreddit)",
     "val subreddit = redditThing!!.asSubreddit()!!\n                                output.add(subreddit)", 1),
    # L1006: PostFields(postFields) — postFields is now LinkedList<PostField>
    # (no change needed if the param is already de-nulled; the error was
    #  because createPostRequest took LinkedList<PostField?> which fed PostFields)
    # L860: SubredditListResponse(output, after) — after is now Optional<String>
    # (no change needed if SubredditListResponse is de-nulled above)
])

print("=== RedditSubredditManager.kt (getSubreddit chain) ===")
apply(root/"reddit/RedditSubredditManager.kt", [
    # getSubreddit: handler type args
    ("handler: RequestResponseHandler<RedditSubreddit?, RRError?>",
     "handler: RequestResponseHandler<RedditSubreddit, RRError>", 1),
    # getSubreddit: subredditCanonicalId param
    ("subredditCanonicalId: SubredditCanonicalId?,",
     "subredditCanonicalId: SubredditCanonicalId,", 1),
    # getSubreddit: updatedVersionListener param
    ("updatedVersionListener: UpdatedVersionListener<SubredditCanonicalId?, RedditSubreddit?>?",
     "updatedVersionListener: UpdatedVersionListener<SubredditCanonicalId, RedditSubreddit>?", 1),
])

print("=== PostListingFragment.kt (getSubreddit call site) ===")
apply(root/"fragments/PostListingFragment.kt", [
    # subredditPostListURL.subreddit (already !!) — check if it's passing SubredditCanonicalId?
    # The call passes SubredditCanonicalId(subredditPostListURL.subreddit!!) which is already non-null.
    # The handler param `subredditHandler` might be RequestResponseHandler<RedditSubreddit?, RRError?>
    # -> needs de-null
    ("subredditHandler: RequestResponseHandler<RedditSubreddit?, RRError?>",
     "subredditHandler: RequestResponseHandler<RedditSubreddit, RRError>", 1),
])

print("DONE")
