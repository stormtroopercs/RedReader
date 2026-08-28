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

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Minimal Hilt component-holder host for the Compose screen tests that call
 * `hiltViewModel()`.
 *
 * `createComposeRule()` launches a plain `androidx.activity.ComponentActivity`
 * to host `setContent`, which is **not** a Hilt entry point, so
 * `hiltViewModel()` throws ("does not implement interface
 * GeneratedComponentManager"). `createAndroidComposeRule<HiltTestHostActivity>`
 * hosts the same content in this `@AndroidEntryPoint` activity instead, so the
 * ViewModel factory can resolve through the Hilt graph.
 */
@AndroidEntryPoint
class HiltTestHostActivity : ComponentActivity()
