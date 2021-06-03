package fnsb.macstransit.Threads;

import org.json.JSONObject;

import fnsb.macstransit.Activities.SplashActivity;
import fnsb.macstransit.R;

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
	public void onResponse(JSONObject response) {

		// Set the progress and message.
		this.activity.setProgressBar(1);
		this.activity.setMessage(R.string.loading_bus_routes);

		// Get the routes from the JSONObject
		org.json.JSONArray routes = fnsb.macstransit.RouteMatch.RouteMatch.parseData(response);

		// If the routes length is 0, say that there are no buses for the day.
		if (routes.length() == 0) {
			this.activity.setMessage(R.string.its_sunday);

			// Also add a chance for the user to retry.
			this.activity.showRetryButton();
			SplashActivity.loaded = true;
			return;
		}

		// Set all the routes to the generated routes.
		fnsb.macstransit.Activities.MapsActivity.allRoutes = fnsb.macstransit.RouteMatch.Route.generateRoutes(routes);

		// Notify the activity to continue.
		synchronized (SplashActivityLock.LOCK) {
			SplashActivityLock.LOCK.notifyAll();
		}
	}
}
