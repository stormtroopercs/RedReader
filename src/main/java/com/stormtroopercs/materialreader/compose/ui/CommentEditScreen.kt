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

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stormtroopercs.materialreader.R
import com.stormtroopercs.materialreader.activities.BaseActivity
import com.stormtroopercs.materialreader.navigation.CommentEditViewModel
import com.stormtroopercs.materialreader.reddit.kthings.RedditIdAndType
import com.stormtroopercs.materialreader.reddit.prepared.markdown.MarkdownParser

/**
 * Compose comment/post edit screen (replaces the legacy `CommentEditActivity`).
 *
 * The same `api/editusertext` request covers both cases — the legacy
 * activity's two titles ("Edit Comment" / "Edit Post") map to
 * [isSelfPost]. State + the write live in [CommentEditViewModel]. The
 * markdown preview dialog re-uses the same [MarkdownParser] the legacy
 * `MarkdownPreviewDialog` used, embedded in a `MaterialAlertDialog`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentEditScreen(
	idAndType: RedditIdAndType,
	initialText: String,
	isSelfPost: Boolean,
	onDone: () -> Unit,
	onNavigateBack: () -> Unit,
) {
	val viewModel: CommentEditViewModel = hiltViewModel()
	val context = LocalContext.current
	val state by viewModel.state.collectAsStateWithLifecycle()

	var text by remember { mutableStateOf(initialText) }
	var showPreview by remember { mutableStateOf(false) }

	// Seed the (per-navigation-entry) ViewModel.
	LaunchedEffect(Unit) {
		viewModel.setThing(idAndType, initialText)
	}

	LaunchedEffect(state) {
		when (state) {
			is CommentEditViewModel.EditUiState.Success -> {
				context.getString(
					if (isSelfPost) R.string.post_edit_done else R.string.comment_edit_done,
				).let { com.stormtroopercs.materialreader.common.General.quickToast(context, it) }
				onDone()
			}

			is CommentEditViewModel.EditUiState.Error ->
				com.stormtroopercs.materialreader.common.General.quickToast(
					context,
					(state as CommentEditViewModel.EditUiState.Error).message,
				)

			else -> Unit
		}
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = {
					Text(
						context.getString(
							if (isSelfPost) {
								R.string.edit_post_actionbar
							} else {
								R.string.edit_comment_actionbar
							},
						),
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
				actions = {
					val submitting = state is CommentEditViewModel.EditUiState.Submitting
					IconButton(
						onClick = {
							(context as? AppCompatActivity)?.let { viewModel.submit(it, text) }
						},
						enabled = text.isNotBlank() && !submitting,
					) {
						if (submitting) {
							CircularProgressIndicator(
								modifier = Modifier.padding(4.dp),
								strokeWidth = 2.dp,
							)
						} else {
							Icon(
								imageVector = Icons.Default.Save,
								contentDescription = "Save",
							)
						}
					}
					TextButton(
						onClick = { showPreview = true },
						enabled = text.isNotBlank(),
					) {
						Text(context.getString(R.string.comment_reply_preview))
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
		) {
			OutlinedTextField(
				value = text,
				onValueChange = { text = it },
				modifier = Modifier
					.fillMaxWidth(),
				minLines = 12,
			)
			Button(
				onClick = {
					(context as? AppCompatActivity)?.let { viewModel.submit(it, text) }
				},
				enabled = text.isNotBlank() && state !is CommentEditViewModel.EditUiState.Submitting,
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 16.dp),
			) {
				Text(context.getString(R.string.comment_edit_save))
			}
		}
	}

	if (showPreview) {
		MarkdownPreviewDialog(markdown = text, onDismiss = { showPreview = false })
	}
}

/**
 * Markdown preview dialog — the Compose equivalent of the legacy
 * `MarkdownPreviewDialog`: the same [MarkdownParser] `buildView` output in a
 * scrollable `MaterialAlertDialog`.
 */
@Composable
internal fun MarkdownPreviewDialog(
	markdown: String,
	onDismiss: () -> Unit,
) {
	val context = LocalContext.current
	androidx.compose.material3.AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(context.getString(R.string.comment_reply_preview)) },
		text = {
			val activity = context as? BaseActivity
			if (activity != null) {
				AndroidView(
					factory = {
						MarkdownParser.parse(markdown.toCharArray()).buildView(
							activity,
							null,
							14f,
							false,
						)
					},
				)
			} else {
				Text(markdown)
			}
		},
		confirmButton = {
			TextButton(onClick = onDismiss) { Text("OK") }
		},
	)
}
