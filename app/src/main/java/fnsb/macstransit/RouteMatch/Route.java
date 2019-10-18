package fnsb.macstransit.RouteMatch;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 */
public class Route {

	/**
	 * The name of the route.
	 */
	public String routeName;

	/**
	 * The color of the route. This is optional, as there is a high chance that the route does not have one.
	 * <p>
	 * This is an int instead of a Color object because for whatever reason android stores its colors as ints.
	 */
	public int color;

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
	 * Dynamically generates the routes that are used by parsing the master schedule.
	 *
	 * @return An array of routes that <b><i>can be</i></b> tracked.
	 */
	public static Route[] generateRoutes(String url, android.content.Context context) throws InterruptedException {

		// Create an array to store all the generated routes. This will be returned in the end.
		ArrayList<Route> routes = new ArrayList<>();

		// Run the following on a new thread (as android doesn't like running network requests on the UI thread).
		Thread t = new Thread(() -> {
			// First, get the master schedule from the provided url
			JSONObject masterSchedule = RouteMatch.readJsonFromUrl(url + "masterRoute", context);
			Log.d("Master Schedule", masterSchedule.toString());

			// Now get the data array from the JSON object
			org.json.JSONArray data;
			try {
				data = masterSchedule.getJSONArray("data");
			} catch (JSONException e) {
				e.printStackTrace();
				// If there was a JSONException in parsing the data, just return from the thread now!
				return;
			}

			// Display the schedule data for debugging purposes.
			Log.d("Schedule data", data.toString());

			// Iterate through the data array to begin parsing the routes
			for (int index = 0; index < data.length(); index++) {
				JSONObject routeData;
				try {

					// Get the routeData that we are currently parsing as its own JSONObject variable.
					routeData = data.getJSONObject(index);
					Log.d("routeData", "Parsing routeData: " + routeData);

					// First, parse the name
					String name = routeData.getString("shortName");
					Log.d("routeData", "Name: " + name);

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
					// If there was a JSONException with either the routeData, or the name,
					// just break from the loop now.
					// This will also cause the thread to return as this is the last thing in the thread.
					break;
				}
			}
		});
		t.start();
		t.join(1500);

		return routes.toArray(new Route[0]);
	}
}
