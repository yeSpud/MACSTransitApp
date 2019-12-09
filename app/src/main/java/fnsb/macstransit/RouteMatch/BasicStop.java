package fnsb.macstransit.RouteMatch;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;

import java.util.ArrayList;

/**
 * Created by Spud on 2019-11-03 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.2
 * @since Beta 7.
 */
@SuppressWarnings("WeakerAccess")
public class BasicStop extends MarkedObject {

	/**
	 * The starting radius of the parent circle for the stop.
	 * Because this is used for a reference, it must never be changed during run time!
	 */
	public static final double PARENT_RADIUS = 50.0d;

	/**
	 * The latitude and longitude (coordinates) of the stop.
	 */
	public double latitude, longitude;

	/**
	 * The parentRoute that this stop corresponds to.
	 */
	public Route parentRoute;

	/**
	 * The ID of the stop. This is usually the stop name.
	 */
	public String stopID;

	/**
	 * The circle options that will be applied to the parent circle of the stop.
	 */
	public CircleOptions parentCircleOptions;

	/**
	 * The parent circle of the stop.
	 * The reason this is private is because it should be retrieved via the getter method,
	 * and should be set via the setter method for finer control.
	 */
	private Circle parentCircle;

	/**
	 * Constructor for the BasicStop object. All that is required is the stopID, latitude, longitude,
	 * and route.
	 *
	 * @param stopID    The ID of the stop. This is typically the name of the Stop.
	 * @param latitude  The latitude of the stop.
	 * @param longitude The longitude of the stop.
	 * @param route     The route this stop corresponds to.
	 */
	public BasicStop(String stopID, double latitude, double longitude, Route route) {
		// Set the stop ID, coordinates, and the corresponding parentRoute.
		this.stopID = stopID;
		this.latitude = latitude;
		this.longitude = longitude;
		this.parentRoute = route;
	}

	/**
	 * Loads all the stops from the provided routes into an array of BasicStop objects.
	 *
	 * @param routes The routes to load the stops for.
	 * @return The array of all the stops for the routes provided.
	 */
	public static BasicStop[] loadAllStops(Route[] routes) {
		// Create an array-list to store all the stops.
		ArrayList<BasicStop> stops = new ArrayList<>();

		// Iterate through all the childRoutes provided in the argument.
		for (Route r : routes) {

			// Iterate through all the stops in the parentRoute.
			for (fnsb.macstransit.RouteMatch.Stop s : r.stops) {

				// Add the stop to the array of stops.
				stops.add(new BasicStop(s.stopID, s.latitude, s.longitude, s.parentRoute));
			}
		}
		Log.i("loadAllStops", String.format("Successfully loaded %d stops", stops.size()));

		// Convert the stop array list to an array of stops, and return it.
		return stops.toArray(new BasicStop[0]);
	}

	/**
	 * Creates and adds a parentCircle to the provided map with the provided circle options.
	 * It should be noted that this does not override the current parentCircle defined for this class,
	 * but rather just creates a new, unconstrained circle.
	 *
	 * @param map       The map to add the parentCircle to.
	 * @param options   The circle options to apply to the circle.
	 *                  Note that this will not override what is set for the
	 *                  parentCircleOptions object in this class.
	 * @param clickable Whether or not the circle should be clickable.
	 * @return The newly created parentCircle.
	 */
	public Circle addCircle(GoogleMap map, CircleOptions options, boolean clickable) {
		// Add the circle to the map.
		Log.d("addCircle", "Adding circle to map");
		Circle circle = map.addCircle(options);

		// Set the tag of the circle.
		Log.d("addCircle", "Setting circle tag to: " + this);
		circle.setTag(this);

		// Set it to be clickable or not (depending on the boolean value of clickable).
		Log.d("addCircle", "Setting circle to be clickable: " + clickable);
		circle.setClickable(clickable);

		// Return the circle.
		return circle;
	}

	/**
	 * Retrieves the parentCircle that corresponds to this stop.
	 * This may be null if the parentCircle was never created.
	 *
	 * @return The parent circle.
	 */
	public Circle getCircle() {
		return this.parentCircle;
	}

	/**
	 * Adds the parent circle to the map, as well as sets the objects parent circle.
	 * Using this method will default to the corresponding parentCircleOptions
	 * as opposed to something that is custom.
	 *
	 * @param map       The map to add the parentCircle to.
	 * @param clickable Whether or not the parentCircle should be clickable.
	 */
	public void setCircle(GoogleMap map, boolean clickable) {
		this.parentCircle = this.addCircle(map, this.parentCircleOptions, clickable);
	}
}
