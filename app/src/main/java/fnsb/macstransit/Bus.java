package fnsb.macstransit;

import com.google.android.gms.maps.model.Marker;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 */
public class Bus {

	/**
	 * TODO Documentation
	 */
	public String busID;

	/**
	 * TODO Documentation
	 */
	public double latitude, longitude;

	/**
	 * TODO Documentation
	 */
	public Heading heading;

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
	 *
	 * @param busID
	 * @param route
	 */
	public Bus(String busID, Route route) {
		this.busID = busID;
		this.route = route;
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
