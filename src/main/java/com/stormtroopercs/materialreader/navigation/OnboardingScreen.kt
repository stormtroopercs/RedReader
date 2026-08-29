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
package com.stormtroopercs.materialreader.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stormtroopercs.materialreader.common.PrefsUtility

/**
 * The multi-step first-run onboarding (FINAL-DESIGN 8.4 / DESIGN §11).
 *
 * The frame is the reference's: a centered step that animates between
 * illustrations, and a **persistent bottom Back/Next bar** whose last
 * button is "Finish". The final step carries a live setting row (icon +
 * title + subtitle + switch) the user can flip before entering the app.
 * Content is RedReader's own first-run copy (app intro, sign-in, browse,
 * and the "hide read posts" live setting).
 *
 * Shown once: the composition root gates this on [PrefsUtility.pref_onboarding_complete]
 * and flips it on finish (or skip).
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    // The step survives configuration changes (rotation) via rememberSaveable.
    var step by rememberSaveable { mutableStateOf(0) }
    val last = OnboardingStep.last
    // The live setting on the final step (8.4): hide read posts.
    var hideRead by remember {
        mutableStateOf(PrefsUtility.pref_behaviour_hide_read_posts())
    }
    // The shared fast-out-extra-slow-in step-transition timing (8.5).
    val stepSpec: FiniteAnimationSpec<Float> =
        tween(durationMillis = 320, easing = CubicBezierEasing(0.1f, 0f, 0.2f, 1f))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Skip (top-right, small) — onboarding is skippable.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onSkip) { Text("Skip") }
        }

        Spacer(Modifier.weight(1f))

        // The animated step content (crossfade + a subtle scale, matching the
        // 8.5 fast-out-extra-slow-in curve).
        AnimatedContent(
            targetState = step,
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = {
                (scaleIn(initialScale = 0.94f, animationSpec = stepSpec) +
                        fadeIn(animationSpec = stepSpec)) togetherWith
                        (scaleOut(targetScale = 1.06f, animationSpec = stepSpec) +
                                fadeOut(animationSpec = stepSpec))
            }
        ) { index ->
            OnboardingStep.entries[index].let { data ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Per-step illustration (a large themed icon in a card).
                    Card(
                        modifier = Modifier.size(128.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = data.icon,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = data.body,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // The live setting row, only on the final step (8.4).
                    if (index == last) {
                        Spacer(Modifier.height(32.dp))
                        LiveSettingRow(
                            icon = Icons.Filled.Visibility,
                            title = "Hide read posts",
                            subtitle = "Hide posts you've already read from feeds",
                            checked = hideRead,
                            onCheckedChange = { hideRead = it }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // The persistent bottom Back / Next bar (last = Finish, 8.4).
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 0) {
                TextButton(onClick = { step -= 1 }) { Text("Back") }
            } else {
                Spacer(Modifier.width(88.dp))
            }
            val isLast = step == last
            TextButton(onClick = {
                if (isLast) {
                    PrefsUtility.pref_behaviour_hide_read_posts_set(hideRead)
                    PrefsUtility.pref_onboarding_complete_set(true)
                    onFinish()
                } else {
                    step += 1
                }
            }) {
                Text(if (isLast) "Finish" else "Next")
            }
        }
    }
}

/**
 * A live setting row (8.4): icon + title + subtitle + a Switch. Used on the
 * final onboarding step so a preference can be set before entering the app.
 */
@Composable
private fun LiveSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

/**
 * The onboarding steps (RedReader's own first-run content).
 */
private enum class OnboardingStep(
    val icon: ImageVector,
    val title: String,
    val body: String
) {
    Welcome(
        icon = Icons.Filled.Check,
        title = "Welcome to MaterialReader",
        body = "A fast, minimal client for Reddit, built in Material 3."
    ),
    Signin(
        icon = Icons.Filled.Person,
        title = "Sign in to Reddit",
        body = "Link a Reddit account to vote, save, and post. You can always sign in later from the menu."
    ),
    Browse(
        icon = Icons.Filled.Refresh,
        title = "Browse your communities",
        body = "Swipe through your home feed, jump to any community, and filter by sort."
    ),
    Done(
        icon = Icons.Filled.Check,
        title = "That's it",
        body = "One last setting, then you're in."
    );

    companion object {
        val last = entries.lastIndex
    }
}
