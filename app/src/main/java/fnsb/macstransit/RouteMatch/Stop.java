package fnsb.macstransit.RouteMatch;

import com.google.android.gms.maps.model.Marker;

/**
 * Created by Spud on 2019-10-18 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 */
public class Stop {

	/**
	 * TODO Documentation
	 */
	public String stopID;

	/**
	 * TODO Documentation
	 */
	public double latitude, longitude;

	/**
	 * TODO Documentation
	 */
	public int color;

	/**
	 * TODO Documentation
	 */
	public Route route;
	/**
	 * TODO Documentation
	 */
	private Marker marker;

	/**
	 * TODO Documentation
	 */
	public Stop(String stopID, double latitude, double longitude, Route route) {
		this.stopID = stopID;
		this.latitude = latitude;
		this.longitude = longitude;
		this.route = route;
	}

	/**
	 * TODO Documentation
	 *
	 * @return
	 */
	public Marker getMarker() {
		// TODO
		return null;
	}

	/**
	 * TODO Document
	 */
	public void setMarker(Marker marker) {
		// TODO
	}

}
