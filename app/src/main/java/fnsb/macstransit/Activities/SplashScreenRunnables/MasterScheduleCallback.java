package fnsb.macstransit.Activities.SplashScreenRunnables;

import android.util.Log;

import org.json.JSONObject;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.Activities.SplashActivity;
import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.Route;

/**
 * Created by Spud on 5/17/21 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.2.6.
 */
public class MasterScheduleCallback implements com.android.volley.Response.Listener<JSONObject> {

	/**
	 * The activity this callback corresponds to.
	 * This is deprecated because it is a potential memory leak.
	 */
	@Deprecated
	private final SplashActivity activity;

	/**
	 * Constructor for the MasterScheduleCallback
	 *
	 * @param activity The activity this callback belongs to.
	 */
	public MasterScheduleCallback(SplashActivity activity) {
		this.activity = activity;
	}

	@Override
	public void onResponse(JSONObject response) { // TODO Comments

		// Set the progress and message.
		this.activity.setProgressBar(SplashActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS);
		this.activity.setMessage(R.string.loading_bus_routes);

		// Get the routes from the JSONObject
		org.json.JSONArray routes = fnsb.macstransit.RouteMatch.RouteMatch.parseData(response);

		// If the routes length is 0, say that there are no buses for the day.
		int count = routes.length();
		if (count == 0) {
			this.activity.setMessage(R.string.its_sunday);

			// Also add a chance for the user to retry.
			this.activity.showRetryButton();
			SplashActivity.loaded = true;
			return;
		}

		// Create an array to store all the generated routes.
		Route[] potentialRoutes = new Route[count];
		int routeCount = 0;
		final double step = (double) SplashActivity.PARSE_MASTER_SCHEDULE / count;
		this.activity.setMessage(R.string.parsing_master_schedule);

		// Iterate though each route in the master schedule.
		for (int index = 0; index < count; index++) {
			Log.d("MasterScheduleCallback", String.format("Parsing route %d/%d", index + 1, count));

			// Try to get the route data from the array.
			// If there's an issue parsing the data simply continue to the next iteration of the loop.
			JSONObject routeData;
			try {
				routeData = routes.getJSONObject(index);
			} catch (org.json.JSONException e) {
				Log.w("MasterScheduleCallback", "Issue retrieving the route data", e);
				continue;
			}

			// Try to create the route using the route data obtained above.
			// If there was a route exception thrown simply log it.
			try {
				Route route = Route.generateRoute(routeData);
				potentialRoutes[routeCount] = route;
				routeCount++;
			} catch (Route.RouteException | java.io.UnsupportedEncodingException e) {
				Log.w("MasterScheduleCallback", "Issue creating route from route data", e);
			}

			this.activity.setProgressBar(SplashActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS + step);
		}

		// Down size our potential routes array to fit the actual number of routes.
		MapsActivity.allRoutes = new Route[routeCount];
		System.arraycopy(potentialRoutes, 0, MapsActivity.allRoutes, 0, routeCount);

		// Map bus routes (map polyline coordinates).
		this.activity.downloadBusRoutes();
	}
}
