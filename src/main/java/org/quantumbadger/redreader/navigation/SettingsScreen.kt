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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.quantumbadger.redreader.BuildConfig
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.PrefsUtility.AppearanceStatusBarMode
import org.quantumbadger.redreader.common.PrefsUtility.CommentAction
import org.quantumbadger.redreader.common.PrefsUtility.GifViewMode
import org.quantumbadger.redreader.common.PrefsUtility.ImageViewMode
import org.quantumbadger.redreader.common.PrefsUtility.PostFlingAction
import org.quantumbadger.redreader.common.PrefsUtility.PostTapAction
import org.quantumbadger.redreader.common.PrefsUtility.VideoViewMode
import org.quantumbadger.redreader.reddit.PostCommentSort
import org.quantumbadger.redreader.settings.types.AppearanceTheme

/**
 * Settings screen composable — real data from PrefsUtility.
 * Shows categorized settings grouped by Appearance, Behaviour, Network, Cache, and About.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
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
        SettingsContent(modifier = Modifier.padding(paddingValues))
    }
}

/**
 * Settings content with grouped preferences.
 */
@Composable
private fun SettingsContent(
    modifier: Modifier = Modifier
) {
    val settings = getSettingsCategories()

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
}

// ============================================================================
// Build settings categories (wired to real PrefsUtility getters/setters)
// ============================================================================

private fun getSettingsCategories(): List<SettingsCategory> {
    return listOf(
        // ─── Appearance ───
        SettingsCategory(
            id = "appearance",
            title = "Appearance",
            items = listOf(
                SettingsItem.BooleanSetting(
                    key = "linkbuttons",
                    label = "Show link buttons",
                    description = "Show buttons for common actions on posts",
                    get = { PrefsUtility.pref_appearance_linkbuttons() },
                    set = PrefsUtility::pref_appearance_linkbuttons_set
                ),
                SettingsItem.BooleanSetting(
                    key = "hide_comments_from_blocked_users",
                    label = "Hide comments from blocked users",
                    description = "Hide comments made by users you have blocked",
                    get = { PrefsUtility.pref_appearance_hide_comments_from_blocked_users() },
                    set = PrefsUtility::pref_appearance_hide_comments_from_blocked_users_set
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

        // ─── Behaviour ───
        SettingsCategory(
            id = "behaviour",
            title = "Behaviour",
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
                    key = "imageview_mode",
                    label = "Image viewer mode",
                    description = "How images are displayed",
                    entries = ImageViewMode.entries,
                    get = { PrefsUtility.pref_behaviour_imageview_mode() },
                    set = PrefsUtility::pref_behaviour_imageview_mode_set
                ),
                SettingsItem.EnumSetting(
                    key = "videoview_mode",
                    label = "Video viewer mode",
                    description = "How videos are displayed",
                    entries = VideoViewMode.entries,
                    get = { PrefsUtility.pref_behaviour_videoview_mode() },
                    set = PrefsUtility::pref_behaviour_videoview_mode_set
                ),
                SettingsItem.EnumSetting(
                    key = "gifview_mode",
                    label = "GIF viewer mode",
                    description = "How animated images are displayed",
                    entries = GifViewMode.entries,
                    get = { PrefsUtility.pref_behaviour_gifview_mode() },
                    set = PrefsUtility::pref_behaviour_gifview_mode_set
                ),
                SettingsItem.EnumSetting(
                    key = "fling_post_left",
                    label = "Fling post left",
                    description = "Action when swiping a post to the left",
                    entries = PostFlingAction.entries,
                    get = { PrefsUtility.pref_behaviour_fling_post_left() },
                    set = PrefsUtility::pref_behaviour_fling_post_left_set
                ),
                SettingsItem.EnumSetting(
                    key = "fling_post_right",
                    label = "Fling post right",
                    description = "Action when swiping a post to the right",
                    entries = PostFlingAction.entries,
                    get = { PrefsUtility.pref_behaviour_fling_post_right() },
                    set = PrefsUtility::pref_behaviour_fling_post_right_set
                ),
                SettingsItem.EnumSetting(
                    key = "comment_tap",
                    label = "Comment tap action",
                    description = "What happens when you tap a comment",
                    entries = CommentAction.entries,
                    get = { PrefsUtility.pref_behaviour_actions_comment_tap() },
                    set = PrefsUtility::pref_behaviour_actions_comment_tap_set
                ),
                SettingsItem.EnumSetting(
                    key = "comment_longclick",
                    label = "Comment long-press action",
                    description = "What happens when you long-press a comment",
                    entries = CommentAction.entries,
                    get = { PrefsUtility.pref_behaviour_actions_comment_longclick() },
                    set = PrefsUtility::pref_behaviour_actions_comment_longclick_set
                ),
                SettingsItem.EnumSetting(
                    key = "commentsort",
                    label = "Comment sort",
                    description = "Default comment sorting",
                    entries = PostCommentSort.entries,
                    get = { PrefsUtility.pref_behaviour_commentsort() },
                    set = PrefsUtility::pref_behaviour_commentsort_set
                ),
                SettingsItem.BooleanSetting(
                    key = "notifications",
                    label = "Enable notifications",
                    description = "Show notifications for new activity",
                    get = { PrefsUtility.pref_behaviour_notifications() },
                    set = PrefsUtility::set_pref_behaviour_notifications
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
                    onClick = { /* no-op */ }
                ),
                SettingsItem.PreferenceItem(
                    key = "changelog",
                    label = "Changelog",
                    description = "View what's new",
                    onClick = { /* TODO: open changelog */ }
                ),
                SettingsItem.PreferenceItem(
                    key = "license",
                    label = "License",
                    description = "View open-source license",
                    onClick = { /* TODO: open license */ }
                ),
                SettingsItem.PreferenceItem(
                    key = "github",
                    label = "GitHub",
                    description = "View source code on GitHub",
                    onClick = { /* TODO: open GitHub */ }
                )
            )
        )
    )
}
