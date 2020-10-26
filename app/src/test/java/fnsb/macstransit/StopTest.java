package fnsb.macstransit;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.RouteMatch;
import fnsb.macstransit.RouteMatch.Stop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Created by Spud on 10/26/20 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0
 * @since Release 1.2.
 */
public class StopTest {

	@Test
	public void StopCreationTest() {

		JSONObject BlueStopJson = null;
		try {
			BlueStopJson = Helper.getJSON(Helper.BLUE_STOPS);
		} catch (JSONException e) {
			e.printStackTrace();
			fail();
		}

		JSONArray BlueStopJsonArray = RouteMatch.parseData(BlueStopJson);

		Stop[] blueStops = null;

		try {
			blueStops = StopTest.parseJsonArray(BlueStopJsonArray, new Route("Blue"));
		} catch (JSONException | Route.RouteException e) {
			e.printStackTrace();
			fail();
		}

		assertNotNull(blueStops);

		// Before duplication checking this should be 232 in length
		assertEquals(233, blueStops.length);

	}

	private static Stop @NotNull [] parseJsonArray(@NotNull JSONArray array, Route route) throws JSONException {
		int count = array.length();
		Stop[] stops = new Stop[count];

		for (int i = 0; i < count; i++) {
			Stop stop = new Stop(array.getJSONObject(i), route);
			stops[i] = stop;
		}

		return stops;
	}


}
