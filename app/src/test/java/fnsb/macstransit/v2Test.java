package fnsb.macstransit;

import com.google.android.gms.maps.GoogleMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;

import fnsb.macstransit.Settings.CurrentSettings;
import fnsb.macstransit.Settings.v2;

import static org.junit.Assert.*;

/**
 * Created by Spud on 6/19/20 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0
 * @since Release 1.2.
 */
public class v2Test {

	public v2 settings = new v2();

	@Test
	public void testRead() {
		// First, verify the new settings file exists.
		assertTrue(Helper.SETTINGS_JSON.exists());
		assertTrue(Helper.SETTINGS_JSON.canRead());

		// Now test the read function.
		JSONObject out = settings.readFromSettingsFile(Helper.SETTINGS_JSON);
		assertNotNull(out);
		assertEquals("{\"enable dark theme\":true," + "\"enable polylines\":true," +
				"\"favorited routes\":[],\"enable traffic view\":false,\"map type\":2," +
				"\"enable streetview\":false,\"version\":2}",out.toString());

		// Test a bad file
		assertEquals(new JSONObject().toString(), settings.readFromSettingsFile(new File("")).toString());
	}

	@Test
	public void testFormat() {
		try {
			assertNotEquals(CurrentSettings.settings.formatSettingsToJsonString(false,
					false, false, false, GoogleMap.MAP_TYPE_NORMAL),
					new JSONObject());
			assertEquals("{\"enable dark theme\":false,\"enable polylines\":false," +
					"\"favorited routes\":[],\"enable traffic view\":false,\"map type\":1," +
					"\"enable streetview\":false,\"version\":2}",
					CurrentSettings.settings.formatSettingsToJsonString(false,
							false, false, false,
							GoogleMap.MAP_TYPE_NORMAL).toString());

			assertEquals("{\"enable dark theme\":true,\"enable polylines\":true," +
							"\"favorited routes\":[],\"enable traffic view\":true,\"map type\":1," +
							"\"enable streetview\":true,\"version\":2}",
					CurrentSettings.settings.formatSettingsToJsonString(true,
							true, true, true,
							GoogleMap.MAP_TYPE_NORMAL).toString());
		} catch (JSONException e) {
			fail();
		}
	}

	@Test
	public void testParse() {
		// First, verify the new settings file exists.
		System.out.println(Helper.SETTINGS_JSON.getAbsolutePath());
		assertTrue(Helper.SETTINGS_JSON.exists());
		assertTrue(Helper.SETTINGS_JSON.canRead());

		// Now test the read function.
		JSONObject out = settings.readFromSettingsFile(Helper.SETTINGS_JSON);
		assertNotNull(out);

		// Parse the output of the read function.
		try {
			this.settings.parseSettings(out);
		} catch (JSONException e) {
			fail();
		}

		// Check the result of the parse.
		assertFalse(this.settings.getTraffic());
		assertTrue(this.settings.getDarktheme());
		assertTrue(this.settings.getPolylines());
		assertFalse(this.settings.getStreetView());
		assertSame(GoogleMap.MAP_TYPE_SATELLITE, this.settings.getMaptype());
	}
}
