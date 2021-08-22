package fnsb.macstransit

import fnsb.macstransit.Helper.OLD_SETTINGS_TXT
import fnsb.macstransit.settings.V1
import org.junit.Assert
import org.junit.Test
import java.io.File

/**
 * Created by Spud on 6/19/20 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Release 1.2.
 */
@Suppress("Deprecation")
class V1Test {

	/**
	 * TODO Documentation
	 */
	private val settings = V1

	@Test
	fun testName() {
		Assert.assertEquals("settings.txt", this.settings.FILENAME)
		Assert.assertNotEquals("settings.json", this.settings.FILENAME)
	}

	@Test
	fun testRead() {

		// First, verify the old settings file exists
		println(OLD_SETTINGS_TXT.absolutePath)
		Assert.assertTrue(OLD_SETTINGS_TXT.exists())
		Assert.assertTrue(OLD_SETTINGS_TXT.canRead())

		// Now test the read function
		val out: Collection<String> = this.settings.readFromSettingsFile(OLD_SETTINGS_TXT)
		Assert.assertNotNull(out)
		Assert.assertArrayEquals(arrayOf("Enable Traffic View:true", "Enable Dark Theme:false",
				"Show Polylines:false", "Show VR Options:false"), out.toTypedArray())

		// Test a bad file
		Assert.assertEquals(emptyList<String>(), settings.readFromSettingsFile(File("")))
	}

	@Test
	fun testParse() {

		// First, verify the old settings file exists
		println(OLD_SETTINGS_TXT.absolutePath)
		Assert.assertTrue(OLD_SETTINGS_TXT.exists())
		Assert.assertTrue(OLD_SETTINGS_TXT.canRead())

		// Not test the parse
		this.settings.parseSettings(this.settings.readFromSettingsFile(OLD_SETTINGS_TXT))
		Assert.assertTrue(this.settings.ENABLE_TRAFFIC_VIEW)
		Assert.assertFalse(this.settings.DEFAULT_NIGHT_MODE)
		Assert.assertFalse(this.settings.SHOW_POLYLINES)
		Assert.assertFalse(this.settings.ENABLE_VR_OPTIONS)
	}
}