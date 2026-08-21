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

package org.quantumbadger.redreader.benchmark

import android.view.KeyEvent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

/**
 * Macro benchmark for app startup performance.
 * Measures cold, warm, and hot startup times.
 *
 * Run with: ./gradlew :assembleAndroidTest && ./gradlew connectedAndroidTest
 */
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartup() {
        benchmarkRule.measureRepeated(
            packageName = "org.quantumbadger.redreader",
            metrics = listOf(StartupTimingMetric()),
            startupMode = StartupMode.COLD,
            iterations = 3,
            compilationMode = CompilationMode.DEFAULT
        ) {
            device.wait(Until.hasObject(By.pkg("org.quantumbadger.redreader")), 10_000)
        }
    }

    @Test
    fun warmStartup() {
        benchmarkRule.measureRepeated(
            packageName = "org.quantumbadger.redreader",
            metrics = listOf(StartupTimingMetric()),
            startupMode = StartupMode.WARM,
            iterations = 3,
            compilationMode = CompilationMode.DEFAULT
        ) {
            device.pressKeyCode(KeyEvent.KEYCODE_BACK)
        }
    }

    @Test
    fun hotStartup() {
        benchmarkRule.measureRepeated(
            packageName = "org.quantumbadger.redreader",
            metrics = listOf(StartupTimingMetric()),
            startupMode = StartupMode.HOT,
            iterations = 3,
            compilationMode = CompilationMode.DEFAULT
        ) {
            device.pressKeyCode(KeyEvent.KEYCODE_BACK)
        }
    }
}
