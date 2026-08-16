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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.NeverAlwaysOrWifiOnly
import org.quantumbadger.redreader.reddit.PostCommentSort
import org.quantumbadger.redreader.reddit.PostSort
import org.quantumbadger.redreader.settings.types.AppearanceTheme
import javax.inject.Inject

/**
 * ViewModel for Settings screen.
 * Manages settings state and provides reactive updates via StateFlow.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    sealed class SettingsUiState {
        object Loading : SettingsUiState()
        data class Ready(
            val linkButtons: Boolean,
            val hideCommentsFromBlockedUsers: Boolean,
            val theme: AppearanceTheme,
            val imageQuality: NeverAlwaysOrWifiOnly,
            val postTapAction: PrefsUtility.PostTapAction,
            val commentTapAction: PrefsUtility.CommentAction,
            val postSort: PostSort,
            val commentSort: PostCommentSort,
            val imageViewerMode: PrefsUtility.ImageViewMode,
            val albumViewerMode: PrefsUtility.AlbumViewMode,
            val statusBarMode: PrefsUtility.AppearanceStatusBarMode
        ) : SettingsUiState()
        data class Error(val message: String) : SettingsUiState()
    }

    private val _state = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                _state.value = SettingsUiState.Ready(
                    linkButtons = PrefsUtility.pref_appearance_linkbuttons(),
                    hideCommentsFromBlockedUsers = PrefsUtility.pref_appearance_hide_comments_from_blocked_users(),
                    theme = PrefsUtility.appearance_theme(),
                    imageQuality = PrefsUtility.images_high_res_thumbnails(),
                    postTapAction = PrefsUtility.pref_behaviour_post_tap_action(),
                    commentTapAction = PrefsUtility.pref_behaviour_actions_comment_tap(),
                    postSort = PrefsUtility.pref_behaviour_postsort(),
                    commentSort = PrefsUtility.pref_behaviour_commentsort(),
                    imageViewerMode = PrefsUtility.pref_behaviour_imageview_mode(),
                    albumViewerMode = PrefsUtility.pref_behaviour_albumview_mode(),
                    statusBarMode = PrefsUtility.pref_appearance_android_status()
                )
            } catch (e: Exception) {
                _state.value = SettingsUiState.Error("Failed to load settings: ${e.message}")
            }
        }
    }

    fun setLinkButtons(enabled: Boolean) {
        PrefsUtility.pref_appearance_linkbuttons_set(enabled)
        updateState { ready ->
            ready.copy(linkButtons = enabled)
        }
    }

    fun setHideCommentsFromBlockedUsers(enabled: Boolean) {
        PrefsUtility.pref_appearance_hide_comments_from_blocked_users_set(enabled)
        updateState { ready ->
            ready.copy(hideCommentsFromBlockedUsers = enabled)
        }
    }

    fun setTheme(theme: AppearanceTheme) {
        PrefsUtility.appearance_theme_set(theme)
        updateState { ready ->
            ready.copy(theme = theme)
        }
    }

    fun setImageQuality(quality: NeverAlwaysOrWifiOnly) {
        PrefsUtility.images_high_res_thumbnails_set(quality)
        updateState { ready ->
            ready.copy(imageQuality = quality)
        }
    }

    fun setPostTapAction(action: PrefsUtility.PostTapAction) {
        PrefsUtility.pref_behaviour_post_tap_action_set(action)
        updateState { ready ->
            ready.copy(postTapAction = action)
        }
    }

    fun setCommentTapAction(action: PrefsUtility.CommentAction) {
        PrefsUtility.pref_behaviour_actions_comment_tap_set(action)
        updateState { ready ->
            ready.copy(commentTapAction = action)
        }
    }

    fun setPostSort(sort: PostSort) {
        PrefsUtility.pref_behaviour_postsort_set(sort)
        updateState { ready ->
            ready.copy(postSort = sort)
        }
    }

    fun setCommentSort(sort: PostCommentSort) {
        PrefsUtility.pref_behaviour_commentsort_set(sort)
        updateState { ready ->
            ready.copy(commentSort = sort)
        }
    }

    fun setImageViewerMode(mode: PrefsUtility.ImageViewMode) {
        PrefsUtility.pref_behaviour_imageview_mode_set(mode)
        updateState { ready ->
            ready.copy(imageViewerMode = mode)
        }
    }

    fun setAlbumViewerMode(mode: PrefsUtility.AlbumViewMode) {
        PrefsUtility.pref_behaviour_albumview_mode_set(mode)
        updateState { ready ->
            ready.copy(albumViewerMode = mode)
        }
    }

    fun setStatusBarMode(mode: PrefsUtility.AppearanceStatusBarMode) {
        PrefsUtility.pref_appearance_android_status_set(mode)
        updateState { ready ->
            ready.copy(statusBarMode = mode)
        }
    }

    private fun updateState(transform: (SettingsUiState.Ready) -> SettingsUiState.Ready) {
        val currentState = _state.value
        if (currentState is SettingsUiState.Ready) {
            _state.value = transform(currentState)
        }
    }
}
