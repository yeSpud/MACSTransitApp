package fnsb.macstransit.Activities.ActivityListeners;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

import fnsb.macstransit.RouteMatch.BasicStop;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.Stop;

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
	 * Gets the color of the marker icon based off of the color value given.
	 * The reason why there needs to be a function for this is because there are only 10 colors that a marker icon can be.
	 *
	 * @param color The desired color value as an int.
	 * @return The BitmapDescriptor used for defining the color of a markers's icon.
	 */
	public static com.google.android.gms.maps.model.BitmapDescriptor getMarkerIcon(int color) {
		float[] hsv = new float[3];
		android.graphics.Color.colorToHSV(color, hsv);
		return com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(hsv[0]);
	}

	/**
	 * Loads all the stops from the provided routes.
	 *
	 * @param routes The routes to get the stops from.
	 * @return The full array of Stops in all the routes.
	 * This is a basic stop for now as there are no markers or circles associated with the stop yet.
	 */
	public static BasicStop[] loadAllStops(Route[] routes) {
		// Create an array-list to store all the stops.
		ArrayList<BasicStop> stops = new ArrayList<>();

		// Iterate through all the routes provided in the argument.
		for (Route r : routes) {

			// Iterate through all the stops in the route.
			for (fnsb.macstransit.RouteMatch.Stop s : r.stops) {

				// Add the stop to the array of stops.
				stops.add(new BasicStop(s.stopID, s.latitude, s.longitude, s.route));
			}
		}
		Log.i("loadAllStops", String.format("Successfully loaded %d stops", stops.size()));

		// Convert the stop array list to an array of stops, and return it.
		return stops.toArray(new BasicStop[0]);
	}

	/**
	 * Helper function that adds a circle to the map with the provided options,
	 * and even sets the object this circle belongs to via its tag.
	 *
	 * @param map       The map to add the circle to.
	 * @param options   The options to apply to the circle (position, color, size, etc...)
	 * @param tag       The class this circle will belong to.
	 * @param clickable Whether or not this circle should be clickable.
	 * @return The generated circle.
	 */
	public static Circle addCircle(GoogleMap map, com.google.android.gms.maps.model.CircleOptions options, Object tag, boolean clickable) {
		// Add the circle to the map.
		Circle circle = map.addCircle(options);

		// Set the tag of the circle.
		circle.setTag(tag);

		// Set it to be clickable or not (depending on the boolean value of clickable).
		circle.setClickable(clickable);

		// Return the circle.
		return circle;
	}

	/**
	 * Helper function that finds the stop within a shared stop based on the route in common.
	 *
	 * @param sharedStop The shared stop to look through.
	 * @return The stop that is contained within the shared stop, or null if none exists.
	 */
	public static Stop findStopInSharedStop(fnsb.macstransit.RouteMatch.SharedStop sharedStop) {
		// Iterate through the routes within the shared stop
		for (Route route : sharedStop.routes) {
			// Iterate through the stops withing that route.
			for (Stop stop : route.stops) {
				// If the stop equals the route ID, return that stop.
				if (stop.stopID.equals(sharedStop.stopID)) {
					return stop;
				}
			}
		}
		// If no stop was ever found, return null.
		return null;
	}

	/**
	 * Helper function that determines the number of times a given character occurs within a given string.
	 *
	 * @param character The character to get the number of occurrences of.
	 * @param string    The string to check.
	 * @return The number of times that character occurs within the given within the given string.
	 */
	public static int getCharacterOccurrence(char character, String string) {
		int count = 0;
		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) == character) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Helper function that creates the polyline for the specified route.
	 *
	 * @param route The route that the polyline corresponds to.
	 * @param map   The map that will have the polyline added to.
	 * @return The polyline (already added to the map, and set to be visible).
	 */
	public static com.google.android.gms.maps.model.Polyline createPolyLine(Route route, GoogleMap map) {
		// Add the polyline based off the polyline coordinates within the route.
		PolylineOptions options = new PolylineOptions().add(route.polyLineCoordinates);

		// Make sure its not clickable.
		options.clickable(false);

		// Set the color of the polylines based on the route color.
		options.color(route.color);

		// Make sure the polyline is visible.
		options.visible(true);

		// Add the polyline to the map, and return it.
		return map.addPolyline(options);
	}

}
