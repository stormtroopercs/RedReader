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
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MaterialReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.stormtroopercs.materialreader.test.general

import com.stormtroopercs.materialreader.compose.activity.ComposeBaseActivity

/**
 * Test-only [ComposeBaseActivity] host for the edge-to-edge window test.
 *
 * `MainActivityCompose` (the production host) is `@AndroidEntryPoint`, which
 * needs the `@HiltAndroidApp` Application that Robolectric doesn't install,
 * so it can't be `.setup()` here. This plain subclass exercises the same
 * `ComposeBaseActivity.onCreate` window-insets logic under test without the
 * Hilt dependency (the same role `OAuthLoginActivity` played before the 50th
 * increment retired it).
 */
class EdgeToEdgeInsetsTestActivity : ComposeBaseActivity()
