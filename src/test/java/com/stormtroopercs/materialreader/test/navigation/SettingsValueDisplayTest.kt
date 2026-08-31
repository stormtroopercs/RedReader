package com.stormtroopercs.materialreader.test.navigation

import com.stormtroopercs.materialreader.navigation.displaySettingValue
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [displaySettingValue], the human-readable renderer for
 * enum-backed setting values (bug report 2026-08-30: "clean up the names
 * of the values" - rows showed raw constants like `WIFI_ONLY`).
 */
class SettingsValueDisplayTest {

	@Test
	fun underscoresBecomeSpaces() {
		assertEquals("WiFi Only", displaySettingValue("WIFI_ONLY"))
		assertEquals("Left Handed", displaySettingValue("LEFT_HANDED"))
	}

	@Test
	fun commonAcronymsAreCorrected() {
		assertEquals("NSFW", displaySettingValue("NSFW"))
		assertEquals("Always", displaySettingValue("ALWAYS"))
		assertEquals("Never", displaySettingValue("NEVER"))
		assertEquals("Auto", displaySettingValue("AUTO"))
	}

	@Test
	fun wordsAreTitleCased() {
		assertEquals("Absolute", displaySettingValue("ABSOLUTE"))
		assertEquals("Normal", displaySettingValue("NORMAL"))
		assertEquals("WiFi Only", displaySettingValue("WIFI_ONLY"))
	}

	@Test
	fun emptyIsEmpty() {
		assertEquals("", displaySettingValue(""))
	}
}
