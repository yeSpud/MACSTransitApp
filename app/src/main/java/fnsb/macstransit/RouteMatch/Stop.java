package fnsb.macstransit.RouteMatch;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
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
	private Circle icon;

	/**
	 * TODO Documentation
	 */
	private CircleOptions iconOptions;

	/**
	 * TODO Documentation
	 */
	public Stop(String stopID, double latitude, double longitude, Route route) {
		this.stopID = stopID;
		this.latitude = latitude;
		this.longitude = longitude;
		this.route = route;
		CircleOptions options = new CircleOptions().center(new LatLng(this.latitude, this.longitude)).radius(25);
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
