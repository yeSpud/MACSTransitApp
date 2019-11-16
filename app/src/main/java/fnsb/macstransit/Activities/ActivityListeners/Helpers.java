package fnsb.macstransit.Activities.ActivityListeners;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import fnsb.macstransit.RouteMatch.BasicStop;
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
 * @version 1.0
 * @since Beta 7
 */
public class Helpers {

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

		// Convert the stop arraylist to an array of stops, and return it.
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
	public static Circle addCircle(GoogleMap map, CircleOptions options, Object tag, boolean clickable) {
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
	 * Helper function that adds a marker to the map at the provided location,
	 * with the provided title, and sets the color to the provided color.
	 * <p>
	 * The marker can also be assigned a class that it belongs to via the tag argument.
	 *
	 * @param map       The map to add the marker to.
	 * @param latitude  The latitude of the marker.
	 * @param longitude The longitude of the marker.
	 * @param color     The color of the marker.
	 * @param title     The title of the marker.
	 * @param tag       The object that this maker belongs to.
	 * @return The generated marker.
	 */
	public static Marker addMarker(GoogleMap map, double latitude, double longitude, int color, String title, Object tag) {
		// Create a new maker options object
		MarkerOptions options = new MarkerOptions();

		// Set the position of the marker via the latitude and longitude.
		options.position(new com.google.android.gms.maps.model.LatLng(latitude, longitude));

		// Set the color of the marker.
		options.icon(Helpers.getMarkerIcon(color));

		// Add the marker to the map.
		Marker marker = map.addMarker(options);

		// Set the marker title.
		marker.setTitle(title);

		// Set the marker's tag.
		marker.setTag(tag);

		// Return the generated marker.
		return marker;
	}


}
