#!/usr/bin/env python3
"""Revert RedditOAuth from Hilt `class @Inject` back to `object`.

Evidence:
 - 13 'Cannot import' errors = object-member imports (import ...RedditOAuth.completeLogin
   etc.) that only compile on an `object`, not a `class`.
 - ~74 call sites use static-style RedditOAuth.method(...).
 - ZERO DI sites exist (no @Inject field of type RedditOAuth, no Hilt module).
 - Original Java was a `public static` utility class -> faithful Kotlin form is `object`
   (the converter's first output, de80c548^). Commit de80c548 ('convert to Hilt-injected
   class') was an incorrect modernization that broke every call site with no DI value.
 - The injected `context` field is dead weight: every method using `context` declares its
   own `context: Context` param (verified per-scope).

Keeps the current file's improvements (license header, lint suppressions, fixed imports,
de-nulled onSuccess) and fixes the 7 in-file errors:
  150  anyNeedRelogin placeholder -> faithful Boolean impl (pre-Hilt form)
  547  loginAsynchronous getAnon() placeholder -> getInstance(context)
  578  user.refreshToken.token (nullable) -> user.refreshToken!!.token
  631/741/763  FetchRefreshTokenResultStatus inside FetchAccessTokenResult(
               -> FetchAccessTokenResultStatus
  + missing `import ...account.RedditAccountManager`
"""
from pathlib import Path

f = Path(__file__).resolve().parent.parent / 'src/main/java/org/quantumbadger/redreader/reddit/api/RedditOAuth.kt'
t = f.read_text(errors="replace")
n_bad = 0

def sub(old, new, expect=None):
    global t, n_bad
    c = t.count(old)
    tag = "OK " if (expect is None and c >= 1) or (expect is not None and c == expect) else "!! "
    if tag == "!! ":
        n_bad += 1
    print(f"[{tag}] found={c}  {old[:70]!r}")
    if c >= 1:
        t = t.replace(old, new) if expect is None else t.replace(old, new)

# --- 1. class -> object (drop @ViewModelScoped/@Inject ctor + dead context field) ---
sub(
"""@ViewModelScoped
class RedditOAuth @Inject constructor(
    @ApplicationContext private val context: Context
) {""",
"""object RedditOAuth {""", 1)

# --- 2. doc comment accuracy ---
sub(
"""/**
 * Hilt-injected OAuth manager for Reddit authentication.
 * Replaces companion object singleton pattern.
 */""",
"""/**
 * OAuth manager for Reddit authentication (singleton object).
 */""", 1)

# --- 3. drop the now-unused Hilt imports ---
sub("import dagger.hilt.android.qualifiers.ApplicationContext\n", "", 1)
sub("import dagger.hilt.android.components.ViewModelComponent\n", "", 1)
sub("import dagger.hilt.android.scopes.ViewModelScoped\n", "", 1)
sub("import javax.inject.Inject\n", "", 1)

# --- 4. add missing RedditAccountManager import ---
sub(
"import org.quantumbadger.redreader.account.RedditAccount\n",
"import org.quantumbadger.redreader.account.RedditAccount\nimport org.quantumbadger.redreader.account.RedditAccountManager\n", 1)

# --- 5. anyNeedRelogin: broken placeholder -> faithful Boolean impl (pre-Hilt form) ---
sub(
"""    fun anyNeedRelogin(context: Context) =         RedditAccountManager.getAnon() // Placeholder - will use Hilt-injected manager""",
"""    fun anyNeedRelogin(context: Context) =
        RedditAccountManager.getInstance(context).accounts.any(this::needsRelogin)""", 1)

# --- 6. loginAsynchronous: getAnon() placeholder -> getInstance(context) (pre-Hilt form) ---
sub(
"""                    val accountManager = RedditAccountManager.getAnon() // Placeholder
""",
"""                    val accountManager = RedditAccountManager.getInstance(context)
""", 1)

# --- 7. nullable refreshToken receiver (RefreshToken? -> !!; PostField needs non-null String) ---
sub(
"""        postFields.add(PostField("refresh_token", user.refreshToken.token))""",
"""        postFields.add(PostField("refresh_token", user.refreshToken!!.token))""", 1)

# --- 8. wrong status enum: FetchAccessTokenResult ctor needs FetchAccessTokenResultStatus ---
# two identical occurrences (fetchAccessTokenSynchronous + fetchAnonymousAccessTokenSynchronous)
sub(
"""                        result.set(
                            FetchAccessTokenResult(
                                FetchRefreshTokenResultStatus.CONNECTION_ERROR,""",
"""                        result.set(
                            FetchAccessTokenResult(
                                FetchAccessTokenResultStatus.CONNECTION_ERROR,""", 2)

# third: the catch(t: Throwable) one in fetchAnonymousAccessTokenSynchronous
sub(
"""            FetchAccessTokenResult(
                FetchRefreshTokenResultStatus.UNKNOWN_ERROR,""",
"""            FetchAccessTokenResult(
                FetchAccessTokenResultStatus.UNKNOWN_ERROR,""", 1)

f.write_text(t, encoding="utf-8")
print("\nDONE. bad=", n_bad)
