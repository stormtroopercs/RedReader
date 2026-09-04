/*******************************************************************************
 * This file is part of MaterialReader.
 *
 * MaterialReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MaterialReader is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MaterialReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.stormtroopercs.materialreader.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.stormtroopercs.materialreader.R

/**
 * The default community avatar — Reddit's Snoo — shown for communities that
 * send no icon (`icon_img` / `community_icon` / `banner` all empty, e.g.
 * r/Home) or whose icon URL fails to load. Matches how the Reddit web/app
 * clients render icon-less communities: a neutral circle with the Snoo glyph,
 * instead of a letter.
 *
 * The caller sizes + circular-clips the box (32dp on directory rows, 56dp in
 * the community header); this composable fills that box with a neutral
 * `surfaceVariant` circle and a theme-adaptive Snoo tinted to
 * `onSurfaceVariant`.
 */
@Composable
fun CommunityDefaultAvatar() {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.surfaceVariant),
		contentAlignment = Alignment.Center,
	) {
		Icon(
			painter = painterResource(id = R.drawable.ic_community_default_snoo),
			contentDescription = null,
			tint = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.fillMaxSize(0.55f),
		)
	}
}
