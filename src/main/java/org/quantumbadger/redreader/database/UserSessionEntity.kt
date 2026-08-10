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

package org.quantumbadger.redreader.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a user session for offline access.
 * Mirrors key fields from RedditUser for caching and local lookup.
 */
@Entity(tableName = "user_sessions")
data class UserSessionEntity(
    @PrimaryKey val name: String,
    val iconUrl: String? = null,
    val linkKarma: Long? = null,
    val commentKarma: Long? = null,
    val hasMail: Boolean? = null,
    val requiresCamera: Boolean? = null,
    val requiresVideo: Boolean? = null,
    val goldHelpfulness: Float? = null,
    val totalKarma: Long? = null,
    val createdUtc: Long? = null,
    val isEmployee: Boolean? = null,
    val isGold: Boolean? = null,
    val isMod: Boolean? = null,
    val hasStripeAccount: Boolean? = null,
    val hideFromRobots: Boolean? = null,
    val over18: Boolean? = null,
    val prefsLanguage: String? = null,
    val prefsThreadsStyle: String? = null,
    val prefsHideUpvoteRatio: Boolean? = null,
    val prefsThreadSort: String? = null,
    val prefsFrontPage: String? = null,
    val prefsPostSort: String? = null,
    val prefsShowLowKarmaPosts: Boolean? = null,
    val prefsShowLowKarmaComments: Boolean? = null,
    val prefsShowNewCommentExpando: Boolean? = null,
    val prefsShowExpandedFlairs: Boolean? = null,
    val prefsShowSnoovatar: Boolean? = null,
    val prefsBlurNSFWImages: Boolean? = null,
    val prefsBlurNSFWVideos: Boolean? = null,
    val prefsClickoverBehavior: String? = null,
    val prefsCommentSort: String? = null,
    val prefsDefaultImageSize: String? = null,
    val prefsImageSize: String? = null,
    val prefsPostLinksSize: String? = null,
    val prefsShowDefaultImages: Boolean? = null,
    val prefsUseGlobalBotsList: Boolean? = null,
    val prefsVideoStreamOnlyAudio: Boolean? = null,
    val prefsVideoStreamVideoQuality: String? = null,
    val prefsVideoStreamUseHLS: Boolean? = null,
    val prefsVideoStreamVideoBitrate: String? = null,
    val prefsVideoStreamVideoFps: String? = null,
    val prefsVideoStreamVideoResolution: String? = null,
    val prefsVideoStreamVideoCodecs: String? = null,
    val prefsVideoStreamVideoContainer: String? = null,
    val prefsVideoStreamVideoMimeType: String? = null,
    val prefsVideoStreamVideoAudioMimeType: String? = null,
    val prefsVideoStreamVideoAudioBitrate: String? = null,
    val prefsVideoStreamVideoAudioCodec: String? = null,
    val prefsVideoStreamVideoAudioChannels: String? = null,
    val prefsVideoStreamVideoAudioSampleRate: String? = null,
    val prefsVideoStreamVideoAudioSampleSize: String? = null,
    val prefsVideoStreamVideoAudioAudioEncoding: String? = null,
    val prefsVideoStreamVideoVideoEncoding: String? = null,
    val prefsVideoStreamVideoVideoWidth: String? = null,
    val prefsVideoStreamVideoVideoHeight: String? = null,
    val prefsVideoStreamVideoVideoFps: String? = null,
    val prefsVideoStreamVideoVideoBitrate: String? = null,
    val prefsVideoStreamVideoVideoCodecs: String? = null,
    val prefsVideoStreamVideoVideoContainer: String? = null,
    val prefsVideoStreamVideoVideoMimeType: String? = null,
    val prefsVideoStreamVideoAudioEnabled: Boolean? = null,
    val prefsVideoStreamVideoAudioOnly: Boolean? = null,
    val prefsVideoStreamVideoHLS: Boolean? = null,
    val prefsVideoStreamVideoQuality: String? = null,
    val prefsVideoStreamVideoBitrate: String? = null,
    val prefsVideoStreamVideoFps: String? = null,
    val prefsVideoStreamVideoResolution: String? = null,
    val prefsVideoStreamVideoCodecs: String? = null,
    val prefsVideoStreamVideoContainer: String? = null,
    val prefsVideoStreamVideoMimeType: String? = null,
    val prefsVideoStreamVideoAudioMimeType: String? = null,
    val prefsVideoStreamVideoAudioBitrate: String? = null,
    val prefsVideoStreamVideoAudioCodec: String? = null,
    val prefsVideoStreamVideoAudioChannels: String? = null,
    val prefsVideoStreamVideoAudioSampleRate: String? = null,
    val prefsVideoStreamVideoAudioSampleSize: String? = null,
    val prefsVideoStreamVideoAudioAudioEncoding: String? = null,
    val prefsVideoStreamVideoVideoEncoding: String? = null,
    val prefsVideoStreamVideoVideoWidth: String? = null,
    val prefsVideoStreamVideoVideoHeight: String? = null,
    val prefsVideoStreamVideoVideoFps: String? = null,
    val prefsVideoStreamVideoVideoBitrate: String? = null,
    val prefsVideoStreamVideoVideoCodecs: String? = null,
    val prefsVideoStreamVideoVideoContainer: String? = null,
    val prefsVideoStreamVideoVideoMimeType: String? = null,
    val prefsVideoStreamVideoAudioEnabled: Boolean? = null,
    val prefsVideoStreamVideoAudioOnly: Boolean? = null,
    val prefsVideoStreamVideoHLS: Boolean? = null,
    val prefsVideoStreamVideoQuality: String? = null,
    val prefsVideoStreamVideoBitrate: String? = null,
    val prefsVideoStreamVideoFps: String? = null,
    val prefsVideoStreamVideoResolution: String? = null,
    val prefsVideoStreamVideoCodecs: String? = null,
    val prefsVideoStreamVideoContainer: String? = null,
    val prefsVideoStreamVideoMimeType: String? = null,
    val lastSessionTime: Long? = null,
    val isAuthenticated: Boolean? = null,
    val authToken: String? = null,
    val refreshToken: String? = null
)
