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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stormtroopercs.materialreader.compose.prefs.LocalComposePrefs
import com.stormtroopercs.materialreader.compose.theme.colorToHex
import com.stormtroopercs.materialreader.compose.theme.parseColorHex
import com.stormtroopercs.materialreader.compose.theme.resolveManualAccent
import com.stormtroopercs.materialreader.settings.types.ThemeColorMode

/**
 * The theme-colour settings panel (reference `Theme` page): an
 * `Automatic` / `Manual` segmented control, the manual accent picker, a
 * live light/dark preview grid, and accessibility upvote/downvote colour
 * overrides. All state lives in [com.stormtroopercs.materialreader.compose.prefs.ComposePrefs],
 * so the whole app re-themes live as the user picks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeColorPanel(
	modifier: Modifier = Modifier,
	onBack: () -> Unit,
) {
	val prefs = LocalComposePrefs.current
	var showAccentPicker by remember { mutableStateOf(false) }
	var showUpvotePicker by remember { mutableStateOf(false) }
	var showDownvotePicker by remember { mutableStateOf(false) }

	val accent = resolveManualAccent(prefs.themeColorManual.value)
	val upvoteOverride = prefs.themeUpvoteColor.value?.let(::parseColorHex)
	val downvoteOverride = prefs.themeDownvoteColor.value?.let(::parseColorHex)

	Column(modifier = modifier.fillMaxSize()) {
		TopAppBar(
			title = { Text("Theme", fontWeight = FontWeight.SemiBold) },
			navigationIcon = {
				IconButton(onClick = onBack) {
					Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
				}
			},
		)

		Column(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 16.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp),
		) {
			SectionLabel("Colour source")

			SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
				val mode = prefs.themeColorMode.value
				val segmentShape = RoundedCornerShape(10.dp)
				SegmentedButton(
					selected = mode == ThemeColorMode.AUTOMATIC,
					onClick = { prefs.themeColorMode.value = ThemeColorMode.AUTOMATIC },
					shape = segmentShape,
					label = { Text("Automatic") },
				)
				SegmentedButton(
					selected = mode == ThemeColorMode.MANUAL,
					onClick = { prefs.themeColorMode.value = ThemeColorMode.MANUAL },
					shape = segmentShape,
					label = { Text("Manual") },
				)
			}
			Text(
				text = "Automatic uses the device's wallpaper colours (Material You). Manual uses your own accent.",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)

			if (prefs.themeColorMode.value == ThemeColorMode.MANUAL) {
				SectionLabel("Accent colour")
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.clickable { showAccentPicker = true }
						.padding(vertical = 4.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Box(
						modifier = Modifier
							.size(40.dp)
							.background(accent, CircleShape)
							.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
					)
					Spacer(Modifier.width(12.dp))
					Column(modifier = Modifier.weight(1f)) {
						Text("Highlight colour", style = MaterialTheme.typography.bodyLarge)
						Text(
							text = colorToHex(accent),
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
						)
					}
				}
			}

			SectionLabel("Preview")
			ThemePreviewGrid(accent = accent)

			SectionLabel("Accessibility")
			VoteColorRow(
				label = "Upvote colour",
				overrideColor = upvoteOverride,
				defaultColor = Color(0xFF2E7D32),
				modifier = Modifier.fillMaxWidth(),
				onClick = { showUpvotePicker = true },
			)
			VoteColorRow(
				label = "Downvote colour",
				overrideColor = downvoteOverride,
				defaultColor = Color(0xFFC62828),
				modifier = Modifier.fillMaxWidth(),
				onClick = { showDownvotePicker = true },
			)
			Spacer(Modifier.height(8.dp))
		}
	}

	if (showAccentPicker) {
		SelectColorDialog(
			title = "Select accent colour",
			initial = accent,
			onDismiss = { showAccentPicker = false },
			onSelect = { prefs.themeColorManual.value = colorToHex(it) },
		)
	}
	if (showUpvotePicker) {
		SelectColorDialog(
			title = "Select upvote colour",
			initial = upvoteOverride ?: Color(0xFF2E7D32),
			onDismiss = { showUpvotePicker = false },
			onSelect = { prefs.themeUpvoteColor.value = colorToHex(it) },
		)
	}
	if (showDownvotePicker) {
		SelectColorDialog(
			title = "Select downvote colour",
			initial = downvoteOverride ?: Color(0xFFC62828),
			onDismiss = { showDownvotePicker = false },
			onSelect = { prefs.themeDownvoteColor.value = colorToHex(it) },
		)
	}
}

@Composable
private fun SectionLabel(text: String) {
	Text(
		text = text,
		style = MaterialTheme.typography.titleSmall,
		fontWeight = FontWeight.Bold,
		color = MaterialTheme.colorScheme.primary,
		modifier = Modifier.padding(top = 4.dp),
	)
}

@Composable
private fun VoteColorRow(
	label: String,
	overrideColor: Color?,
	defaultColor: Color,
	modifier: Modifier = Modifier,
	onClick: () -> Unit,
) {
	val effective = overrideColor ?: defaultColor
	Row(
		modifier = modifier
			.clickable(onClick = onClick)
			.padding(vertical = 4.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Box(
			modifier = Modifier
				.size(32.dp)
				.background(effective, CircleShape)
				.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
		)
		Spacer(Modifier.width(12.dp))
		Column(modifier = Modifier.weight(1f)) {
			Text(label, style = MaterialTheme.typography.bodyLarge)
			Text(
				text = if (overrideColor == null) "Default" else colorToHex(effective),
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
		if (overrideColor != null) {
			Text(
				text = "Reset",
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.primary,
			)
		}
	}
}
