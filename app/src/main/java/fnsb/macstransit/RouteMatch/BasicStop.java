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
 * @since Beta 7
 */
public class BasicStop extends MarkedObject {

	/**
	 * TODO Documentation
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
	 * TODO Documentation
	 */
	public CircleOptions parentCircleOptions;

	/**
	 * TODO Documentation
	 */
	private Circle parentCircle;

	/**
	 * Constructor for the BasicStop object. All that is required is the stopID, latitude, longitude,
	 * and parentRoute.
	 *
	 * @param stopID    The ID of the stop. This is typically the name of the Stop.
	 * @param latitude  The latitude of the stop.
	 * @param longitude The longitude of the stop.
	 * @param route     The parentRoute this stop corresponds to.
	 */
	public BasicStop(String stopID, double latitude, double longitude, Route route) {
		// Set the stop ID, coordinates, and the corresponding parentRoute.
		this.stopID = stopID;
		this.latitude = latitude;
		this.longitude = longitude;
		this.parentRoute = route;
	}

	/**
	 * TODO Documentation
	 *
	 * @param routes
	 * @return
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
	 * TODO Documentation
	 *
	 * @param map
	 * @param options
	 * @param clickable
	 * @return
	 */
	Circle addCircle(GoogleMap map, com.google.android.gms.maps.model.CircleOptions options, boolean clickable) {
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
	 * TODO Documentation
	 *
	 * @return
	 */
	public Circle getCircle() {
		return this.parentCircle;
	}

	/**
	 * TODO Documentation
	 *
	 * @param map
	 * @param clickable
	 */
	public void setCircle(GoogleMap map, boolean clickable) {
		this.parentCircle = this.addCircle(map, this.parentCircleOptions, clickable);
	}
}
