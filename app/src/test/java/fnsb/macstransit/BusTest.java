package fnsb.macstransit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import fnsb.macstransit.RouteMatch.Bus;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.RouteMatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
		assertTrue(Helper.ALL_VEHICLES_JSON.canRead());
		String data = Helper.getText(Helper.ALL_VEHICLES_JSON);
		assertNotNull(data);
		JSONObject testData;
		try {
			testData = new JSONObject(data);
		} catch (JSONException e) {
			e.printStackTrace();
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
		// TODO
	}

	@Test
	public void noBusMatchTest() {
		// Create various dummy test routes.
		Route greenRoute, blueRoute, otherGreen;
		try {
			greenRoute = new Route("Green");
			blueRoute = new Route("Blue");
			otherGreen = new Route("Green");
		} catch (Route.RouteException e) {
			e.printStackTrace();
			fail();
			return;
		}

		// Create various dummy buses.
		Bus bus0 = new Bus("0", greenRoute, 0.0, 0.0),
				bus1 = new Bus("1", greenRoute, 0.0, 0.0),
				bus2 = new Bus("2", greenRoute, 0.0, 0.0),
				bus3 = new Bus("3", greenRoute, 0.0, 0.0),
				bus2Blue = new Bus("2", blueRoute, 0.0, 0.0),
				bus0OG = new Bus("0", otherGreen, 0.0, 0.0);

		// Load most of the dummy buses into a test array.
		Bus[] testBusArray = new Bus[]{bus0, bus1, bus2};

		// Test the noBusMatch method.
		assertFalse(Bus.noBusMatch(bus0, testBusArray));
		assertFalse(Bus.noBusMatch(bus1, testBusArray));
		assertFalse(Bus.noBusMatch(bus2, testBusArray));
		assertTrue(Bus.noBusMatch(bus3, testBusArray));
		assertTrue(Bus.noBusMatch(bus2Blue, testBusArray));
		assertFalse(Bus.noBusMatch(bus0OG, testBusArray));

	}

}
