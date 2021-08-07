package fnsb.macstransit.Activities.SplashScreenRunnables;

import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.Activities.SplashActivity;
import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.RouteMatch;
import fnsb.macstransit.RouteMatch.Stop;

/**
 * Created by Spud on 8/6/21 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
public class MapBusStops {

	/**
	 * TODO Documentation
	 */
	private final Collection<Pair<Route, SplashListener>> pairs = new ArrayList<>(); // TODO Don't use arrayList

	/**
	 * TODO Documentation
	 * @param pair TODO
	 */
	public void addListener(Pair<Route, SplashListener> pair) {
		pairs.add(pair);
	}

	/**
	 * TODO Documentation & comments
	 */
	public void getBusStops(SplashActivity activity) {

		// Verify that allRoutes is not null. If it is then log and return early.
		if (MapsActivity.allRoutes == null) {
			Log.w("getBusRoutes", "All routes is null!");
			return;
		}

		final double step = (double) SplashActivity.DOWNLOAD_BUS_STOPS / MapsActivity.allRoutes.length;
		double progress =  SplashActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS + SplashActivity.PARSE_MASTER_SCHEDULE
				+ SplashActivity.DOWNLOAD_BUS_ROUTES + SplashActivity.LOAD_BUS_ROUTES;
		Log.d("getBusStops", "Step value: " + step);

		for (final Pair<Route, SplashListener> pair : pairs) {
			BusStopCallback callback = new BusStopCallback(pair.second, pair.first, activity);
			MapsActivity.routeMatch.callAllStops(pair.first, callback, error -> Log.w("loadStops",
					"Unable to get stops from RouteMatch server", error), this);

			progress += step;
			activity.setProgressBar(progress);
		}

		activity.setProgressBar(progress);

	}

	class BusStopCallback implements com.android.volley.Response.Listener<JSONObject> {

		/**
		 * TODO Documentation
		 */
		@Deprecated
		private final SplashActivity activity;

		/**
		 * TODO Documentation
		 */
		private final SplashListener listener;

		/**
		 * TODO Documentation
		 */
		private final Route route;

		/**
		 * TODO Documentation
		 * @param listener TODO
		 * @param route TODO
		 */
		BusStopCallback(SplashListener listener, Route route, SplashActivity activity) {
			this.activity = activity;
			this.listener = listener;
			this.route = route;
		}

		@Override
		public void onResponse(JSONObject response) {

			// Display that we are mapping bus stops to the user.
			activity.setMessage(R.string.mapping_bus_stops);

			// Get the data from all the stops and store it in a JSONArray.
			JSONArray data = RouteMatch.parseData(response);

			// Load in all the potential stops for the route.
			// The reason why this is considered potential stops is because at this stage duplicate
			// stops have not yet been handled.
			Stop[] potentialStops = Stop.generateStops(data, route);

			// Create a variable to store the true size of the stops that have been validated.
			int validatedSize = 0;

			// Create an array to store the validated stops.
			// While we don't know the specific size of this array until done, we do know the maximum size,
			// so use that for setting the array size.
			Stop[] validatedStops = new Stop[potentialStops.length];

			// Iterate through each stop in our array of potential stops.
			for (Stop stop : potentialStops) {

				// Check to see if the stop is in our array of validated stops. If its not,
				// add it to the array and add 1 to the true index size of stops that have been validated.
				if (!Stop.isDuplicate(stop, validatedStops)) {
					validatedStops[validatedSize] = stop;
					validatedSize++;
				}
			}

			// Create an array for our actual stops.
			// Since we now know the number of validated stops we can use that as its size.
			Stop[] actualStops = new Stop[validatedSize];

			// Copy our validated stops into our smaller actual stops array, and return it.
			System.arraycopy(validatedStops, 0, actualStops, 0, actualStops.length);

			// At this point duplicate stops have now been handled and removed.
			this.route.stops = actualStops;

			this.listener.splashRunnableFinished();

		}
	}
}
