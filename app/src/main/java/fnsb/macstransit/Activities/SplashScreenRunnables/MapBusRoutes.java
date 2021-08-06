package fnsb.macstransit.Activities.SplashScreenRunnables;

import android.util.Log;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.RouteMatch;

/**
 * Created by Spud on 8/5/21 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
class MapBusRoutes {

	/**
	 * TODO Documentation
	 */
	private List<Pair<Route, SplashListener>> listeners = new ArrayList<>(); // TODO Don't use arrayList

	/**
	 * TODO Documentation
	 * @param pair TODO
	 */
	public void addListener(Pair<Route, SplashListener> pair) {
		listeners.add(pair);
	}

	public void getBusRoutes() {

		// Display that we are mapping bus routes to the user.
		// this.setMessage(R.string.mapping_bus_routes); FIXME

		// Verify that allRoutes is not null. If it is then log and return early.
		if (MapsActivity.allRoutes == null) {
			Log.w("mapBusRoutes", "All routes is null!");
			return;
		}

		// Iterate though each route, and try to load the polyline in each of them.
		for (final Pair<Route, SplashListener> pair : listeners) {
			//route.loadPolyLineCoordinates();

			// Get the land route from the routematch API using an asynchronous process.
			MapsActivity.routeMatch.callLandRoute(pair.first, response -> {

				try {
					// Get the land route data array from the land route object.
					JSONArray landRouteData = RouteMatch.parseData(response);

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
					pair.first.polyLineCoordinates = coordinates;

					// TODO Comments
					pair.second.splashRunnableFinished();

				} catch (JSONException exception) {
					Log.e("loadPolyLineCoordinates", "Error parsing json", exception);
				}
			}, error -> Log.w("loadPolyLineCoordinates", "Unable to get polyline from routematch server", error), this);
		}

		// Update the progress bar one final time for this method.
		// this.setProgressBar(1 + 8 + 1); FIXME

	}

}
