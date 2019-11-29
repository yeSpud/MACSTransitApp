package fnsb.macstransit.RouteMatch;

import com.google.android.gms.maps.model.Marker;

/**
 * Created by Spud on 2019-11-20 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Beta 8.
 */
public class MarkedObject {

	/**
	 * The marker of the marker of the marked object.
	 */
	private Marker marker;

	/**
	 * Retrieves the marker of the object.
	 *
	 * @return The marker.
	 */
	public Marker getMarker() {
		return this.marker;
	}

	/**
	 * Sets the marker of the object.
	 *
	 * @param marker The marker to be set.
	 */
	public void setMarker(Marker marker) {
		this.marker = marker;
	}

}
