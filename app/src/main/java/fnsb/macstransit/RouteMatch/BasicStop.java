package fnsb.macstransit.RouteMatch;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Spud on 2019-11-03 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.2
 * @since Beta 7.
 */
@Deprecated
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

		Log.i("loadAllStops", String.format("Successfully loaded %d stops", stops.size()));

		// Convert the stop array list to an array of stops, and return it.
		return stops.toArray(new BasicStop[0]);
	}
}
