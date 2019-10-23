package fnsb.macstransit.RouteMatch;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import fnsb.macstransit.Network;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.0
 * @since Beta 3
 */
public class Route {

	/**
	 * The name of the route.
	 */
	public String routeName; // TODO Make sure this doesn't have whitespace

	/**
	 * The color of the route. This is optional, as there is a high chance that the route does not have one.
	 * <p>
	 * This is an int instead of a Color object because for whatever reason android stores its colors as ints.
	 */
	public int color;

	/**
	 * TODO Documentation
	 */
	public Stop[] stops;

	/**
	 * Constructor for the route. The name of the route is the only thing that is required.
	 *
	 * @param routeName The name of the route.
	 */
	public Route(String routeName) {
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
	 * TODO Update comments and code
	 * Dynamically generates the routes that are used by parsing the master schedule.
	 *
	 * @return An array of routes that <b><i>can be</i></b> tracked.
	 */
	public static Route[] generateRoutes(String url) {

		// Create an array to store all the generated routes. This will be returned in the end.
		ArrayList<Route> routes = new ArrayList<>();

		// First, get the master schedule from the provided url
		JSONObject masterSchedule = Network.getJsonFromUrl(url + "masterRoute");

		// Now get the data array from the JSON object
		JSONArray data;
		try {
			data = masterSchedule.getJSONArray("data");
		} catch (JSONException e) {
			// If there was a JSONException in parsing the data, just return from the thread now!
			Log.w("generateRoutes", "Unable to parse master route! Returning empty route instead.");
			return new Route[0];
		}

		// Iterate through the data array to begin parsing the routes
		int count = data.length();
		for (int index = 0; index < count; index++) {
			JSONObject routeData;
			try {

				// TODO Documentation update
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

		return routes.toArray(new Route[0]);
	}

	/**
	 * TODO Documentation
	 *
	 * @param url
	 * @return
	 */
	public Stop[] loadStops(String url) {
		ArrayList<Stop> returnArray = new ArrayList<>();

		JSONObject allStops = Network.getJsonFromUrl(url + "stops/" + this.routeName);

		JSONArray data;
		try {
			data = allStops.getJSONArray("data");
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}

		int count = data.length();
		for (int index = 0; index < count; index++) {
			JSONObject stopData;
			try {

				Log.d("loadStops", String.format("Parsing stop %d/%d", index + 1, count));
				stopData = data.getJSONObject(index);

				Stop stop = new Stop(stopData.getString("stopId"),
						stopData.getDouble("latitude"),
						stopData.getDouble("longitude"), this);

				stop.color = this.color;

				boolean found = false;
				for (Stop s : returnArray) {
					if (stop.latitude == s.latitude && s.longitude == s.longitude && stop.route.equals(s.route)) {
						found = true;
						break;
					}
				}

				if (!found) {
					Log.d("loadStops", "Adding stop: " + stop.stopID + " to the array");
					returnArray.add(stop);
				}
			} catch (JSONException e) {
				e.printStackTrace();
				break;
			}
		}

		return returnArray.toArray(new Stop[0]);
	}
}
