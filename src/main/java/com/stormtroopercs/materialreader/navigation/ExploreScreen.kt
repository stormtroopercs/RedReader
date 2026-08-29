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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The reference's Explore tab (FINAL-DESIGN Phase 6 target): a search field
 * plus a feed directory. This is the Phase 2 base (search + a flat directory
 * of default feeds); Phase 6 adds the collapsible "Recently searched" /
 * "Feeds" sections, the per-feed kebab, and the communities directory.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
	onNavigateToSubreddit: (String) -> Unit,
) {
	val query = remember { mutableStateOf("") }
	val feeds = listOf(
		"frontpage" to "Your home feed",
		"popular" to "Popular across Reddit",
		"all" to "All public posts",
	)

	Column(modifier = Modifier.fillMaxSize()) {
		Column(
			modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
		) {
			OutlinedTextField(
				value = query.value,
				onValueChange = { query.value = it },
				placeholder = { Text("Search") },
				leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
			)
		}
		Column(modifier = Modifier.padding(horizontal = 16.dp)) {
			Text(
				text = "Feeds",
				style = MaterialTheme.typography.titleSmall,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.primary,
			)
			Spacer(Modifier.height(4.dp))
		}
		if (query.value.isBlank()) {
			LazyColumn {
				items(feeds) { (name, description) ->
					MaterialRow(
						title = name,
						subtitle = description,
						modifier = Modifier
							.fillMaxWidth()
							.clickable { onNavigateToSubreddit(name) },
					)
				}
			}
		} else {
			// Phase 6 wires live subreddit search here.
			Column(modifier = Modifier.padding(16.dp)) {
				Text(
					text = "Search for \"$query.value\"",
					style = MaterialTheme.typography.bodyMedium,
				)
				Spacer(Modifier.height(8.dp))
				Text(
					text = "Community search will appear here.",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		}
	}
}
