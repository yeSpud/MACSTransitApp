package fnsb.macstransit.RouteMatch;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Spud on 2019-11-01 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Beta 7
 */
public class SharedStop {

	/**
	 * The latitude and longitude of the SharedStops.
	 */
	public double latitude, longitude;

	/**
	 * The array of routes that this stop is shared with.
	 */
	public Route[] routes;

	/**
	 * The stop ID (usually the stop name).
	 */
	public String stopID;

	/**
	 * The array of options that will be applied to the circles that correspond to this Stop.
	 */
	public CircleOptions[] circleOptions;

	/**
	 * The circles that correspond to this stop.
	 */
	private Circle[] circles;

	/**
	 * The marker that corresponds to this stop.
	 */
	private Marker marker;

	/**
	 * Constructor for the Shared Stop.
	 *
	 * @param stopID    The Stop ID. This is typically the name of the stop.
	 * @param latitude  The latitude of the Stop.
	 * @param longitude The longitude of the Stop.
	 * @param routes    The routes that this Stop corresponds to.
	 */
	@SuppressWarnings("WeakerAccess")
	public SharedStop(String stopID, double latitude, double longitude, Route[] routes) {
		this.stopID = stopID;
		this.latitude = latitude;
		this.longitude = longitude;
		this.routes = routes;

		// Be sure to create the circle options for this object at this point.
		this.circleOptions = this.createCircleOptions();
	}

	/**
	 * Simpler constructor the the Shared Stop.
	 *
	 * @param basicStop The basic stop that will be changed into a Shared Stop.
	 * @param routes    The route that this Stop corresponds to.
	 */
	public SharedStop(BasicStop basicStop, Route[] routes) {
		this(basicStop.stopID, basicStop.latitude, basicStop.longitude, routes);
	}

	/**
	 * Gets the circles that belong to this Stop.
	 *
	 * @return The array of circles that belong to this Stop.
	 */
	public Circle[] getCircles() {
		return this.circles;
	}

	/**
	 * Sets the circles corresponding to the Shared Stop.
	 *
	 * @param circles The circles that apply to the Shared Stop.
	 */
	public void setCircles(Circle[] circles) {
		this.circles = circles;
	}

	/**
	 * Gets the marker that belongs to this Stop.
	 *
	 * @return The marker that belongs to this Stop.
	 */
	public Marker getMarker() {
		return this.marker;
	}

	/**
	 * Sets the marker that belongs to this Stop.
	 *
	 * @param marker The marker that belongs to this Stop.
	 */
	public void setMarker(Marker marker) {
		this.marker = marker;
	}

	/**
	 * Creates the circle options that will correspond to this class.
	 *
	 * @return The array circle opinions.
	 */
	private CircleOptions[] createCircleOptions() {
		CircleOptions[] circleOptions = new CircleOptions[this.routes.length];

		// Iterate though the circles and circle options and initialize them.
		for (int index = 0; index < circleOptions.length; index++) {

			// Make the circle options for the first one the biggest, and the only one that is clickable
			CircleOptions circleOption = new CircleOptions()
					.center(new com.google.android.gms.maps.model.LatLng(this.latitude, this.longitude))
					.radius(Stop.RADIUS * (1d / (index + 1)));


			// Set the color to the index of the route
			int color = this.routes[index].color;
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
