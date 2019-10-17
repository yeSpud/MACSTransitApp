package fnsb.macstransit.RouteMatch;

import android.graphics.Color;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import fnsb.macstransit.Exceptions.RouteMatchException;

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
	 * @param color     The routes color. This is optional,
	 *                  and of the color is non-existent simply use the {@code Route(String routeName)} constrctor.
	 */
	public Route(String routeName, int color) {
		this(routeName);
		this.color = color;
	}

	/**
	 * TODO Documentaiton and comments
	 *
	 * @return
	 */
	public static Route[] generateRoutes(String url) throws InterruptedException {
		ArrayList<Route> routes = new ArrayList<>();

		// TODO Fix exception handling
		Thread t = new Thread(() -> {
			// First, get the master schedual from the provided url
			JSONObject masterSchedual = RouteMatch.readJsonFromUrl(url + "masterRoute");
			Log.d("Master Schedual", masterSchedual.toString());

			// Now get the data array from the JSON oblect
			JSONArray data = null;
			try {
				data = masterSchedual.getJSONArray("data");
			} catch (JSONException e) {
				e.printStackTrace();
				return;
			}

			Log.d("Schedual data", data.toString());

			// Itterate through the data array to begin parsing the routes
			for (int index = 0; index < data.length(); index++) {
				JSONObject routeData = null;
				try {
					routeData = data.getJSONObject(index);

					Log.d("routeData", "Parsing routeData: " + routeData);

					// First, parse the name
					String name = routeData.getString("shortName");
					Log.d("routeData", "Name: " + name);

					// Now try to parse the color
					try {
						int color = Color.parseColor(routeData.getString("routeColor"));
						routes.add(new Route(name, color));
					} catch (IllegalArgumentException | JSONException colorError) {
						Log.w("generateRoutes", "Unable to determine route color");
						// Just return the route with the name
						routes.add(new Route(name));
					}

				} catch (JSONException e) {
					e.printStackTrace();
					break;
				}
			}
		});
		t.start();
		t.join(1000);

		return routes.toArray(new Route[0]);
	}
}
