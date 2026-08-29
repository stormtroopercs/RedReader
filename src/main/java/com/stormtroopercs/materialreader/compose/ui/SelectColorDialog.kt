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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import com.stormtroopercs.materialreader.compose.theme.colorToHex
import com.stormtroopercs.materialreader.compose.theme.parseColorHex
import kotlin.math.roundToInt

/**
 * The reference app's `Select color` dialog: a large colour preview, an
 * editable `#RRGGBB` field, and channel-coloured R/G/B sliders, with
 * `Dismiss` / `Select` actions.
 */
@Composable
fun SelectColorDialog(
	title: String,
	initial: Color,
	onDismiss: () -> Unit,
	onSelect: (Color) -> Unit,
) {
	var r by remember { mutableStateOf((initial.red * 255).roundToInt()) }
	var g by remember { mutableStateOf((initial.green * 255).roundToInt()) }
	var b by remember { mutableStateOf((initial.blue * 255).roundToInt()) }
	var hexText by remember { mutableStateOf(colorToHex(initial)) }

	val current = Color(r / 255f, g / 255f, b / 255f)

	fun applyHex(text: String) {
		hexText = text
		parseColorHex(text)?.let {
			r = (it.red * 255).roundToInt()
			g = (it.green * 255).roundToInt()
			b = (it.blue * 255).roundToInt()
		}
	}

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(title, fontWeight = FontWeight.SemiBold) },
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.height(96.dp)
						.background(current, RoundedCornerShape(12.dp))
				)
				OutlinedTextField(
					value = hexText,
					onValueChange = ::applyHex,
					label = { Text("#RRGGBB") },
					modifier = Modifier.fillMaxWidth(),
					singleLine = true,
				)
				ColorChannelSlider("R", r) { v -> r = v }
				ColorChannelSlider("G", g) { v -> g = v }
				ColorChannelSlider("B", b) { v -> b = v }
			}
		},
		confirmButton = {
			Button(onClick = {
				onSelect(current)
				onDismiss()
			}) {
				Text("Select")
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text("Dismiss") }
		},
	)
}

@Composable
private fun ColorChannelSlider(label: String, value: Int, onChange: (Int) -> Unit) {
	val channelColor = when (label) {
		"R" -> Color(0xFFEF5350)
		"G" -> Color(0xFF66BB6A)
		"B" -> Color(0xFF42A5F5)
		else -> MaterialTheme.colorScheme.primary
	}
	Row(
		modifier = Modifier.fillMaxWidth(),
		verticalAlignment = Alignment.CenterVertically
	) {
		Text(
			text = label,
			color = channelColor,
			fontWeight = FontWeight.SemiBold,
			modifier = Modifier.width(20.dp)
		)
		Slider(
			value = value.toFloat(),
			onValueChange = { onChange(it.toInt()) },
			valueRange = 0f..255f,
			modifier = Modifier.weight(1f),
		)
		Text(
			text = value.toString(),
			modifier = Modifier.width(36.dp),
			style = MaterialTheme.typography.bodySmall
		)
	}
}

/**
 * A live 2-column preview grid of sample post cards (light column + dark
 * column). The cards are real app components that re-render as the accent /
 * lightness change — used on the theme-colour settings panel.
 */
@Composable
fun ThemePreviewGrid(accent: Color) {
	Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
		PreviewCard(isLight = true, accent = accent)
		PreviewCard(isLight = false, accent = accent)
	}
}

@Composable
private fun PreviewCard(isLight: Boolean, accent: Color) {
	val background = if (isLight) Color(0xFFFFFFFF) else Color(0xFF1C1B1F)
	val surface = if (isLight) Color(0xFFF4F4F8) else Color(0xFF2B2930)
	val textPrimary = if (isLight) Color(0xFF1C1B1F) else Color(0xFFE6E1E5)
	val textSecondary = if (isLight) Color(0xFF49454F) else Color(0xFFCAC4D0)

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.background(surface, RoundedCornerShape(12.dp))
			.padding(12.dp),
	) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Box(
				modifier = Modifier
					.size(32.dp)
					.background(accent, RoundedCornerShape(16.dp))
			)
			Spacer(Modifier.width(8.dp))
			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = if (isLight) "Light mode" else "Dark mode",
					color = textPrimary,
					fontWeight = FontWeight.SemiBold,
					fontSize = 14.sp
				)
				Text(
					text = "A community name • 2.3k subs",
					color = textSecondary,
					fontSize = 12.sp
				)
			}
		}
		Spacer(Modifier.height(10.dp))
		Text(
			text = "Sample post title that wraps to a second line so the card has some height.",
			color = textPrimary,
			fontSize = 14.sp,
		)
		Spacer(Modifier.height(8.dp))
		Row {
			Box(modifier = Modifier.weight(1f).height(60.dp).background(background, RoundedCornerShape(8.dp)))
			Spacer(Modifier.width(8.dp))
			Box(modifier = Modifier.weight(1f).height(60.dp).background(accent.copy(alpha = 0.25f), RoundedCornerShape(8.dp)))
		}
		Spacer(Modifier.height(8.dp))
		Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
			PreviewChip(isLight, accent, "▲ 123")
			PreviewChip(isLight, accent, "💬 21")
		}
	}
}

@Composable
private fun PreviewChip(isLight: Boolean, accent: Color, label: String) {
	val bg = if (isLight) Color(0xFFECE6F0) else Color(0xFF3A3740)
	val fg = if (isLight) Color(0xFF21005D) else Color(0xFFEADDFF)
	Row(
		modifier = Modifier
			.background(
				if (label.startsWith("▲")) accent.copy(alpha = 0.18f) else bg,
				RoundedCornerShape(999.dp)
			)
			.padding(horizontal = 10.dp, vertical = 6.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Text(
			text = label,
			color = if (label.startsWith("▲")) (if (isLight) accent else accent) else fg,
			fontSize = 12.sp,
			fontWeight = FontWeight.Medium,
		)
	}
}
