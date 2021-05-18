package fnsb.macstransit.Threads;

import com.android.volley.Response;

import org.json.JSONObject;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.Activities.SplashActivity;
import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.RouteMatch;

/**
 * Created by Spud on 5/17/21 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.2.6.
 */
public class MasterScheduleCallback implements Response.Listener<JSONObject> {

	/**
	 * TODO Documentation
	 */
	private final SplashActivity activity;

	/**
	 * TODO Documentation
	 * @param activity
	 */
	public MasterScheduleCallback(SplashActivity activity) {
		this.activity = activity;
	}

	@Override
	public void onResponse(JSONObject response) {
		this.activity.setProgressBar(1);
		this.activity.setMessage(R.string.loading_bus_routes);

		org.json.JSONArray routes = RouteMatch.parseData(response);
		if (routes.length() == 0) {
			this.activity.setMessage(R.string.its_sunday);

			// Also add a chance for the user to retry.
			this.activity.showRetryButton();
			SplashActivity.loaded = true;
			return;
		}

		MapsActivity.allRoutes = Route.generateRoutes(routes);
		this.activity.setProgressBar(1 + 8);

		// Map bus routes (map polyline coordinates).
		this.activity.mapBusRoutes();

		// Map bus stops.
		this.activity.mapBusStops();

		// Map shared stops.
		this.activity.mapSharedStops();

		// Validate stops.
		this.activity.validateStops();

		// Finally, launch the maps activity.
		this.activity.launchMapsActivity();

	}
}
