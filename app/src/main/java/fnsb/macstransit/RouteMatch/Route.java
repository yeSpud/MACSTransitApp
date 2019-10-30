package fnsb.macstransit.RouteMatch;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


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
	 *
	 * @param routeName The name of the route.
	 */
	public Route(String routeName) {
		// TODO Make sure this doesn't have whitespace
		this.routeName = routeName;
	}

	/**
	 * Constructor for the route. The name of the route is the only thing that is required.
	 *
	 * @param routeName The name of the route.
	 * @param color     The route's color. This is optional,
	 *                  and of the color is non-existent simply use the {@code Route(String routeName)} constructor.
	 */
	public Route(String routeName, int color) {
		this(routeName);
		this.color = color;
	}

	/**
	 * Dynamically generates the routes that are used by parsing the master schedule.
	 * This may return an empty route array if there was an issue parsing the data.
	 * TODO Documentation
	 *
	 * @param routeMatch
	 * @return An array of routes that <b><i>can be</i></b> tracked.
	 */
	public static Route[] generateRoutes(RouteMatch routeMatch) {

		// Create an array to store all the generated routes. This will be returned in the end.
		ArrayList<Route> routes = new ArrayList<>();

		// TODO Refresh comments
		// First, get the master schedule from the provided url


		// Now get the data array from the JSON object
		JSONArray data = RouteMatch.parseData(routeMatch.getMasterSchedule());

		// Iterate through the data array to begin parsing the routes
		int count = data.length();
		for (int index = 0; index < count; index++) {
			JSONObject routeData;
			try {

				// Get the current progress for parsing the routes
				Log.d("generateRoutes", String.format("Parsing route %d/%d", index + 1, count));

				// Get the routeData that we are currently parsing as its own JSONObject variable.
				routeData = data.getJSONObject(index);

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

			} catch (JSONException e) {
				e.printStackTrace();
				// If there was a JSONException with either the routeData, or the name, just break from the loop now.
				// This will also cause the thread to return as this is the last thing in the thread.
				break;
			}
		}

		// Return the route array list as a new array of routes. Yes, they are different.
		return routes.toArray(new Route[0]);
	}

	/**
	 * Loads the stops from the provided url.
	 * This may return an empty stop array if there was an issue parsing the data.
	 * TODO Documentation
	 *
	 * @param routeMatch
	 * @return The array of stops that were loaded.
	 */
	public Stop[] loadStops(RouteMatch routeMatch) {

		// Create an array that will store the parsed stop objects
		ArrayList<Stop> returnArray = new ArrayList<>();

		// TODO Update comments
		// Fetch the JSON object that contains all the stops.

		// Parse the JSON object of the stops into a json array based on the data.
		JSONArray data = RouteMatch.parseData(routeMatch.getAllStops(this));

		// Iterate through the data in the JSONArray
		int count = data.length();
		for (int index = 0; index < count; index++) {
			JSONObject stopData;
			try {

				// Output the current index for debugging reasons
				Log.d("loadStops", String.format("Parsing stop %d/%d", index + 1, count));

				// Pull the data from the JSONArray, and store it as a JSONObject
				stopData = data.getJSONObject(index);

				// Create a stop object from the JSONObject
				Stop stop = new Stop(stopData.getString("stopId"),
						stopData.getDouble("latitude"),
						stopData.getDouble("longitude"), this);

				// If the route has a color, assign the color to the stop
				if (this.color != 0) {
					stop.color = this.color;
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

				// If the stop was never found, add it to the return array
				if (!found) {
					Log.d("loadStops", "Adding stop: " + stop.stopID + " to the array");
					returnArray.add(stop);
				}

			} catch (JSONException e) {
				// If there was a JSON error just print a stack trace for now, and break from the for loop
				e.printStackTrace();
				break;
			}
		}

		// Return the array list that contains all the stops as a new Stop array.
		return returnArray.toArray(new Stop[0]);
	}
}
