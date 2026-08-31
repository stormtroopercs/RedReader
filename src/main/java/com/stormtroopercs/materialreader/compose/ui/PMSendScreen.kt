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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stormtroopercs.materialreader.R
import com.stormtroopercs.materialreader.common.General
import com.stormtroopercs.materialreader.navigation.PMSendDraft
import com.stormtroopercs.materialreader.navigation.PMViewModel

/**
 * Compose PM composer (replaces the legacy `PMSendActivity`).
 *
 * Account dropdown (non-anonymous accounts) + recipient / subject / message
 * fields, a Send action (with progress) and a markdown preview — the same
 * fields and the same `api/compose` request the legacy activity had, with the
 * draft-memory behaviour kept via [PMSendDraft] (prefill when opened without
 * explicit values, save on leave without a successful send, clear on success).
 *
 * [initialRecipient] / [initialSubject] / [initialText] are the explicit
 * values a launcher carried (the `cm:` URL parts); when absent the draft
 * memory is used, exactly like the legacy activity's fallbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PMSendScreen(
	initialRecipient: String? = null,
	initialSubject: String? = null,
	initialText: String? = null,
	onDone: () -> Unit,
	onNavigateBack: () -> Unit,
) {
	val viewModel: PMViewModel = hiltViewModel()
	val context = LocalContext.current
	val state by viewModel.state.collectAsStateWithLifecycle()

	var account by remember { mutableStateOf(viewModel.accounts.firstOrNull()) }
	var recipient by remember { mutableStateOf(initialRecipient ?: PMSendDraft.recipient ?: "") }
	var subject by remember { mutableStateOf(initialSubject ?: PMSendDraft.subject ?: "") }
	var text by remember { mutableStateOf(initialText ?: PMSendDraft.text ?: "") }
	var showAccountMenu by remember { mutableStateOf(false) }
	var showPreview by remember { mutableStateOf(false) }

	val canSend = recipient.isNotBlank() &&
		text.isNotBlank() &&
		state !is PMViewModel.PMUiState.Submitting

	// Draft memory (the legacy activity saved in onDestroy when the send had
	// not succeeded): snapshot the fields on leaving the composition, and
	// clear them once a send succeeds.
	var sentSuccessfully by remember { mutableStateOf(false) }
	DisposableEffect(Unit) {
		onDispose {
			if (!sentSuccessfully) {
				PMSendDraft.save(recipient, subject, text)
			}
		}
	}

	// React to the send result: success clears the draft + leaves; a failure
	// shows the same result dialog the legacy activity used.
	LaunchedEffect(state) {
		val s = state
		when (s) {
			is PMViewModel.PMUiState.Success -> {
				sentSuccessfully = true
				PMSendDraft.clear()
				General.quickToast(context, context.getString(R.string.pm_send_done))
				viewModel.onDone()
				onDone()
			}

			is PMViewModel.PMUiState.Failed -> {
				val error = s.error
				(context as? AppCompatActivity)?.let {
					General.showResultDialog(it, error)
				}
			}

			else -> Unit
		}
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(context.getString(R.string.pm_send_actionbar)) },
				navigationIcon = {
					IconButton(onClick = onNavigateBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = "Back",
						)
					}
				},
				actions = {
					IconButton(
						onClick = {
							(context as? AppCompatActivity)?.let {
								viewModel.submit(
									it,
									account ?: return@let,
									recipient,
									subject,
									text,
								)
							}
						},
						enabled = canSend && account != null,
					) {
						if (state is PMViewModel.PMUiState.Submitting) {
							CircularProgressIndicator(
								modifier = Modifier.padding(4.dp),
								strokeWidth = 2.dp,
							)
						} else {
							Icon(
								imageVector = Icons.AutoMirrored.Filled.Send,
								contentDescription = context.getString(R.string.comment_reply_send),
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
			// Account dropdown (the legacy spinner) — send-as selection.
			Column(
				modifier = Modifier.fillMaxWidth(),
				horizontalAlignment = Alignment.Start,
			) {
				Button(onClick = { showAccountMenu = true }) {
					Text(account ?: context.getString(R.string.error_toast_notloggedin))
				}
				DropdownMenu(
					expanded = showAccountMenu,
					onDismissRequest = { showAccountMenu = false },
				) {
					viewModel.accounts.forEach { name ->
						DropdownMenuItem(
							text = { Text(name) },
							onClick = {
								account = name
								showAccountMenu = false
							},
						)
					}
				}
			}

			OutlinedTextField(
				value = recipient,
				onValueChange = { recipient = it },
				label = { Text(context.getString(R.string.pm_send_hint_recipient)) },
				singleLine = true,
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 16.dp),
			)

			OutlinedTextField(
				value = subject,
				onValueChange = { subject = it.take(100) },
				label = { Text(context.getString(R.string.pm_send_hint_subject)) },
				singleLine = true,
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 12.dp),
			)

			OutlinedTextField(
				value = text,
				onValueChange = { text = it },
				label = { Text(context.getString(R.string.pm_send_hint_message_text)) },
				minLines = 8,
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 12.dp),
			)

			Button(
				onClick = {
					(context as? AppCompatActivity)?.let {
						viewModel.submit(
							it,
							account ?: return@let,
							recipient,
							subject,
							text,
						)
					}
				},
				enabled = canSend && account != null,
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 16.dp),
			) {
				Text(context.getString(R.string.comment_reply_send))
			}
		}
	}

	if (showPreview) {
		MarkdownPreviewDialog(markdown = text, onDismiss = { showPreview = false })
	}
}
