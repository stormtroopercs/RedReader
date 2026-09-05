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
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme as m3LightColorScheme
import androidx.compose.material3.darkColorScheme as m3DarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.stormtroopercs.materialreader.compose.prefs.ComposePrefs
import com.stormtroopercs.materialreader.settings.types.ThemeColorMode
import com.stormtroopercs.materialreader.settings.types.ThemeLightness
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
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
fun resolveManualAccent(hex: String?): Color = parseColorHex(hex ?: DEFAULT_MANUAL_ACCENT) ?: parseColorHex(DEFAULT_MANUAL_ACCENT)!!

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
		// A full accent-driven scheme — NOT `lightColorScheme(primary=accent)`,
		// which only overrides two slots and leaves the *container* colours and
		// surfaceTint at the Material 3 baseline purple. Those are the chip /
		// item backgrounds and the ripple, so the accent must own them too.
		// Text slots stay neutral (near-black / near-white) so no text ever
		// takes on the accent.
		buildAccentScheme(accent, light)
	}
}

/**
 * Build a complete [ColorScheme] from a single accent colour, following the
 * official Material 3 role→tone mapping (see the M3 Color System spec):
 * primary P40/P80, containers P90/P30, onPrimaryContainer P10/P90,
 * surface N99/N6, surfaceVariant N90/N30, onSurface N10/N90,
 * outline N50/N60, …
 *
 * Every slot is derived in OKLCH so the whole scheme shares the accent's hue:
 * the accent's chroma drives primary / secondary / tertiary (and their
 * containers, fixed variants and `surfaceTint`, which is the ripple / press
 * tint), while the *surface* family and *on-surface* text tones are neutral
 * grays tinted with a small fraction of the accent's chroma. That tint is what
 * makes card / item backgrounds visibly follow the chosen colour, and it is
 * why `lightColorScheme(primary = accent)` alone was wrong: it left `surface`,
 * `surfaceVariant`, `surfaceContainer*` and `outline` at the Material 3
 * baseline (a purple-tinted neutral), so every background stayed purple no
 * matter the accent.
 *
 * Text stays near-black on light and near-white on dark regardless of accent:
 * `onSurface` / `onSurfaceVariant` are zero-chroma and `onPrimary` /
 * `onPrimaryContainer` are chosen for contrast against the accent tone.
 */
private fun buildAccentScheme(accent: Color, light: Boolean): ColorScheme {
	val ok = srgbToOklab(accent)
	val accentHue = okLChHue(ok)
	// Accent chroma, kept in a pleasant band (avoid oversaturated / near-gray).
	val chroma = okLChChroma(ok).coerceIn(0.02f, 0.14f)
	// Surface tint: a small share of the accent chroma at the same hue, so
	// backgrounds read as the chosen colour at a glance without being loud.
	val neutralChroma = (chroma * 0.35f).coerceIn(0.006f, 0.03f)

	fun tone(t: Int, c: Float, h: Float = accentHue) = okLChToColor(t / 100f, c, h)
	val errLight = tone(40, 0.11f, 27f)
	val errLightContainer = tone(90, 0.05f, 27f)
	val errDark = tone(80, 0.11f, 27f)
	val errDarkContainer = tone(30, 0.07f, 27f)

	return if (light) {
		m3LightColorScheme(
			primary = tone(40, chroma),
			onPrimary = Color.White,
			primaryContainer = tone(90, chroma * 0.5f),
			onPrimaryContainer = tone(10, chroma * 0.5f),
			inversePrimary = tone(80, chroma * 0.8f),
			secondary = tone(40, chroma * 0.8f),
			onSecondary = Color.White,
			secondaryContainer = tone(90, chroma * 0.4f),
			onSecondaryContainer = tone(10, chroma * 0.4f),
			tertiary = tone(40, chroma * 0.7f),
			onTertiary = Color.White,
			tertiaryContainer = tone(90, chroma * 0.35f),
			onTertiaryContainer = tone(10, chroma * 0.35f),
			background = tone(99, neutralChroma),
			onBackground = tone(10, 0f),
			surface = tone(99, neutralChroma),
			onSurface = tone(10, 0f),
			surfaceVariant = tone(90, neutralChroma),
			onSurfaceVariant = tone(30, neutralChroma * 1.2f),
			surfaceTint = tone(40, chroma),
			inverseSurface = tone(20, neutralChroma),
			inverseOnSurface = tone(95, neutralChroma * 0.5f),
			error = errLight,
			onError = Color.White,
			errorContainer = errLightContainer,
			onErrorContainer = tone(10, 0.05f, 27f),
			outline = tone(50, neutralChroma * 1.5f),
			outlineVariant = tone(80, neutralChroma * 1.2f),
			scrim = Color.Black,
			surfaceBright = tone(98, neutralChroma),
			surfaceContainer = tone(94, neutralChroma),
			surfaceContainerHigh = tone(92, neutralChroma),
			surfaceContainerHighest = tone(90, neutralChroma),
			surfaceContainerLow = tone(96, neutralChroma),
			surfaceContainerLowest = tone(100, neutralChroma * 0.6f),
			surfaceDim = tone(87, neutralChroma),
			primaryFixed = tone(90, chroma * 0.5f),
			primaryFixedDim = tone(80, chroma * 0.6f),
			onPrimaryFixed = tone(10, chroma * 0.5f),
			onPrimaryFixedVariant = tone(30, chroma * 0.7f),
			secondaryFixed = tone(90, chroma * 0.4f),
			secondaryFixedDim = tone(80, chroma * 0.5f),
			onSecondaryFixed = tone(10, chroma * 0.4f),
			onSecondaryFixedVariant = tone(30, chroma * 0.6f),
			tertiaryFixed = tone(90, chroma * 0.35f),
			tertiaryFixedDim = tone(80, chroma * 0.45f),
			onTertiaryFixed = tone(10, chroma * 0.35f),
			onTertiaryFixedVariant = tone(30, chroma * 0.55f),
		)
	} else {
		m3DarkColorScheme(
			primary = tone(80, chroma),
			onPrimary = tone(20, chroma * 0.8f),
			primaryContainer = tone(30, chroma * 0.8f),
			onPrimaryContainer = tone(90, chroma * 0.4f),
			inversePrimary = tone(40, chroma * 0.9f),
			secondary = tone(80, chroma * 0.8f),
			onSecondary = tone(20, chroma * 0.5f),
			secondaryContainer = tone(30, chroma * 0.6f),
			onSecondaryContainer = tone(90, chroma * 0.3f),
			tertiary = tone(80, chroma * 0.7f),
			onTertiary = tone(20, chroma * 0.4f),
			tertiaryContainer = tone(30, chroma * 0.5f),
			onTertiaryContainer = tone(90, chroma * 0.25f),
			background = tone(6, neutralChroma),
			onBackground = tone(90, neutralChroma * 0.5f),
			surface = tone(6, neutralChroma),
			onSurface = tone(90, neutralChroma * 0.5f),
			surfaceVariant = tone(30, neutralChroma),
			onSurfaceVariant = tone(80, neutralChroma * 1.2f),
			surfaceTint = tone(80, chroma),
			inverseSurface = tone(90, neutralChroma),
			inverseOnSurface = tone(20, neutralChroma * 0.5f),
			error = errDark,
			onError = tone(10, 0.05f, 27f),
			errorContainer = errDarkContainer,
			onErrorContainer = tone(90, 0.04f, 27f),
			outline = tone(60, neutralChroma * 1.5f),
			outlineVariant = tone(30, neutralChroma * 1.2f),
			scrim = Color.Black,
			surfaceBright = tone(12, neutralChroma),
			surfaceContainer = tone(12, neutralChroma),
			surfaceContainerHigh = tone(17, neutralChroma),
			surfaceContainerHighest = tone(22, neutralChroma),
			surfaceContainerLow = tone(8, neutralChroma),
			surfaceContainerLowest = tone(0, neutralChroma * 0.4f),
			surfaceDim = tone(4, neutralChroma * 0.6f),
			primaryFixed = tone(80, chroma * 0.8f),
			primaryFixedDim = tone(70, chroma * 0.9f),
			onPrimaryFixed = tone(20, chroma * 0.6f),
			onPrimaryFixedVariant = tone(80, chroma * 0.5f),
			secondaryFixed = tone(80, chroma * 0.6f),
			secondaryFixedDim = tone(70, chroma * 0.7f),
			onSecondaryFixed = tone(20, chroma * 0.4f),
			onSecondaryFixedVariant = tone(80, chroma * 0.4f),
			tertiaryFixed = tone(80, chroma * 0.5f),
			tertiaryFixedDim = tone(70, chroma * 0.6f),
			onTertiaryFixed = tone(20, chroma * 0.3f),
			onTertiaryFixedVariant = tone(80, chroma * 0.35f),
		)
	}
}

// --- OKLCH <-> sRGB helpers (self-contained; the material3 palette API in this
// build is `internal`, so tones are derived here). ---

private fun srgbToOklab(c: Color): Triple<Float, Float, Float> {
	val r = c.red; val g = c.green; val b = c.blue
	val lr = if (r <= 0.04045f) r / 12.92f else ((r + 0.055f) / 1.055f).pow(2.4f)
	val lg = if (g <= 0.04045f) g / 12.92f else ((g + 0.055f) / 1.055f).pow(2.4f)
	val lb = if (b <= 0.04045f) b / 12.92f else ((b + 0.055f) / 1.055f).pow(2.4f)
	val l = 0.4122214708f * lr + 0.5363325363f * lg + 0.0514459929f * lb
	val m = 0.2119034982f * lr + 0.6806995451f * lg + 0.1073969566f * lb
	val s = 0.0883024619f * lr + 0.2817188376f * lg + 0.6299787005f * lb
	val l_ = l.pow(1f / 3f); val m_ = m.pow(1f / 3f); val s_ = s.pow(1f / 3f)
	return Triple(
		0.2104542553f * l_ + 0.7936177850f * m_ - 0.0040720468f * s_,
		1.9779984951f * l_ - 2.4285922050f * m_ + 0.4505937099f * s_,
		0.0259040371f * l_ + 0.7827717662f * m_ - 0.8086757660f * s_,
	)
}

private fun okLChToColor(L: Float, C: Float, H: Float): Color {
	val h = Math.toRadians(H.toDouble()).toFloat()
	val a = (C * cos(h.toDouble())).toFloat()
	val b = (C * sin(h.toDouble())).toFloat()
	return oklabToColor(L, a, b)
}

private fun oklabToColor(L: Float, a: Float, b: Float): Color {
	val l_ = L + 0.3963377774f * a + 0.2158037573f * b
	val m_ = L - 0.1055613458f * a - 0.0638541728f * b
	val s_ = L - 0.0894841775f * a - 1.2914855480f * b
	val l = l_.pow(3); val m = m_.pow(3); val s = s_.pow(3)
	val lr = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s
	val lg = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s
	val lb = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s
	fun enc(x: Float): Int {
		val c = if (x <= 0.0031308f) 12.92f * x else 1.055f * x.pow(1f / 2.4f) - 0.055f
		return (c.coerceIn(0f, 1f) * 255).roundToInt()
	}
	return Color(enc(lr), enc(lg), enc(lb))
}

private fun okLChHue(ok: Triple<Float, Float, Float>): Float {
	val (_, a, b) = ok
	val deg = atan2(b.toDouble(), a.toDouble()) * 180.0 / Math.PI
	return (deg + 360).toFloat() % 360f
}

private fun okLChChroma(ok: Triple<Float, Float, Float>): Float {
	val (_, a, b) = ok
	return sqrt((a * a + b * b).toDouble()).toFloat()
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
