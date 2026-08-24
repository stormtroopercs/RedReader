/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.quantumbadger.redreader.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.quantumbadger.redreader.activities.HtmlViewActivity
import org.quantumbadger.redreader.BuildConfig
import org.quantumbadger.redreader.common.NeverAlwaysOrWifiOnly
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.PrefsUtility.AlbumViewMode
import org.quantumbadger.redreader.common.PrefsUtility.AppearanceNavbarColour
import org.quantumbadger.redreader.common.PrefsUtility.AppearanceStatusBarMode
import org.quantumbadger.redreader.common.PrefsUtility.AppearanceTwopane
import org.quantumbadger.redreader.common.PrefsUtility.BehaviourCollapseStickyComments
import org.quantumbadger.redreader.common.PrefsUtility.BlockedSubredditSort
import org.quantumbadger.redreader.common.PrefsUtility.CommentAction
import org.quantumbadger.redreader.common.PrefsUtility.CommentFlingAction
import org.quantumbadger.redreader.common.PrefsUtility.CommentAgeMode
import org.quantumbadger.redreader.common.PrefsUtility.PostCount
import org.quantumbadger.redreader.common.PrefsUtility.PostFlingAction
import org.quantumbadger.redreader.common.PrefsUtility.PostTapAction
import org.quantumbadger.redreader.common.PrefsUtility.PinnedSubredditSort
import org.quantumbadger.redreader.common.PrefsUtility.SaveLocation
import org.quantumbadger.redreader.common.PrefsUtility.ScreenOrientation
import org.quantumbadger.redreader.common.PrefsUtility.SelfpostAction
import org.quantumbadger.redreader.common.PrefsUtility.SharingDomain
import org.quantumbadger.redreader.reddit.PostCommentSort
import org.quantumbadger.redreader.reddit.PostSort
import org.quantumbadger.redreader.reddit.UserCommentSort
import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.settings.types.AppearanceTheme

/**
 * Settings screen composable — real data from PrefsUtility.
 * Shows categorized settings grouped by Appearance, Behaviour, Network, Cache, and About.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChangelog: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* no-op */ }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        var showAppbar by remember { mutableStateOf(false) }
        if (showAppbar) {
            AppbarScreen(
                modifier = Modifier.padding(paddingValues),
                onBack = { showAppbar = false }
            )
        } else {
            SettingsContent(
                modifier = Modifier.padding(paddingValues),
                onNavigateToChangelog = onNavigateToChangelog,
                onOpenAppbar = { showAppbar = true }
            )
        }
    }
}

/**
 * Settings content with grouped preferences.
 */
@Composable
private fun SettingsContent(
    modifier: Modifier = Modifier,
    onNavigateToChangelog: () -> Unit,
    onOpenAppbar: () -> Unit = {}
) {
    val context = LocalContext.current
    val settings = getSettingsCategories(context, onNavigateToChangelog, onOpenAppbar)

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        settings.forEach { category ->
            item(key = "header_${category.id}") {
                SettingsCategoryHeader(title = category.title)
            }

            items(category.items) { item ->
                when (item) {
                    is SettingsItem.BooleanSetting -> BooleanSettingItem(item)
                    is SettingsItem.EnumSetting<*> -> EnumSettingItem(item)
                    is SettingsItem.PreferenceItem -> PreferenceItem(item)
                    is SettingsItem.StringSetting -> StringSettingItem(item)
                    is SettingsItem.ChoiceSetting -> ChoiceSettingItem(item)
                    is SettingsItem.MultiSelectSetting -> MultiSelectSettingItem(item)
                }
            }
        }
    }
}

/**
 * Category header.
 */
@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/**
 * Boolean setting (checkbox toggle).
 */
@Composable
private fun BooleanSettingItem(item: SettingsItem.BooleanSetting) {
    var enabled by remember(item.key) {
        mutableStateOf(item.get())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                enabled = !enabled
                item.set(enabled)
            })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            item.description.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Checkbox(checked = enabled, onCheckedChange = {
            enabled = it
            item.set(it)
        })
    }
}

/**
 * Enum setting with dropdown selection.
 */
@Composable
private fun EnumSettingItem(item: SettingsItem.EnumSetting<*>) {
    var expanded by remember { mutableStateOf(false) }
    var selected by remember(item.key) {
        mutableStateOf(item.get())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { expanded = true })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            item.description.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = selected.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            item.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        selected = option
                        item.applyOption(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Choice setting (free-form labels/values, mirrors the legacy ListPreference).
 */
@Composable
private fun ChoiceSettingItem(item: SettingsItem.ChoiceSetting) {
    var expanded by remember { mutableStateOf(false) }
    val selected = item.selected()
    val selectedLabel = item.options.firstOrNull { it.second == selected }?.first
        ?: selected

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { expanded = true })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            item.description.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = selectedLabel ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            item.options.forEach { (label, value) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        item.set(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Multi-select setting (checkbox dialog, mirrors the legacy
 * MultiSelectListPreference). The row shows the number of selected options;
 * the dialog lets the user toggle each one, writing the set on each change.
 */
@Composable
private fun MultiSelectSettingItem(item: SettingsItem.MultiSelectSetting) {
    var showDialog by remember { mutableStateOf(false) }
    var selected by remember(item.key) {
        mutableStateOf(item.get())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { showDialog = true })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            item.description.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = if (selected.isEmpty()) "None" else "${selected.size} selected",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(item.label) },
            text = {
                Column {
                    item.options.forEach { (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val next = selected.toMutableSet()
                                    if (!next.add(value)) next.remove(value)
                                    selected = next
                                    item.set(next)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = value in selected,
                                onCheckedChange = {
                                    val next = selected.toMutableSet()
                                    if (!next.add(value)) next.remove(value)
                                    selected = next
                                    item.set(next)
                                }
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("Done") }
            }
        )
    }
}

/**
 * Simple preference item that navigates or opens a dialog.
 */
@Composable
private fun PreferenceItem(item: SettingsItem.PreferenceItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            item.description.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * String setting (text input), written to PrefsUtility on commit.
 */
@Composable
private fun StringSettingItem(item: SettingsItem.StringSetting) {
    var text by remember(item.key) {
        mutableStateOf(item.get() ?: "")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        item.description.takeIf { it.isNotBlank() }?.let { desc ->
            Spacer(Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(6.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(item.placeholder) },
            modifier = Modifier.fillMaxWidth()
        )

        TextButton(
            onClick = { item.set(text.ifBlank { null }) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Save")
        }
    }
}

// ============================================================================
// Data models
// ============================================================================

data class SettingsCategory(
    val id: String,
    val title: String,
    val items: List<SettingsItem>
)

sealed class SettingsItem {
    abstract val key: String

    data class BooleanSetting(
        override val key: String,
        val label: String,
        val description: String = "",
        val get: () -> Boolean,
        val set: (Boolean) -> Unit
    ) : SettingsItem()

    data class EnumSetting<T : Enum<T>>(
        override val key: String,
        val label: String,
        val description: String = "",
        val entries: List<T>,
        val get: () -> T,
        private val set: (T) -> Unit
    ) : SettingsItem() {
        // Takes Any? (not T) so it stays callable on a star-projected
        // EnumSetting<*> receiver; the option always comes from `entries`.
        @Suppress("UNCHECKED_CAST")
        fun applyOption(option: Any?) {
            set(option as T)
        }
    }

    data class PreferenceItem(
        override val key: String,
        val label: String,
        val description: String = "",
        val onClick: () -> Unit
    ) : SettingsItem()

    data class StringSetting(
        override val key: String,
        val label: String,
        val description: String = "",
        val placeholder: String = "",
        val get: () -> String?,
        val set: (String?) -> Unit
    ) : SettingsItem()

    /**
     * A choice with free-form labels and values (mirrors the legacy ListPreference:
     * entry labels shown to the user, entry values stored in prefs).
     */
    data class ChoiceSetting(
        override val key: String,
        val label: String,
        val description: String = "",
        val options: List<Pair<String, String>>,
        val get: () -> String?,
        val set: (String) -> Unit
    ) : SettingsItem() {
        fun selected(): String? {
            val value = get()
            return value ?: options.firstOrNull()?.second
        }
    }

    /**
     * A multi-select group of labelled options (mirrors the legacy
     * MultiSelectListPreference): each option's stored value is an enum name,
     * the set of selected values is persisted as a string set.
     */
    data class MultiSelectSetting(
        override val key: String,
        val label: String,
        val description: String = "",
        val options: List<Pair<String, String>>,
        val get: () -> Set<String>,
        val set: (Set<String>) -> Unit
    ) : SettingsItem()
}

// ============================================================================
// Build settings categories (wired to real PrefsUtility getters/setters)
// ============================================================================

private fun getSettingsCategories(
    context: Context,
    onNavigateToChangelog: () -> Unit,
    onOpenAppbar: () -> Unit = {}
): List<SettingsCategory> {
    return listOf(
        // ─── Appearance (general) ───
        SettingsCategory(
            id = "appearance",
            title = "Appearance",
            items = listOf(
                SettingsItem.BooleanSetting(
                    key = "left_handed",
                    label = "Left-handed mode",
                    get = { PrefsUtility.pref_appearance_left_handed() },
                    set = PrefsUtility::pref_appearance_left_handed_set
                ),
                SettingsItem.EnumSetting(
                    key = "twopane",
                    label = "Tablet mode (two pane)",
                    entries = AppearanceTwopane.entries,
                    get = { PrefsUtility.appearance_twopane() },
                    set = PrefsUtility::pref_appearance_twopane_set
                ),
                SettingsItem.EnumSetting(
                    key = "theme",
                    label = "Theme",
                    description = "Colour theme of the app",
                    entries = AppearanceTheme.entries,
                    get = { PrefsUtility.appearance_theme() },
                    set = PrefsUtility::appearance_theme_set
                ),
                SettingsItem.EnumSetting(
                    key = "navbar_colour",
                    label = "Navigation bar colour",
                    entries = AppearanceNavbarColour.entries,
                    get = { PrefsUtility.appearance_navbar_colour() },
                    set = PrefsUtility::pref_appearance_navbar_colour_set
                ),
                SettingsItem.ChoiceSetting(
                    key = "langforce",
                    label = "Language",
                    options = listOf(
                        "Automatic" to "auto",
                        "English" to "en",
                        "Dansk" to "da",
                        "Deutsch" to "de",
                        "Français" to "fr",
                        "عربي" to "ar",
                        "Español" to "es",
                        "Česky" to "cs",
                        "Italiano" to "it",
                        "Nederlands" to "nl",
                        "Português" to "pt",
                        "Romanian" to "ro",
                        "Magyar" to "hu",
                        "Esperanto" to "eo",
                        "Polski" to "pl",
                        "Bahasa Indonesia" to "in",
                        "Suomi" to "fi",
                        "မြန်မာဘာသာ" to "my",
                        "Norsk bokmål" to "nb-rNO",
                        "ру́сский язы́к" to "ru",
                        "українська мова" to "uk",
                        "汉语" to "zh-rCN",
                        "漢語" to "zh-rTW",
                        "Euskara" to "eu",
                        "ελληνικά" to "el",
                        "हिन्दी" to "hi",
                        "日本語" to "ja",
                        "lietuvių kalba" to "lt",
                        "svenska" to "sv",
                        "मराठी" to "mr",
                        "Bahasa Melayu" to "ms",
                        "Türkçe" to "tr",
                        "Català" to "ca",
                        "فارسی" to "fa",
                        "한국어" to "ko",
                        "Latviešu" to "lv",
                        "Norsk nynorsk" to "nn",
                        "ଓଡ଼ିଆ" to "or",
                        "ਪੰਜਾਬੀ" to "pa",
                        "Português (Brasil)" to "pt-rBR",
                        "српски" to "sr",
                        "ไทย" to "th"
                    ),
                    get = {
                        PrefsUtility.getString(
                            org.quantumbadger.redreader.R.string.pref_appearance_langforce_key,
                            "auto"
                        )
                    },
                    set = PrefsUtility::pref_appearance_langforce_set
                ),
                SettingsItem.EnumSetting(
                    key = "statusbar",
                    label = "Status bar mode",
                    description = "System status bar behaviour",
                    entries = AppearanceStatusBarMode.entries,
                    get = { PrefsUtility.pref_appearance_android_status() },
                    set = PrefsUtility::pref_appearance_android_status_set
                ),
                SettingsItem.BooleanSetting(
                    key = "hide_toolbar_on_scroll",
                    label = "Hide toolbar on scroll",
                    description = "Automatically hide the toolbar when scrolling",
                    get = { PrefsUtility.pref_appearance_hide_toolbar_on_scroll() },
                    set = PrefsUtility::pref_appearance_hide_toolbar_on_scroll_set
                ),
                SettingsItem.BooleanSetting(
                    key = "bottom_toolbar",
                    label = "Use bottom toolbar",
                    description = "Show the toolbar at the bottom of the screen",
                    get = { PrefsUtility.pref_appearance_bottom_toolbar() },
                    set = PrefsUtility::pref_appearance_bottom_toolbar_set
                )
            )
        ),

        // ─── Post appearance ───
        SettingsCategory(
            id = "post_appearance",
            title = "Post appearance",
            items = listOf(
                SettingsItem.MultiSelectSetting(
                    key = "post_subtitle_items",
                    label = "Post subtitle entries",
                    options = listOf(
                        "Author" to "author",
                        "Flair" to "flair",
                        "Score" to "score",
                        "Percent upvoted" to "upvote_ratio",
                        "Comments" to "comments",
                        "Age" to "age",
                        "Reddit Gold" to "gold",
                        "Subreddit" to "subreddit",
                        "Domain" to "domain",
                        "Sticky tag" to "sticky",
                        "Spoiler tag" to "spoiler",
                        "NSFW tag" to "nsfw",
                        "Crosspost tag" to "crosspost"
                    ),
                    get = {
                        PrefsUtility.appearance_post_subtitle_items()
                            .map { StringUtils.asciiLowercase(it.name) }
                            .toSet()
                    },
                    set = PrefsUtility::pref_appearance_post_subtitle_items_set
                ),
                SettingsItem.ChoiceSetting(
                    key = "post_age_units",
                    label = "Post age precision",
                    options = listOf(
                        "One unit (x hours)" to "1",
                        "Two units (x hours, x mins)" to "2",
                        "Three units (x hours, x mins, x secs)" to "3"
                    ),
                    get = {
                        PrefsUtility.getString(
                            org.quantumbadger.redreader.R.string.pref_appearance_post_age_units_key,
                            "2"
                        )
                    },
                    set = PrefsUtility::pref_appearance_post_age_units_set
                ),
                SettingsItem.BooleanSetting(
                    key = "post_subtitle_items_use_different_settings",
                    label = "Use different settings for opened posts",
                    get = {
                        PrefsUtility.appearance_post_subtitle_items_use_different_settings()
                    },
                    set = PrefsUtility::pref_appearance_post_subtitle_items_use_different_settings_set
                ),
                SettingsItem.MultiSelectSetting(
                    key = "post_header_subtitle_items",
                    label = "Opened post subtitle entries",
                    description = "Used when 'Use different settings for opened posts' is on",
                    options = listOf(
                        "Author" to "author",
                        "Flair" to "flair",
                        "Score" to "score",
                        "Percent upvoted" to "upvote_ratio",
                        "Comments" to "comments",
                        "Age" to "age",
                        "Reddit Gold" to "gold",
                        "Subreddit" to "subreddit",
                        "Domain" to "domain",
                        "Sticky tag" to "sticky",
                        "Spoiler tag" to "spoiler",
                        "NSFW tag" to "nsfw",
                        "Crosspost tag" to "crosspost"
                    ),
                    get = {
                        PrefsUtility.appearance_post_header_subtitle_items()
                            .map { StringUtils.asciiLowercase(it.name) }
                            .toSet()
                    },
                    set = PrefsUtility::pref_appearance_post_header_subtitle_items_set
                ),
                SettingsItem.ChoiceSetting(
                    key = "post_header_age_units",
                    label = "Opened post age precision",
                    description = "Used when 'Use different settings for opened posts' is on",
                    options = listOf(
                        "One unit (x hours)" to "1",
                        "Two units (x hours, x mins)" to "2",
                        "Three units (x hours, x mins, x secs)" to "3"
                    ),
                    get = {
                        PrefsUtility.getString(
                            org.quantumbadger.redreader.R.string.pref_appearance_post_header_age_units_key,
                            "2"
                        )
                    },
                    set = PrefsUtility::pref_appearance_post_header_age_units_set
                ),
                SettingsItem.BooleanSetting(
                    key = "post_show_comments_button",
                    label = "Show comment count button",
                    get = { PrefsUtility.appearance_post_show_comments_button() },
                    set = PrefsUtility::pref_appearance_post_show_comments_button_set
                ),
                SettingsItem.BooleanSetting(
                    key = "post_hide_subreddit_header",
                    label = "Hide subreddit header",
                    get = { PrefsUtility.pref_appearance_post_hide_subreddit_header() },
                    set = PrefsUtility::pref_appearance_post_hide_subreddit_header_set
                ),
                SettingsItem.BooleanSetting(
                    key = "hide_headertoolbar_postlist",
                    label = "Hide toolbar in post lists",
                    get = { PrefsUtility.pref_appearance_hide_headertoolbar_postlist() },
                    set = PrefsUtility::pref_appearance_hide_headertoolbar_postlist_set
                )
            )
        ),

        // ─── Comment appearance ───
        SettingsCategory(
            id = "comment_appearance",
            title = "Comment appearance",
            items = listOf(
                SettingsItem.BooleanSetting(
                    key = "comments_show_floating_toolbar",
                    label = "Show floating comment toolbar",
                    get = { PrefsUtility.pref_appearance_comments_show_floating_toolbar() },
                    set = PrefsUtility::pref_appearance_comments_show_floating_toolbar_set
                ),
                SettingsItem.MultiSelectSetting(
                    key = "comment_header_items",
                    label = "Comment header entries",
                    options = listOf(
                        "Author" to "author",
                        "Flair" to "flair",
                        "Score" to "score",
                        "Controversial indicator (†)" to "controversiality",
                        "Age" to "age",
                        "Reddit Gold" to "gold",
                        "Subreddit" to "subreddit"
                    ),
                    get = {
                        PrefsUtility.appearance_comment_header_items()
                            .map { StringUtils.asciiLowercase(it.name) }
                            .toSet()
                    },
                    set = PrefsUtility::pref_appearance_comment_header_items_set
                ),
                SettingsItem.ChoiceSetting(
                    key = "comment_age_units",
                    label = "Comment age precision",
                    options = listOf(
                        "One unit (x hours)" to "1",
                        "Two units (x hours, x mins)" to "2",
                        "Three units (x hours, x mins, x secs)" to "3"
                    ),
                    get = {
                        PrefsUtility.getString(
                            org.quantumbadger.redreader.R.string.pref_appearance_comment_age_units_key,
                            "2"
                        )
                    },
                    set = PrefsUtility::pref_appearance_comment_age_units_set
                ),
                SettingsItem.EnumSetting(
                    key = "comment_age_mode",
                    label = "Comment age mode",
                    entries = CommentAgeMode.entries,
                    get = { PrefsUtility.appearance_comment_age_mode() },
                    set = PrefsUtility::pref_appearance_comment_age_mode_set
                ),
                SettingsItem.BooleanSetting(
                    key = "linkbuttons",
                    label = "Show link buttons",
                    description = "Show buttons for common actions on posts",
                    get = { PrefsUtility.pref_appearance_linkbuttons() },
                    set = PrefsUtility::pref_appearance_linkbuttons_set
                ),
                SettingsItem.BooleanSetting(
                    key = "link_text_clickable",
                    label = "Clickable link text",
                    get = { PrefsUtility.pref_appearance_link_text_clickable() },
                    set = PrefsUtility::pref_appearance_link_text_clickable_set
                ),
                SettingsItem.BooleanSetting(
                    key = "indentlines",
                    label = "Show indentation lines",
                    get = { PrefsUtility.pref_appearance_indentlines() },
                    set = PrefsUtility::pref_appearance_indentlines_set
                ),
                SettingsItem.BooleanSetting(
                    key = "hide_headertoolbar_commentlist",
                    label = "Hide toolbar in comment lists",
                    get = { PrefsUtility.pref_appearance_hide_headertoolbar_commentlist() },
                    set = PrefsUtility::pref_appearance_hide_headertoolbar_commentlist_set
                ),
                SettingsItem.BooleanSetting(
                    key = "hide_comments_from_blocked_users",
                    label = "Hide comments from blocked users",
                    description = "Hide comments made by users you have blocked",
                    get = { PrefsUtility.pref_appearance_hide_comments_from_blocked_users() },
                    set = PrefsUtility::pref_appearance_hide_comments_from_blocked_users_set
                ),
                SettingsItem.BooleanSetting(
                    key = "highlight_own_username",
                    label = "Highlight own username",
                    get = { PrefsUtility.pref_appearance_highlight_own_username() },
                    set = PrefsUtility::pref_appearance_highlight_own_username_set
                )
            )
        ),

        // ─── User appearance ───
        SettingsCategory(
            id = "user_appearance",
            title = "Users",
            items = listOf(
                SettingsItem.BooleanSetting(
                    key = "user_show_avatars",
                    label = "Show user avatars",
                    get = { PrefsUtility.appearance_user_show_avatars() },
                    set = PrefsUtility::pref_appearance_user_show_avatars_set
                )
            )
        ),

        // ─── Inbox appearance ───
        SettingsCategory(
            id = "inbox_appearance",
            title = "Inbox",
            items = listOf(
                SettingsItem.ChoiceSetting(
                    key = "inbox_age_units",
                    label = "Inbox entry age precision",
                    options = listOf(
                        "One unit (x hours)" to "1",
                        "Two units (x hours, x mins)" to "2",
                        "Three units (x hours, x mins, x secs)" to "3"
                    ),
                    get = {
                        PrefsUtility.getString(
                            org.quantumbadger.redreader.R.string.pref_appearance_inbox_age_units_key,
                            "2"
                        )
                    },
                    set = PrefsUtility::pref_appearance_inbox_age_units_set
                )
            )
        ),

        // ─── Inline image previews ───
        SettingsCategory(
            id = "inline_previews",
            title = "Inline image previews",
            items = listOf(
                SettingsItem.EnumSetting(
                    key = "images_inline_image_previews",
                    label = "Inline image previews",
                    description = "Show inline image previews in comment threads",
                    entries = NeverAlwaysOrWifiOnly.entries,
                    get = { PrefsUtility.images_inline_image_previews() },
                    set = PrefsUtility::images_inline_image_previews_set
                ),
                SettingsItem.BooleanSetting(
                    key = "images_inline_image_previews_nsfw",
                    label = "Show NSFW previews",
                    description = "Show inline previews for images marked NSFW",
                    get = { PrefsUtility.images_inline_image_previews_nsfw() },
                    set = PrefsUtility::images_inline_image_previews_nsfw_set
                ),
                SettingsItem.BooleanSetting(
                    key = "images_inline_image_previews_spoiler",
                    label = "Show spoiler previews",
                    description = "Show inline previews for images marked as spoilers",
                    get = { PrefsUtility.images_inline_image_previews_spoiler() },
                    set = PrefsUtility::images_inline_image_previews_spoiler_set
                )
            )
        ),

        // ─── Image / video viewer ───
        SettingsCategory(
            id = "image_video_viewer",
            title = "Image / video viewer",
            items = listOf(
                SettingsItem.BooleanSetting(
                    key = "video_playback_controls",
                    label = "Enable video playback controls",
                    get = { PrefsUtility.pref_behaviour_video_playback_controls() },
                    set = PrefsUtility::pref_behaviour_video_playback_controls_set
                ),
                SettingsItem.BooleanSetting(
                    key = "video_frame_step",
                    label = "Enable stepping frame by frame",
                    get = { PrefsUtility.pref_behaviour_video_frame_step() },
                    set = PrefsUtility::pref_behaviour_video_frame_step_set
                ),
                SettingsItem.BooleanSetting(
                    key = "video_mute_default",
                    label = "Mute videos by default",
                    get = { PrefsUtility.pref_behaviour_video_mute_default() },
                    set = PrefsUtility::pref_behaviour_video_mute_default_set
                ),
                SettingsItem.BooleanSetting(
                    key = "video_zoom_default",
                    label = "Crop videos to fill screen",
                    get = { PrefsUtility.pref_behaviour_video_zoom_default() },
                    set = PrefsUtility::pref_behaviour_video_zoom_default_set
                ),
                SettingsItem.BooleanSetting(
                    key = "imagevideo_tap_close",
                    label = "Tap to close images/videos",
                    description = "Does not affect videos if playback controls are enabled",
                    get = { PrefsUtility.pref_behaviour_imagevideo_tap_close() },
                    set = PrefsUtility::pref_behaviour_imagevideo_tap_close_set
                ),
                SettingsItem.BooleanSetting(
                    key = "videos_download_before_playing",
                    label = "Fully download videos before starting playback",
                    get = { PrefsUtility.pref_videos_download_before_playing() },
                    set = PrefsUtility::pref_videos_download_before_playing_set
                ),
                SettingsItem.EnumSetting(
                    key = "albumview_mode",
                    label = "Album viewer",
                    description = "How multi-image albums are opened",
                    entries = AlbumViewMode.entries,
                    get = { PrefsUtility.pref_behaviour_albumview_mode() },
                    set = PrefsUtility::pref_behaviour_albumview_mode_set
                ),
                SettingsItem.BooleanSetting(
                    key = "image_viewer_show_floating_toolbar",
                    label = "Show floating buttons over images and videos",
                    get = { PrefsUtility.pref_appearance_image_viewer_show_floating_toolbar() },
                    set = PrefsUtility::pref_appearance_image_viewer_show_floating_toolbar_set
                ),
                SettingsItem.BooleanSetting(
                    key = "show_aspect_ratio_indicator",
                    label = "Show aspect ratio indicator",
                    description = "Show a visual of loading media when available",
                    get = { PrefsUtility.pref_appearance_show_aspect_ratio_indicator() },
                    set = PrefsUtility::pref_appearance_show_aspect_ratio_indicator_set
                )
            )
        ),

        // ─── Album viewer ───
        SettingsCategory(
            id = "album_viewer",
            title = "Album viewer",
            items = listOf(
                SettingsItem.BooleanSetting(
                    key = "album_skip_to_first",
                    label = "Automatically open first album image",
                    get = { PrefsUtility.pref_album_skip_to_first() },
                    set = PrefsUtility::pref_album_skip_to_first_set
                ),
                SettingsItem.ChoiceSetting(
                    key = "gallery_swipe_length",
                    label = "Album swipe length",
                    description = "How far to swipe to advance between album images",
                    options = listOf(
                        "25 dp" to "25",
                        "50 dp" to "50",
                        "100 dp" to "100",
                        "150 dp" to "150",
                        "200 dp" to "200",
                        "250 dp" to "250",
                        "300 dp" to "300"
                    ),
                    get = {
                        PrefsUtility.getString(
                            org.quantumbadger.redreader.R.string.pref_behaviour_gallery_swipe_length_key,
                            "150"
                        )
                    },
                    set = PrefsUtility::pref_behaviour_gallery_swipe_length_set
                )
            )
        ),

        // ─── Thumbnails ───
        SettingsCategory(
            id = "thumbnails",
            title = "Thumbnails",
            items = listOf(
                SettingsItem.EnumSetting(
                    key = "thumbnails_show",
                    label = "Show thumbnails",
                    entries = NeverAlwaysOrWifiOnly.entries,
                    get = { PrefsUtility.appearance_thumbnails_show() },
                    set = PrefsUtility::appearance_thumbnails_show_set
                ),
                SettingsItem.BooleanSetting(
                    key = "thumbnails_nsfw_show",
                    label = "Show NSFW thumbnails",
                    get = { PrefsUtility.appearance_thumbnails_nsfw_show() },
                    set = PrefsUtility::appearance_thumbnails_nsfw_show_set
                ),
                SettingsItem.BooleanSetting(
                    key = "thumbnails_spoiler_show",
                    label = "Show spoiler thumbnails",
                    get = { PrefsUtility.appearance_thumbnails_spoiler_show() },
                    set = PrefsUtility::appearance_thumbnails_spoiler_show_set
                ),
                SettingsItem.EnumSetting(
                    key = "high_res_thumbnails",
                    label = "High resolution thumbnails",
                    entries = NeverAlwaysOrWifiOnly.entries,
                    get = { PrefsUtility.images_high_res_thumbnails() },
                    set = PrefsUtility::images_high_res_thumbnails_set
                ),
                SettingsItem.ChoiceSetting(
                    key = "thumbnail_size",
                    label = "Thumbnail size",
                    options = listOf(
                        "0.4x" to "24",
                        "0.5x" to "32",
                        "0.75x" to "48",
                        "1.0x" to "64",
                        "1.1x" to "70",
                        "1.25x" to "80",
                        "1.5x" to "96",
                        "2.0x" to "128",
                        "2.5x" to "160",
                        "3.0x" to "192",
                        "4.0x" to "256"
                    ),
                    get = {
                        PrefsUtility.getString(
                            org.quantumbadger.redreader.R.string.pref_images_thumbnail_size_key,
                            "64"
                        )
                    },
                    set = PrefsUtility::images_thumbnail_size_set
                )
            )
        ),

        // ─── Behaviour (general) ───
        SettingsCategory(
            id = "behaviour",
            title = "Behaviour",
            items = listOf(
                SettingsItem.BooleanSetting(
                    key = "skiptofrontpage",
                    label = "Skip to front page",
                    get = { PrefsUtility.pref_behaviour_skiptofrontpage() },
                    set = PrefsUtility::pref_behaviour_skiptofrontpage_set
                ),
                SettingsItem.BooleanSetting(
                    key = "useinternalbrowser",
                    label = "Use internal browser",
                    get = { PrefsUtility.pref_behaviour_useinternalbrowser() },
                    set = PrefsUtility::pref_behaviour_useinternalbrowser_set
                ),
                SettingsItem.BooleanSetting(
                    key = "usecustomtabs",
                    label = "Use Android Custom Tabs",
                    description = "Use an installed browser with Custom Tabs integration, rather than the system WebView",
                    get = { PrefsUtility.pref_behaviour_usecustomtabs() },
                    set = PrefsUtility::pref_behaviour_usecustomtabs_set
                ),
                SettingsItem.BooleanSetting(
                    key = "notifications",
                    label = "Notifications",
                    get = { PrefsUtility.pref_behaviour_notifications() },
                    set = PrefsUtility::set_pref_behaviour_notifications
                ),
                SettingsItem.EnumSetting(
                    key = "screenorientation",
                    label = "Screen orientation",
                    entries = ScreenOrientation.entries,
                    get = { PrefsUtility.pref_behaviour_screen_orientation() },
                    set = PrefsUtility::pref_behaviour_screen_orientation_set
                ),
                SettingsItem.BooleanSetting(
                    key = "enable_swipe_refresh",
                    label = "Swipe down to refresh",
                    get = { PrefsUtility.pref_behaviour_enable_swipe_refresh() },
                    set = PrefsUtility::pref_behaviour_enable_swipe_refresh_set
                ),
                SettingsItem.EnumSetting(
                    key = "save_location",
                    label = "Save destination",
                    entries = SaveLocation.entries,
                    get = { PrefsUtility.pref_behaviour_save_location() },
                    set = PrefsUtility::pref_behaviour_save_location_set
                ),
                SettingsItem.BooleanSetting(
                    key = "block_screenshots",
                    label = "Block screenshots",
                    description = "Prevent screenshots and hide app content in the recents menu",
                    get = { PrefsUtility.behaviour_block_screenshots() },
                    set = PrefsUtility::pref_behaviour_block_screenshots_set
                ),
                SettingsItem.BooleanSetting(
                    key = "keep_screen_awake",
                    label = "Keep screen awake",
                    description = "Keep the screen awake while RedReader is in the foreground",
                    get = { PrefsUtility.pref_behaviour_keep_screen_awake() },
                    set = PrefsUtility::pref_behaviour_keep_screen_awake_set
                ),
                SettingsItem.BooleanSetting(
                    key = "postlist_back_again",
                    label = "Press back twice to exit post list",
                    get = { PrefsUtility.pref_behaviour_back_again() },
                    set = PrefsUtility::pref_behaviour_postlist_back_again_set
                )
            )
        ),

        // ─── Sorting ───
        SettingsCategory(
            id = "sorting",
            title = "Sorting",
            items = listOf(
                SettingsItem.EnumSetting(
                    key = "postsort",
                    label = "Posts (default)",
                    entries = PostSort.entries,
                    get = { PrefsUtility.pref_behaviour_postsort() },
                    set = PrefsUtility::pref_behaviour_postsort_set
                ),
                SettingsItem.EnumSetting(
                    key = "user_postsort",
                    label = "User posts (default)",
                    entries = PostSort.entries,
                    get = { PrefsUtility.pref_behaviour_user_postsort() },
                    set = PrefsUtility::pref_behaviour_user_postsort_set
                ),
                SettingsItem.EnumSetting(
                    key = "multi_postsort",
                    label = "Multireddit posts (default)",
                    entries = PostSort.entries,
                    get = { PrefsUtility.pref_behaviour_multi_postsort() },
                    set = PrefsUtility::pref_behaviour_multi_postsort_set
                ),
                SettingsItem.EnumSetting(
                    key = "commentsort",
                    label = "Comments (default)",
                    entries = PostCommentSort.entries,
                    get = { PrefsUtility.pref_behaviour_commentsort() },
                    set = PrefsUtility::pref_behaviour_commentsort_set
                ),
                SettingsItem.EnumSetting(
                    key = "user_commentsort",
                    label = "User comments (default)",
                    entries = UserCommentSort.entries,
                    get = { PrefsUtility.pref_behaviour_user_commentsort() },
                    set = PrefsUtility::pref_behaviour_user_commentsort_set
                ),
                SettingsItem.EnumSetting(
                    key = "pinned_subredditsort",
                    label = "Pinned subreddits",
                    entries = PinnedSubredditSort.entries,
                    get = { PrefsUtility.pref_behaviour_pinned_subredditsort() },
                    set = PrefsUtility::pref_behaviour_pinned_subredditsort_set
                ),
                SettingsItem.EnumSetting(
                    key = "blocked_subredditsort",
                    label = "Blocked subreddits",
                    entries = BlockedSubredditSort.entries,
                    get = { PrefsUtility.pref_behaviour_blocked_subredditsort() },
                    set = PrefsUtility::pref_behaviour_blocked_subredditsort_set
                )
            )
        ),

        // ─── Post actions ───
        SettingsCategory(
            id = "post_actions",
            title = "Post actions",
            items = listOf(
                SettingsItem.EnumSetting(
                    key = "post_tap_action",
                    label = "Post tap action",
                    description = "What happens when you tap a post",
                    entries = PostTapAction.entries,
                    get = { PrefsUtility.pref_behaviour_post_tap_action() },
                    set = PrefsUtility::pref_behaviour_post_tap_action_set
                ),
                SettingsItem.EnumSetting(
                    key = "self_post_tap_actions",
                    label = "Post text tap",
                    entries = SelfpostAction.entries,
                    get = { PrefsUtility.pref_behaviour_self_post_tap_actions() },
                    set = PrefsUtility::pref_behaviour_self_post_tap_actions_set
                ),
                SettingsItem.EnumSetting(
                    key = "fling_post_left",
                    label = "Fling left",
                    description = "Action when swiping a post to the left",
                    entries = PostFlingAction.entries,
                    get = { PrefsUtility.pref_behaviour_fling_post_left() },
                    set = PrefsUtility::pref_behaviour_fling_post_left_set
                ),
                SettingsItem.EnumSetting(
                    key = "fling_post_right",
                    label = "Fling right",
                    description = "Action when swiping a post to the right",
                    entries = PostFlingAction.entries,
                    get = { PrefsUtility.pref_behaviour_fling_post_right() },
                    set = PrefsUtility::pref_behaviour_fling_post_right_set
                )
            )
        ),

        // ─── Comment actions ───
        SettingsCategory(
            id = "comment_actions",
            title = "Comment actions",
            items = listOf(
                SettingsItem.EnumSetting(
                    key = "comment_tap",
                    label = "Press",
                    description = "What happens when you tap a comment",
                    entries = CommentAction.entries,
                    get = { PrefsUtility.pref_behaviour_actions_comment_tap() },
                    set = PrefsUtility::pref_behaviour_actions_comment_tap_set
                ),
                SettingsItem.EnumSetting(
                    key = "comment_longclick",
                    label = "Long press",
                    description = "What happens when you long-press a comment",
                    entries = CommentAction.entries,
                    get = { PrefsUtility.pref_behaviour_actions_comment_longclick() },
                    set = PrefsUtility::pref_behaviour_actions_comment_longclick_set
                ),
                SettingsItem.EnumSetting(
                    key = "fling_comment_left",
                    label = "Fling left",
                    description = "Action when swiping a comment to the left",
                    entries = CommentFlingAction.entries,
                    get = { PrefsUtility.pref_behaviour_fling_comment_left() },
                    set = PrefsUtility::pref_behaviour_fling_comment_left_set
                ),
                SettingsItem.EnumSetting(
                    key = "fling_comment_right",
                    label = "Fling right",
                    description = "Action when swiping a comment to the right",
                    entries = CommentFlingAction.entries,
                    get = { PrefsUtility.pref_behaviour_fling_comment_right() },
                    set = PrefsUtility::pref_behaviour_fling_comment_right_set
                )
            )
        ),

        // ─── Posts ───
        SettingsCategory(
            id = "posts",
            title = "Posts",
            items = listOf(
                SettingsItem.BooleanSetting(
                    key = "nsfw",
                    label = "Show NSFW content",
                    get = { PrefsUtility.pref_behaviour_nsfw() },
                    set = PrefsUtility::pref_behaviour_nsfw_set
                ),
                SettingsItem.BooleanSetting(
                    key = "hide_read_posts",
                    label = "Hide read posts",
                    get = { PrefsUtility.pref_behaviour_hide_read_posts() },
                    set = PrefsUtility::pref_behaviour_hide_read_posts_set
                ),
                SettingsItem.BooleanSetting(
                    key = "mark_posts_as_read",
                    label = "Mark posts as read",
                    get = { PrefsUtility.pref_behaviour_mark_posts_as_read() },
                    set = PrefsUtility::pref_behaviour_mark_posts_as_read_set
                ),
                SettingsItem.EnumSetting(
                    key = "postcount",
                    label = "Restrict post count",
                    entries = PostCount.entries,
                    get = { PrefsUtility.pref_behaviour_post_count() },
                    set = PrefsUtility::pref_behaviour_post_count_set
                )
            )
        ),

        // ─── Comments ───
        SettingsCategory(
            id = "comments",
            title = "Comments",
            items = listOf(
                SettingsItem.StringSetting(
                    key = "comment_min",
                    label = "Minimum comment score",
                    description = "Hide comments with a score below this (blank = no limit)",
                    placeholder = "-4",
                    get = { PrefsUtility.pref_behaviour_comment_min()?.toString() },
                    set = PrefsUtility::pref_behaviour_comment_min_set
                ),
                SettingsItem.EnumSetting(
                    key = "collapse_sticky_comments",
                    label = "Collapse sticky comments",
                    entries = BehaviourCollapseStickyComments.entries,
                    get = { PrefsUtility.behaviour_collapse_sticky_comments() },
                    set = PrefsUtility::pref_behaviour_collapse_sticky_comments_set
                )
            )
        ),

        // ─── Sharing ───
        SettingsCategory(
            id = "sharing",
            title = "Sharing",
            items = listOf(
                SettingsItem.EnumSetting(
                    key = "sharing_domain",
                    label = "Share as domain",
                    entries = SharingDomain.entries,
                    get = { PrefsUtility.pref_behaviour_sharing_domain() },
                    set = PrefsUtility::pref_behaviour_sharing_domain_set
                ),
                SettingsItem.BooleanSetting(
                    key = "share_permalink",
                    label = "Share as permalink",
                    get = { PrefsUtility.pref_behaviour_share_permalink() },
                    set = PrefsUtility::pref_behaviour_share_permalink_set
                ),
                SettingsItem.BooleanSetting(
                    key = "sharing_include_desc",
                    label = "Include title/description when sharing",
                    get = { PrefsUtility.pref_behaviour_sharing_include_desc() },
                    set = PrefsUtility::pref_behaviour_sharing_include_desc_set
                ),
                SettingsItem.BooleanSetting(
                    key = "sharing_share_text",
                    label = "Include text when sharing comment",
                    get = { PrefsUtility.pref_behaviour_sharing_share_text() },
                    set = PrefsUtility::pref_behaviour_sharing_share_text_set
                ),
                SettingsItem.BooleanSetting(
                    key = "sharing_share_dialog",
                    label = "Use built-in share dialog",
                    get = { PrefsUtility.pref_behaviour_sharing_dialog() },
                    set = PrefsUtility::pref_behaviour_sharing_share_dialog_set
                )
            )
        ),

        // ─── Post side-toolbar ───
        SettingsCategory(
            id = "side_toolbar",
            title = "Post side-toolbar",
            items = listOf(
                SettingsItem.ChoiceSetting(
                    key = "bezel_toolbar_swipezone",
                    label = "Side-toolbar swipe zone size",
                    description = "How far from the edge to swipe to reveal the post side-toolbar",
                    options = listOf(
                        "0 dp" to "0",
                        "10 dp" to "10",
                        "20 dp" to "20",
                        "30 dp" to "30",
                        "40 dp" to "40"
                    ),
                    get = {
                        PrefsUtility.getString(
                            org.quantumbadger.redreader.R.string.pref_behaviour_bezel_toolbar_swipezone_key,
                            "10"
                        )
                    },
                    set = PrefsUtility::pref_behaviour_bezel_toolbar_swipezone_set
                )
            )
        ),

        // ─── Network ───
        SettingsCategory(
            id = "network",
            title = "Network",
            items = listOf(
                SettingsItem.BooleanSetting(
                    key = "tor",
                    label = "Use Tor",
                    description = "Route traffic through the Tor network",
                    get = { PrefsUtility.network_tor() },
                    set = PrefsUtility::network_tor_set
                ),
                SettingsItem.StringSetting(
                    key = "reddit_client_id_override",
                    label = "Reddit client ID override",
                    description = "Override the compiled-in Reddit OAuth client ID (self-compiled builds)",
                    placeholder = "Your Reddit OAuth client ID",
                    get = { PrefsUtility.pref_reddit_client_id_override() },
                    set = PrefsUtility::pref_reddit_client_id_override_set
                )
            )
        ),

        // ─── Cache ───
        SettingsCategory(
            id = "cache",
            title = "Cache",
            items = listOf(
                SettingsItem.PreferenceItem(
                    key = "cache_location",
                    label = "Cache location",
                    description = "Choose storage location for cached data",
                    onClick = { /* TODO: show storage chooser */ }
                )
            )
        ),

        // ─── About ───
        SettingsCategory(
            id = "about",
            title = "About",
            items = listOf(
                SettingsItem.PreferenceItem(
                    key = "version",
                    label = "Version",
                    description = "RedReader ${BuildConfig.VERSION_NAME}",
                    onClick = { /* informational only */ }
                ),
                SettingsItem.PreferenceItem(
                    key = "changelog",
                    label = "Changelog",
                    description = "View what's new",
                    onClick = onNavigateToChangelog
                ),
                SettingsItem.PreferenceItem(
                    key = "license",
                    label = "License",
                    description = "View open-source license",
                    onClick = { HtmlViewActivity.showAsset(context, "license.html") }
                ),
                SettingsItem.PreferenceItem(
                    key = "github",
                    label = "GitHub",
                    description = "View source code on GitHub",
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/RedReaderOrg/RedReader")
                                )
                            )
                        }
                    }
                )
            )
        ),

        // ─── Menus (30th) ───
        SettingsCategory(
            id = "menus",
            title = "Menus",
            items = listOf(
                SettingsItem.PreferenceItem(
                    key = "action_bar_items",
                    label = "Action bar items",
                    description = "Which buttons appear in the app bar",
                    onClick = onOpenAppbar
                ),
                SettingsItem.BooleanSetting(
                    key = "quick_account_switcher",
                    label = "Use quick account switching",
                    get = { PrefsUtility.pref_menus_quick_account_switcher() },
                    set = { PrefsUtility.pref_menus_quick_account_switcher_set(it) }
                ),
                SettingsItem.MultiSelectSetting(
                    key = "link_context_items",
                    label = "Link action menu items",
                    options = listOf(
                        "View in External Browser" to "external",
                        "Save Media" to "save_image",
                        "Share" to "share",
                        "Share Media" to "share_image",
                        "Copy Link" to "copy_url"
                    ),
                    get = {
                        PrefsUtility.getStringSet(
                            org.quantumbadger.redreader.R.string.pref_menus_link_context_items_key,
                            org.quantumbadger.redreader.R.array.pref_menus_link_context_items_default
                        )
                    },
                    set = { PrefsUtility.pref_menus_link_context_items_set(it) }
                ),
                SettingsItem.MultiSelectSetting(
                    key = "subreddit_context_items",
                    label = "Subreddit action menu items",
                    options = listOf(
                        "View in External Browser" to "external",
                        "Share" to "share",
                        "Copy Link" to "copy_url",
                        "Pin Subreddit to Main Menu" to "pin",
                        "Subscribe" to "subscribe",
                        "Block Subreddit" to "block"
                    ),
                    get = {
                        PrefsUtility.getStringSet(
                            org.quantumbadger.redreader.R.string.pref_menus_subreddit_context_items_key,
                            org.quantumbadger.redreader.R.array.pref_menus_subreddit_context_items_default
                        )
                    },
                    set = { PrefsUtility.pref_menus_subreddit_context_items_set(it) }
                ),
                SettingsItem.MultiSelectSetting(
                    key = "mainmenu_shortcutitems",
                    label = "Shortcuts",
                    description = "Shortcuts shown in the main menu",
                    options = listOf(
                        "Front Page" to "frontpage",
                        "Popular Subreddits" to "popular",
                        "All Posts" to "all",
                        "Find Location" to "subreddit_search",
                        "Custom Location" to "custom"
                    ),
                    get = {
                        PrefsUtility.getStringSet(
                            org.quantumbadger.redreader.R.string.pref_menus_mainmenu_shortcutitems_key,
                            org.quantumbadger.redreader.R.array.pref_menus_mainmenu_shortcutitems_items_default
                        )
                    },
                    set = { PrefsUtility.pref_menus_mainmenu_shortcutitems_set(it) }
                ),
                SettingsItem.MultiSelectSetting(
                    key = "mainmenu_useritems",
                    label = "User items",
                    description = "User items shown in the main menu",
                    options = listOf(
                        "My Profile" to "profile",
                        "Inbox" to "inbox",
                        "Sent Messages" to "sent_messages",
                        "Submitted Posts" to "submitted",
                        "Submitted Comments" to "submitted_comments",
                        "Saved Posts" to "saved",
                        "Hidden Posts" to "hidden",
                        "Upvoted Posts" to "upvoted",
                        "Downvoted Posts" to "downvoted",
                        "Modmail" to "modmail"
                    ),
                    get = {
                        PrefsUtility.getStringSet(
                            org.quantumbadger.redreader.R.string.pref_menus_mainmenu_useritems_key,
                            org.quantumbadger.redreader.R.array.pref_menus_mainmenu_useritems_items_default
                        )
                    },
                    set = { PrefsUtility.pref_menus_mainmenu_useritems_set(it) }
                ),
                SettingsItem.BooleanSetting(
                    key = "hide_username_main_menu",
                    label = "Hide username",
                    get = { PrefsUtility.pref_appearance_hide_username_main_menu() },
                    set = { PrefsUtility.pref_appearance_hide_username_main_menu_set(it) }
                ),
                SettingsItem.BooleanSetting(
                    key = "show_blocked_subreddits_main_menu",
                    label = "Show blocked subreddits",
                    get = { PrefsUtility.pref_appearance_show_blocked_subreddits_main_menu() },
                    set = { PrefsUtility.pref_appearance_show_blocked_subreddits_main_menu_set(it) }
                ),
                SettingsItem.BooleanSetting(
                    key = "show_multireddit_main_menu",
                    label = "Show multireddits",
                    get = { PrefsUtility.pref_show_multireddit_main_menu() },
                    set = { PrefsUtility.pref_show_multireddit_main_menu_set(it) }
                ),
                SettingsItem.BooleanSetting(
                    key = "show_subscribed_subreddits_main_menu",
                    label = "Show subscribed subreddits",
                    get = { PrefsUtility.pref_show_subscribed_subreddits_main_menu() },
                    set = { PrefsUtility.pref_show_subscribed_subreddits_main_menu_set(it) }
                ),
                SettingsItem.BooleanSetting(
                    key = "mainmenu_dev_announcements",
                    label = "Show announcements from developer",
                    get = { PrefsUtility.pref_menus_mainmenu_dev_announcements() },
                    set = { PrefsUtility.pref_menus_mainmenu_dev_announcements_set(it) }
                ),
                SettingsItem.MultiSelectSetting(
                    key = "post_context_items",
                    label = "Post action menu items",
                    description = "Actions available from a post's context menu",
                    options = listOf(
                        "Upvote" to "upvote",
                        "Downvote" to "downvote",
                        "View Comments" to "comments",
                        "Go to Crosspost Origin" to "crosspost_origin",
                        "Save" to "save",
                        "Hide" to "hide",
                        "Delete" to "delete",
                        "Report" to "report",
                        "Reply" to "reply",
                        "View in External Browser" to "external",
                        "Links in Text" to "selftext_links",
                        "Save Media" to "save_image",
                        "Go to Subreddit" to "goto_subreddit",
                        "Pin Subreddit to Main Menu" to "pin",
                        "Subscribe to Subreddit" to "subscribe",
                        "Block Subreddit" to "block",
                        "Share Link" to "share",
                        "Share Comments" to "share_comments",
                        "Share Media" to "share_image",
                        "Copy Link" to "copy",
                        "Copy Self-Text" to "copy_selftext",
                        "User Profile" to "user_profile",
                        "Properties" to "properties",
                        "Edit" to "edit",
                        "Mark as Read" to "mark_read"
                    ),
                    get = {
                        PrefsUtility.getStringSet(
                            org.quantumbadger.redreader.R.string.pref_menus_post_context_items_key,
                            org.quantumbadger.redreader.R.array.pref_menus_post_context_items_default
                        )
                    },
                    set = { PrefsUtility.pref_menus_post_context_items_set(it) }
                ),
                SettingsItem.MultiSelectSetting(
                    key = "post_toolbar_items",
                    label = "Post side-toolbar items",
                    description = "Actions available from a post's side toolbar",
                    options = listOf(
                        "Open Action Menu" to "action_menu",
                        "Switch To Comments" to "comments_switch",
                        "Switch To Link" to "link_switch",
                        "Upvote" to "upvote",
                        "Downvote" to "downvote",
                        "Save" to "save",
                        "Hide" to "hide",
                        "Reply" to "reply",
                        "View in External Browser" to "external",
                        "Save Media" to "save_image",
                        "Share Link" to "share",
                        "Copy Link" to "copy",
                        "User Profile" to "user_profile",
                        "Properties" to "properties"
                    ),
                    get = {
                        PrefsUtility.getStringSet(
                            org.quantumbadger.redreader.R.string.pref_menus_post_toolbar_items_key,
                            org.quantumbadger.redreader.R.array.pref_menus_post_toolbar_items_return
                        )
                    },
                    set = { PrefsUtility.pref_menus_post_toolbar_items_set(it) }
                ),
                SettingsItem.MultiSelectSetting(
                    key = "comment_context_items",
                    label = "Comment action menu items",
                    description = "Actions available from a comment's context menu",
                    options = listOf(
                        "Upvote" to "upvote",
                        "Downvote" to "downvote",
                        "Save" to "save",
                        "Report" to "report",
                        "Reply" to "reply",
                        "Edit" to "edit",
                        "Delete" to "delete",
                        "View in External Browser" to "external",
                        "Context" to "context",
                        "Go to Comment" to "go_to_comment",
                        "Links in Comment" to "comment_links",
                        "Toggle Collapse" to "collapse",
                        "Share" to "share",
                        "Copy Text" to "copy_text",
                        "Copy Link" to "copy_url",
                        "User Profile" to "user_profile",
                        "Properties" to "properties"
                    ),
                    get = {
                        PrefsUtility.getStringSet(
                            org.quantumbadger.redreader.R.string.pref_menus_comment_context_items_key,
                            org.quantumbadger.redreader.R.array.pref_menus_comment_context_items_return
                        )
                    },
                    set = { PrefsUtility.pref_menus_comment_context_items_set(it) }
                )
            )
        )
    )
}

/**
 * Appbar-screen sub-screen (30th): the 14 "Action bar items" settings, each a
 * ChoiceSetting over the shared show-as-action values (mirrors the legacy
 * prefs_menus_appbar_screen.xml ListPreferences).
 */
@Composable
private fun AppbarScreen(modifier: Modifier, onBack: () -> Unit) {
    val appbarOptions = listOf(
        "Always show button" to "2",
        "Show button if room" to "1",
        "Only show in three-dot menu" to "0",
        "Do not show" to "-1"
    )

    fun choice(key: String, label: String, keyRes: Int, default: String, setter: (String) -> Unit): SettingsItem.ChoiceSetting =
        SettingsItem.ChoiceSetting(
            key = key,
            label = label,
            options = appbarOptions,
            get = { PrefsUtility.getString(keyRes, default) },
            set = { setter(it) }
        )

    val appbarItems = listOf(
        choice("appbar_sort", "Sort", org.quantumbadger.redreader.R.string.pref_menus_appbar_sort_key, "2") { PrefsUtility.pref_menus_appbar_sort_set(it) },
        choice("appbar_refresh", "Refresh", org.quantumbadger.redreader.R.string.pref_menus_appbar_refresh_key, "2") { PrefsUtility.pref_menus_appbar_refresh_set(it) },
        choice("appbar_past", "Past Versions", org.quantumbadger.redreader.R.string.pref_menus_appbar_past_key, "0") { PrefsUtility.pref_menus_appbar_past_set(it) },
        choice("appbar_submit_post", "Submit Post", org.quantumbadger.redreader.R.string.pref_menus_appbar_submit_post_key, "0") { PrefsUtility.pref_menus_appbar_submit_post_set(it) },
        choice("appbar_pin", "Pin to Main Menu", org.quantumbadger.redreader.R.string.pref_menus_appbar_pin_key, "0") { PrefsUtility.pref_menus_appbar_pin_set(it) },
        choice("appbar_subscribe", "Subscribe", org.quantumbadger.redreader.R.string.pref_menus_appbar_subscribe_key, "0") { PrefsUtility.pref_menus_appbar_subscribe_set(it) },
        choice("appbar_block", "Block Subreddit", org.quantumbadger.redreader.R.string.pref_menus_appbar_block_key, "0") { PrefsUtility.pref_menus_appbar_block_set(it) },
        choice("appbar_sidebar", "View Sidebar", org.quantumbadger.redreader.R.string.pref_menus_appbar_sidebar_key, "0") { PrefsUtility.pref_menus_appbar_sidebar_set(it) },
        choice("appbar_accounts", "Accounts", org.quantumbadger.redreader.R.string.pref_menus_appbar_accounts_key, "0") { PrefsUtility.pref_menus_appbar_accounts_set(it) },
        choice("appbar_theme", "Themes", org.quantumbadger.redreader.R.string.pref_menus_appbar_theme_key, "0") { PrefsUtility.pref_menus_appbar_theme_set(it) },
        choice("appbar_settings", "Settings", org.quantumbadger.redreader.R.string.pref_menus_appbar_settings_key, "0") { PrefsUtility.pref_menus_appbar_settings_set(it) },
        choice("appbar_close_all", "Close All", org.quantumbadger.redreader.R.string.pref_menus_appbar_close_all_key, "-1") { PrefsUtility.pref_menus_appbar_close_all_set(it) },
        choice("appbar_reply", "Reply", org.quantumbadger.redreader.R.string.pref_menus_appbar_reply_key, "0") { PrefsUtility.pref_menus_appbar_reply_set(it) },
        choice("appbar_search", "Search", org.quantumbadger.redreader.R.string.pref_menus_appbar_search_key, "0") { PrefsUtility.pref_menus_appbar_search_set(it) }
    )

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "header_appbar") {
            Text(
                text = "Action bar items",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        items(appbarItems) { item ->
            ChoiceSettingItem(item)
        }
    }
}
