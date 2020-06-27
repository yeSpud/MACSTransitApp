package fnsb.macstransit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;

import fnsb.macstransit.RouteMatch.Bus;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.RouteMatch;
import fnsb.macstransit.Settings.CurrentSettings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by Spud on 6/27/20 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0
 * @since Release 1.2.
 */
public class BusTest {

	@Test
	public void getBusesTest() {
		assertTrue(Helper.ALL_VEHICLES_JSON.exists());
		assertTrue(this.testFile.canRead());
		String data = CurrentSettings.readFile(this.testFile);
		assertNotNull(data);
		JSONObject testData;
		try {
			testData = new JSONObject(data);
		} catch (JSONException e) {
			fail();
			return;
		}

		JSONArray array = RouteMatch.parseData(testData);
		assertNotNull(array);
		assertEquals(3, array.length());

		// Test the exception when there are no routes loaded.
		assertThrows(Route.RouteException.class, () -> Bus.getBuses(new JSONArray()));
		assertThrows(Route.RouteException.class, () -> Bus.getBuses(null));
		assertThrows(Route.RouteException.class, () -> Bus.getBuses(array));

		// Load in all routes
	}

}
