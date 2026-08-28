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

package com.stormtroopercs.materialreader.compose.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.stormtroopercs.materialreader.activities.BaseActivity
import com.stormtroopercs.materialreader.common.PrefsUtility
import com.stormtroopercs.materialreader.compose.ctx.RRComposeContext

open class ComposeBaseActivity: BaseActivity() {

	// This activity calls enableEdgeToEdge() itself, and Compose handles the
	// window insets
	override fun baseActivityConfiguresEdgeToEdge() = false

	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		PrefsUtility.applyTheme(this)
		super.onCreate(savedInstanceState)
	}

	protected fun setContentCompose(content: @Composable () -> Unit) {
		setContentView(ComposeView(this).also { view ->
			view.setContent {
				// NavDisplay requires a NavigationEventDispatcherOwner to be present
				// in the composition (it reads LocalNavigationEventDispatcherOwner,
				// whose default is null at these androidx versions). Provide a
				// root, no-input dispatcher: it satisfies the lookup without adding a
				// second back source, because system back is routed through the
				// activity's OnBackPressedCallback (see BaseActivity), which the
				// Compose hosts wire into the Navigation 3 back stack.
				CompositionLocalProvider(
					LocalNavigationEventDispatcherOwner.provides(
						remember {
							object : NavigationEventDispatcherOwner {
								private val dispatcher = NavigationEventDispatcher()
								override val navigationEventDispatcher: NavigationEventDispatcher
									get() = dispatcher
							}
						}
					)
				) {
					RRComposeContext(this, controlsStatusBar = true) {
						content()
					}
				}
			}
		})
	}
}
