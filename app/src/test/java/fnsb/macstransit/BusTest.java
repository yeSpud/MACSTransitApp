package fnsb.macstransit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Objects;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.routematch.Bus;
import fnsb.macstransit.routematch.Route;
import fnsb.macstransit.routematch.RouteMatch;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
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

	public BusTest() {
		Route brown, green, red;
		brown = new Route("Brown");
		green = new Route("Green");
		red = new Route("Red");
		MapsActivity.allRoutes = new Route[]{brown, green, red};
	}

	private static Bus[] ArrayListTest(JSONArray vehicles) throws Route.RouteException {
		long startTime = System.nanoTime();

		ArrayList<Bus> buses = new ArrayList<>();

		for (int i = 0; i < vehicles.length(); i++) {
			try {
				Bus bus = new Bus(vehicles.getJSONObject(i));
				buses.add(bus);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		long endTime = System.nanoTime();
		Helper.printTime(startTime, endTime);

		return buses.toArray(new Bus[0]);

	}

	private static Bus[] StandardArraysTest(JSONArray vehicles) throws Route.RouteException {
		long startTime = System.nanoTime();

		Bus[] potentialBuses = new Bus[vehicles.length()];

		for (int i = 0; i < vehicles.length(); i++) {
			try {
				Bus bus = new Bus(vehicles.getJSONObject(i));
				potentialBuses[i] = bus;
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		long endTime = System.nanoTime();

		Helper.printTime(startTime, endTime);

		return potentialBuses;
	}

	@SuppressWarnings("MagicNumber")
	@Test
	public void getBusesTest() {

		// First test bad arguments,
		Bus[] empty = new Bus[0];
		assertArrayEquals(empty, Bus.getBuses(new JSONArray()));
		//assertArrayEquals(empty, Bus.getBuses(null));

		// Now test valid buses.
		try {
			assertArrayEquals(empty, Bus.getBuses(RouteMatch.parseData(Helper.getJSON(Helper.ALL_VEHICLES_EMPTY_JSON))));
			Bus[] buses = Bus.getBuses(RouteMatch.parseData(Helper.getJSON(Helper.ALL_VEHICLES_JSON)));
			assertEquals(3, buses.length);

			// Test the individual buses.
			String[] ids = new String[]{"Bus 142", "Bus 131", "Bus 71"};
			double[] lat = new double[]{64.85543060302734, 64.81417083740234, 64.84135437011719};
			double[] lon = new double[]{-147.7141876220703, -147.61318969726562, -147.71914672851562};
			for (int i = 0; i < buses.length; i++) {
				Bus bus = buses[i];
				assertEquals(ids[i], bus.getName());
				assertSame(Objects.requireNonNull(MapsActivity.allRoutes)[i], bus.getRoute());
				assertEquals(lat[i], bus.getLatitude(), 0.0);
				assertEquals(lon[i], bus.getLongitude(), 0.0);
			}
		} catch (JSONException | NullPointerException e) {
			e.printStackTrace();
			fail();
		}
	}

	@SuppressWarnings("MagicNumber")
	@Test
	public void createNewBusTest() {

		// Test against bad arguments.
		assertThrows(NullPointerException.class, () -> new Bus(null));
		assertThrows(JSONException.class, () -> new Bus(new JSONObject()));

		// Test with valid arguments.
		String[] ids = new String[]{"Bus 142", "Bus 131", "Bus 71"};
		double[] lat = new double[]{64.85543060302734, 64.81417083740234, 64.84135437011719};
		double[] lon = new double[]{-147.7141876220703, -147.61318969726562, -147.71914672851562};
		try {
			JSONArray busArray = RouteMatch.parseData(Helper.getJSON(Helper.ALL_VEHICLES_JSON));

			for (int i = 0; i < busArray.length(); i++) {
				Bus bus = new Bus(busArray.getJSONObject(i));
				assertEquals(ids[i], bus.getName());
				assertSame(Objects.requireNonNull(MapsActivity.allRoutes)[i], bus.getRoute());
				assertEquals(lat[i], bus.getLatitude(), 0.0);
				assertEquals(lon[i], bus.getLongitude(), 0.0);
			}
		} catch (JSONException | Route.RouteException | NullPointerException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void noBusMatchTest() {

		// Create various dummy test routes.
		Route greenRoute = new Route("Green"), blueRoute = new Route("Blue"),
				otherGreen = new Route("Green");

		// Create various dummy buses.
		Bus bus0, bus1, bus2, bus3, bus2Blue, bus0OG;
		bus0 = new Bus("0", greenRoute, 0.0, 0.0);
		bus1 = new Bus("1", greenRoute, 0.0, 0.0);
		bus2 = new Bus("2", greenRoute, 0.0, 0.0);
		bus3 = new Bus("3", greenRoute, 0.0, 0.0);
		bus2Blue = new Bus("2", blueRoute, 0.0, 0.0);
		bus0OG = new Bus("0", otherGreen, 0.0, 0.0);

		// Load most of the dummy buses into a test array.
		Bus[] testBusArray = new Bus[]{bus0, bus1, bus2};

		// Test the noBusMatch method.
		assertFalse(bus0.isBusNotInArray(testBusArray));
		assertFalse(bus1.isBusNotInArray(testBusArray));
		assertFalse(bus2.isBusNotInArray(testBusArray));
		assertTrue(bus3.isBusNotInArray(testBusArray));
		assertTrue(bus2Blue.isBusNotInArray(testBusArray));
		assertFalse(bus0OG.isBusNotInArray(testBusArray));

	}

	@Test // Proof of concept for standard arrays vs array lists.
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
}
