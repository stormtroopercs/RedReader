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

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stormtroopercs.materialreader.common.LinkHandler.onLinkClicked
import com.stormtroopercs.materialreader.common.PrefsUtility
import com.stormtroopercs.materialreader.common.UriString

/**
 * Compose screen for Reddit Terms of Service / User Agreement.
 * Replaces legacy RedditTermsActivity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedditTermsScreen(
    launchMainOnClose: Boolean = false,
    onDone: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reddit User Agreement") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Done"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Before continuing, please review Reddit's User Agreement.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    (context as? AppCompatActivity)?.let { host ->
                        onLinkClicked(
                            host,
                            UriString("https://www.redditinc.com/policies/user-agreement-april-18-2023")
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View User Agreement")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    PrefsUtility.acceptRedditUserAgreement()
                    onDone()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text("Accept & Continue")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    PrefsUtility.declineRedditUserAgreement()
                    onDone()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Decline")
            }
        }
    }
}
