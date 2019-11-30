package fnsb.macstransit.Activities.ActivityListeners;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.PolylineOptions;

import fnsb.macstransit.RouteMatch.Route;

/**
 * Created by Spud on 2019-11-01 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 * <p>
 * <p>
 * This class is full of static methods that are simply used as helper methods,
 * and thus can be used anywhere.
 *
 * @version 1.1
 * @since Beta 7
 */
@Deprecated
public class Helpers { // TODO Deprecate all of these!

	/**
	 * Helper function that creates the polyline for the specified parentRoute.
	 *
	 * @param route The parentRoute that the polyline corresponds to.
	 * @param map   The map that will have the polyline added to.
	 * @return The polyline (already added to the map, and set to be visible).
	 */
	public static com.google.android.gms.maps.model.Polyline createPolyLine(Route route, GoogleMap map) {
		// Add the polyline based off the polyline coordinates within the parentRoute.
		PolylineOptions options = new PolylineOptions().add(route.polyLineCoordinates);

		// Make sure its not clickable.
		options.clickable(false);

		// Set the color of the polylines based on the parentRoute color.
		options.color(route.color);

		// Make sure the polyline is visible.
		options.visible(true);

		// Add the polyline to the map, and return it.
		return map.addPolyline(options);
	}
}
