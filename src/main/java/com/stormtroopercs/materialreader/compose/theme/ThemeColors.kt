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

package com.stormtroopercs.materialreader.compose.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.stormtroopercs.materialreader.compose.prefs.ComposePrefs
import com.stormtroopercs.materialreader.settings.types.ThemeColorMode
import com.stormtroopercs.materialreader.settings.types.ThemeLightness
import kotlin.math.roundToInt

/**
 * Neutral-gray fallback accents for dynamic colour on devices below API 31
 * (where there is no wallpaper-driven palette to derive from). Chosen to read
 * like a desaturated Material neutral so the UI still looks intentional.
 */
internal val NeutralLightAccent = Color(0xFF777777)
internal val NeutralDarkAccent = Color(0xFFBDBDBD)

private const val DEFAULT_MANUAL_ACCENT = "#6750A4"

/** Parse `#RRGGBB` / `#AARRGGBB` (leading `#` optional) into a [Color], or null if malformed. */
fun parseColorHex(value: String): Color? {
	val s = value.trim().removePrefix("#")
	return try {
		when (s.length) {
			6 -> Color(android.graphics.Color.parseColor("#" + s))
			8 -> Color(android.graphics.Color.parseColor("#" + s))
			else -> null
		}
	} catch (e: IllegalArgumentException) {
		null
	}
}

/** Format a [Color] as `#RRGGBB` (alpha dropped). */
fun colorToHex(color: Color): String {
	val a = (color.alpha * 255).roundToInt()
	val r = (color.red * 255).roundToInt()
	val g = (color.green * 255).roundToInt()
	val b = (color.blue * 255).roundToInt()
	return if (a == 255) {
		"#" + String.format("%02X%02X%02X", r, g, b)
	} else {
		"#" + String.format("%02X%02X%02X%02X", a, r, g, b)
	}
}

/** Resolve the manual accent, falling back to a safe default if the stored hex is malformed. */
fun resolveManualAccent(hex: String?): Color =
	parseColorHex(hex ?: DEFAULT_MANUAL_ACCENT) ?: parseColorHex(DEFAULT_MANUAL_ACCENT)!!

/**
 * Build the [ColorScheme] for the current settings:
 * - **Automatic** mode → Material You dynamic colour on API 31+ (the user's
 *   wallpaper palette), neutral-gray fallback below.
 * - **Manual** mode → the hand-picked accent, mapped onto an otherwise
 *   neutral light/dark scheme.
 *
 * RedReader's own lightness (Light/Dark) is respected; the accent is the only
 * thing the reference's dynamic/manual model overrides.
 */
@Composable
fun resolveThemeColorScheme(
	prefs: ComposePrefs,
	lightness: ThemeLightness,
	context: Context,
): ColorScheme {
	val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
	val isDynamic = prefs.themeColorMode.value == ThemeColorMode.AUTOMATIC && dynamicAvailable
	val isAutomatic = prefs.themeColorMode.value == ThemeColorMode.AUTOMATIC

	val light = lightness == ThemeLightness.Light

	return if (isDynamic) {
		if (light) dynamicLightColorScheme(context) else dynamicDarkColorScheme(context)
	} else {
		val accent = if (isAutomatic) {
			// Automatic mode on a device without dynamic colour: neutral fallback.
			if (light) NeutralLightAccent else NeutralDarkAccent
		} else {
			resolveManualAccent(prefs.themeColorManual.value)
		}
		if (light) {
			lightColorScheme(primary = accent, secondary = accent)
		} else {
			darkColorScheme(primary = accent, secondary = accent)
		}
	}
}

/**
 * The upvote / downvote tint for vote arrows: the user's accessibility
 * override when set, otherwise the theme's positive (upvote) / negative
 * (downvote) colour.
 */
@Composable
fun resolveVoteColors(
	prefs: ComposePrefs,
	upvote: Color,
	downvote: Color,
): Pair<Color, Color> {
	val up = prefs.themeUpvoteColor.value?.let(::parseColorHex) ?: upvote
	val down = prefs.themeDownvoteColor.value?.let(::parseColorHex) ?: downvote
	return up to down
}
