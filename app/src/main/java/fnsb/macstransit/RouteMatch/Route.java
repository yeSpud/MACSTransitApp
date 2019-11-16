package fnsb.macstransit.RouteMatch;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;

import fnsb.macstransit.Activities.MapsActivity;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.1
 * @since Beta 3
 */
public class Route {

	/**
	 * The name of the route. Note: This cannot contain whitespace characters (ie spaces, tabs, or new lines),
	 * as its used in a url.
	 */
	public String routeName;

	/**
	 * The color of the route. This is optional, as there is a high chance that the route does not have one.
	 * <p>
	 * This is an int instead of a Color object because for whatever reason android stores its colors as ints.
	 */
	public int color;

	/**
	 * The array of stops for this route. This may be empty / null if the route has not been initialized,
	 * and the stops haven't been loaded.
	 */
	public Stop[] stops;

	/**
	 * Constructor for the route. The name of the route is the only thing that is required.
	 * Be sure that the provided route name does <b>NOT</b> contain any whitespace characters!
	 *
	 * @param routeName The name of the route. Be sure this does <b>NOT</b> contain any whitespace characters!
	 * @throws Exception Thrown if the route name contains white space characters.
	 */
	public Route(String routeName) throws Exception {
		if (routeName.contains(" ") || routeName.contains("\n") || routeName.contains("\t")) {
			throw new Exception("Route name cannot contain white space!");
		} else {
			this.routeName = routeName;
		}
	}

	/**
	 * Constructor for the route. The name of the route is the only thing that is required.
	 * Be sure that the provided route name does <b>NOT</b> contain any whitespace characters!
	 *
	 * @param routeName The name of the route. e sure this does <b>NOT</b> contain any whitespace characters!
	 * @param color     The route's color. This is optional,
	 *                  and of the color is non-existent simply use the {@code Route(String routeName)} constructor.
	 * @throws Exception Thrown if the route name contains white space characters.
	 */
	public Route(String routeName, int color) throws Exception {
		this(routeName);
		this.color = color;
	}

	/**
	 * Dynamically generates the routes that are used by parsing the master schedule.
	 * This may return an empty route array if there was an issue parsing the data.
	 *
	 * @param routeMatch The route match instance (for pulling from the RouteMatch server).
	 * @return An array of routes that <b><i>can be</i></b> tracked.
	 */
	public static Route[] generateRoutes(RouteMatch routeMatch) {

		// Create an array to store all the generated routes. This will be returned in the end.
		ArrayList<Route> routes = new ArrayList<>();

		// Get the data from the master schedule, and store it in a JSONArray.
		JSONArray data = RouteMatch.parseData(routeMatch.getMasterSchedule());

		// Iterate through the data array to begin parsing the routes
		int count = data.length();
		for (int index = 0; index < count; index++) {
			try {

				// Get the current progress for parsing the routes
				Log.d("generateRoutes", String.format("Parsing route %d/%d", index + 1, count));

				// Get the routeData that we are currently parsing as its own JSONObject variable.
				org.json.JSONObject routeData = data.getJSONObject(index);

				// First, parse the name
				String name = routeData.getString("shortName");

				// Now try to parse the color
				try {
					int color = android.graphics.Color.parseColor(routeData.getString("routeColor"));
					routes.add(new Route(name, color));
				} catch (IllegalArgumentException | JSONException colorError) {
					Log.w("generateRoutes", "Unable to determine route color");
					// Just return the route with the name
					routes.add(new Route(name));
				}

			} catch (Exception e) {
				// If there was an exception, simply print the stacktrace, and break from the for loop.
				e.printStackTrace();
				break;
			}
		}

		// Return the route array list as a new array of routes. Yes, they are different.
		return routes.toArray(new Route[0]);
	}

	/**
	 * TODO Documentation
	 * @param routeName
	 * @param oldRoutes
	 * @return
	 */
	public static Route[] enableRoutes(String routeName, Route[] oldRoutes) {
		Log.d("enableRoutes", "Enabling route: " + routeName);

		Route[] routes = Arrays.copyOf(oldRoutes, oldRoutes.length+1);

		// If the route is to be enabled, iterate through all the allRoutes that are able to be tracked.
		for (Route route : MapsActivity.allRoutes) {

			// If the route that is able to be tracked is equal to that of the route entered as an argument,
			// add that route to the selected allRoutes array.
			if (route.routeName.equals(routeName)) {
				Log.d("enableRoutes", "Found matching route!");
				routes[oldRoutes.length] = route;

				// Since we only add one route at a time (as there is only one routeName argument),
				// break as soon as its added.
				break;
			}
		}

		return routes;
	}

	/**
	 * Loads the stops from the provided url.
	 * If there was an error parsing the JSON data for the stop object,
	 * then this will return what stops it had parsed successfully up until that point.
	 * As a result, the Stop array returned may be smaller than expected, or have a length of 0.
	 *
	 * @param routeMatch The route match instance (for pulling from the RouteMatch server).
	 * @return The array of stops that were loaded.
	 */
	public Stop[] loadStops(RouteMatch routeMatch) {

		// Create an array that will store the parsed stop objects
		ArrayList<Stop> returnArray = new ArrayList<>();

		// Get the data from all the stops and store it in a JSONArray.
		JSONArray data = RouteMatch.parseData(routeMatch.getAllStops(this));

		// Iterate through the data in the JSONArray
		int count = data.length();
		for (int index = 0; index < count; index++) {

			// Output the current index for debugging reasons
			Log.d("loadStops", String.format("Parsing stop %d/%d", index + 1, count));

			Stop stop;
			try {
				// Using the data from the stop array, try to create a new stop object.
				stop = new Stop(data.getJSONObject(index), this);
			} catch (JSONException e) {
				// If a JSONException was thrown, that means that there was an issue parsing the stop data.
				// Just print a warning message, and then break from the for loop.
				// This will cause the function to return whatever was in the return array.
				Log.w("loadStops", "An issue occurred while attempting to parse the JSON data");
				break;
			}

			// Iterate through the return array and check if the created stop is a duplicate.
			boolean found = false;
			for (Stop s : returnArray) {
				// If the stop is a duplicate (matches latitude, longitude, and route),
				// set the found boolean to true, abd break form the for loop
				if (stop.latitude == s.latitude && s.longitude == s.longitude && stop.route.equals(s.route)) {
					found = true;
					break;
				}
			}

			// If the stop was never found, add it to the return array.
			if (!found) {
				Log.d("loadStops", "Adding stop: " + stop.stopID + " to the array");
				returnArray.add(stop);
			}
		}

		// Return the array list that contains all the stops as a new Stop array.
		return returnArray.toArray(new Stop[0]);
	}
}
