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

package com.stormtroopercs.materialreader.compose.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.stormtroopercs.materialreader.R
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.common.datastream.parseDataUri
import com.stormtroopercs.materialreader.compose.net.FileRequestResult
import com.stormtroopercs.materialreader.compose.net.NetRequestStatus
import com.stormtroopercs.materialreader.compose.net.fetchImage
import com.stormtroopercs.materialreader.navigation.UserProfileViewModel
import com.stormtroopercs.materialreader.reddit.things.RedditUser
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Compose User Profile Screen.
 * Full-screen user profile with karma, badges, avatar, account age and the
 * same actions the legacy user-profile dialog offered: view posts / comments,
 * send a message, more-info, and block / unblock (via the Reddit API, with a
 * re-login offer when the block permission is denied).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
	username: String,
	onNavigateBack: () -> Unit,
	onNavigateToPosts: () -> Unit,
	onNavigateToComments: () -> Unit,
	onSendMessage: () -> Unit,
	onSignOut: () -> Unit = {},
	onReLogin: () -> Unit = {},
	viewModel: UserProfileViewModel = hiltViewModel(),
) {
	val state by viewModel.state.collectAsState()
	val context = LocalContext.current

	// Raw RedditUser shown by the "more info" dialog (compose UserPropertiesDialog).
	var moreInfoUser by remember { mutableStateOf<RedditUser?>(null) }

	// Transient block/unblock feedback surfaces.
	var permissionDeniedDialog by remember { mutableStateOf(false) }
	var failureDialogError by remember { mutableStateOf<RRError?>(null) }

	LaunchedEffect(username) {
		viewModel.loadProfile(username)
	}

	// OAuth re-login (block permission denied): the host navigates to the
	// in-app OAuth route (the 50th increment retired the legacy
	// OAuthLoginActivity + result-code-123 handoff).

	// Surface a block/unblock feedback event exactly once, then clear it.
	LaunchedEffect(viewModel.blockFeedback.value) {
		val feedback = viewModel.blockFeedback.value ?: return@LaunchedEffect
		viewModel.clearBlockFeedback()
		when (feedback) {
			UserProfileViewModel.BlockFeedback.PermissionDenied -> permissionDeniedDialog = true
			is UserProfileViewModel.BlockFeedback.Failure -> failureDialogError = feedback.error
		}
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = {
					Text(text = "u/$username")
				},
				navigationIcon = {
					IconButton(onClick = onNavigateBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Default.ArrowBack,
							contentDescription = "Back",
						)
					}
				},
			)
		},
	) { paddingValues ->
		when (val uiState = state) {
			is UserProfileViewModel.UserProfileUiState.Loading -> {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.padding(paddingValues),
					contentAlignment = Alignment.Center,
				) {
					Column(
						horizontalAlignment = Alignment.CenterHorizontally,
					) {
						CircularProgressIndicator()
						Spacer(modifier = Modifier.height(16.dp))
						Text("Loading user profile...")
					}
				}
			}

			is UserProfileViewModel.UserProfileUiState.Ready -> {
				UserProfileContent(
					uiState = uiState,
					viewModel = viewModel,
					context = context,
					onMoreInfo = { moreInfoUser = it },
					onNavigateToPosts = onNavigateToPosts,
					onNavigateToComments = onNavigateToComments,
					onSendMessage = onSendMessage,
					onSignOut = onSignOut,
					modifier = Modifier
						.fillMaxSize()
						.padding(paddingValues),
				)
			}

			is UserProfileViewModel.UserProfileUiState.Error -> {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.padding(paddingValues),
					contentAlignment = Alignment.Center,
				) {
					Column(
						horizontalAlignment = Alignment.CenterHorizontally,
					) {
						Text(
							text = uiState.message,
							color = MaterialTheme.colorScheme.error,
							style = MaterialTheme.typography.bodyLarge,
						)
						Spacer(modifier = Modifier.height(16.dp))
						Button(onClick = { viewModel.loadProfile(username) }) {
							Text("Retry")
						}
					}
				}
			}
		}
	}

	if (permissionDeniedDialog) {
		AlertDialog(
			onDismissRequest = { permissionDeniedDialog = false },
			title = { Text(stringResource(R.string.block_permission_denied_title)) },
			text = { Text(stringResource(R.string.block_permission_denied_message)) },
			confirmButton = {
				TextButton(
					onClick = {
						permissionDeniedDialog = false
						onReLogin()
					},
				) { Text(stringResource(R.string.block_permission_denied_relogin)) }
			},
			dismissButton = {
				TextButton(onClick = { permissionDeniedDialog = false }) {
					Text(stringResource(R.string.dialog_cancel))
				}
			},
		)
	}

	failureDialogError?.let { error ->
		AlertDialog(
			onDismissRequest = { failureDialogError = null },
			title = { Text(error.title ?: "Error") },
			text = { Text(error.message ?: "The request failed. Please try again.") },
			confirmButton = {
				TextButton(onClick = { failureDialogError = null }) { Text("OK") }
			},
		)
	}

	moreInfoUser?.let { user ->
		UserPropertiesDialog(user = user, onDismiss = { moreInfoUser = null })
	}
}

@Composable
private fun UserProfileContent(
	uiState: UserProfileViewModel.UserProfileUiState.Ready,
	viewModel: UserProfileViewModel,
	context: Context,
	onMoreInfo: (RedditUser) -> Unit,
	onNavigateToPosts: () -> Unit,
	onNavigateToComments: () -> Unit,
	onSendMessage: () -> Unit,
	onSignOut: () -> Unit,
	modifier: Modifier = Modifier,
) {
	LazyColumn(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(16.dp),
		contentPadding = PaddingValues(16.dp),
	) {
		// User header with avatar
		item {
			UserHeader(uiState = uiState, onMoreInfo = onMoreInfo)
		}

		// Karma summary
		item {
			KarmaSummary(uiState)
		}

		// Account badges
		item {
			AccountBadges(uiState)
		}

		// Quick actions
		item {
			QuickActions(
				canMessage = uiState.canMessage,
				onNavigateToPosts = onNavigateToPosts,
				onNavigateToComments = onNavigateToComments,
				onSendMessage = onSendMessage,
			)
		}

		// Block / unblock (signed in, and not viewing your own profile)
		if (uiState.canBlock) {
			item {
				BlockActions(
					uiState = uiState,
					viewModel = viewModel,
					context = context,
				)
			}
		}

		// Sign out (your own profile, signed in)
		if (uiState.isSelf && uiState.canMessage) {
			item {
				SignOutCard(onSignOut = onSignOut)
			}
		}

		// Account details
		item {
			AccountDetails(uiState)
		}
	}
}

@Composable
private fun UserHeader(
	uiState: UserProfileViewModel.UserProfileUiState.Ready,
	onMoreInfo: (RedditUser) -> Unit,
) {
	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.primaryContainer,
		),
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(24.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			// Avatar
			UserAvatar(uiState.iconUrl)

			Spacer(modifier = Modifier.height(16.dp))

			// Username
			Text(
				text = uiState.username,
				style = MaterialTheme.typography.headlineMedium,
				fontWeight = FontWeight.Bold,
			)

			Spacer(modifier = Modifier.height(4.dp))

			// Account type
			Text(
				text = uiState.accountType,
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)

			uiState.accountAge?.let { age ->
				Spacer(modifier = Modifier.height(4.dp))
				Text(
					text = age,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}

			uiState.latestUser?.let { user ->
				Spacer(modifier = Modifier.height(8.dp))
				Text(
					text = "More info",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.clickable { onMoreInfo(user) },
				)
			}
		}
	}
}

@Composable
private fun UserAvatar(iconUrl: String?) {
	// Two sources: modern Reddit returns the account picture as a base64
	// data URI (in `icon`) — decoded in memory, no network; the legacy
	// `icon_img` is a plain URL and goes through the fetch pipeline.
	val parsed = remember(iconUrl) {
		iconUrl?.takeIf { it.isNotEmpty() }?.let { parseDataUri(it) }
	}
	val uri = remember(iconUrl) {
		if (parsed == null) iconUrl?.takeIf { it.isNotEmpty() }?.let { UriString(it) } else null
	}

	val imageState: State<NetRequestStatus<FileRequestResult<ImageBitmap>>>? =
		if (uri != null) fetchImage(uri, scaleToMaxAxis = 256) else null

	val dataUriBitmap: ImageBitmap? = parsed?.let { decodeDataUriImage(it.bytes, 256) }

	Box(
		modifier = Modifier
			.size(80.dp)
			.clip(CircleShape),
		contentAlignment = Alignment.Center,
	) {
		when {
			dataUriBitmap != null -> {
				Image(
					bitmap = dataUriBitmap,
					contentDescription = "User avatar",
					modifier = Modifier
						.size(80.dp)
						.clip(CircleShape),
				)
			}
			imageState == null -> AvatarPlaceholder()
			imageState.value is NetRequestStatus.Success -> {
				val bitmap = (imageState.value as NetRequestStatus.Success).result.data
				Image(
					bitmap = bitmap,
					contentDescription = "User avatar",
					modifier = Modifier
						.size(80.dp)
						.clip(CircleShape),
				)
			}
			else -> AvatarPlaceholder()
		}
	}
}

/**
 * Decodes a data-URI payload to an [ImageBitmap], downscaled so its longest
 * axis is at most [maxAxis] px (an avatar is shown at 80dp — the oversized
 * decode is only a transient memory cost of a few hundred KB).
 */
private fun decodeDataUriImage(bytes: ByteArray, maxAxis: Int = 256): ImageBitmap? {
	return try {
		val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
		val result = if (max(decoded.width, decoded.height) <= maxAxis) {
			decoded
		} else {
			val scale = maxAxis / max(decoded.width, decoded.height).toFloat()
			decoded.scale(
				(decoded.width * scale).roundToInt(),
				(decoded.height * scale).roundToInt(),
			)
		}
		result.asImageBitmap()
	} catch (e: Exception) {
		null
	}
}

@Composable
private fun AvatarPlaceholder() {
	Surface(
		modifier = Modifier.fillMaxSize(),
		color = MaterialTheme.colorScheme.secondaryContainer,
	) {
		Icon(
			imageVector = Icons.Default.Person,
			contentDescription = "User avatar",
			modifier = Modifier.size(48.dp),
			tint = MaterialTheme.colorScheme.onSecondaryContainer,
		)
	}
}

@Composable
private fun KarmaSummary(uiState: UserProfileViewModel.UserProfileUiState.Ready) {
	Card(
		modifier = Modifier.fillMaxWidth(),
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			Text(
				text = "Karma",
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold,
			)

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceAround,
			) {
				KarmaCard(
					label = "Total",
					value = uiState.karma,
				)
				KarmaCard(
					label = "Post",
					value = uiState.linkKarma,
				)
				KarmaCard(
					label = "Comment",
					value = uiState.commentKarma,
				)
			}
		}
	}
}

@Composable
private fun KarmaCard(label: String, value: Int) {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Text(
			text = value.toString(),
			style = MaterialTheme.typography.headlineMedium,
			fontWeight = FontWeight.Bold,
			color = MaterialTheme.colorScheme.onSurface,
		)
		Text(
			text = label,
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)
	}
}

@Composable
private fun AccountBadges(uiState: UserProfileViewModel.UserProfileUiState.Ready) {
	Card(
		modifier = Modifier.fillMaxWidth(),
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp),
		) {
			Text(
				text = "Account Badges",
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.SemiBold,
			)

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				if (uiState.isGold) {
					BadgeChip(
						icon = Icons.Default.Star,
						label = "Gold",
						color = Color(0xFFFFD700),
					)
				}
				if (uiState.isMod) {
					BadgeChip(
						icon = Icons.Default.VerifiedUser,
						label = "Mod",
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}
				if (uiState.isEmployee) {
					BadgeChip(
						icon = Icons.Default.Badge,
						label = "Employee",
						color = MaterialTheme.colorScheme.secondary,
					)
				}
				if (uiState.isSuspended) {
					BadgeChip(
						icon = Icons.Default.Block,
						label = "Suspended",
						color = MaterialTheme.colorScheme.error,
					)
				}
				if (uiState.isFriend) {
					BadgeChip(
						icon = Icons.Default.Star,
						label = "Friend",
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}
				if (uiState.isSelf) {
					BadgeChip(
						icon = Icons.Default.Person,
						label = "You",
						color = MaterialTheme.colorScheme.onSurface,
					)
				}
			}
		}
	}
}

@Composable
private fun BadgeChip(
	icon: androidx.compose.ui.graphics.vector.ImageVector,
	label: String,
	color: Color,
) {
	Row(
		modifier = Modifier
			.background(color.copy(alpha = 0.2f), CircleShape)
			.padding(horizontal = 12.dp, vertical = 6.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Icon(
			imageVector = icon,
			contentDescription = null,
			tint = color,
			modifier = Modifier.size(16.dp),
		)
		Spacer(modifier = Modifier.width(4.dp))
		Text(
			text = label,
			style = MaterialTheme.typography.bodySmall,
			color = color,
			fontWeight = FontWeight.Medium,
		)
	}
}

@Composable
private fun QuickActions(
	canMessage: Boolean,
	onNavigateToPosts: () -> Unit,
	onNavigateToComments: () -> Unit,
	onSendMessage: () -> Unit,
) {
	Card(
		modifier = Modifier.fillMaxWidth(),
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp),
		) {
			Text(
				text = "Actions",
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.SemiBold,
				modifier = Modifier.padding(bottom = 8.dp),
			)

			// View posts
			ActionRow(
				icon = Icons.Default.Person,
				label = "View Posts",
				onClick = onNavigateToPosts,
			)

			HorizontalDivider()

			// View comments
			ActionRow(
				icon = Icons.AutoMirrored.Filled.Comment,
				label = "View Comments",
				onClick = onNavigateToComments,
			)

			// Send message (signed in only, mirroring the legacy dialog)
			if (canMessage) {
				HorizontalDivider()
				ActionRow(
					icon = Icons.Default.Email,
					label = "Send Message",
					onClick = onSendMessage,
				)
			}
		}
	}
}

@Composable
private fun BlockActions(
	uiState: UserProfileViewModel.UserProfileUiState.Ready,
	viewModel: UserProfileViewModel,
	context: Context,
) {
	var confirmBlock by remember { mutableStateOf(false) }

	Card(
		modifier = Modifier.fillMaxWidth(),
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween,
		) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Icon(
					imageVector = Icons.Default.Block,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.error,
					modifier = Modifier.size(24.dp),
				)
				Spacer(modifier = Modifier.width(12.dp))
				Text(
					text = if (uiState.isBlocked) {
						"Blocked"
					} else {
						stringResource(R.string.userprofile_button_block)
					},
					style = MaterialTheme.typography.titleMedium,
					color = MaterialTheme.colorScheme.error,
				)
			}

			if (uiState.isBlocked) {
				Button(
					onClick = {
						(context as? AppCompatActivity)?.let {
							viewModel.unblockUser(it, uiState.username)
						}
					},
				) {
					Text(stringResource(R.string.userprofile_button_unblock))
				}
			} else {
				Button(
					onClick = { confirmBlock = true },
				) {
					Text(stringResource(R.string.userprofile_button_block))
				}
			}
		}
	}

	if (confirmBlock) {
		AlertDialog(
			onDismissRequest = { confirmBlock = false },
			title = { Text(stringResource(R.string.block_confirmation)) },
			text = { Text(stringResource(R.string.are_you_sure_block_user)) },
			confirmButton = {
				TextButton(
					onClick = {
						confirmBlock = false
						(context as? AppCompatActivity)?.let {
							viewModel.blockUser(it, uiState.username)
						}
					},
				) { Text(stringResource(R.string.dialog_yes)) }
			},
			dismissButton = {
				TextButton(onClick = { confirmBlock = false }) {
					Text(stringResource(R.string.dialog_cancel))
				}
			},
		)
	}
}

@Composable
private fun ActionRow(
	icon: androidx.compose.ui.graphics.vector.ImageVector,
	label: String,
	onClick: () -> Unit,
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick)
			.padding(vertical = 12.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Icon(
			imageVector = icon,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.primary,
			modifier = Modifier.size(24.dp),
		)
		Spacer(modifier = Modifier.width(16.dp))
		Text(
			text = label,
			style = MaterialTheme.typography.bodyLarge,
		)
	}
}

@Composable
private fun SignOutCard(
	onSignOut: () -> Unit,
) {
	var confirmSignOut by remember { mutableStateOf(false) }

	Card(
		modifier = Modifier.fillMaxWidth(),
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp)
				.clickable { confirmSignOut = true },
			verticalAlignment = Alignment.CenterVertically,
		) {
			Icon(
				imageVector = Icons.AutoMirrored.Filled.Logout,
				contentDescription = "Sign out",
				tint = MaterialTheme.colorScheme.error,
				modifier = Modifier.size(24.dp),
			)
			Spacer(modifier = Modifier.width(12.dp))
			Text(
				text = "Sign out",
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.error,
			)
		}
	}

	if (confirmSignOut) {
		AlertDialog(
			onDismissRequest = { confirmSignOut = false },
			title = { Text("Sign out of MaterialReader?") },
			text = { Text("Your Reddit account will be removed from this device. You can sign in again at any time.") },
			confirmButton = {
				TextButton(
					onClick = {
						confirmSignOut = false
						onSignOut()
					},
				) { Text("Sign out") }
			},
			dismissButton = {
				TextButton(onClick = { confirmSignOut = false }) {
					Text(stringResource(R.string.dialog_cancel))
				}
			},
		)
	}
}

@Composable
private fun AccountDetails(uiState: UserProfileViewModel.UserProfileUiState.Ready) {
	Card(
		modifier = Modifier.fillMaxWidth(),
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			Text(
				text = "Account Details",
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.SemiBold,
			)

			DetailRow(
				label = "Account Type",
				value = uiState.accountType,
			)

			DetailRow(
				label = "Gold Status",
				value = if (uiState.isGold) "Yes" else "No",
				valueColor = if (uiState.isGold) Color(0xFFFFD700) else null,
			)

			DetailRow(
				label = "Moderator",
				value = if (uiState.isMod) "Yes" else "No",
				valueColor = if (uiState.isMod) MaterialTheme.colorScheme.onSurface else null,
			)
		}
	}
}

@Composable
private fun DetailRow(
	label: String,
	value: String,
	valueColor: Color? = null,
) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.SpaceBetween,
	) {
		Text(
			text = label,
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)
		Text(
			text = value,
			style = MaterialTheme.typography.bodyMedium,
			fontWeight = FontWeight.Medium,
			color = valueColor ?: MaterialTheme.colorScheme.onSurface,
		)
	}
}
