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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stormtroopercs.materialreader.navigation.PostSubmitViewModel
import com.stormtroopercs.materialreader.navigation.PostSubmitViewModel.FlairState
import com.stormtroopercs.materialreader.navigation.PostSubmitViewModel.ImgurState
import com.stormtroopercs.materialreader.navigation.PostSubmitViewModel.PostType
import com.stormtroopercs.materialreader.navigation.PostSubmitViewModel.SubmitUiState
import com.stormtroopercs.materialreader.reddit.things.SubredditCanonicalId

/**
 * Compose post-submission screen (replaces `PostSubmitActivity` + its two
 * fragments + `ImgurUploadActivity`).
 *
 * Form state and the `api/submit` request live in [PostSubmitViewModel].
 * The subreddit picker is an in-screen dialog over the account's subreddit
 * history (most-recent-first, prefix/substring ranked — the same source and
 * ranking the legacy `PostSubmitSubredditSelectionFragment` used), with a
 * free-form "post to r/<name>" action for subreddits not in the history.
 *
 * Flair: when the selected subreddit offers flair for new links, a flair
 * dropdown (the same `api/flairselector` request the legacy content fragment
 * fetched) appears and the chosen `flair_id` is sent with the submission.
 * Imgur upload: link posts offer an "Upload to Imgur" chip (the same
 * multipart `api.imgur.com/3/image` request the legacy `ImgurUploadActivity`
 * issued); a successful upload fills the URL field with the imgur URL.
 * A shared-text launch (ACTION_SEND) pre-fills the URL and opens the
 * subreddit picker up front.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostSubmitScreen(
	subreddit: String,
	shareUrl: String? = null,
	onNavigateBack: () -> Unit,
	onSubmitted: () -> Unit,
) {
	val viewModel: PostSubmitViewModel = hiltViewModel()
	val context = LocalContext.current
	val submitState by viewModel.submitState.collectAsStateWithLifecycle()
	val flairState by viewModel.flairState.collectAsStateWithLifecycle()
	val imgurState by viewModel.imgurState.collectAsStateWithLifecycle()

	var title by remember { mutableStateOf("") }
	var bodyText by remember { mutableStateOf("") }
	// A shared-text launch (ACTION_SEND) pre-fills the link URL.
	var bodyUrl by remember { mutableStateOf(shareUrl ?: "") }
	var postType by remember { mutableStateOf<PostType>(PostType.Link) }
	var selectedSubreddit by remember { mutableStateOf(subreddit) }
	var showSubredditPicker by remember { mutableStateOf(false) }
	var showFlairDropdown by remember { mutableStateOf(false) }
	var errorText by remember { mutableStateOf<String?>(null) }
	var initialized by remember { mutableStateOf(false) }

	val imgurPicker = rememberLauncherForActivityResult(
		ActivityResultContracts.GetContent(),
	) { uri ->
		if (uri != null) {
			(context as? AppCompatActivity)?.let {
				viewModel.uploadImgur(it, uri)
			}
		}
	}

	// Seed the (per-navigation-entry) ViewModel from the route parameters.
	LaunchedEffect(Unit) {
		if (!initialized) {
			initialized = true
			viewModel.setSubreddit(selectedSubreddit)
			viewModel.setPostType(postType)
			shareUrl?.let { viewModel.setBodyUrl(it) }
			if (selectedSubreddit.isBlank()) {
				// A share launch (ACTION_SEND) arrives with no subreddit —
				// open the picker up front, as the legacy selection fragment did.
				showSubredditPicker = true
			}
		}
	}

	// Load the flair selector when the target subreddit changes.
	LaunchedEffect(selectedSubreddit) {
		if (selectedSubreddit.isNotBlank()) {
			viewModel.loadFlairFor(selectedSubreddit)
		}
	}

	// An Imgur upload that succeeded fills the link-post URL field.
	LaunchedEffect(imgurState) {
		if (imgurState is ImgurState.Success) {
			bodyUrl = (imgurState as ImgurState.Success).url
			viewModel.setBodyUrl(bodyUrl)
		}
	}

	LaunchedEffect(submitState) {
		when (val state = submitState) {
			is SubmitUiState.Success -> {
				// The route is popped by the caller; nothing else to do here.
				onSubmitted()
			}
			is SubmitUiState.Error -> {
				errorText = state.message
			}
			else -> Unit
		}
	}

	val canSubmit = !submitState.equals(SubmitUiState.Submitting) &&
		selectedSubreddit.isNotBlank() &&
		title.isNotBlank() &&
		title.length <= 300 &&
		(if (postType == PostType.Self) true else bodyUrl.isNotBlank())

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Create post") },
				navigationIcon = {
					IconButton(onClick = onNavigateBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Default.ArrowBack,
							contentDescription = "Back",
						)
					}
				},
				actions = {
					FilledTonalButton(
						onClick = {
							viewModel.setSubreddit(selectedSubreddit)
							viewModel.setPostType(postType)
							viewModel.setTitle(title)
							viewModel.setBodyText(bodyText)
							viewModel.setBodyUrl(bodyUrl)
							(context as? AppCompatActivity)?.let {
								viewModel.submit(it)
							}
						},
						enabled = canSubmit,
					) {
						Text("Post", fontWeight = FontWeight.Bold)
					}
				},
			)
		},
	) { paddingValues ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues)
				.verticalScroll(rememberScrollState())
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp),
		) {
			// Subreddit selector
			Card(
				modifier = Modifier
					.fillMaxWidth()
					.clickable(onClick = { showSubredditPicker = true }),
			) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(16.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Icon(
						imageVector = Icons.Default.Group,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.primary,
					)
					Spacer(modifier = Modifier.width(16.dp))
					Column {
						Text(
							text = "r/$selectedSubreddit",
							style = MaterialTheme.typography.bodyLarge,
							fontWeight = FontWeight.Medium,
						)
						Text(
							text = "Tap to change subreddit",
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
						)
					}
				}
			}

			// Post type toggle
			Card(modifier = Modifier.fillMaxWidth()) {
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.padding(16.dp),
				) {
					Text(
						text = "Post type",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.SemiBold,
						modifier = Modifier.padding(bottom = 12.dp),
					)
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(8.dp),
					) {
						FilterChip(
							selected = postType == PostType.Self,
							onClick = { postType = PostType.Self },
							label = { Text("Text") },
						)
						FilterChip(
							selected = postType == PostType.Link,
							onClick = { postType = PostType.Link },
							label = { Text("Link") },
						)
					}
				}
			}

			// Flair selector (available when the subreddit offers flair)
			if (flairState is FlairState.Available || flairState is FlairState.Loading) {
				ExposedDropdownMenuBox(
					expanded = showFlairDropdown,
					onExpandedChange = {
						if (flairState is FlairState.Available) {
							showFlairDropdown = !showFlairDropdown
						}
					},
				) {
					OutlinedTextField(
						value = (
							viewModel.selectedFlair?.text
								?: if (flairState is FlairState.Loading) "Loading flair..." else "No flair"
							),
						onValueChange = {},
						readOnly = true,
						label = { Text("Flair") },
						trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFlairDropdown) },
						modifier = Modifier
							.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
							.fillMaxWidth(),
					)
					ExposedDropdownMenu(
						expanded = showFlairDropdown,
						onDismissRequest = { showFlairDropdown = false },
					) {
						DropdownMenuItem(
							text = { Text("No flair") },
							onClick = {
								viewModel.setSelectedFlair(null)
								showFlairDropdown = false
							},
						)
						(flairState as? FlairState.Available)?.choices?.forEach { choice ->
							DropdownMenuItem(
								text = { Text(choice.text) },
								onClick = {
									viewModel.setSelectedFlair(choice)
									showFlairDropdown = false
								},
							)
						}
					}
				}
			}

			// Title field
			OutlinedTextField(
				value = title,
				onValueChange = { if (it.length <= 300) title = it },
				label = { Text("Title *") },
				placeholder = { Text("Enter post title") },
				modifier = Modifier.fillMaxWidth(),
				singleLine = false,
				maxLines = 3,
				leadingIcon = {
					Icon(
						imageVector = Icons.Default.Title,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.primary,
					)
				},
				supportingText = {
					Text("${title.length}/300")
				},
				isError = title.length > 300,
			)

			// Body field (for text posts)
			if (postType == PostType.Self) {
				OutlinedTextField(
					value = bodyText,
					onValueChange = { bodyText = it },
					label = { Text("Body") },
					placeholder = { Text("Write your post content here (markdown supported)") },
					modifier = Modifier.fillMaxWidth(),
					minLines = 6,
					maxLines = 12,
					leadingIcon = {
						Icon(
							imageVector = Icons.Default.Edit,
							contentDescription = null,
							tint = MaterialTheme.colorScheme.primary,
						)
					},
					supportingText = {
						Text("${bodyText.length} characters")
					},
				)
			}

			// URL field (for link posts)
			if (postType == PostType.Link) {
				OutlinedTextField(
					value = bodyUrl,
					onValueChange = { bodyUrl = it },
					label = { Text("URL *") },
					placeholder = { Text("https://example.com") },
					modifier = Modifier.fillMaxWidth(),
					singleLine = true,
					keyboardOptions = KeyboardOptions(
						keyboardType = KeyboardType.Uri,
						imeAction = ImeAction.Done,
					),
					leadingIcon = {
						Icon(
							imageVector = Icons.Default.Link,
							contentDescription = null,
							tint = MaterialTheme.colorScheme.primary,
						)
					},
					isError = bodyUrl.isNotBlank() &&
						!bodyUrl.trim().startsWith("http"),
				)

				// "Upload to Imgur" (the legacy ImgurUploadActivity's job)
				Row(
					modifier = Modifier.fillMaxWidth(),
					verticalAlignment = Alignment.CenterVertically,
				) {
					AssistChip(
						onClick = { imgurPicker.launch("image/*") },
						leadingIcon = {
							Icon(
								imageVector = Icons.Default.PhotoLibrary,
								contentDescription = null,
								modifier = Modifier.size(18.dp),
							)
						},
						label = { Text("Upload to Imgur") },
						enabled = imgurState !is ImgurState.Uploading,
					)
					Spacer(modifier = Modifier.width(8.dp))
					when (val state = imgurState) {
						is ImgurState.Uploading -> {
							CircularProgressIndicator(
								modifier = Modifier.size(20.dp),
								strokeWidth = 2.dp,
							)
							Spacer(modifier = Modifier.width(8.dp))
							Text(
								"Uploading...",
								style = MaterialTheme.typography.bodySmall,
							)
						}
						is ImgurState.Success -> {
							Text(
								"Uploaded (${state.summary})",
								style = MaterialTheme.typography.bodySmall,
								color = Color(0xFF4CAF50),
							)
						}
						is ImgurState.Error -> {
							Text(
								state.message,
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.error,
							)
						}
						ImgurState.Idle -> Unit
					}
				}
			}

			// Error from a failed submit
			errorText?.let {
				Text(
					text = it,
					color = MaterialTheme.colorScheme.error,
					style = MaterialTheme.typography.bodyMedium,
				)
			}

			// Submit button
			Button(
				onClick = {
					viewModel.setSubreddit(selectedSubreddit)
					viewModel.setPostType(postType)
					viewModel.setTitle(title)
					viewModel.setBodyText(bodyText)
					viewModel.setBodyUrl(bodyUrl)
					(context as? AppCompatActivity)?.let {
						viewModel.submit(it)
					}
				},
				modifier = Modifier
					.fillMaxWidth()
					.height(56.dp),
				enabled = canSubmit,
			) {
				if (submitState is SubmitUiState.Submitting) {
					CircularProgressIndicator(
						modifier = Modifier.padding(end = 8.dp),
						strokeWidth = 2.dp,
					)
					Text("Submitting...")
				} else {
					Icon(
						imageVector = Icons.AutoMirrored.Filled.Send,
						contentDescription = null,
						modifier = Modifier.padding(end = 8.dp),
					)
					Text("Submit post")
				}
			}
		}
	}

	if (showSubredditPicker) {
		SubredditPickerDialog(
			currentSubreddit = selectedSubreddit,
			suggestions = viewModel.subredditSuggestions(),
			onSelected = { name ->
				selectedSubreddit = name
				viewModel.setSubreddit(name)
				showSubredditPicker = false
			},
			onDismiss = { showSubredditPicker = false },
		)
	}
}

/**
 * In-screen subreddit picker: a search box over the account's subreddit
 * history (prefix matches first, then substring matches — mirroring the
 * legacy autocomplete ranking), plus a "Post to r/<text>" action for any
 * subreddit not in the history.
 */
@Composable
private fun SubredditPickerDialog(
	currentSubreddit: String,
	suggestions: List<SubredditCanonicalId>,
	onSelected: (String) -> Unit,
	onDismiss: () -> Unit,
) {
	var query by remember { mutableStateOf("") }
	val focusRequester = remember { FocusRequester() }

	val filtered = remember(query, suggestions) {
		val q = query.trim().lowercase().removePrefix("r/")
		if (q.isEmpty()) {
			suggestions
		} else {
			val prefix = suggestions.filter {
				it.displayNameLowercase.startsWith(q)
			}
			val contains = suggestions
				.filter { it.displayNameLowercase.contains(q) }
				.filterNot { it.displayNameLowercase.startsWith(q) }
			(prefix + contains).take(50)
		}
	}

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Select subreddit") },
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
				OutlinedTextField(
					value = query,
					onValueChange = { query = it },
					modifier = Modifier
						.fillMaxWidth()
						.focusRequester(focusRequester),
					label = { Text("Subreddit") },
					placeholder = { Text("Search your subreddits") },
					singleLine = true,
					leadingIcon = {
						Icon(
							imageVector = Icons.Default.Search,
							contentDescription = null,
						)
					},
				)
				LazyColumn(
					modifier = Modifier
						.fillMaxWidth()
						.weight(1f),
				) {
					items(filtered) { sr ->
						ListItem(
							headlineContent = {
								Text(
									"r/${sr.displayNameLowercase}",
									maxLines = 1,
									overflow = TextOverflow.Ellipsis,
								)
							},
							modifier = Modifier.clickable { onSelected(sr.displayNameLowercase) },
						)
					}
					if (filtered.isEmpty()) {
						item {
							Text(
								"No matches in your subreddit history",
								modifier = Modifier.padding(16.dp),
								color = MaterialTheme.colorScheme.onSurfaceVariant,
							)
						}
					}
				}
			}
		},
		confirmButton = {
			TextButton(
				onClick = {
					val target = query.trim().removePrefix("r/").lowercase()
					if (target.isNotBlank()) {
						onSelected(target)
					} else {
						onDismiss()
					}
				},
			) {
				Text("Post to r/${query.trim().removePrefix("r/").ifEmpty { currentSubreddit }}")
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text("Cancel")
			}
		},
	)
}
