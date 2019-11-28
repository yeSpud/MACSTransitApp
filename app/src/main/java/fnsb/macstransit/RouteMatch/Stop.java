package fnsb.macstransit.RouteMatch;

import android.util.Log;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Marker;

import fnsb.macstransit.Activities.ActivityListeners.Helpers;

/**
 * Created by Spud on 2019-10-18 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.2
 * @since beta 6.
 */
public class Stop extends BasicStop {

	/**
	 * The initial radius size (in meters) for the circle that represents a stop on the map.
	 */
	public static final double RADIUS = 50.0d;

	/**
	 * The options that correspond to the stop icon (which is a circle).
	 * This is primarily used for the position, as well as determining and setting the current radius.
	 */
	public CircleOptions iconOptions;

	/**
	 * The stop icon that is shown on the map.
	 * <p>
	 * Its really just a circle :P
	 * <p>
	 * The reason why this is kept private is because it cannot be created outside a maps activity class
	 * (otherwise an error is invoked).
	 */
	private Circle icon;

	/**
	 * Creates a stop object.
	 *
	 * @param stopID    The ID of the stop. This is usually the stop name.
	 * @param latitude  The latitude of the stop.
	 * @param longitude The longitude of the stop.
	 * @param route     The route this stop corresponds to.
	 */
	public Stop(String stopID, double latitude, double longitude, Route route) {
		super(stopID, latitude, longitude, route);

		// Setup the options that will be used on the stop icon.
		// This is really just setting up the coordinates, and the initial radius of the stop.
		CircleOptions options = new CircleOptions().center(new com.google.android.gms.maps.model.LatLng(this.latitude, this.longitude))
				.radius(Stop.RADIUS);

		// If the route color isn't null, set the stop color to the same color as the route color.
		if (this.route.color != 0) {
			options.fillColor(this.route.color);
			options.strokeColor(this.route.color);
		}

		// Set the icon options to the newly created options, though don't apply the options to the icon just yet.
		this.iconOptions = options;
	}

	/**
	 * Loads the stop from the provided JSON.
	 * <p>
	 * This simply parses the JSON to the constructor.
	 *
	 * @param json  The JSON to be parsed for the stop.
	 * @param route The route this stop belongs to. This will also be passed to the constructor.
	 * @throws org.json.JSONException Thrown if there is an exception in parsing the JSON (ie missing a queried field).
	 */
	public Stop(org.json.JSONObject json, Route route) throws org.json.JSONException {
		this(json.getString("stopId"), json.getDouble("latitude"), json.getDouble("longitude"), route);
	}

	/**
	 * Adds a stop to the map based on the routes.
	 * This will not add the stop to the map if it is a shared stop.
	 * Because of this check, this should be called after the sharedStops have been found and created.
	 *
	 * @param map         The map to add the stops to.
	 * @param routes      The routes to add the stops to.
	 * @param sharedStops The array of shared stops to check if the stops have already been added.
	 */
	public static void addStop(com.google.android.gms.maps.GoogleMap map, Route[] routes, SharedStop[] sharedStops) {
		for (Route route : routes) {

			// Iterate through the stops in the route and execute the following:
			for (Stop stop : route.stops) {

				// Create a boolean that will be used to verify if a stop has been found or not
				boolean found = false;

				// Iterate through the shared stops and check if the stop we are using to iterate is also within the shared stop array (by stop id only).
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
					stop.setIcon(Helpers.addCircle(map, stop.iconOptions, stop, true));
					Marker marker = Helpers.addMarker(map, stop.latitude, stop.longitude, stop.route.color, stop.stopID, stop);
					marker.setVisible(false);
					stop.setMarker(marker);
				}
			}
		}
	}

	/**
	 * Clears all the stops in the provided routes.
	 *
	 * @param routes The routes to have the stops cleared from.
	 */
	public static void removeStops(Route[] routes) {
		Log.d("removeStops", "Clearing all stops");

		// Iterate through all the stops in the selected routes and execute the following:
		for (Route route : routes) {
			Log.d("removeStops", "Clearing stops for route: " + route.routeName);

			// Iterate through all the stops in the route and execute the following:
			for (Stop stop : route.stops) {

				// Get the marker from the stop, and remove it if its not null.
				Marker marker = stop.getMarker();
				if (marker != null) {
					marker.remove();
				}

				// Get the circle from the stop, and remove it of its not null.
				Circle circle = stop.getIcon();
				if (circle != null) {
					circle.remove();
				}
			}
		}
	}

	/**
	 * Gets the icon corresponding to the stop. This is really just a circle that may or may not be colored as well.
	 * This may be null if the icon has never been set via {@code setIcon()}.
	 *
	 * @return The stop icon (a Circle object).
	 */
	public Circle getIcon() {
		return this.icon;
	}

	/**
	 * Sets the icon, as well as providing the tag, and makes sure that the icon is clickable.
	 *
	 * @param icon The icon created in the map activity (usually done by {@code map.addCircle()}).
	 */
	public void setIcon(Circle icon) {
		this.icon = icon;
		this.icon.setTag(this);
		this.icon.setClickable(true);
	}
}
