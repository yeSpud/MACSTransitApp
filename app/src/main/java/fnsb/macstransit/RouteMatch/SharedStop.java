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
	 * TODO Documentation
	 */
	public CircleOptions[] circleOptions;

	/**
	 * TODO Documentation
	 */
	private Circle[] circles;

	/**
	 * TODO Documentation
	 */
	private Marker marker;

	/**
	 * TODO Documentation
	 *
	 * @param stopID
	 * @param latitude
	 * @param longitude
	 * @param routes
	 */
	public SharedStop(String stopID, double latitude, double longitude, Route[] routes) {
		this.stopID = stopID;
		this.latitude = latitude;
		this.longitude = longitude;
		this.routes = routes;

		this.circleOptions = this.createCircleOptions();
	}

	/**
	 * TODO Documentation
	 *
	 * @param basicStop
	 * @param routes
	 */
	public SharedStop(BasicStop basicStop, Route[] routes) {
		this(basicStop.stopID, basicStop.latitude, basicStop.longitude, routes);
	}

	/**
	 * TODO Documentation
	 *
	 * @return
	 */
	public Circle[] getCircles() {
		return this.circles;
	}

	/**
	 * TODO Documentation
	 *
	 * @param circles
	 */
	public void setCircles(Circle[] circles) {
		this.circles = circles;
	}

	/**
	 * TODO Documentation
	 *
	 * @return
	 */
	public Marker getMarker() {
		return this.marker;
	}

	/**
	 * TODO Documentation
	 *
	 * @param marker
	 */
	public void setMarker(Marker marker) {
		this.marker = marker;
	}

	/**
	 * TODO Documentation
	 *
	 * @return
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

			circleOptions[index] = circleOption;
		}

		return circleOptions;
	}
}
