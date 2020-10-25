package fnsb.macstransit.RouteMatch;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Spud on 2019-11-01 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.0
 * @since Beta 7.
 */
public class SharedStop extends MarkedObject {

	/**
	 * TODO Documentation
	 */
	private static final double STARTING_RADIUS = 60.0d;

	/**
	 * TODO Documentation
	 */
	public Route[] routes;

	/**
	 * TODO Documentation
	 */
	String stopName;

	/**
	 * TODO Documentation
	 */
	public CircleOptions[] circleOptions;

	/**
	 * TODO Documentation
	 */
	public Circle[] circles;

	/**
	 * TODO Documentation
	 */
	public LatLng location;

	public SharedStop(LatLng latLng, String stopName, Route @NotNull [] routes) {

		this.location = latLng;
		this.stopName = stopName;
		this.routes = routes;

		this.circleOptions = new CircleOptions[routes.length];
		for (int i = 0; i < routes.length; i++) {
			this.circleOptions[i] = new CircleOptions().center(this.location)
					.radius(SharedStop.STARTING_RADIUS - (10*i));

			Route route = routes[i];
			if (route.color != 0) {
				this.circleOptions[i].fillColor(route.color);
				this.circleOptions[i].strokeColor(route.color);
			}
		}
	}

	/**
	 * TODO Documentation
	 * @param map
	 */
	public void showSharedStop(GoogleMap map) {
		// TODO
	}

	/**
	 * TODO Documentation
	 */
	public void hideStop() {
		// TODO
	}
}
