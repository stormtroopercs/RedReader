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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stormtroopercs.materialreader.common.General
import com.stormtroopercs.materialreader.navigation.CommentReplyViewModel

/**
 * Compose comment-reply screen. Replaces the legacy `CommentReplyActivity`:
 * the user types a reply to a post or comment and the [CommentReplyViewModel]
 * issues the `api/comment` request. [parentThingId] is the thing being
 * replied to, as its full Reddit id-and-type string (`t3_…` for a post,
 * `t1_…` for a comment) — the same id `RedditAPI.comment` posts against.
 * On success a toast is shown and the screen dismisses back to the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentReplyScreen(
    parentThingId: String,
    onDone: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val viewModel: CommentReplyViewModel = hiltViewModel()
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var body by remember { mutableStateOf("") }

    // Seed the (per-navigation-entry) ViewModel with the thing being
    // replied to.
    LaunchedEffect(Unit) {
        viewModel.setParent(parentThingId)
    }

    LaunchedEffect(state) {
        when (state) {
            is CommentReplyViewModel.ReplyUiState.Success -> {
                General.quickToast(context, "Reply submitted")
                viewModel.onDone()
                onDone()
            }

            is CommentReplyViewModel.ReplyUiState.Error ->
                General.quickToast(
                    context,
                    (state as CommentReplyViewModel.ReplyUiState.Error).message
                )

            else -> Unit
        }
    }

    val isComment = parentThingId.startsWith("t1_")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = if (isComment) "Reply to Comment" else "Reply to Post")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    val submitting = state is CommentReplyViewModel.ReplyUiState.Submitting
                    IconButton(
                        onClick = {
                            (context as? AppCompatActivity)?.let {
                                viewModel.submit(it, body)
                            }
                        },
                        enabled = body.isNotBlank() && !submitting
                    ) {
                        if (submitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(4.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Submit"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                placeholder = { Text("Write your reply...") },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                minLines = 6,
                maxLines = 20
            )
        }
    }
}
