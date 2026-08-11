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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.quantumbadger.redreader.common.PrefsUtility
import javax.inject.Inject

/**
 * ViewModel for Settings screen.
 * Manages settings state and provides reactive updates via StateFlow.
 */
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    sealed class SettingsUiState {
        object Loading : SettingsUiState()
        data class Ready(
            val linkButtons: Boolean,
            val hideCommentsFromBlockedUsers: Boolean,
            val theme: PrefsUtility.AppearanceTheme,
            val imageQuality: PrefsUtility.ImageQuality,
            val videoQuality: PrefsUtility.VideoQuality,
            val gifQuality: PrefsUtility.GifQuality,
            val postTapAction: PrefsUtility.PostTapAction,
            val commentTapAction: PrefsUtility.CommentTapAction,
            val postSort: PrefsUtility.PostSort,
            val commentSort: PrefsUtility.PostCommentSort,
            val imageViewerMode: PrefsUtility.ImageViewerMode,
            val albumViewerMode: PrefsUtility.AlbumViewerMode,
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
                    linkButtons = PrefsUtility.prefLinkbuttons(context),
                    hideCommentsFromBlockedUsers = PrefsUtility.prefHideCommentsFromBlockedUsers(context),
                    theme = PrefsUtility.prefTheme(context),
                    imageQuality = PrefsUtility.prefImageQuality(context),
                    videoQuality = PrefsUtility.prefVideoQuality(context),
                    gifQuality = PrefsUtility.prefGifQuality(context),
                    postTapAction = PrefsUtility.prefPostTapAction(context),
                    commentTapAction = PrefsUtility.prefCommentTapAction(context),
                    postSort = PrefsUtility.prefPostsSort(context),
                    commentSort = PrefsUtility.prefCommentsSort(context),
                    imageViewerMode = PrefsUtility.prefImageViewerMode(context),
                    albumViewerMode = PrefsUtility.prefAlbumViewerMode(context),
                    statusBarMode = PrefsUtility.prefStatusBarMode(context)
                )
            } catch (e: Exception) {
                _state.value = SettingsUiState.Error("Failed to load settings: ${e.message}")
            }
        }
    }

    fun setLinkButtons(enabled: Boolean) {
        PrefsUtility.prefLinkbuttons_set(context, enabled)
        updateState { ready ->
            ready.copy(linkButtons = enabled)
        }
    }

    fun setHideCommentsFromBlockedUsers(enabled: Boolean) {
        PrefsUtility.prefHideCommentsFromBlockedUsers_set(context, enabled)
        updateState { ready ->
            ready.copy(hideCommentsFromBlockedUsers = enabled)
        }
    }

    fun setTheme(theme: PrefsUtility.AppearanceTheme) {
        PrefsUtility.prefTheme_set(context, theme)
        updateState { ready ->
            ready.copy(theme = theme)
        }
    }

    fun setImageQuality(quality: PrefsUtility.ImageQuality) {
        PrefsUtility.prefImageQuality_set(context, quality)
        updateState { ready ->
            ready.copy(imageQuality = quality)
        }
    }

    fun setVideoQuality(quality: PrefsUtility.VideoQuality) {
        PrefsUtility.prefVideoQuality_set(context, quality)
        updateState { ready ->
            ready.copy(videoQuality = quality)
        }
    }

    fun setGifQuality(quality: PrefsUtility.GifQuality) {
        PrefsUtility.prefGifQuality_set(context, quality)
        updateState { ready ->
            ready.copy(gifQuality = quality)
        }
    }

    fun setPostTapAction(action: PrefsUtility.PostTapAction) {
        PrefsUtility.prefPostTapAction_set(context, action)
        updateState { ready ->
            ready.copy(postTapAction = action)
        }
    }

    fun setCommentTapAction(action: PrefsUtility.CommentTapAction) {
        PrefsUtility.prefCommentTapAction_set(context, action)
        updateState { ready ->
            ready.copy(commentTapAction = action)
        }
    }

    fun setPostSort(sort: PrefsUtility.PostSort) {
        PrefsUtility.prefPostsSort_set(context, sort)
        updateState { ready ->
            ready.copy(postSort = sort)
        }
    }

    fun setCommentSort(sort: PrefsUtility.PostCommentSort) {
        PrefsUtility.prefCommentsSort_set(context, sort)
        updateState { ready ->
            ready.copy(commentSort = sort)
        }
    }

    fun setImageViewerMode(mode: PrefsUtility.ImageViewerMode) {
        PrefsUtility.prefImageViewerMode_set(context, mode)
        updateState { ready ->
            ready.copy(imageViewerMode = mode)
        }
    }

    fun setAlbumViewerMode(mode: PrefsUtility.AlbumViewerMode) {
        PrefsUtility.prefAlbumViewerMode_set(context, mode)
        updateState { ready ->
            ready.copy(albumViewerMode = mode)
        }
    }

    fun setStatusBarMode(mode: PrefsUtility.AppearanceStatusBarMode) {
        PrefsUtility.prefStatusBarMode_set(context, mode)
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
