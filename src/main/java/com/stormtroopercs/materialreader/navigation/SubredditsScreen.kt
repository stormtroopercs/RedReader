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

package com.stormtroopercs.materialreader.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The full-screen Subreddits list — the Posts tab's **Subreddits** chip
 * (formerly the Lemmy app's "Communities" directory chip) opens this screen:
 * the signed-in user's subscribed subreddits, the same list the drawer's
 * Subscriptions section shows (same [MainScreenViewModel], same
 * [SubscriptionRow] rows). Tapping a row opens that subreddit's feed.
 *
 * Signed out (or the account has no subscriptions) → an empty state with a
 * hint to sign in; there is no subreddit list to show anonymous accounts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubredditsScreen(
	onNavigateBack: () -> Unit,
	onOpenSubreddit: (String) -> Unit,
) {
	val viewModel: MainScreenViewModel = hiltViewModel()
	val state by viewModel.state.collectAsStateWithLifecycle()

	Scaffold(
		topBar = {
			TopAppBar(
				title = {
					Text(
						text = "Subreddits",
						fontWeight = FontWeight.SemiBold,
					)
				},
				navigationIcon = {
					IconButton(onClick = onNavigateBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = "Back",
						)
					}
				},
			)
		},
	) { paddingValues ->
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues),
		) {
			when (val subscribed = state) {
				is MainScreenViewModel.SubscribedState.Loading -> {
					Box(
						modifier = Modifier.fillMaxSize(),
						contentAlignment = Alignment.Center,
					) {
						CircularProgressIndicator()
					}
				}

				is MainScreenViewModel.SubscribedState.Error -> {
					Box(
						modifier = Modifier.fillMaxSize(),
						contentAlignment = Alignment.Center,
					) {
						Text(subscribed.message)
					}
				}

				is MainScreenViewModel.SubscribedState.Success -> {
					if (subscribed.subreddits.isEmpty()) {
						Box(
							modifier = Modifier.fillMaxSize(),
							contentAlignment = Alignment.Center,
						) {
							Text(
								text = "No subreddits",
								style = MaterialTheme.typography.bodyLarge,
							)
						}
					} else {
						LazyColumn(
							modifier = Modifier.fillMaxSize(),
							content = {
								items(subscribed.subreddits, key = { it.name }) { subreddit ->
									SubscriptionRow(
										name = subreddit.name,
										iconUrl = subreddit.iconUrl,
										onClick = { onOpenSubreddit(subreddit.name) },
									)
								}
							},
						)
					}
				}

				is MainScreenViewModel.SubscribedState.Idle -> {
					Box(
						modifier = Modifier.fillMaxSize(),
						contentAlignment = Alignment.Center,
					) {
						Text(
							text = "Sign in to see your subreddits",
							style = MaterialTheme.typography.bodyLarge,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
						)
					}
				}
			}
		}
	}
}
