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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stormtroopercs.materialreader.compose.net.NetRequestStatus
import com.stormtroopercs.materialreader.compose.net.fetchImage
import com.stormtroopercs.materialreader.common.UriString

/**
 * The Explore tab (FINAL-DESIGN Phase 6): a search field (opens the subreddit
 * search), the **communities directory** as sub-tabs (Popular / All / New /
 * Controversial — each row is icon + name + `Nk subs`, tapping opens the
 * community detail), and the **Feeds** section (the default listings, each
 * carrying an active `Default` chip). Tapping a directory row opens
 * [CommunityDetailScreen] (Phase 6.3); tapping a feed opens its list feed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onNavigateToSubreddit: (String) -> Unit,
    onNavigateToCommunity: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
) {
    val directoryVm: CommunityDirectoryViewModel = hiltViewModel()
    val rows by directoryVm.rows.collectAsStateWithLifecycle()
    val loading by directoryVm.loading.collectAsStateWithLifecycle()
    val error by directoryVm.error.collectAsStateWithLifecycle()
    var directoryTab by remember { mutableStateOf(CommunityDirectoryTab.POPULAR) }

    LaunchedEffect(directoryTab) {
        directoryVm.load(directoryTab)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // The search field (the reference's Explore header). Tapping the
        // magnifier or pressing the IME action opens the subreddit search.
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Search") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search communities")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onNavigateToSearch() }),
            )
        }

        // The communities directory sub-tabs (Popular / All / New /
        // Controversial) — FINAL-DESIGN 6.2.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CommunityDirectoryTab.entries.forEach { t ->
                FilterChip(
                    selected = t == directoryTab,
                    onClick = { directoryTab = t },
                    label = { Text(t.label) },
                )
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // The directory rows (icon + name + Nk subs).
            if (loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                error?.let { message ->
                    item {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                items(rows) { item ->
                    CommunityDirectoryRow(
                        name = item.name,
                        iconUrl = item.iconUrl,
                        subscribers = item.subscribers,
                        onClick = { onNavigateToCommunity(item.name) },
                    )
                }
                // The Feeds section (the default listings) — each carries an
                // active `Default` chip (FINAL-DESIGN 6.1).
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "Feeds",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                items(
                    items = listOf(
                        "frontpage" to "Your home feed",
                        "popular" to "Popular across Reddit",
                        "all" to "All public posts",
                    ),
                ) { (feed, description) ->
                    FeedDirectoryRow(
                        title = feed,
                        subtitle = description,
                        onClick = { onNavigateToSubreddit(feed) },
                    )
                }
            }
        }
    }
}

/**
 * One directory row (FINAL-DESIGN 6.2): a 32dp circular community icon (the
 * letter fallback when there is none), the name, and the compact `Nk subs`
 * meta line. The whole row ripples and opens the community detail.
 */
@Composable
private fun CommunityDirectoryRow(
    name: String,
    iconUrl: String?,
    subscribers: Int?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (iconUrl != null) {
                val data by fetchImage(UriString(iconUrl), scaleToMaxAxis = 96)
                when (val it = data) {
                    is NetRequestStatus.Success -> Image(
                        bitmap = it.result.data,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                    else -> Unit
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = "r/$name",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            subscribers?.let {
                Text(
                    text = "${CommunityViewModel.formatCount(it)} subs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * One Feeds row (FINAL-DESIGN 6.1): the feed title, its subtitle, and an
 * active `Default` chip (the reference's per-feed default marker). Tapping
 * opens the feed's list view.
 */
@Composable
private fun FeedDirectoryRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        AssistChip(
            onClick = {},
            label = { Text("Default") },
        )
    }
}
