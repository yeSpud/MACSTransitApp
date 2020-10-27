package fnsb.macstransit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.RouteMatch;
import fnsb.macstransit.RouteMatch.Stop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

		JSONObject blueStopJson = null;
		try {
			blueStopJson = Helper.getJSON(Helper.BLUE_STOPS);
		} catch (JSONException e) {
			e.printStackTrace();
			fail();
		}

		JSONArray blueStopJsonArray = RouteMatch.parseData(blueStopJson);

		Route blueRoute = null;
		try {
			blueRoute = new Route("Blue");
		} catch (Route.RouteException e) {
			e.printStackTrace();
			fail();
		}

		long startTime = System.nanoTime();
		Stop[] blueStops = Stop.generateStops(blueStopJsonArray, blueRoute);
		long endTime = System.nanoTime();
		Helper.printTime(startTime, endTime);

		// Before duplication checking this should be 232 in length
		assertNotNull(blueStops);
		assertEquals(233, blueStops.length);

		startTime = System.nanoTime();
		blueStops = Stop.validateGeneratedStops(blueStops);
		endTime = System.nanoTime();
		Helper.printTime(startTime, endTime);

		// After duplication checking
		assertNotNull(blueStops);
		assertEquals(66, blueStops.length);

	}

	@Test
	public void isDuplicateCheck() {
		assertFalse(Stop.isDuplicate(null, null));
		assertFalse(Stop.isDuplicate(new Stop("", 0.0d, 0.0d, null), null));
		assertFalse(Stop.isDuplicate(null, new Stop[0]));
		assertFalse(Stop.isDuplicate(null, new Stop[]{null}));

	}

}
