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
	 * TODO Documentation
	 */
	public double latitude, longitude;

	/**
	 * TODO Documentation
	 */
	public Route[] routes;

	/**
	 * TODO Documentation
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
}
