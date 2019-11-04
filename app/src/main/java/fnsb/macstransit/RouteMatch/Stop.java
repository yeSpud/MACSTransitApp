package fnsb.macstransit.RouteMatch;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Marker;

import fnsb.macstransit.ActivityListeners.Helpers;


/**
 * Created by Spud on 2019-10-18 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since beta 6.
 */
public class Stop extends BasicStop {

	/**
	 * The initial radius size (in meters) for the circle that represents a stop on the map.
	 */
	public static final double RADIUS = 50.0d;

	/**
	 * The color of the stop. This is typically derived from the route that this stop corresponds to.
	 */
	@Deprecated
	public int color;

	/**
	 * The options that correspond to the stop icon (which is a circle).
	 * This is primarily used for the position, as well as determining and setting the current radius.
	 */
	public CircleOptions iconOptions;

	/**
	 * The stop icon that is shown on the map.
	 * <p>
	 * Its really just a circle :P
	 * <p>
	 * The reason why this is kept private is because it cannot be created outside a maps activity class
	 * (otherwise an error is invoked).
	 */
	private Circle icon;

	/**
	 * The marker object for the stop. This is shown to user when the click on a stop icon (circle).
	 * This is then made visible, and the info window is also shown.
	 * <p>
	 * The reason why this is kept private is because it cannot be created outside a maps activity class
	 * (otherwise an error is invoked).
	 */
	private Marker marker;

	/**
	 * Creates a stop object.
	 *
	 * @param stopID    The ID of the stop. This is usually the stop name.
	 * @param latitude  The latitude of the stop.
	 * @param longitude The longitude of the stop.
	 * @param route     The route this stop corresponds to.
	 */
	public Stop(String stopID, double latitude, double longitude, Route route) {
		super(stopID, latitude, longitude, route);

		// Setup the options that will be used on the stop icon.
		// This is really just setting up the coordinates, and the initial radius of the stop.
		CircleOptions options = new CircleOptions().center(new com.google.android.gms.maps.model.LatLng(this.latitude, this.longitude))
				.radius(Stop.RADIUS);

		// If the route color isn't null, set the stop color to the same color as the route color.
		if (this.route.color != 0) {
			options.fillColor(this.route.color);
			options.strokeColor(this.route.color);
		}

		// Set the icon options to the newly created options, though don't apply the options to the icon just yet.
		this.iconOptions = options;
	}

	/**
	 * Gets the icon corresponding to the stop. This is really just a circle that may or may not be colored as well.
	 * This may be null if the icon has never been set via {@code setIcon()}.
	 *
	 * @return The stop icon (a Circle object).
	 */
	public Circle getIcon() {
		return this.icon;
	}

	/**
	 * Sets the icon, as well as providing the tag, and makes sure that the icon is clickable.
	 *
	 * @param icon The icon created in the map activity (usually done by {@code map.addCircle()}).
	 */
	public void setIcon(Circle icon) {
		this.icon = icon;
		this.icon.setTag(this);
		this.icon.setClickable(true);
	}

	/**
	 * Gets the marker object for the stop. This may be null if it has never been set by {@code setMarker()}.
	 *
	 * @return The marker object corresponding to the stop.
	 */
	public Marker getMarker() {
		return this.marker;
	}

	/**
	 * Sets the marker for the stop for when its clicked on, as well as adding the tag, the title,
	 * and applying the icon color if there is one.
	 *
	 * @param marker The marker created in the map activity (usually done by {@code map.addMarker()}).
	 */
	public void setMarker(Marker marker) {
		this.marker = marker;
		this.marker.setTag(this);
		this.marker.setTitle(this.stopID);
		if (this.route.color != 0) {
			this.marker.setIcon(Helpers.getMarkerIcon(this.route.color));
		}

	}

	/**
	 * Gets the icon options as a CircleOptions object.
	 *
	 * @return the icon options as a CircleOptions object.
	 */
	@Deprecated
	public CircleOptions getIconOptions() {
		return this.iconOptions;
	}

}
