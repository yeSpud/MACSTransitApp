package fnsb.macstransit

import com.google.android.gms.maps.GoogleMap
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

/**
 * Created by Spud on 6/19/20 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Release 1.2.
 */
class V2Test {

	/**
	 * TODO Documentation
	 */
	private val settings = fnsb.macstransit.settings.V2

	@Test
	fun testRead() {

		// First, verify the new settings file exists.
		Assert.assertTrue(Helper.SETTINGS_JSON.exists())
		Assert.assertTrue(Helper.SETTINGS_JSON.canRead())

		// Now test the read function.
		val out = settings.readFromSettingsFile(Helper.SETTINGS_JSON)
		Assert.assertNotNull(out)
		Assert.assertEquals(
				"{\"enable dark theme\":true," + "\"enable polylines\":true," + "\"favorited routes\":[],\"enable traffic view\":false,\"map type\":2," + "\"enable streetview\":false,\"version\":2}",
				out.toString())

		// Test a bad file
		Assert.assertEquals(JSONObject().toString(),
		                    settings.readFromSettingsFile(java.io.File("")).toString())
	}

	@Test
	fun testFormat() {
		try {
			Assert.assertNotEquals(settings.formatSettingsToJsonString(false, false, false, false,
			                                                           GoogleMap.MAP_TYPE_NORMAL),
			                       JSONObject())
			Assert.assertEquals(
					"{\"enable dark theme\":false,\"enable polylines\":false," + "\"favorited routes\":[],\"enable traffic view\":false,\"map type\":1," + "\"enable streetview\":false,\"version\":2}",
					settings.formatSettingsToJsonString(false, false, false, false,
					                                    GoogleMap.MAP_TYPE_NORMAL).toString())
			Assert.assertEquals(
					"{\"enable dark theme\":true,\"enable polylines\":true," + "\"favorited routes\":[],\"enable traffic view\":true,\"map type\":1," + "\"enable streetview\":true,\"version\":2}",
					settings.formatSettingsToJsonString(true, true, true, true,
					                                    GoogleMap.MAP_TYPE_NORMAL).toString())
		} catch (e: org.json.JSONException) {
			Assert.fail()
		}
	}

	@Test
	fun testParse() {

		// First, verify the new settings file exists.
		println(Helper.SETTINGS_JSON.absolutePath)
		Assert.assertTrue(Helper.SETTINGS_JSON.exists())
		Assert.assertTrue(Helper.SETTINGS_JSON.canRead())

		// Now test the read function.
		val out = settings.readFromSettingsFile(Helper.SETTINGS_JSON)
		Assert.assertNotNull(out)

		// Parse the output of the read function.
		settings.parseSettings(out)

		// Check the result of the parse.
		Assert.assertFalse(settings.traffic)
		Assert.assertTrue(settings.darktheme)
		Assert.assertTrue(settings.polylines)
		Assert.assertFalse(settings.streetView)
		Assert.assertSame(GoogleMap.MAP_TYPE_SATELLITE, settings.maptype)
	}
}