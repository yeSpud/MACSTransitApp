package fnsb.macstransit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import fnsb.macstransit.Activities.ActivityListeners.StopClicked;
import fnsb.macstransit.routematch.RouteMatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

/**
 * Created by Spud on 6/25/20 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0
 * @since Release 1.2.
 */
public class StopClickedTest {

	@Test
	public void timeFormatTest() {
		assertEquals("12:00 pm", StopClicked.formatTime("12:00").toLowerCase());
		assertEquals("12:30 pm", StopClicked.formatTime("12:30").toLowerCase());
		assertEquals("11:30 am", StopClicked.formatTime("11:30").toLowerCase());
		assertEquals("1:00 pm", StopClicked.formatTime("13:00").toLowerCase());
		assertEquals("11:59 am", StopClicked.formatTime("11:59").toLowerCase());
		assertEquals("12:01 pm", StopClicked.formatTime("12:01").toLowerCase());
		assertEquals("12:59 am", StopClicked.formatTime("00:59").toLowerCase());
		assertEquals("12:00 am", StopClicked.formatTime("00:00").toLowerCase());
		assertEquals("11:59 pm", StopClicked.formatTime("23:59").toLowerCase());
		assertEquals("", StopClicked.formatTime(""));
		assertEquals("", StopClicked.formatTime(null));
	}

	@Test
	public void newLineOccurrenceTest() {
		assertEquals(2, StopClicked.getNewlineOccurrence("Foo\nBar\nBaz"));
		assertEquals(2, StopClicked.getNewlineOccurrence("Foo\n\nBar"));
		assertEquals(0, StopClicked.getNewlineOccurrence("Foo"));
		assertEquals(0, StopClicked.getNewlineOccurrence("String with spaces."));
		assertEquals(0, StopClicked.getNewlineOccurrence(null));
		assertEquals(1, StopClicked.getNewlineOccurrence("\n"));
	}

	@Test
	public void getTimeTest() {

		// Test the raw function with bad parameters.
		assertEquals("", StopClicked.getTime(new JSONObject(), ""));
		assertEquals("", StopClicked.getTime(new JSONObject(), null));
		assertEquals("", StopClicked.getTime(null, ""));
		assertEquals("", StopClicked.getTime(null, null));

		try {
			// Load test json array from files.
			JSONArray geist = RouteMatch.parseData(Helper.getJSON(Helper.GEIST_MCDS)),
					woodCenter = RouteMatch.parseData(Helper.getJSON(Helper.WOOD_CENTER)),
					transitCenter = RouteMatch.parseData(Helper.getJSON(Helper.TRANSIT_CENTER));

			// Iterate though each of the arrays individually.
			// Start with geist.
			String[] geistArrivalTime = new String[]{"17:51", "18:20", "19:04"};
			String[] geistDepartureTime = new String[]{"17:51", "18:20", "19:04"};
			for (int g = 0; g < geist.length(); g++) {
				JSONObject stopObject = geist.getJSONObject(g);
				StopClickedTest.bulkGetTimeTest(stopObject, geistArrivalTime[g], geistDepartureTime[g]);
			}

			// Move onto wood center.
			String[] woodArrivalTime = new String[]{"17:47", "18:10", "18:15", "18:22", "18:32",
					"18:45", "18:50", "19:00", "19:30"};
			String[] woodDepartureTime = new String[]{"17:47", "18:15", "18:15", "18:22", "18:32",
					"18:45", "18:50", "19:00", "19:30"};
			for (int w = 0; w < woodCenter.length(); w++) {
				JSONObject stopObject = woodCenter.getJSONObject(w);
				StopClickedTest.bulkGetTimeTest(stopObject, woodArrivalTime[w], woodDepartureTime[w]);
			}

			// Finish with transit center.
			String[] transitArrivalTime = new String[]{"17:08", "17:39", "17:44", "17:56", "18:02",
					"18:14", "18:22", "18:52", "18:54", "19:09", "19:22", "02:17", "02:40", "03:05"};
			String[] transitDepartureTime = new String[]{"18:00", "17:45", "17:45", "18:00", "18:02",
					"18:20", "18:30", "20:00", "18:54", "19:10", "19:30", "02:17", "02:40", "03:05"};
			for (int t = 0; t < transitCenter.length(); t++) {
				JSONObject stopObject = transitCenter.getJSONObject(t);
				StopClickedTest.bulkGetTimeTest(stopObject, transitArrivalTime[t], transitDepartureTime[t]);
			}

		} catch (JSONException e) {
			e.printStackTrace();
			fail();
		}
	}

	static void bulkGetTimeTest(JSONObject stopObject, String arrivalTime, String departureTime) {

		// Test valid keys against the object.
		// Start with those that should not equal the arrival or departure times.
		String[] failKeys = new String[]{"arriveComplete", "cancelled",
				"dailyTimetableSameAsArrivedTimeTable", "departComplete", "departTime", "destination",
				"distanceOffRoute", "etaEnabled", "firstStop", "lastETACalcDateTime", "lastTimePointCrossedDate",
				"lastTimePointCrossedId", "masterRouteLongName", "masterRouteShortName", "predictedReal",
				"routeId", "routeLongName", "routeShortName", "routeStatus", "scheduleAdherence",
				"scheduleAdherenceEnabled", "status", "stopId", "stopTypeDefinitionBitset", "subRouteLongName",
				"subRouteShortName", "templates", "timePoint", "tripDirection", "tripId", "vehicleId"};
		for (String failKey : failKeys) {
			// System.out.println(String.format("Checking key %s", failKey));
			assertNotEquals(arrivalTime, StopClicked.getTime(stopObject, failKey));
			assertNotEquals(departureTime, StopClicked.getTime(stopObject, failKey));
		}

		// Test keys that either should contain the actual arrival and departure time,
		// or has the possibility of containing the actual arrival / departure time.
		assertNotEquals("", StopClicked.getTime(stopObject, "arriveTime"));
		assertEquals(arrivalTime, StopClicked.getTime(stopObject, "predictedArrivalTime"));
		assertEquals(departureTime, StopClicked.getTime(stopObject, "predictedDepartureTime"));
		assertNotEquals("", StopClicked.getTime(stopObject, "realETA"));
		assertNotEquals("", StopClicked.getTime(stopObject, "scheduledArrivalTime"));
		assertNotEquals("", StopClicked.getTime(stopObject, "scheduledDepartureTime"));

		// Test invalid keys against the object.
		assertEquals("", StopClicked.getTime(stopObject, "fadskjhbgfdsajhk"));
		assertEquals("", StopClicked.getTime(stopObject, ""));
		assertEquals("", StopClicked.getTime(stopObject, null));
	}
}
