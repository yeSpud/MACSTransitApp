package fnsb.macstransit.RouteMatch;

import android.util.Log;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Spud on 2019-10-18 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.3
 * @since Beta 6.
 */
public class Stop extends BasicStop {

	/**
	 * Creates a stop object.
	 *
	 * @param stopID    The ID of the stop. This is usually the stop name.
	 * @param latitude  The latitude of the stop.
	 * @param longitude The longitude of the stop.
	 * @param route     The parentRoute this stop corresponds to.
	 */
	public Stop(String stopID, double latitude, double longitude, Route route) {
		super(stopID, latitude, longitude, route);

		// Setup the parentCircleOptions that will be used on the stop icon.
		// This is really just setting up the coordinates, and the initial radius of the stop.
		CircleOptions circleOptions = new CircleOptions()
				.center(new com.google.android.gms.maps.model.LatLng(this.latitude, this.longitude))
				.radius(Stop.PARENT_RADIUS);

		// If the parentRoute color isn't null, set the stop color to the same color as the parentRoute color.
		if (this.parentRoute.color != 0) {
			circleOptions.fillColor(this.parentRoute.color);
			circleOptions.strokeColor(this.parentRoute.color);
		}

		// Set the icon parentCircleOptions to the newly created parentCircleOptions,
		// though don't apply the parentCircleOptions to the icon just yet.
		this.parentCircleOptions = circleOptions;
	}

	/**
	 * Loads the stop from the provided JSON.
	 * <p>
	 * This simply parses the JSON to the constructor.
	 *
	 * @param json  The JSON to be parsed for the stop.
	 * @param route The parentRoute this stop belongs to. This will also be passed to the constructor.
	 * @throws org.json.JSONException Thrown if there is an exception in parsing the JSON (ie missing a queried field).
	 */
	public Stop(org.json.JSONObject json, Route route) throws org.json.JSONException {
		this(json.getString("stopId"), json.getDouble("latitude"), json.getDouble("longitude"), route);
	}

	/**
	 * Adds a stop to the map based on the childRoutes.
	 * This will not add the stop to the map if it is a shared stop.
	 * Because of this check, this should be called after the sharedStops have been found and created.
	 *
	 * @param map         The map to add the stops to.
	 * @param routes      The childRoutes to add the stops to.
	 * @param sharedStops The array of shared stops to check if the stops have already been added.
	 */
	public static void addStop(com.google.android.gms.maps.GoogleMap map, Route[] routes,
	                           SharedStop[] sharedStops) {
		for (Route route : routes) {

			// Iterate through the stops in the parentRoute and execute the following:
			for (Stop stop : route.stops) {

				// Create a boolean that will be used to verify if a stop has been found or not
				boolean found = false;

				// Iterate through the shared stops and check if the stop we are using
				// to iterate is also within the shared stop array (by stop id only).
				for (SharedStop sharedStop : sharedStops) {
					if (sharedStop.stopID.equals(stop.stopID)) {
						// If the stop was indeed found (by id), set the found boolean to true,
						// and break from the shared stop for loop.
						found = true;
						break;
					}
				}

				// If the stop was never found (was never in the shared stop array),
				// add it to the map, but set it to invisible.
				if (!found) {
					stop.setCircle(map, true);
					Marker marker = stop.addMarker(map, stop.latitude, stop.longitude,
							stop.parentRoute.color, stop.stopID);
					marker.setVisible(false);
					stop.setMarker(marker);
				}
			}
		}
	}

	/**
	 * Clears all the stops in the provided childRoutes.
	 *
	 * @param routes The childRoutes to have the stops cleared from.
	 */
	public static void removeStops(Route[] routes) {
		Log.d("removeStops", "Clearing all stops");

		// Iterate through all the stops in the selected childRoutes and execute the following:
		for (Route route : routes) {
			Log.d("removeStops", "Clearing stops for parentRoute: " + route.routeName);

			// Iterate through all the stops in the parentRoute and execute the following:
			for (Stop stop : route.stops) {

				// Get the marker from the stop, and remove it if its not null.
				Marker marker = stop.getMarker();
				if (marker != null) {
					marker.remove();
				}

				// Get the circle from the stop, and remove it of its not null.
				Circle circle = stop.getCircle();
				if (circle != null) {
					circle.remove();
				}
			}
		}
	}
}
