package fnsb.macstransit.RouteMatch;

import android.util.Log;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;

/**
 * Created by Spud on 2019-11-01 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.0
 * @since Beta 7.
 */
@SuppressWarnings("WeakerAccess")
public class SharedStop extends Stop {

	/**
	 * The array of childRoutes that this stop is shared with.
	 */
	public Route[] childRoutes;

	/**
	 * The array of parentCircleOptions that will be applied to the childCircles that correspond to this Stop.
	 */
	public CircleOptions[] childCircleOptions;

	/**
	 * The childCircles that correspond to this stop.
	 */
	private Circle[] childCircles;

	/**
	 * Constructor for the Shared Stop.
	 *
	 * @param stopID    The Stop ID. This is typically the name of the stop.
	 * @param latitude  The latitude of the Stop.
	 * @param longitude The longitude of the Stop.
	 * @param routes    The childRoutes that this Stop corresponds to.
	 */
	private SharedStop(String stopID, double latitude, double longitude, Route[] routes) {
		super(stopID, latitude, longitude, routes[0]);

		// Copy all of the provided routes into the childRoutes array.
		this.childRoutes = new Route[routes.length - 1];
		System.arraycopy(routes, 1, this.childRoutes, 0, this.childRoutes.length);

		// Be sure to create the circle options for the childCircles at this point.
		this.childCircleOptions = this.createCircleOptions();
	}

	/**
	 * Simpler constructor the the Shared Stop.
	 *
	 * @param basicStop The basic stop that will be changed into a Shared Stop.
	 * @param routes    The routes that this Stop corresponds to.
	 */
	private SharedStop(BasicStop basicStop, Route[] routes) {
		this(basicStop.stopID, basicStop.latitude, basicStop.longitude, routes);
	}

	/**
	 * Finds stops that are shared by different routes, as well as current shared stops.
	 *
	 * @param routes             The routes to check for shared stops.
	 * @param currentSharedStops Previous shared stops.
	 * @return An array of Shared Stops that were found.
	 */
	public static SharedStop[] findSharedStops(Route[] routes, SharedStop[] currentSharedStops) {
		// Check if there is only 1 or less routes being tracked. If there is,
		// then there will be no shared stops, so just return.
		if (routes.length <= 1) {
			return new SharedStop[0];
		}

		ArrayList<SharedStop> sharedStops = new ArrayList<>();

		// Put all the stops into an array
		BasicStop[] allStops = BasicStop.loadAllStops(routes);

		// Iterate through all the stops
		for (BasicStop basicStop : allStops) {

			// Create an ArrayList of routes to store all the childRoutes that share this basic stop.
			ArrayList<Route> sharedRoutes = new ArrayList<>();

			// Find which childRoutes share the current basicStop (by ID).
			// Start by iterating though the childRoutes.
			for (Route r : routes) {
				Log.d("findSharedStops", "Checking parentRoute: " + r.routeName);
				// For each route,
				// check the stops to see if their stop ID matches the stop ID we are checking.
				for (Stop s : r.stops) {
					// If the stop matches our ID, then add that route to an array,
					// so that we can add it to a newly created shared stop.
					if (s.stopID.equals(basicStop.stopID)) {
						sharedRoutes.add(r);
						// Since we are only checking the 1 route,
						// break from this stop for loop to check another stop.
						break;
					}
				}
			}

			// Check the sharedRoute array. If its greater than size 1,
			// then it found an additional parentRoute that had a stop in common (so it shares a stop).
			if (sharedRoutes.size() > 1) {
				// Make sure this shared stop isn't already accounted for (will have the same id)!
				boolean found = false;
				for (SharedStop s : currentSharedStops) {
					// If a shared stop has this basic stop's id, mark it as found and break
					if (s.stopID.equals(basicStop.stopID)) {
						found = true;
						break;
					}
				}

				// If the stop was never found, add it to the sharedStops array.
				if (!found) {
					// Convert this basic stop to a shared stop.
					Log.d("findSharedStops", String.format("Found %d shared stop for stop %s",
							sharedRoutes.size(), basicStop.stopID));
					sharedStops.add(new SharedStop(basicStop, sharedRoutes.toArray(new Route[0])));
				}
			}
		}
		return sharedStops.toArray(new SharedStop[0]);
	}

	/**
	 * Adds the shared stops to the map.
	 *
	 * @param map         The map to add the shared stops to.
	 * @param sharedStops The array of shared stops to be added to the map.
	 */
	public static void addSharedStop(com.google.android.gms.maps.GoogleMap map, SharedStop[] sharedStops) {
		// Iterate through all the shared stops provided and execute the following:
		for (SharedStop sharedStop : sharedStops) {
			Log.d("addSharedStop", String.format("Adding stop %s to the map", sharedStop.stopID));

			// Set the parent circle for the shared stop, and make sure its clickable.
			sharedStop.setCircle(map, true);

			// Create a new Circles array based on the number of childRoutes.
			Circle[] circles = new Circle[sharedStop.childRoutes.length];

			// Create and add the childCircles to the map.
			for (int index = 0; index < circles.length; index++) {
				Circle circle = sharedStop.addCircle(map, sharedStop.childCircleOptions[index],
						false);

				// If this is the first circle (will have an index of 0), add a marker to the stop.
				if (index == 0) {
					Marker marker = sharedStop.addMarker(map, sharedStop.latitude, sharedStop.longitude,
							sharedStop.parentRoute.color, sharedStop.stopID);
					marker.setVisible(false);
					sharedStop.setMarker(marker);
				}

				// Apply the circle to the circle array.
				circles[index] = circle;
			}

			// Now apply the Circles to the SharedStop object
			sharedStop.setCircles(circles);
		}
	}

	/**
	 * Clears all the elements from the shared stop (as in removes the markers and circles from the map).
	 *
	 * @param sharedStops The array of shared stops to remove from the map.
	 * @return An array of shared stops with the size of 0.
	 */
	public static SharedStop[] clearSharedStops(SharedStop[] sharedStops) {
		Log.d("clearSharedStops", "Clearing all sharedStops");

		// Iterate through all the shared stops and execute the following.
		for (SharedStop sharedStop : sharedStops) {

			// Get the shared stops marker, and if it's not null remove it.
			Marker marker = sharedStop.getMarker();
			if (marker != null) {
				marker.remove();
			}

			// Get the parent circle from the shared stop, and remove it if its not null.
			Circle parentCircle = sharedStop.getCircle();
			if (parentCircle != null) {
				parentCircle.remove();
			}

			// Get the childCircles from the shared shared stop, and if its not null remove them.
			for (Circle c : sharedStop.getCircles()) {
				if (c != null) {
					c.remove();
				}
			}
		}

		// Finally clear the shared stop array.
		return new SharedStop[0];
	}

	/**
	 * Gets the circles that belong to this Shared Stop.
	 * Note: This will only return the child circles, not the parent circle.
	 *
	 * @return The array of childCircles that belong to this Stop.
	 */
	public Circle[] getCircles() {
		return this.childCircles;
	}

	/**
	 * Sets the (child) circles corresponding to the Shared Stop.
	 *
	 * @param circles The childCircles that apply to the Shared Stop.
	 */
	public void setCircles(Circle[] circles) {
		this.childCircles = circles;
	}

	/**
	 * Creates the array of circle options used by the child circles.
	 *
	 * @return The array circle opinions.
	 */
	private CircleOptions[] createCircleOptions() {

		int count = this.childRoutes.length;
		Log.d("createCircleOptions", String.format("Adding %d new circle option(s)", count));

		CircleOptions[] circleOptions = new CircleOptions[count];

		// Iterate though the childCircles and circle parentCircleOptions and initialize them.
		for (int index = 0; index < count; index++) {

			double size = Stop.PARENT_RADIUS * (1d / (index + 2));
			Log.d("createCircleOptions", "Size: " + size);

			// Make the circle parentCircleOptions for the first one the biggest,
			// and the only one that is clickable.
			CircleOptions circleOption = new CircleOptions()
					.center(new com.google.android.gms.maps.model.LatLng(this.latitude, this.longitude))
					.radius(size);

			// Set the color to the index of the parentRoute.
			int color = this.childRoutes[index].color;
			if (color != 0) {
				circleOption.strokeColor(color);
				circleOption.fillColor(color);
			}

			// Finally apply that generated CirCleOption to the CircleOptions array.
			circleOptions[index] = circleOption;
		}

		// Return the array of CircleOptions.
		return circleOptions;
	}
}
