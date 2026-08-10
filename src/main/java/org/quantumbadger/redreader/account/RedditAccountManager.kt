/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package org.quantumbadger.redreader.account

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.quantumbadger.redreader.activities.BugReportActivity.Companion.handleGlobalError
import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.common.UpdateNotifier
import org.quantumbadger.redreader.reddit.api.RedditOAuth.RefreshToken
import java.util.LinkedList
import java.util.Locale

import dagger.hilt.android.scopes.SingletonScoped
import javax.inject.Inject
import javax.inject.Named

/**
 * Hilt-injected account manager. Replaces manual singleton pattern.
 * Provides Reddit account CRUD operations backed by SQLite.
 */
@SingletonScoped
class RedditAccountManager @Inject constructor(
    @ApplicationContext private val context: Context
) : SQLiteOpenHelper(
    context.applicationContext,
    ACCOUNTS_DB_FILENAME,
    null,
    ACCOUNTS_DB_VERSION
) {
    private var accountsCache: MutableList<RedditAccount?>? = null
    private var defaultAccountCache: RedditAccount? = null

    private val updateNotifier
            : UpdateNotifier<RedditAccountChangeListener?> =
        object : UpdateNotifier<RedditAccountChangeListener?>() {
            override fun notifyListener(listener: RedditAccountChangeListener) {
                listener.onRedditAccountChanged()
            }
        }

    override fun onCreate(db: SQLiteDatabase) {
        val queryString = String.format(
            "CREATE TABLE %s (" +
                    "%s TEXT NOT NULL PRIMARY KEY ON CONFLICT REPLACE," +
                    "%s TEXT," +
                    "%s INTEGER," +
                    "%s BOOLEAN NOT NULL," +
                    "%s TEXT)",
            TABLE,
            FIELD_USERNAME,
            FIELD_REFRESH_TOKEN,
            FIELD_PRIORITY,
            FIELD_USES_NEW_CLIENT_ID,
            FIELD_CLIENT_ID
        )

        db.execSQL(queryString)

        addAccount(anon, db)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        if (oldVersion < 2) {
            db.execSQL(
                String.format(
                    Locale.US,
                    "UPDATE %s SET %2\$s=TRIM(%2\$s) WHERE %2\$s <> TRIM(%2\$s)",
                    TABLE,
                    FIELD_USERNAME
                )
            )
        }

        if (oldVersion < 3) {
            db.execSQL(
                String.format(
                    Locale.US,
                    "ALTER TABLE %s ADD COLUMN %s BOOLEAN NOT NULL DEFAULT 0",
                    TABLE,
                    FIELD_USES_NEW_CLIENT_ID
                )
            )
        }

        if (oldVersion < 4) {
            db.execSQL(
                String.format(
                    Locale.US,
                    "ALTER TABLE %s ADD COLUMN %s TEXT DEFAULT NULL",
                    TABLE,
                    FIELD_CLIENT_ID
                )
            )
        }
    }

    @Synchronized
    fun addAccount(account: RedditAccount) {
        addAccount(account, null)
    }

    @Synchronized
    private fun addAccount(
        account: RedditAccount,
        inDb: SQLiteDatabase?
    ) {
        val db: SQLiteDatabase?
        if (inDb == null) {
            db = getWritableDatabase()
        } else {
            db = inDb
        }

        val row = ContentValues()

        row.put(FIELD_USERNAME, account.username)

        if (account.refreshToken == null) {
            row.putNull(FIELD_REFRESH_TOKEN)
        } else {
            row.put(FIELD_REFRESH_TOKEN, account.refreshToken.token)
        }

        row.put(FIELD_PRIORITY, account.priority)
        row.put(FIELD_USES_NEW_CLIENT_ID, 1)
        row.put(FIELD_CLIENT_ID, account.clientId)

        db.insert(TABLE, null, row)

        reloadAccounts(db)
        updateNotifier.updateAllListeners()

        if (inDb == null) {
            db.close()
        }
    }

    @get:Synchronized
    val accounts: ArrayList<RedditAccount>
        get() {
            if (accountsCache == null) {
                val db = getReadableDatabase()
                reloadAccounts(db)
                db.close()
            }

            return java.util.ArrayList<RedditAccount>(accountsCache)
        }

    fun getAccount(username: String): RedditAccount? {
        val usernameCanonical = StringUtils.asciiLowercase(username.trim { it <= ' ' })

        if (usernameCanonical.isEmpty()) {
            return anon
        }

        val accounts = this.accounts
        var selectedAccount: RedditAccount? = null

        for (account in accounts) {
            if (!account.isAnonymous && account.canonicalUsername == usernameCanonical) {
                selectedAccount = account
                break
            }
        }

        return selectedAccount
    }

    @get:Synchronized
    @set:Synchronized
    var defaultAccount: RedditAccount?
        get() {
            if (defaultAccountCache == null) {
                val db = getReadableDatabase()
                reloadAccounts(db)
                db.close()
            }

            return defaultAccountCache
        }
        set(newDefault) {
            val db = getWritableDatabase()

            db.execSQL(
                String.format(
                    Locale.US,
                    "UPDATE %s SET %s=(SELECT MIN(%s)-1 FROM %s) WHERE %s=?",
                    TABLE,
                    FIELD_PRIORITY,
                    FIELD_PRIORITY,
                    TABLE,
                    FIELD_USERNAME
                ),
                arrayOf<String>(newDefault!!.username)
            )

            reloadAccounts(db)
            db.close()

            updateNotifier.updateAllListeners()
        }

    @Synchronized
    private fun reloadAccounts(db: SQLiteDatabase) {
        val fields = arrayOf<String?>(
            FIELD_USERNAME,
            FIELD_REFRESH_TOKEN,
            FIELD_PRIORITY,
            FIELD_CLIENT_ID
        )

        val cursor = db.query(
            TABLE,
            fields,
            null,
            null,
            null,
            null,
            FIELD_PRIORITY + " ASC"
        )

        accountsCache = LinkedList<RedditAccount?>()
        defaultAccountCache = null

        if (cursor != null) {
            while (cursor.moveToNext()) {
                val username = cursor.getString(0)

                val refreshToken: RefreshToken?
                if (cursor.isNull(1)) {
                    refreshToken = null
                } else {
                    refreshToken = RefreshToken(cursor.getString(1))
                }

                val priority = cursor.getLong(2)
                val clientId = cursor.getString(3)

                val account = RedditAccount(
                    username,
                    refreshToken,
                    priority,
                    clientId
                )

                accountsCache!!.add(account)

                if (defaultAccountCache == null
                    || account.priority < defaultAccountCache!!.priority
                ) {
                    defaultAccountCache = account
                }
            }

            cursor.close()
        } else {
            handleGlobalError(context, "Cursor was null after query")
        }
    }

    fun addUpdateListener(listener: RedditAccountChangeListener?) {
        updateNotifier.addListener(listener)
    }

    fun removeUpdateListener(listener: RedditAccountChangeListener?) {
        updateNotifier.removeListener(listener)
    }

    fun deleteAccount(account: RedditAccount) {
        val db = getWritableDatabase()
        db.delete(TABLE, FIELD_USERNAME + "=?", arrayOf<String>(account.username))
        reloadAccounts(db)
        updateNotifier.updateAllListeners()
        db.close()
    }

    companion object {
        val anon: RedditAccount = RedditAccount(
            "",
            null,
            10,
            null
        )

        private const val ACCOUNTS_DB_FILENAME = "accounts_oauth2.db"
        private const val TABLE = "accounts_oauth2"
        private const val FIELD_USERNAME = "username"
        private const val FIELD_REFRESH_TOKEN = "refresh_token"
        private const val FIELD_PRIORITY = "priority"
        private const val FIELD_CLIENT_ID = "client_id"
        private const val FIELD_USES_NEW_CLIENT_ID = "uses_new_client_id"

        private const val ACCOUNTS_DB_VERSION = 4
    }
}
