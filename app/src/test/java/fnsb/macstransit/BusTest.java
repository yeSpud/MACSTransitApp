package fnsb.macstransit;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;

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

	public static Bus[] ArrayListTest(JSONArray vehicles) throws Route.RouteException {
		long startTime = System.nanoTime();

		ArrayList<Bus> buses = new ArrayList<>();

		for (int i = 0; i < vehicles.length(); i++) {
			try {
				Bus bus = BusTest.createBus(vehicles, i);
				buses.add(bus);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		long endTime = System.nanoTime();
		Helper.printTime(startTime, endTime);

		return buses.toArray(new Bus[0]);

	}

	public static Bus[] StandardArraysTest(JSONArray vehicles) throws Route.RouteException {
		long startTime = System.nanoTime();

		Bus[] potentialBuses = new Bus[vehicles.length()];

		for (int i = 0; i < vehicles.length(); i++) {
			try {
				Bus bus = BusTest.createBus(vehicles, i);
				potentialBuses[i] = bus;
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		long endTime =  System.nanoTime();

		Helper.printTime(startTime, endTime);

		return potentialBuses;
	}

	@Test // Proof of concept
	public void ArrayTests() {
		JSONObject testData = null;
		try {
			testData = Helper.getJSON(Helper.ALL_VEHICLES_JSON);
		} catch (JSONException e) {
			e.printStackTrace();
			fail();
		}

		JSONArray array = RouteMatch.parseData(testData);

		try {
			Bus[] b1 = BusTest.ArrayListTest(array);
			Bus[] b2 = BusTest.StandardArraysTest(array);

			assertEquals(b1.length, b2.length);

		} catch (Route.RouteException e) {
			fail();
		}
	}

	private static Bus createBus(JSONArray vehicles, int i) throws Route.RouteException, JSONException {
		JSONObject busObject = vehicles.getJSONObject(i);

		// Try to get the necessary value (the vehicle id) for creating a new bus object.
		// If unsuccessful continue on the loop without executing any of the lower checks.
		String vehicleId = busObject.getString("vehicleId");

		// Try to get the necessary value (the route name) for creating a new bus object.
		// If unsuccessful continue on the loop without executing any of the lower checks.
		String routeName = busObject.getString("masterRouteId");

		// Try to get the necessary value (the latitude) for creating a new bus object.
		// If unsuccessful continue on the loop without executing any of the lower checks.
		double latitude =  busObject.getDouble("latitude");

		// Try to get the necessary value (the longitude) for creating a new bus object.
		// If unsuccessful continue on the loop without executing any of the lower checks.
		double longitud = busObject.getDouble("longitude");

		// Try to get any extra values (the heading) for creating a new bus object.
		// These valeus arent necessary, but are nice to have.
		String heading = busObject.getString("headingName");

		// Try to get any extra values (the speed) for creating a new bus object.
		// These valeus arent necessary, but are nice to have.
		int speed = busObject.getInt("speed");

		Bus bus = new Bus(vehicleId, new Route("test"), latitude, longitud);
		bus.heading = heading;
		bus.speed = speed;
		return bus;
	}

}
