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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One entry in an [ActionBarRow]: an icon (with an optional count label under
 * it) that fills an equal weight of the row. Tappable with ripple.
 */
data class ActionBarButton(
	val icon: ImageVector,
	val contentDescription: String,
	val countLabel: String? = null,
	val enabled: Boolean = true,
	val onClick: () -> Unit = {},
)

/**
 * The reference's equal-weight icon action bar (DESIGN §6): a `Row` of
 * icon buttons, each taking an equal `weight(1f)`, 56dp tall, ripple,
 * `colorSecondary` tint. A button may show a count label under its icon (the
 * score inside the upvote button, the comment count inside the comment
 * button). Reused by the slides feed page and the comment-detail screen.
 */
@Composable
fun ActionBarRow(
	buttons: List<ActionBarButton>,
	modifier: Modifier = Modifier,
) {
	Row(
		modifier = modifier
			.fillMaxWidth()
			.height(56.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		buttons.forEach { button ->
			Box(
				modifier = Modifier.weight(1f).height(56.dp),
				contentAlignment = Alignment.Center,
			) {
				IconButton(
					onClick = button.onClick,
					enabled = button.enabled,
					modifier = Modifier.height(56.dp),
				) {
					Column(
						verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
						horizontalAlignment = Alignment.CenterHorizontally,
					) {
						Icon(
							imageVector = button.icon,
							contentDescription = button.contentDescription,
							tint = MaterialTheme.colorScheme.secondary,
						)
						button.countLabel?.let { count ->
							Text(
								text = count,
								color = MaterialTheme.colorScheme.secondary,
								fontSize = 10.sp,
							)
						}
					}
				}
			}
		}
	}
}
