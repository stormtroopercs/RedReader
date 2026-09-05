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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Compose Changelog Screen.
 * Replaces legacy ChangelogActivity.
 * Reads changelog.txt / changelog-alpha.txt from assets.
 */
data class ChangelogVersion(
    val versionName: String,
    val entries: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val changelog = remember { parseChangelog(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Changelog") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(changelog) { version ->
                ChangelogVersionCard(version)
            }
        }
    }
}

@Composable
private fun ChangelogVersionCard(version: ChangelogVersion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = version.versionName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            version.entries.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = entry,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

private fun parseChangelog(context: android.content.Context): List<ChangelogVersion> {
    val packageName = context.packageName
    val filename = if (packageName.contains("alpha")) "changelog-alpha.txt" else "changelog.txt"

    return try {
        val versions = mutableListOf<ChangelogVersion>()
        var curVersion: ChangelogVersion? = null

        context.assets.open(filename).bufferedReader().useLines { lines ->
            for (line in lines) {
                if (line.isEmpty()) {
                    curVersion?.let { versions.add(it) }
                    curVersion = null
                } else if (curVersion == null) {
                    // Version header line: "some text/versionName"
                    val parts = line.split("/")
                    if (parts.size >= 2) {
                        curVersion = ChangelogVersion(parts[1], mutableListOf())
                    }
                } else {
                    (curVersion.entries as? MutableList<String>)?.add(line)
                }
            }
        }
        curVersion?.let { versions.add(it) }
        versions
    } catch (e: Exception) {
        emptyList()
    }
}
