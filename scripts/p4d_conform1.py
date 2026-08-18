import io

base = str(Path(__file__).resolve().parent.parent / 'src/main/java/org/quantumbadger/redreader/')

# Signature-conformance only: conform override param types to the compiler's
# nullable-after-substitution targets. (Body !! fixes come after a build.)
edits = {}

# ---- fragments/MainMenuFragment.kt ----
edits['fragments/MainMenuFragment.kt'] = [
  ("                    override fun onRequestFailed(failureReason: RRError) {\n                        onMultiredditError(failureReason)",
   "                    override fun onRequestFailed(failureReason: RRError?) {\n                        onMultiredditError(failureReason!!)"),
  ("                    override fun onRequestFailed(failureReason: RRError) {\n                        onSubredditError(failureReason)",
   "                    override fun onRequestFailed(failureReason: RRError?) {\n                        onSubredditError(failureReason!!)"),
]

# ---- reddit/RedditAPI.kt ----
edits['reddit/RedditAPI.kt'] = [
  ("                override fun onRequestFailed(failureReason: RRError) {\n                    responseHandler.notifyFailure(failureReason)",
   "                override fun onRequestFailed(failureReason: RRError?) {\n                    responseHandler.notifyFailure(failureReason!!)"),
  ("                override fun onRequestSuccess(\n                    subreddit: RedditSubreddit,\n                    timeCached: TimestampUTC?\n                ) {\n                    val postFields = LinkedList<PostField?>()\n\n                    postFields.add(PostField(\"sr\", subreddit.name))",
   "                override fun onRequestSuccess(\n                    subreddit: RedditSubreddit?,\n                    timeCached: TimestampUTC?\n                ) {\n                    val postFields = LinkedList<PostField?>()\n\n                    postFields.add(PostField(\"sr\", subreddit!!.name))"),
]

# ---- reddit/api/RedditAPIIndividualSubredditDataRequester.kt ----
edits['reddit/api/RedditAPIIndividualSubredditDataRequester.kt'] = [
  ("    override fun performRequest(\n        subredditCanonicalId: SubredditCanonicalId,\n        timestampBound: TimestampBound?,\n        handler: RequestResponseHandler<RedditSubreddit?, RRError?>\n    ) {\n        val url = Reddit.getUri(subredditCanonicalId.toString() + \"/about.json\")",
   "    override fun performRequest(\n        subredditCanonicalId: SubredditCanonicalId?,\n        timestampBound: TimestampBound?,\n        handler: RequestResponseHandler<RedditSubreddit?, RRError?>\n    ) {\n        val url = Reddit.getUri(subredditCanonicalId!!.toString() + \"/about.json\")"),
  ("                override fun onJsonParsed(\n                    result: JsonValue,\n                    timestamp: TimestampUTC,\n                    session: UUID,\n                    fromCache: Boolean\n                ) {\n                    try {\n                        val subredditThing = result.asObject<RedditThing>(RedditThing::class.java)\n                        val subreddit = subredditThing!!.asSubreddit()\n                        subreddit.downloadTime = timestamp.toUtcMs()",
   "                override fun onJsonParsed(\n                    result: JsonValue,\n                    timestamp: TimestampUTC?,\n                    session: UUID,\n                    fromCache: Boolean\n                ) {\n                    try {\n                        val subredditThing = result.asObject<RedditThing>(RedditThing::class.java)\n                        val subreddit = subredditThing!!.asSubreddit()\n                        subreddit.downloadTime = timestamp!!.toUtcMs()"),
  ("    override fun performRequest(\n        subredditCanonicalIds: MutableCollection<SubredditCanonicalId>,",
   "    override fun performRequest(\n        subredditCanonicalIds: MutableCollection<SubredditCanonicalId?>,"),
  ("                override fun onRequestFailed(failureReason : RRError) {\n                    synchronized(result) {\n                        if (stillOkay.get()) {\n                            stillOkay.set(false)\n                            handler.onRequestFailed(failureReason)",
   "                override fun onRequestFailed(failureReason: RRError?) {\n                    synchronized(result) {\n                        if (stillOkay.get()) {\n                            stillOkay.set(false)\n                            handler.onRequestFailed(failureReason)"),
  ("                override fun onRequestSuccess(\n                    innerResult: RedditSubreddit,\n                    timeCached: TimestampUTC\n                ) {\n                    synchronized(result) {\n                        if (stillOkay.get()) {\n                            try {\n                                val canonicalId = innerResult.canonicalId",
   "                override fun onRequestSuccess(\n                    innerResult: RedditSubreddit?,\n                    timeCached: TimestampUTC?\n                ) {\n                    synchronized(result) {\n                        if (stillOkay.get()) {\n                            try {\n                                val canonicalId = innerResult!!.canonicalId"),
  ("                                            TimestampUTC.oldest(\n                                                oldestResult.get()!!,\n                                                timeCached\n                                            )",
   "                                            TimestampUTC.oldest(\n                                                oldestResult.get()!!,\n                                                timeCached!!\n                                            )"),
  ("                            } catch (e: InvalidSubredditNameException) {\n                                Log.e(TAG, \"Invalid subreddit name \" + innerResult.name, e)",
   "                            } catch (e: InvalidSubredditNameException) {\n                                Log.e(TAG, \"Invalid subreddit name \" + innerResult!!.name, e)"),
]

for fname, pairs in edits.items():
    path = base + fname
    txt = io.open(path, encoding='utf-8').read()
    ok = True
    for old, new in pairs:
        if old == new:
            continue
        cnt = txt.count(old)
        if cnt != 1:
            print(f"FAIL {fname}: expected 1 match, got {cnt} for: {old[:70]!r}")
            ok = False
        else:
            txt = txt.replace(old, new)
    if ok:
        io.open(path, 'w', encoding='utf-8').write(txt)
        print(f"OK   {fname}")
    else:
        print(f"SKIP {fname} (no changes written) — will handle residuals")
