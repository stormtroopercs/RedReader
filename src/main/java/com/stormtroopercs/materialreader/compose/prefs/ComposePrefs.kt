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

package com.stormtroopercs.materialreader.compose.prefs

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.stormtroopercs.materialreader.R
import com.stormtroopercs.materialreader.common.General
import com.stormtroopercs.materialreader.settings.types.AlbumViewMode
import com.stormtroopercs.materialreader.settings.types.AppearanceTheme
import com.stormtroopercs.materialreader.settings.types.NavigationType
import com.stormtroopercs.materialreader.settings.types.SettingSerializer
import com.stormtroopercs.materialreader.settings.types.ThemeColorMode

@Stable
interface Preference<T> {
	var value: T
}

interface ComposePrefs {
	val appearanceTheme: Preference<AppearanceTheme>

	/** Accent colours: dynamic (Material You) vs hand-picked (manual). */
	val themeColorMode: Preference<ThemeColorMode>

	/** Manual accent (primary) colour as `#RRGGBB` hex, used in manual mode. */
	val themeColorManual: Preference<String>

	/** Accessibility overrides: custom upvote / downvote colours (`#RRGGBB`), or null = theme default. */
	val themeUpvoteColor: Preference<String?>
	val themeDownvoteColor: Preference<String?>

	/** Primary navigation style: bottom bar, drawer, or both. */
	val navigationType: Preference<NavigationType>

	val appearanceFontScaleGlobal: Float
	val appearanceFontScaleBodyText: Float
	val appearanceFontScalePosts: Float
	val appearanceFontScalePostSubtitles: Float

	val albumViewMode: Preference<AlbumViewMode>
	val albumCardShowButtons: Preference<Boolean>
	val albumListShowThumbnails: Preference<Boolean>
	val albumListThumbnailSize: Preference<Int>
	val albumListShowButtons: Preference<Boolean>
	val albumGridCropToSquare: Preference<Boolean>
	val albumGridColumns: Preference<Int>
	val albumGridRoundedCorners: Preference<Boolean>
	val albumGridHorizontalPadding: Preference<Boolean>
	val albumCompactTitle: Preference<Boolean>
}

object ComposePrefsSingleton {
	@Suppress("PropertyName")
	private lateinit var SINGLETON: ComposePrefs

	fun init(context: Context) {
		General.checkThisIsUIThread()
		SINGLETON = ComposePrefsImpl(context)
	}

	val instance: ComposePrefs
		get() = SINGLETON
}

@Suppress("PrivatePropertyName")
private class ComposePrefsImpl(private val context: Context) : ComposePrefs {

	private val sharedPrefs = General.getSharedPrefs(context)

	private val changeObservers = HashMap<String, () -> Unit>()

	private val appearance_fontscale_global = FloatPref(R.string.pref_appearance_fontscale_global_key, 1f)
	private val appearance_fontscale_bodytext = FloatPref(R.string.pref_appearance_fontscale_bodytext_key, -1f)
	private val appearance_fontscale_posts = FloatPref(R.string.pref_appearance_fontscale_posts_key, -1f)
	private val appearance_fontscale_post_subtitles = FloatPref(R.string.pref_appearance_fontscale_post_subtitles_key, -1f)

	init {
		General.checkThisIsUIThread()
		sharedPrefs.registerOnSharedPreferenceChangeListener { prefs, key ->
			changeObservers[key]?.invoke()
		}
	}

	private inner class FloatPref(
		private val key: String,
		private val default: Float,
	) : Preference<Float> {

		constructor(@StringRes key: Int, default: Float) : this(context.getString(key), default)

		private val mutableState = mutableFloatStateOf(loadPref())

		init {
			changeObservers[key] = {
				mutableState.floatValue = loadPref()
			}
		}

		private fun loadPref() = sharedPrefs.getString(key, default.toString())?.toFloatOrNull() ?: default

		override var value: Float
			get() = mutableState.floatValue
			set(value) {
				sharedPrefs.edit().putFloat(key, value).apply()
				mutableState.floatValue = value
			}
	}

	private inner class IntPref(
		private val key: String,
		private val default: Int,
	) : Preference<Int> {

		constructor(@StringRes key: Int, default: Int) : this(context.getString(key), default)

		private val mutableState = mutableIntStateOf(loadPref())

		init {
			changeObservers[key] = {
				mutableState.intValue = loadPref()
			}
		}

		private fun loadPref() = sharedPrefs.getInt(key, default)

		override var value: Int
			get() = mutableState.intValue
			set(value) {
				sharedPrefs.edit().putInt(key, value).apply()
				mutableState.intValue = value
			}
	}

	private inner class BoolPref(
		private val key: String,
		private val default: Boolean,
	) : Preference<Boolean> {

		constructor(@StringRes key: Int, default: Boolean) : this(context.getString(key), default)

		private val mutableState = mutableStateOf(loadPref())

		init {
			changeObservers[key] = {
				mutableState.value = loadPref()
			}
		}

		private fun loadPref() = sharedPrefs.getBoolean(key, default)

		override var value: Boolean
			get() = mutableState.value
			set(value) {
				sharedPrefs.edit().putBoolean(key, value).apply()
				mutableState.value = value
			}
	}

	/**
	 * A nullable string pref. `null` is stored as the empty string so the key
	 * round-trips a "not set" value (used for the optional upvote/downvote
	 * colour overrides).
	 */
	private inner class NullableStringPref(
		private val key: String,
		private val default: String?,
	) : Preference<String?> {

		private val mutableState = mutableStateOf(loadPref())

		init {
			changeObservers[key] = {
				mutableState.value = loadPref()
			}
		}

		private fun loadPref(): String? {
			val stored = sharedPrefs.getString(key, null)
			return when (stored) {
				null -> default
				"" -> null
				else -> stored
			}
		}

		override var value: String?
			get() = mutableState.value
			set(value) {
				sharedPrefs.edit().putString(key, value ?: "").apply()
				mutableState.value = value
			}
	}

	/** A non-null string pref (used for the manual accent colour hex). */
	private inner class StringPref(
		private val key: String,
		private val default: String,
	) : Preference<String> {

		private val mutableState = mutableStateOf(loadPref())

		init {
			changeObservers[key] = {
				mutableState.value = loadPref()
			}
		}

		private fun loadPref() = sharedPrefs.getString(key, default) ?: default

		override var value: String
			get() = mutableState.value
			set(value) {
				sharedPrefs.edit().putString(key, value).apply()
				mutableState.value = value
			}
	}

	private inner class EnumPref<T>(
		private val key: String,
		private val default: T,
		private val serializer: SettingSerializer<T>,
	) : Preference<T> {

		constructor(
			@StringRes key: Int,
			default: T,
			serializer: SettingSerializer<T>,
		) : this(context.getString(key), default, serializer)

		private val mutableState = mutableStateOf(loadPref())

		init {
			changeObservers[key] = {
				mutableState.value = loadPref()
			}
		}

		private fun loadPref() = sharedPrefs.getString(key, serializer.serialize(default))?.let(serializer::deserialize)
			?: default

		override var value: T
			get() = mutableState.value
			set(value) {
				sharedPrefs.edit().putString(key, serializer.serialize(value)).apply()
				mutableState.value = value
			}
	}

	private fun fontScale(pref: FloatPref) = pref.value.takeUnless { it == -1f }
		?: appearance_fontscale_global.value

	override val appearanceFontScaleGlobal: Float
		get() = appearance_fontscale_global.value

	override val appearanceFontScaleBodyText: Float
		get() = fontScale(appearance_fontscale_bodytext)

	override val appearanceFontScalePosts: Float
		get() = fontScale(appearance_fontscale_posts)

	override val appearanceFontScalePostSubtitles: Float
		get() = fontScale(appearance_fontscale_post_subtitles)

	override val appearanceTheme: Preference<AppearanceTheme> = EnumPref(
		R.string.pref_appearance_theme_key,
		AppearanceTheme.RED,
		AppearanceTheme.settingSerializer,
	)

	override val themeColorMode: Preference<ThemeColorMode> = EnumPref(
		"theme_color_mode",
		ThemeColorMode.AUTOMATIC,
		ThemeColorMode.settingSerializer,
	)

	override val navigationType: Preference<NavigationType> = EnumPref(
		"navigation_type",
		NavigationType.BOTTOM,
		NavigationType.settingSerializer,
	)

	override val themeColorManual: Preference<String> = StringPref(
		"theme_color_manual",
		"#6750A4",
	)

	override val themeUpvoteColor: Preference<String?> = NullableStringPref(
		"theme_upvote_color",
		null,
	)

	override val themeDownvoteColor: Preference<String?> = NullableStringPref(
		"theme_downvote_color",
		null,
	)

	override val albumViewMode: Preference<AlbumViewMode> = EnumPref(
		"album_view_mode",
		AlbumViewMode.Cards,
		AlbumViewMode.settingSerializer,
	)

	override val albumCardShowButtons: Preference<Boolean> = BoolPref(
		"album_card_show_buttons",
		true,
	)

	override val albumListShowThumbnails: Preference<Boolean> = BoolPref(
		"album_list_show_thumbnails",
		true,
	)

	override val albumListThumbnailSize: Preference<Int> = IntPref(
		"album_list_thumbnail_size",
		64,
	)

	override val albumListShowButtons: Preference<Boolean> = BoolPref(
		"album_list_show_buttons",
		true,
	)

	override val albumGridCropToSquare: Preference<Boolean> = BoolPref(
		"album_grid_crop_to_square",
		true,
	)

	override val albumGridColumns: Preference<Int> = IntPref(
		"album_grid_columns",
		3,
	)

	override val albumGridRoundedCorners: Preference<Boolean> = BoolPref(
		"album_grid_rounded_corners",
		true,
	)

	override val albumGridHorizontalPadding: Preference<Boolean> = BoolPref(
		"album_grid_horizontal_padding",
		true,
	)

	override val albumCompactTitle: Preference<Boolean> = BoolPref(
		"album_compact_title",
		false,
	)
}

val LocalComposePrefs = staticCompositionLocalOf { ComposePrefsSingleton.instance }
