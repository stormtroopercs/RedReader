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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.stormtroopercs.materialreader.reddit.things.RedditUser

/**
 * Compose User Properties Dialog.
 * Shows detailed user properties in a card-based layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPropertiesDialog(
	user: RedditUser,
	onDismiss: () -> Unit,
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = {
			Text(text = user.name ?: "User Properties")
		},
		text = {
			UserPropertiesContent(user)
		},
		confirmButton = {
			TextButton(onClick = onDismiss) {
				Text("Close")
			}
		},
	)
}

@Composable
private fun UserPropertiesContent(user: RedditUser) {
	LazyColumn(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 8.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
	) {
		// User ID
		item {
			PropertyCard(
				icon = Icons.Default.AccountCircle,
				title = "User ID",
				value = user.id ?: "Unknown",
			)
		}

		// Account created
		if (user.created_utc != null) {
			item {
				PropertyCard(
					icon = Icons.Default.CalendarToday,
					title = "Account Created",
					value = com.stormtroopercs.materialreader.common.time.TimestampUTC.fromUtcSecs(user.created_utc!!).format(),
				)
			}
		}

		// Link karma
		if (user.link_karma != null) {
			item {
				PropertyCard(
					icon = Icons.Default.Person,
					title = "Link Karma",
					value = user.link_karma.toString(),
				)
			}
		}

		// Comment karma
		if (user.comment_karma != null) {
			item {
				PropertyCard(
					icon = Icons.AutoMirrored.Filled.Comment,
					title = "Comment Karma",
					value = user.comment_karma.toString(),
				)
			}
		}

		// Friend status
		if (user.is_friend != null) {
			item {
				PropertyCard(
					icon = Icons.Default.Star,
					title = "Is Friend",
					value = if (user.is_friend!!) "Yes" else "No",
				)
			}
		}

		// Gold status
		if (user.is_gold != null) {
			item {
				PropertyCard(
					icon = Icons.Default.Star,
					title = "Is Gold",
					value = if (user.is_gold!!) "Yes" else "No",
				)
			}
		}

		// Moderator status
		if (user.is_mod != null) {
			item {
				PropertyCard(
					icon = Icons.Default.Star,
					title = "Is Moderator",
					value = if (user.is_mod!!) "Yes" else "No",
				)
			}
		}

		// Employee status
		if (user.is_employee != null) {
			item {
				PropertyCard(
					icon = Icons.Default.Star,
					title = "Is Employee",
					value = if (user.is_employee!!) "Yes" else "No",
				)
			}
		}

		// Suspended status
		if (user.is_suspended != null) {
			item {
				PropertyCard(
					icon = Icons.Default.Star,
					title = "Is Suspended",
					value = if (user.is_suspended!!) "Yes" else "No",
					valueColor = if (user.is_suspended!!) MaterialTheme.colorScheme.error else null,
				)
			}
		}

		// Blocked status
		if (user.is_blocked != null) {
			item {
				PropertyCard(
					icon = Icons.Default.Star,
					title = "Is Blocked",
					value = if (user.is_blocked!!) "Yes" else "No",
					valueColor = if (user.is_blocked!!) MaterialTheme.colorScheme.error else null,
				)
			}
		}

		// Avatar URL
		if (user.icon_img != null) {
			item {
				PropertyCard(
					icon = Icons.Default.AccountCircle,
					title = "Avatar URL",
					value = user.icon_img!!,
					isLink = true,
				)
			}
		}
	}
}

@Composable
private fun PropertyCard(
	icon: ImageVector,
	title: String,
	value: String,
	valueColor: Color? = null,
	isLink: Boolean = false,
) {
	Card(
		modifier = Modifier.fillMaxWidth(),
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Icon(
				imageVector = icon,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.primary,
				modifier = Modifier.size(24.dp),
			)
			Spacer(modifier = Modifier.width(16.dp))
			Column(
				modifier = Modifier.weight(1f),
			) {
				Text(
					text = title,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
				Text(
					text = value,
					style = MaterialTheme.typography.bodyLarge,
					fontWeight = FontWeight.Medium,
					color = valueColor ?: MaterialTheme.colorScheme.onSurface,
					textDecoration = if (isLink) TextDecoration.Underline else null,
				)
			}
		}
	}
}
