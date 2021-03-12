package fnsb.macstransit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Objects;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.RouteMatch.Bus;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.RouteMatch;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
		try {
			brown = new Route("Brown");
			green = new Route("Green");
			red = new Route("Red");
		} catch (Route.RouteException e) {
			e.printStackTrace();
			fail();
			return;
		}
		MapsActivity.allRoutes = new Route[]{brown, green, red};
	}


	@SuppressWarnings("MagicNumber")
	@Test
	public void getBusesTest() {
		// First test bad arguments,
		try {
			assertArrayEquals(Bus.EMPTY_BUSES, Bus.getBuses(new JSONArray()));
			assertArrayEquals(Bus.EMPTY_BUSES, Bus.getBuses(null));
		} catch (Route.RouteException e) {
			e.printStackTrace();
			fail();
		}

		// Now test valid buses.
		try {
			assertArrayEquals(Bus.EMPTY_BUSES, Bus.getBuses(RouteMatch.parseData(Helper.getJSON(Helper.ALL_VEHICLES_EMPTY_JSON))));
			Bus[] buses = Bus.getBuses(RouteMatch.parseData(Helper.getJSON(Helper.ALL_VEHICLES_JSON)));
			assertEquals(3, buses.length);

			// Test the individual buses.
			String[] ids = new String[]{"Bus 142", "Bus 131", "Bus 71"};
			double[] lat = new double[]{64.85543060302734, 64.81417083740234, 64.84135437011719};
			double[] lon = new double[]{-147.7141876220703, -147.61318969726562, -147.71914672851562};
			for (int i = 0; i < buses.length; i++) {
				Bus bus = buses[i];
				assertEquals(ids[i], bus.name);
				assertEquals(Objects.requireNonNull(MapsActivity.allRoutes)[i], bus.route);
				assertEquals(lat[i], bus.latitude, 0.0);
				assertEquals(lon[i], bus.longitude, 0.0);
			}
		} catch (JSONException| Route.RouteException | NullPointerException e) {
			e.printStackTrace();
			fail();
		}
	}

	@SuppressWarnings("MagicNumber")
	@Test
	public void createNewBusTest() {

		// Test against bad arguments.
		assertThrows(NullPointerException.class, () -> Bus.createNewBus(null));
		assertThrows(JSONException.class, () -> Bus.createNewBus(new JSONObject()));

		// Test with valid arguments.
		String[] ids = new String[]{"Bus 142", "Bus 131", "Bus 71"};
		double[] lat = new double[]{64.85543060302734, 64.81417083740234, 64.84135437011719};
		double[] lon = new double[]{-147.7141876220703, -147.61318969726562, -147.71914672851562};
		try {
			JSONArray busArray = RouteMatch.parseData(Helper.getJSON(Helper.ALL_VEHICLES_JSON));

			for (int i = 0; i < busArray.length(); i++) {
				Bus bus = Bus.createNewBus(busArray.getJSONObject(i));
				assertEquals(ids[i], bus.name);
				assertEquals(Objects.requireNonNull(MapsActivity.allRoutes)[i], bus.route);
				assertEquals(lat[i], bus.latitude, 0.0);
				assertEquals(lon[i], bus.longitude, 0.0);
			}
		} catch (JSONException | Route.RouteException | NullPointerException e) {
			e.printStackTrace();
			fail();
		}
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
		Bus bus0, bus1, bus2, bus3, bus2Blue, bus0OG;
		try {
			bus0 = new Bus("0", greenRoute, 0.0, 0.0);
			bus1 = new Bus("1", greenRoute, 0.0, 0.0);
			bus2 = new Bus("2", greenRoute, 0.0, 0.0);
			bus3 = new Bus("3", greenRoute, 0.0, 0.0);
			bus2Blue = new Bus("2", blueRoute, 0.0, 0.0);
			bus0OG = new Bus("0", otherGreen, 0.0, 0.0);
		} catch (Route.RouteException e) {
			e.printStackTrace();
			fail();
			return;
		}

		// Load most of the dummy buses into a test array.
		Bus[] testBusArray = new Bus[]{bus0, bus1, bus2};

		// Test the noBusMatch method.
		assertFalse(Bus.isBusNotInArray(bus0, testBusArray));
		assertFalse(Bus.isBusNotInArray(bus1, testBusArray));
		assertFalse(Bus.isBusNotInArray(bus2, testBusArray));
		assertTrue(Bus.isBusNotInArray(bus3, testBusArray));
		assertTrue(Bus.isBusNotInArray(bus2Blue, testBusArray));
		assertFalse(Bus.isBusNotInArray(bus0OG, testBusArray));

	}

	private static Bus[] ArrayListTest(JSONArray vehicles) throws Route.RouteException {
		long startTime = System.nanoTime();

		ArrayList<Bus> buses = new ArrayList<>();

		for (int i = 0; i < vehicles.length(); i++) {
			try {
				Bus bus = Bus.createNewBus(vehicles.getJSONObject(i));
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
				Bus bus = Bus.createNewBus(vehicles.getJSONObject(i));
				potentialBuses[i] = bus;
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		long endTime = System.nanoTime();

		Helper.printTime(startTime, endTime);

		return potentialBuses;
	}

	@Test // Proof of concept for standard arrays vs array lists.
	public void ArrayTests() { // FIXME
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
