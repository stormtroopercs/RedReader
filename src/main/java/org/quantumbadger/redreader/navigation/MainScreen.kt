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
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.quantumbadger.redreader.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Main screen composable.
 * Replaces MainMenuFragment with Compose UI.
 *
 * [accountName] is the signed-in Reddit username, or null when not
 * authenticated. The account row opens the user's own profile when
 * authenticated (avatar, karma, sign out) and offers sign-in otherwise.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    accountName: String? = null,
    onNavigateToPostList: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToSubredditSearch: () -> Unit = {},
    viewModel: MainScreenViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val subscribedState by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(text = "RedReader")
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
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
            item(key = "account") {
                val signedIn = !accountName.isNullOrBlank()
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick = {
                                if (signedIn) {
                                    onNavigateToProfile(accountName ?: "")
                                } else {
                                    onNavigateToLogin()
                                }
                            }
                        ),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null
                        )
                    },
                    headlineContent = {
                        Text(
                            text = accountName?.takeIf { it.isNotBlank() }?.let { "u/$it" } ?: "Sign in to Reddit",
                            fontWeight = if (signedIn) androidx.compose.ui.text.font.FontWeight.Normal else androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    },
                    supportingContent = {
                        if (signedIn) {
                            Text(text = "Profile and sign out")
                        } else {
                            Text(text = "Login required to view posts")
                        }
                    },
                    trailingContent = {
                        if (signedIn) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null
                            )
                        }
                    }
                )
            }

            item(key = "inbox") {
                MainListItem(
                    title = "Messages",
                    onClick = { onNavigateToInbox() }
                )
            }

            item(key = "search") {
                MainListItem(
                    title = "Search subreddits",
                    onClick = { onNavigateToSubredditSearch() }
                )
            }

            // "Your subreddits" — the signed-in user's subscribed subreddits
            // (alphabetical, like the legacy main menu's subreddit group).
            // Idle when signed out or when the account has no subscriptions.
            when (subscribedState) {
                is MainScreenViewModel.SubscribedState.Loading -> {
                    item(key = "subscribed") {
                        ListItem(
                            headlineContent = { Text(text = "Your subreddits") },
                            supportingContent = { Text(text = "Loading…") }
                        )
                    }
                }

                is MainScreenViewModel.SubscribedState.Error -> {
                    val error = (subscribedState as MainScreenViewModel.SubscribedState.Error)
                    item(key = "subscribed") {
                        ListItem(
                            headlineContent = { Text(text = "Your subreddits") },
                            supportingContent = { Text(text = error.message) }
                        )
                    }
                }

                is MainScreenViewModel.SubscribedState.Success -> {
                    val success = subscribedState as MainScreenViewModel.SubscribedState.Success
                    if (success.subreddits.isNotEmpty()) {
                        item(key = "subscribed-header") {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = "Your subreddits",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                },
                                supportingContent = {
                                    Text(text = "${success.subreddits.size} subscribed")
                                }
                            )
                        }
                        items(
                            items = success.subreddits,
                            key = { "subscribed-" + it.name }
                        ) { subreddit ->
                            ListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToPostList(subreddit.name) },
                                headlineContent = { Text(text = "r/${subreddit.name}") },
                                supportingContent = {
                                    Text(
                                        text = subreddit.subscribersLabel()
                                            ?.let { "$it subscribers" }
                                            ?: "Subscribed"
                                    )
                                },
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }

                is MainScreenViewModel.SubscribedState.Idle -> Unit
            }

            items(
                items = listOf("frontpage", "popular", "all"),
                key = { it }
            ) { subreddit ->
                MainListItem(
                    title = subreddit,
                    onClick = { onNavigateToPostList(subreddit) }
                )
            }
        }
    }
}

/**
 * Main list item composable.
 */
@Composable
private fun MainListItem(
    title: String,
    onClick: () -> Unit
) {
    androidx.compose.material3.ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        headlineContent = {
            Text(text = title)
        }
    )
}
