package fnsb.macstransit;

import android.graphics.Color;

import com.google.android.gms.maps.model.Marker;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 */
public class Bus {

	public String busID;

	public double latitude, longitude;

	public Heading heading;

	public int color;

	public Route route;

	private Marker marker;

	public Bus(String busID, Route route) {
		this.busID = busID;
		this.route = route;
	}

	public void setMarker(Marker marker) {
		this.marker = marker;
	}

	public Marker getMarker() {
		return this.marker;
	}

}
