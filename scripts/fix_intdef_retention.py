#!/usr/bin/env python3
"""Systemic fix: the converter left `import java.lang.annotation.Retention`
in 4 files, which SHADOWS kotlin.annotation.Retention (a default import).
Result: @Retention(AnnotationRetention.SOURCE) resolves to the JAVA Retention
annotation (which wants RetentionPolicy) -> 'AnnotationRetention but
RetentionPolicy expected'. And @IntDef([...]) used an array literal where
androidx IntDef is a Kotlin vararg -> 'Array<Int> but Int expected'.

Fixes (per file):
 - remove `import java.lang.annotation.Retention`
 - convert @IntDef([A, B, C])  ->  @IntDef(A, B, C)   (vararg idiom)
"""
from pathlib import Path
root = Path(__file__).resolve().parent.parent / 'src/main/java/org/quantumbadger/redreader'

def apply(p, pairs):
    t = p.read_text(errors="replace")
    for old, new, exp in pairs:
        got = t.count(old)
        if got != exp:
            print(f"  [WARN] {p.name}: expected {exp} of {old[:45]!r} -> got {got}")
        t = t.replace(old, new)
    p.write_text(t)
    print(f"  {p.name}: done")

# ---- RedditAPI.kt ----
apply(root/"reddit/RedditAPI.kt", [
    ("import java.lang.annotation.Retention\n", "", 1),
    ("    @IntDef(\n        [ACTION_UPVOTE, ACTION_UNVOTE, ACTION_DOWNVOTE, ACTION_SAVE, ACTION_HIDE, ACTION_UNSAVE, ACTION_UNHIDE, ACTION_DELETE]\n    )",
     "    @IntDef(ACTION_UPVOTE, ACTION_UNVOTE, ACTION_DOWNVOTE, ACTION_SAVE, ACTION_HIDE, ACTION_UNSAVE, ACTION_UNHIDE, ACTION_DELETE)", 1),
    ("    @IntDef([SUBSCRIPTION_ACTION_SUBSCRIBE, SUBSCRIPTION_ACTION_UNSUBSCRIBE])",
     "    @IntDef(SUBSCRIPTION_ACTION_SUBSCRIBE, SUBSCRIPTION_ACTION_UNSUBSCRIBE)", 1),
])

# ---- BezelSwipeOverlay.kt ----
apply(root/"views/bezelmenu/BezelSwipeOverlay.kt", [
    ("import java.lang.annotation.Retention\n", "", 1),
    ("    @IntDef([LEFT, RIGHT])", "    @IntDef(LEFT, RIGHT)", 1),
])

# ---- RedditURLParser.kt ----
apply(root/"reddit/url/RedditURLParser.kt", [
    ("import java.lang.annotation.Retention\n", "", 1),
    ("    @IntDef(\n        [SUBREDDIT_POST_LISTING_URL, USER_POST_LISTING_URL, SEARCH_POST_LISTING_URL, UNKNOWN_POST_LISTING_URL, USER_PROFILE_URL, USER_COMMENT_LISTING_URL, UNKNOWN_COMMENT_LISTING_URL, POST_COMMENT_LISTING_URL, MULTIREDDIT_POST_LISTING_URL, COMPOSE_MESSAGE_URL, OPAQUE_SHARED_URL\n        ]\n    )",
     "    @IntDef(SUBREDDIT_POST_LISTING_URL, USER_POST_LISTING_URL, SEARCH_POST_LISTING_URL, UNKNOWN_POST_LISTING_URL, USER_PROFILE_URL, USER_COMMENT_LISTING_URL, UNKNOWN_COMMENT_LISTING_URL, POST_COMMENT_LISTING_URL, MULTIREDDIT_POST_LISTING_URL, COMPOSE_MESSAGE_URL, OPAQUE_SHARED_URL)", 1),
])

# ---- MainMenuFragment.kt ----
apply(root/"fragments/MainMenuFragment.kt", [
    ("import java.lang.annotation.Retention\n", "", 1),
    ("    @IntDef(\n        [MENU_MENU_ACTION_FRONTPAGE, MENU_MENU_ACTION_PROFILE, MENU_MENU_ACTION_INBOX, MENU_MENU_ACTION_SUBMITTED, MENU_MENU_ACTION_SUBMITTED_COMMENTS, MENU_MENU_ACTION_UPVOTED, MENU_MENU_ACTION_DOWNVOTED, MENU_MENU_ACTION_SAVED, MENU_MENU_ACTION_MODMAIL, MENU_MENU_ACTION_HIDDEN, MENU_MENU_ACTION_CUSTOM, MENU_MENU_ACTION_ALL, MENU_MENU_ACTION_POPULAR, MENU_MENU_ACTION_SENT_MESSAGES, MENU_MENU_ACTION_FIND_SUBREDDIT]\n    )",
     "    @IntDef(MENU_MENU_ACTION_FRONTPAGE, MENU_MENU_ACTION_PROFILE, MENU_MENU_ACTION_INBOX, MENU_MENU_ACTION_SUBMITTED, MENU_MENU_ACTION_SUBMITTED_COMMENTS, MENU_MENU_ACTION_UPVOTED, MENU_MENU_ACTION_DOWNVOTED, MENU_MENU_ACTION_SAVED, MENU_MENU_ACTION_MODMAIL, MENU_MENU_ACTION_HIDDEN, MENU_MENU_ACTION_CUSTOM, MENU_MENU_ACTION_ALL, MENU_MENU_ACTION_POPULAR, MENU_MENU_ACTION_SENT_MESSAGES, MENU_MENU_ACTION_FIND_SUBREDDIT)", 1),
])

print("DONE")
