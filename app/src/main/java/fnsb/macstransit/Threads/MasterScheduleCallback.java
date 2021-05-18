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
	 * TODO Documentation
	 */
	@Deprecated
	private final SplashActivity activity;

	/**
	 * TODO Documentation
	 *
	 * @param activity
	 */
	public MasterScheduleCallback(SplashActivity activity) {
		this.activity = activity;
	}

	// TODO Comments
	@Override
	public void onResponse(JSONObject response) {
		this.activity.setProgressBar(1);
		this.activity.setMessage(R.string.loading_bus_routes);

		org.json.JSONArray routes = fnsb.macstransit.RouteMatch.RouteMatch.parseData(response);
		if (routes.length() == 0) {
			this.activity.setMessage(R.string.its_sunday);

			// Also add a chance for the user to retry.
			this.activity.showRetryButton();
			SplashActivity.loaded = true;
			return;
		}

		fnsb.macstransit.Activities.MapsActivity.allRoutes = fnsb.macstransit.RouteMatch.Route.generateRoutes(routes);

		synchronized (SplashActivityLock.LOCK) {
			SplashActivityLock.LOCK.notifyAll();
		}
	}
}
