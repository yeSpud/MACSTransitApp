package fnsb.macstransit.Activities.SplashScreenRunnables;

import android.util.Log;
import android.util.Pair;

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
	 * TODO Documentation
	 */
	private int mapBusProgress = 0;

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

		this.activity.setProgressBar(1 + 8);

		MapBusRoutes mapBusRoutes = new MapBusRoutes();
		for (Route route : MapsActivity.allRoutes) {
			this.mapBusProgress++;
			Pair<Route, SplashListener> pair = new Pair<>(route, () -> {
				this.activity.setProgressBar(1 + 8 + 1);
				this.mapBusProgress--;
				this.checkRunnableState();
			});
			mapBusRoutes.addListener(pair);
		}
		mapBusRoutes.getBusRoutes();

	}

	private void checkRunnableState() {
		Log.v("checkRunnableState", "Map progress remaining: " + this.mapBusProgress);
		if (this.mapBusProgress == 0) {
			// TODO Move on.
		}
	}

}
