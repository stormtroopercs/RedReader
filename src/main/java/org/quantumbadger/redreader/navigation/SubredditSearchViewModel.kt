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
import org.quantumbadger.redreader.database.entities.SubredditEntity
import org.quantumbadger.redreader.repository.SubredditRepository
import javax.inject.Inject

/**
 * ViewModel for subreddit search functionality.
 * Manages search state and provides reactive updates via StateFlow.
 */
class SubredditSearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subredditRepository: SubredditRepository
) : ViewModel() {

    sealed class SubredditSearchUiState {
        object Idle : SubredditSearchUiState()
        object Loading : SubredditSearchUiState()
        data class Success(
            val results: List<SubredditEntity>,
            val query: String
        ) : SubredditSearchUiState()
        data class Error(val message: String) : SubredditSearchUiState()
    }

    private val _state = MutableStateFlow<SubredditSearchUiState>(SubredditSearchUiState.Idle)
    val state: StateFlow<SubredditSearchUiState> = _state.asStateFlow()

    /**
     * Search for subreddits matching the query.
     */
    fun searchSubreddits(query: String) {
        if (query.isBlank()) {
            _state.value = SubredditSearchUiState.Idle
            return
        }

        _state.value = SubredditSearchUiState.Loading
        viewModelScope.launch {
            try {
                subredditRepository.searchSubreddits(query).collect { results ->
                    _state.value = SubredditSearchUiState.Success(results, query)
                }
            } catch (e: Exception) {
                _state.value = SubredditSearchUiState.Error("Search failed: ${e.message}")
            }
        }
    }

    /**
     * Clear the current search state.
     */
    fun clearSearch() {
        _state.value = SubredditSearchUiState.Idle
    }

    /**
     * Select a subreddit and navigate to it.
     */
    fun selectSubreddit(subreddit: SubredditEntity): String {
        return subreddit.name
    }
}
