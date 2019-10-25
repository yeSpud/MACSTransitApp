package fnsb.macstransit.RouteMatch;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import fnsb.macstransit.MapsActivity;

/**
 * Created by Spud on 2019-10-18 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since beta 6.
 */
public class Stop {

	/**
	 * TODO Documentation
	 */
	public static final double RADIUS = 50.0d;
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
	private Circle icon;
	/**
	 * TODO Documentation
	 */
	private CircleOptions iconOptions;

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
		CircleOptions options = new CircleOptions().center(new LatLng(this.latitude, this.longitude))
				.radius(Stop.RADIUS);
		if (this.route.color != 0) {
			this.color = this.route.color;
			options.fillColor(this.color);
			options.strokeColor(this.color);
		}
		this.iconOptions = options;
	}

	/**
	 * TODO Documentation
	 *
	 * @return
	 */
	public Circle getIcon() {
		return this.icon;
	}

	/**
	 * TODO Document
	 */
	public void setIcon(Circle icon) {
		this.icon = icon;
		this.icon.setTag(this);
		this.icon.setClickable(true);
	}

	public Marker getMarker() {
		return this.marker;
	}

	/**
	 * TODO Documentation
	 * @param marker
	 */
	public void setMarker(Marker marker) {
		this.marker = marker;
		this.marker.setTag(this);
		this.marker.setTitle(this.stopID);
		this.marker.setIcon(MapsActivity.getMarkerIcon(this.color));

	}

	/**
	 * TODO Documentation
	 *
	 * @return
	 */
	public CircleOptions getIconOptions() {
		return this.iconOptions;
	}

}
