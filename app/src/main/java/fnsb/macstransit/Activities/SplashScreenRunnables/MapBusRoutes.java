package fnsb.macstransit.Activities.SplashScreenRunnables;

import android.util.Log;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.Activities.SplashActivity;
import fnsb.macstransit.RouteMatch.Route;

/**
 * Created by Spud on 8/5/21 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
public class MapBusRoutes {

	/**
	 * TODO Documentation
	 */
	private final java.util.Collection<Pair<Route, SplashListener>> pairs = new java.util.ArrayList<>(); // TODO Don't use arrayList

	/**
	 * TODO Documentation
	 *
	 * @param pair TODO
	 */
	public void addListener(Pair<Route, SplashListener> pair) {
		pairs.add(pair);
	}

	/**
	 * Loads the polyline coordinates for the route object by retrieving the array from the RouteMatch server.
	 * This method will either set the polyline coordinates for the route,
	 * or will return early if the route match object is null.
	 *
	 * @param activity TODO
	 */
	public void getBusRoutes(SplashActivity activity) { // TODO Comments

		// Verify that allRoutes is not null. If it is then log and return early.
		if (MapsActivity.allRoutes == null) {
			Log.w("getBusRoutes", "All routes is null!");
			return;
		}

		final double step = (double) SplashActivity.DOWNLOAD_BUS_ROUTES / MapsActivity.allRoutes.length;
		double progress = SplashActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS + SplashActivity.PARSE_MASTER_SCHEDULE;
		Log.d("getBusRoutes", "Step value: " + step);

		// Iterate though each route, and try to load the polyline in each of them.
		for (final Pair<Route, SplashListener> pair : pairs) {

			BusRoutesCallback callback = new BusRoutesCallback(pair.second, pair.first, activity);

			// Get the land route from the routematch API using an asynchronous process.
			MapsActivity.routeMatch.callLandRoute(pair.first, callback, error ->
					Log.w("getBusRoutes", "Unable to get polyline from routematch server",
							error), this);

			progress += step;
			activity.setProgressBar(progress);
		}

	}

	/**
	 * TODO Documentation
	 */
	class BusRoutesCallback implements com.android.volley.Response.Listener<JSONObject> {

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
		 *
		 * @param listener TODO
		 * @param route    TODO
		 * @param activity TODO
		 */
		BusRoutesCallback(SplashListener listener, Route route, SplashActivity activity) {
			this.activity = activity;
			this.route = route;
			this.listener = listener;
		}

		@Override
		public void onResponse(JSONObject response) { // TODO Comments

			if (MapsActivity.allRoutes == null) {
				return; // TODO Log
			}

			// Display that we are mapping bus routes to the user.
			activity.setMessage(fnsb.macstransit.R.string.mapping_bus_routes);

			try {

				// Get the land route data array from the land route object.
				JSONArray landRouteData = fnsb.macstransit.RouteMatch.RouteMatch.parseData(response);

				// Get the land route points object from the land route data array.
				JSONObject landRoutePoints = landRouteData.getJSONObject(0);

				// Get the land route points array from the land route points object.
				JSONArray landRoutePointsArray = landRoutePoints.getJSONArray("points");

				// Get the number of points in the array.
				int count = landRoutePointsArray.length();

				// Create a new LatLng array to store all the coordinates.
				LatLng[] coordinates = new LatLng[count];

				// Initialize the array of coordinates by iterating through the land route points array.
				for (int i = 0; i < count; i++) {

					// Get the land route point object from the land route points array.
					JSONObject landRoutePoint = landRoutePointsArray.getJSONObject(i);

					// Get the latitude and longitude from the land route point.
					double latitude = landRoutePoint.getDouble("latitude"),
							longitude = landRoutePoint.getDouble("longitude");

					// Create a new LatLng object using the latitude and longitude.
					LatLng latLng = new LatLng(latitude, longitude);

					// Add the newly created LatLng object to the LatLng array.
					coordinates[i] = latLng;
				}

				// Set the polyline coordinates array to the finished LatLng array.
				this.route.polyLineCoordinates = coordinates;

				this.listener.splashRunnableFinished();

			} catch (org.json.JSONException exception) {
				Log.e("getBusRoutes", "Error parsing json", exception);
			}
		}
	}
}
