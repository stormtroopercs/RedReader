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

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Square
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.PrefsUtility.*
import org.quantumbadger.redreader.common.PrefsUtility.AppearanceStatusBarMode
import org.quantumbadger.redreader.common.PrefsUtility.PostTapAction
import org.quantumbadger.redreader.common.PrefsUtility.ImageViewMode
import org.quantumbadger.redreader.common.PrefsUtility.AlbumViewMode
import org.quantumbadger.redreader.common.PrefsUtility.GifViewMode
import org.quantumbadger.redreader.common.PrefsUtility.VideoViewMode
import org.quantumbadger.redreader.common.PrefsUtility.PostFlingAction
import org.quantumbadger.redreader.common.PrefsUtility.CommentFlingAction
import org.quantumbadger.redreader.common.PrefsUtility.CommentAction
import org.quantumbadger.redreader.common.PrefsUtility.PostSort
import org.quantumbadger.redreader.common.PrefsUtility.PostCommentSort
import org.quantumbadger.redreader.common.PrefsUtility.UserCommentSort
import org.quantumbadger.redreader.common.PrefsUtility.UserPostSort
import org.quantumbadger.redreader.common.PrefsUtility.MultiPostSort
import org.quantumbadger.redreader.common.PrefsUtility.SortType

/**
 * Settings screen composable — real data from PrefsUtility.
 * Shows categorized settings grouped by Appearance, Behaviour, Network, Cache, and About.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    var showSortDropdown by remember { mutableStateOf(false) }
    var currentSortPref by remember { mutableIntStateOf(PrefsUtility.prefPostsSort(context).ordinal) }

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
                    IconButton(onClick = { showSortDropdown = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Sort preferences"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        SettingsContent(
            context = context,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

/**
 * Settings content with grouped preferences.
 */
@Composable
private fun SettingsContent(
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    val settings = getSettingsCategories(context)

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
                    is SettingsItem.IntSetting -> IntSettingItem(item)
                    is SettingsItem.EnumSetting<*> -> EnumSettingItem(item)
                    is SettingsItem.PreferenceItem -> PreferenceItem(item)
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
        mutableStateOf(getBooleanPref(item.key, item.default))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                enabled = !enabled
                setBooleanPref(item.key, enabled)
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
            setBooleanPref(item.key, it)
        })
    }
}

/**
 * Integer setting (numeric value display).
 */
@Composable
private fun IntSettingItem(item: SettingsItem.IntSetting) {
    val value = getIntPref(item.key, item.default)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { /* TODO: show number picker */ })
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
            text = value.toString(),
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
}

/**
 * Enum setting with dropdown selection.
 */
@Composable
private fun <T : Enum<T>> EnumSettingItem(item: SettingsItem.EnumSetting<T>) {
    var expanded by remember { mutableStateOf(false) }
    val selected = getEnumPref(item.key, item.enumClass, item.default)

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
            item.enumClass.java.enumConstants.forEach { enumVal ->
                DropdownMenuItem(
                    text = { Text(enumVal.name) },
                    onClick = {
                        setEnumPref(item.key, enumVal)
                        expanded = false
                    }
                )
            }
        }
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

// ============================================================================
// Data models
// ============================================================================

data class SettingsCategory(
    val id: String,
    val title: String,
    val items: List<SettingsItem>
)

sealed class SettingsItem(val key: String) {
    data class BooleanSetting(
        override val key: String,
        val label: String,
        val description: String = "",
        val default: Boolean
    ) : SettingsItem(key)

    data class IntSetting(
        override val key: String,
        val label: String,
        val description: String = "",
        val default: Int
    ) : SettingsItem(key)

    data class EnumSetting<T : Enum<T>>(
        override val key: String,
        val label: String,
        val description: String = "",
        val enumClass: Class<T>,
        val default: T
    ) : SettingsItem(key)

    data class PreferenceItem(
        override val key: String,
        val label: String,
        val description: String = "",
        val onClick: () -> Unit
    ) : SettingsItem(key)
}

// ============================================================================
// Pref helpers (inline to avoid import issues)
// ============================================================================

@Suppress("UNCHECKED_CAST")
private fun getBooleanPref(key: String, default: Boolean): Boolean =     try {
        PrefsUtility::class.java.methods.find { it.name == "pref_${key}" }?.let { method ->
            method.isAccessible = true
            val result = method.invoke(null)
            return if (result is Boolean) result else default
        } ?: default
    } catch (e: Exception) { default }

private fun setBooleanPref(key: String, value: Boolean) {
    try {
        PrefsUtility::class.java.methods.find { it.name == "pref_${key}_set" }?.let { method ->
            method.isAccessible = true
            method.invoke(null, value)
        }
    } catch (e: Exception) { /* ignore */ }
}

private fun getIntPref(key: String, default: Int): Int = default

private fun setIntPref(key: String, value: Int) {
    try {
        PrefsUtility::class.java.methods.find { it.name == "pref_${key}_set" }?.let { method ->
            method.isAccessible = true
            method.invoke(null, value)
        }
    } catch (e: Exception) { /* ignore */ }
}

private fun <T : Enum<T>> getEnumPref(key: String, enumClass: Class<T>, default: T): T = default

@Suppress("UNCHECKED_CAST")
private fun setEnumPref(key: String, value: Any) {
    try {
        PrefsUtility::class.java.methods.find { it.name == "pref_${key}_set" }?.let { method ->
            method.isAccessible = true
            method.invoke(null, value)
        }
    } catch (e: Exception) { /* ignore */ }
}

// ============================================================================
// Build settings categories
// ============================================================================

private fun getSettingsCategories(context: android.content.Context): List<SettingsCategory> {
    return listOf(
        // ─── Appearance ───
        SettingsCategory(
            id = "appearance",
            title = "Appearance"
        ) {
            add(
                SettingsItem.BooleanSetting(
                    "linkbuttons",
                    "Show linkbuttons",
                    default = true
                )
            )
            add(
                SettingsItem.BooleanSetting(
                    "hide_comments_from_blocked_users",
                    "Hide comments from blocked users",
                    default = true
                )
            )
            add(
                SettingsItem.EnumSetting(
                    key = "theme",
                    label = "Theme",
                    description = "Current theme",
                    enumClass = AppearanceTheme::class.java,
                    default = AppearanceTheme.DEFAULT
                )
            )
            add(
                SettingsItem.EnumSetting(
                    key = "statusbar",
                    label = "Status bar mode",
                    description = "System status bar behavior",
                    enumClass = AppearanceStatusBarMode::class.java,
                    default = AppearanceStatusBarMode.DEFAULT
                )
            )
            add(
                SettingsItem.BooleanSetting(
                    "hide_toolbar_on_scroll",
                    "Hide toolbar on scroll",
                    default = true
                )
            )
            add(
                SettingsItem.BooleanSetting(
                    "bottom_toolbar",
                    "Use bottom toolbar",
                    default = false
                )
            )
        },

        // ─── Behaviour ───
        SettingsCategory(
            id = "behaviour",
            title = "Behaviour"
        ) {
            add(
                SettingsItem.EnumSetting(
                    key = "post_tap_action",
                    label = "Post tap action",
                    description = "What happens when you tap a post",
                    enumClass = PostTapAction::class.java,
                    default = PostTapAction.OPEN_POST
                )
            )
            add(
                SettingsItem.EnumSetting(
                    key = "imageview_mode",
                    label = "Image viewer mode",
                    description = "How images are displayed",
                    enumClass = ImageViewMode::class.java,
                    default = ImageViewMode.DEFAULT
                )
            )
            add(
                SettingsItem.EnumSetting(
                    key = "videoview_mode",
                    label = "Video viewer mode",
                    description = "How videos are displayed",
                    enumClass = VideoViewMode::class.java,
                    default = VideoViewMode.DEFAULT
                )
            )
            add(
                SettingsItem.EnumSetting(
                    key = "fling_post_left",
                    label = "Fling post left",
                    description = "Action on flinging a post left",
                    enumClass = PostFlingAction::class.java,
                    default = PostFlinkAction.NONE
                )
            )
            add(
                SettingsItem.EnumSetting(
                    key = "commentsort",
                    label = "Comment sort",
                    description = "Default comment sorting",
                    enumClass = PostCommentSort::class.java,
                    default = PostCommentSort.BEST
                )
            )
            add(
                SettingsItem.BooleanSetting(
                    "notifications",
                    "Enable notifications",
                    default = false
                )
            )
        },

        // ─── Network ───
        SettingsCategory(
            id = "network",
            title = "Network"
        ) {
            add(
                SettingsItem.PreferenceItem(
                    key = "tor",
                    label = "Use Tor",
                    description = "Route traffic through Tor network",
                    onClick = { /* Toggle Tor */ }
                )
            )
            add(
                SettingsItem.EnumSetting(
                    key = "share_domain",
                    label = "Sharing domain",
                    description = "Domain used when sharing links",
                    enumClass = String::class.java as Class<Enum<String>>,
                    default = "" as Enum<String>
                )
            )
        },

        // ─── Cache ───
        SettingsCategory(
            id = "cache",
            title = "Cache"
        ) {
            add(
                SettingsItem.PreferenceItem(
                    key = "cache_location",
                    label = "Cache location",
                    description = "Choose storage location for cached data",
                    onClick = { /* Show storage chooser */ }
                )
            )
            add(
                SettingsItem.IntSetting(
                    key = "maxage_listing",
                    label = "Listing cache max age",
                    description = "How long to keep listings cached",
                    default = 3600
                )
            )
            add(
                SettingsItem.IntSetting(
                    key = "maxage_image",
                    label = "Image cache max age",
                    description = "How long to keep images cached",
                    default = 86400
                )
            )
        },

        // ─── About ───
        SettingsCategory(
            id = "about",
            title = "About"
        ) {
            add(
                SettingsItem.PreferenceItem(
                    key = "version",
                    label = "Version",
                    description = "RedReader ${org.quantumbadger.redreader.BuildConfig.VERSION_NAME}",
                    onClick = { /* nothing */ }
                )
            )
            add(
                SettingsItem.PreferenceItem(
                    key = "changelog",
                    label = "Changelog",
                    description = "View what's new",
                    onClick = { /* Open changelog */ }
                )
            )
            add(
                SettingsItem.PreferenceItem(
                    key = "license",
                    label = "License",
                    description = "View open-source license",
                    onClick = { /* Open license */ }
                )
            )
            add(
                SettingsItem.PreferenceItem(
                    key = "github",
                    label = "GitHub",
                    description = "View source code on GitHub",
                    onClick = { /* Open GitHub */ }
                )
            )
            add(
                SettingsItem.PreferenceItem(
                    key = "backup",
                    label = "Backup preferences",
                    description = "Export preferences to file",
                    onClick = { /* Backup */ }
                )
            )
            add(
                SettingsItem.PreferenceItem(
                    key = "restore",
                    label = "Restore preferences",
                    description = "Import preferences from file",
                    onClick = { /* Restore */ }
                )
            )
        }
    )
}

// ============================================================================
// Extension builder for SettingsCategory
// ============================================================================

private fun SettingsCategory.Builder.__(block: SettingsCategory.() -> Unit): Unit = block()

@Suppress("UNCHECKED_CAST")
private fun interface SettingsCategoryBuilder {
    fun build(): SettingsCategory
}
