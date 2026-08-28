/*******************************************************************************
 * This file is part of MaterialReader.
 *
 * MaterialReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MaterialReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MaterialReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.stormtroopercs.materialreader.compose.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stormtroopercs.materialreader.R
import com.stormtroopercs.materialreader.account.RedditAccount
import com.stormtroopercs.materialreader.account.RedditAccountChangeListener
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.common.AndroidCommon
import com.stormtroopercs.materialreader.common.LinkHandler
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.reddit.api.RedditOAuth

/**
 * Compose account management screen — the Compose replacement for the legacy
 * `AccountListDialog` fragment (retired in the 50th increment).
 *
 * Lists every stored account (including the anonymous one). Tapping an
 * account opens an actions menu: set as active, delete, re-authenticate (when
 * the stored client id no longer matches the running build). Tapping "Add
 * account" confirms the browser-based OAuth prompt and then hands off to the
 * in-app `OAuthLogin` route via [onNavigateToLogin].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val accountManager = remember { RedditAccountManager.getInstance(context) }

    // Live snapshot of the account list: refreshed on the UI thread whenever
    // the manager's update notifier fires (login complete, delete, re-activate).
    var accounts by remember { mutableStateOf(accountManager.accounts) }

    DisposableEffect(accountManager) {
        val listener = RedditAccountChangeListener {
            AndroidCommon.runOnUiThread {
                accounts = accountManager.accounts
            }
        }
        accountManager.addUpdateListener(listener)
        onDispose { accountManager.removeUpdateListener(listener) }
    }

    // Transient dialogs.
    var deleteTarget by remember { mutableStateOf<RedditAccount?>(null) }
    var showLoginPrompt by remember { mutableStateOf(false) }
    var menuTarget by remember { mutableStateOf<RedditAccount?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.options_accounts_long)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // "Add account" header (mirrors the legacy dialog's header row).
            item(key = "add") {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.accounts_add)) },
                    leadingContent = {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showLoginPrompt = true }
                )
            }
            item(key = "divider") { HorizontalDivider() }
            items(accounts, key = { "acct${it.username}" }) { account ->
                AccountRow(
                    account = account,
                    isActive = account == accountManager.defaultAccount,
                    needsRelogin = RedditOAuth.needsRelogin(account),
                    onClick = { menuTarget = account }
                )
                HorizontalDivider()
            }
        }
    }

    // Per-account actions menu (the legacy dialog offered the same three).
    menuTarget?.let { target ->
        val actions = buildList {
            if (target != accountManager.defaultAccount) {
                add(stringResource(R.string.accounts_setactive) to {
                    accountManager.defaultAccount = target
                })
            }
            if (target.isNotAnonymous) {
                add(stringResource(R.string.accounts_delete) to {
                    deleteTarget = target
                })
            }
            if (RedditOAuth.needsRelogin(target)) {
                add(stringResource(R.string.accounts_reauth) to {
                    showLoginPrompt = true
                })
            }
        }
        if (actions.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { menuTarget = null },
                title = {
                    Text(
                        if (target.isAnonymous) stringResource(R.string.accounts_anon)
                        else target.username
                    )
                },
                text = {
                    Column {
                        actions.forEach { (label, action) ->
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    menuTarget = null
                                    action()
                                }
                            ) { Text(label) }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { menuTarget = null }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                }
            )
        }
    }

    // Delete confirmation (mirrors the legacy dialog's confirm dialog).
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.accounts_delete)) },
            text = { Text(stringResource(R.string.accounts_delete_sure)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    accountManager.deleteAccount(target)
                }) { Text(stringResource(R.string.accounts_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    // Pre-login prompt (mirrors the legacy dialog's prelogin dialog): the
    // OAuth flow opens Reddit in an in-app browser and redirects back on
    // success.
    if (showLoginPrompt) {
        AlertDialog(
            onDismissRequest = { showLoginPrompt = false },
            text = {
                Column {
                    Text(stringResource(R.string.reddit_login_browser_popup_line_1))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.reddit_login_browser_popup_line_2_internal_browser_only))
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = {
                        showLoginPrompt = false
                        LinkHandler.onLinkClicked(
                            context as androidx.appcompat.app.AppCompatActivity,
                            UriString("https://redreader.org/loginhelp/")
                        )
                    }) {
                        Text(
                            stringResource(R.string.having_trouble_logging_in),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showLoginPrompt = false
                    onNavigateToLogin()
                }) { Text(stringResource(R.string.dialog_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { showLoginPrompt = false }) {
                    Text(stringResource(R.string.dialog_close))
                }
            }
        )
    }
}

@Composable
private fun AccountRow(
    account: RedditAccount,
    isActive: Boolean,
    needsRelogin: Boolean,
    onClick: () -> Unit
) {
    val label = if (account.isAnonymous) {
        stringResource(R.string.accounts_anon)
    } else {
        account.username
    }
    val subtitle = buildList {
        if (isActive) add(stringResource(R.string.accounts_active))
        if (needsRelogin) add(stringResource(R.string.reddit_relogin_error_title))
    }.joinToString(", ").ifEmpty { null }

    ListItem(
        headlineContent = { Text(label) },
        supportingContent = {
            subtitle?.let { sub -> Text(sub) }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
