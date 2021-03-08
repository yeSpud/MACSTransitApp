package fnsb.macstransit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.RouteMatch;
import fnsb.macstransit.RouteMatch.SharedStop;
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

	@Test
	public void averageStops() {
		File[] files = new File[]{Helper.BLUE_STOPS, Helper.BROWN_STOPS, Helper.GOLD_STOPS,
				Helper.GREEN_STOPS, Helper.PURPLE_STOPS, Helper.RED_STOPS, Helper.YELLOW_STOPS};
		int count = 0;
		for (File file : files) {
			try {
				JSONObject jsonObject = Helper.getJSON(file);

				JSONArray jsonArray = RouteMatch.parseData(jsonObject);

				Stop[] pStops = Stop.generateStops(jsonArray, new Route("Foo"));
				Stop[] vStops = Stop.validateGeneratedStops(pStops);
				count += vStops.length;
			} catch (Exception e) {
				e.printStackTrace();
				fail();
			}
		}
		System.out.println(count / files.length);
	}


	@Test
	/*
	 * This test should test the loading of multiple stops, apply a duplication check, apply shared stops,
	 * and finally remove overlapping shared stops and regular stops.
	 */
	public void stopGauntlet() {
		try {
			// Start by getting the json files to use for testing.
			// TODO Add Gray, Orange lines.
			final int loadedFiles = 6;
			final File[] files = new File[]{Helper.BLUE_STOPS, Helper.BROWN_STOPS,
					Helper.GREEN_STOPS, Helper.PURPLE_STOPS, Helper.RED_STOPS, Helper.YELLOW_STOPS};
			final Route[] routes = new Route[]{new Route("Blue"), new Route("Brown"),
					new Route("Green"), new Route("Purple"), new Route("Red"),
					new Route("Yellow")};


			// Check the stops that will have duplicates.
			ArrayList<Stop[]> stopsWithDuplicates = new ArrayList<>(loadedFiles);

			// Start by loading the stops from each file.
			for (int i = 0; i < loadedFiles; i++) {
				File stopJsonFile = files[i];
				JSONObject jsonObject = Helper.getJSON(stopJsonFile);
				JSONArray dataArray = RouteMatch.parseData(jsonObject);

				// Also iterate though the raw data and print each stops lat long
				/*
				System.out.println(routes[i].routeName + " stops:");
				for (int j = 0; j < dataArray.length(); j++) {
					JSONObject stop = dataArray.getJSONObject(j);
					double lat = stop.getDouble("latitude");
					double lon = stop.getDouble("longitude");
					System.out.println(String.format("%f, %f", lat, lon));
				}
				 */

				Stop[] stops = Stop.generateStops(dataArray, routes[i]);
				stopsWithDuplicates.add(stops);
			}

			// Now iterate though each stop that has duplicates and verify the number of stops.
			// This number should be large as we haven not removed the duplicate stops at this point.
			final int[] validDuplicateStopCounts = new int[]{233, 24, 144, 78, 176, 145};
			for (int i = 0; i < loadedFiles; i++) {
				Stop[] stops = stopsWithDuplicates.get(i);
				System.out.println(String.format("Number of stops for %s (with potential duplicates): %d",
						stops[0].route.routeName, stops.length));
				assertEquals(validDuplicateStopCounts[i], stops.length);
			}


			// Now test the removal of duplicate stops.
			final int[] validateStopCounts = new int[]{66, 24, 104, 39, 58, 56};
			for (int i = 0; i < loadedFiles; i++) {
				Stop[] stops = stopsWithDuplicates.get(i);
				Stop[] vStops = Stop.validateGeneratedStops(stops);
				System.out.println(String.format("Number of stops for %s: %d", vStops[0].route.routeName, vStops.length));
				assertEquals(validateStopCounts[i], vStops.length);
				routes[i].stops = vStops;
			}


			// Temporarily set all routes to not null in order to bypass a null check.
			MapsActivity.allRoutes = routes;

			// Now test the creation of shared stops.
			for (int routeIndex = 0; routeIndex < loadedFiles; routeIndex++) {

				// Get a first comparison route.
				Route route = routes[routeIndex];

				// Iterate through all the stops in our first comparison route.
				for (Stop stop : route.stops) {

					// Make sure our stop is not already in our shared stop.
					if (route.sharedStops != null) {
						boolean found = false;

						// Iterate though the shared stops in the route.
						for (SharedStop ssCheck : route.sharedStops) {

							// If the route was found, continue.
							if (SharedStop.areEqual(ssCheck, stop)) {
								found = true;
								break;
							}
						}
						if (found) {
							continue;
						}
					}

					// Get an array of shared routes.
					Route[] sharedRoutes = SharedStop.getSharedRoutes(route, routeIndex, stop);

					// If the shared routes array has more than one entry, create a new shared stop object.
					if (sharedRoutes.length > 1) {
						SharedStop sharedStop = new SharedStop(stop.circleOptions.getCenter(),
								stop.stopName, sharedRoutes);

						// Iterate though all the routes in the shared route, and add our newly created shared stop.
						for (Route sharedRoute : sharedRoutes) {
							sharedRoute.addSharedStop(sharedStop);
						}
					}
				}
			}

			// Test the number of shared stops.
			final int[] sharedStopsCount = new int[]{14, 3, 10, 10, 12, 17};
			for (int i = 0; i < loadedFiles; i++) {
				Route route = routes[i];
				System.out.println(String.format("%s route stops: %d", route.routeName, route.stops.length));
				if (route.sharedStops != null) {
					System.out.println(String.format("%s route shared stops: %d", route.routeName, route.sharedStops.length));
					assertEquals(sharedStopsCount[i], route.sharedStops.length);
				} else {
					fail();
				}
			}
			// Reset all routes.
			MapsActivity.allRoutes = null;


			// Test removal of stops that have shared stops.
			ArrayList<Stop[]> finalStops = new ArrayList<>(loadedFiles);
			for (Route route : routes) {
				Stop[] stops = SharedStop.recreateStops(route.stops, route.sharedStops);
				System.out.println(String.format("Going from %d stops to %d stops for route %s", route.stops.length,
						stops.length, route.routeName));
				finalStops.add(stops);
			}

			final int[] finalStopCount = new int[]{66 - 14, 24 - 3, 104 - 10, 39 - 10, 58 - 12, 56 - 17};
			for (int i = 0; i < loadedFiles; i++) {
				Stop[] stops = finalStops.get(i);
				assertEquals(finalStopCount[i], stops.length);
			}
		} catch (Exception e) {
			// If anything goes wrong, print and then fail.
			e.printStackTrace();
			fail();
		}
	}
}