package fnsb.macstransit.RouteMatch;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;

/**
 * Created by Spud on 2019-10-18 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Beta 6.
 */
public class Stop extends MarkedObject {

	/**
	 * The starting radius size of the circle for the stop on the map (in meters).
	 */
	private static final double STARTING_RADIUS = 50.0d;

	/**
	 * Fallback array used for returning an array of stops that is zero in length.
	 * This is commonly used for an early exit scenario when bad arguments are provided,
	 * and the method cannot continue otherwise.
	 */
	public static final Stop[] EMPTY_STOPS_ARRAY = new Stop[0];

	/**
	 * This is the route that the stop corresponds to.
	 */
	public final Route route;

	/**
	 * The circle marking the bus stop on the map
	 * (be sure to check if this exists first as it may be null).
	 */
	public Circle circle;

	/**
	 * The options that apply to the circle representing the stop.
	 * These options include:
	 * <ul>
	 * <li>The coordinates of the stop
	 * <li>The current size of the circle
	 * <li>The color of the circle
	 * <li>Whether the circle is clickable or not
	 * </ul><p>
	 * ... and more!
	 */
	public final CircleOptions circleOptions;

	/**
	 * Creates a new Stop object using the name, coordinates on the map, and the route.
	 * <p>
	 * Important note: While this sets up the circle options and the name and route,
	 * it does not create the actual circle object as that requires the map object to be loaded.
	 * <p>
	 * The circle for the stop is created when its called to be shown on the map.
	 *
	 * @param stopName  The name (or ID) of the stop.
	 * @param latitude  The latitude position of the stop on the map.
	 * @param longitude The longitude position of the stop on the map.
	 * @param route     The route that the stop corresponds to.
	 */
	public Stop(String stopName, double latitude, double longitude, Route route) {
		super(stopName);

		// Set our stop name and route.
		this.route = route;

		// Setup the circle options for our stop.
		// This will later be applied to the actual circle object.
		this.circleOptions = new CircleOptions()
				.center(new com.google.android.gms.maps.model.LatLng(latitude, longitude))
				.radius(Stop.STARTING_RADIUS);

		// Add the route color if it has one.
		if (this.route != null) {
			if (this.route.color != 0) {
				this.circleOptions.fillColor(this.route.color);
				this.circleOptions.strokeColor(this.route.color);
			}
		}
	}

	/**
	 * Lazy creation of a new Stop object using the provided JSON and the route.
	 *
	 * @param json  The JSONObject containing the bus stop data.
	 * @param route The route this newly created Stop object will apply to.
	 * @throws JSONException Thrown if there is any issue in parsing the data from the provided JSONObject.
	 */
	public Stop(@NonNull org.json.JSONObject json, Route route) throws JSONException {
		this(json.getString("stopId"), json.getDouble("latitude"),
				json.getDouble("longitude"), route);
	}

	/**
	 * Shows the stops for the given route.
	 * If the stops weren't previously added to the map then this method will also see fit to add them to the map.
	 * <p>
	 * This should be run on the UI thread.
	 *
	 * @param map The google maps object that the stops will be drawn onto.
	 *            Be sure this object has been initialized first.
	 */
	@UiThread
	public void showStop(GoogleMap map) {

		// Check if the circle for the stop needs to be created,
		// or just set to visible if it already exists.
		if (this.circle == null) {

			// Create a new circle object.
			Log.d("showStop", "Creating new stop for " + this.name);
			this.circle = Stop.createStopCircle(map, this.circleOptions, this);
		} else {

			// Since the circle already exists simply set it to visible.
			Log.d("showStop", "Showing stop " + this.name);
			this.circle.setClickable(true);
			this.circle.setVisible(true);
		}
	}

	/**
	 * Hides the objects on the map.
	 * This doesn't dispose of the circle object, but rather sets it to invisible
	 * (and also sets it to not be clickable in an attempt to disable its hit box from overriding other circles).
	 * <p>
	 * This should be run on the UI thread.
	 */
	@UiThread
	public void hideStop() {

		// If the circle is null this will simply return.
		if (this.circle != null) {

			// Since it exists, hide the circle.
			Log.d("hideStop", "Hiding stop " + this.name);
			this.circle.setClickable(false);
			this.circle.setVisible(false);
		}
	}

	/**
	 * Creates a new circle object for new Stops.
	 * This method does not set the circle itself, but rather returns the newly created circle.
	 *
	 * @param map     The google maps object that this newly created circle will be added to.
	 *                This cannot be null.
	 * @param options The options to apply to the circle.
	 * @param stop    The stop that this circle belongs to (this will be set as the circle's tag)
	 * @return The newly created circle.
	 */
	@NonNull
	@UiThread
	private static Circle createStopCircle(@NonNull GoogleMap map, CircleOptions options, Stop stop) {

		// Add the circle to the map.
		Circle circle = map.addCircle(options);

		// Set the tag of the circle to Stop so that it can differentiate between this class
		// and other stop-like classes (such as shared stops).
		circle.setTag(stop);

		// Set the stop to be visible and clickable.
		circle.setClickable(true);
		circle.setVisible(true);

		// Return the newly created circle.
		return circle;
	}

	/**
	 * Creates an array of stops from the provided json array.
	 * If the json array is null then the stop array will be 0 in length.
	 *
	 * @param array The json array containing the stop information.
	 * @param route The route these stops belongs to.
	 * @return The stop array created from the json array.
	 */
	@NonNull
	public static Stop[] generateStops(org.json.JSONArray array, Route route) {

		// Check if the json array is null. If it is then simply return a zero length stop array.
		if (array == null) {
			return Stop.EMPTY_STOPS_ARRAY;
		}

		// Create an array of stops that will be filled using the information from the json array.
		int count = array.length();
		Stop[] uncheckedStops = new Stop[count];

		// Iterate though the json array.
		for (int i = 0; i < count; i++) {
			Stop stop;

			// Try to create a new stop object using the information in the json array.
			try {
				stop = new Stop(array.getJSONObject(i), route);
			} catch (JSONException e) {

				// If unsuccessful simply log the exception and continue iterating.
				Log.e("generateStops", "Exception occurred while creating stop!", e);
				continue;
			}
			uncheckedStops[i] = stop;
		}

		// Return the stop array.
		return uncheckedStops;
	}

	/**
	 * Validates the provided array of potential stops and returned the actual stops in the route,
	 * removing any duplicate or invalid stops.
	 *
	 * @param potentialStops The potential stops that may contain duplicate or invalid stops.
	 * @return The validated stops array (or an empty stop array if the provided potential stops is null).
	 */
	@NonNull
	public static Stop[] validateGeneratedStops(Stop[] potentialStops) {

		// If the supplied potential stops is null simply return a stop array of size 0.
		if (potentialStops == null) {
			return Stop.EMPTY_STOPS_ARRAY;
		}

		// Create a variable to store the true size of the stops that have been validated.
		int validatedSize = 0;

		// Create an array to store the validated stops.
		// While we don't know the specific size of this array until done, we do know the maximum size,
		// so use that for setting the array size.
		Stop[] validatedStops = new Stop[potentialStops.length];

		// Iterate through each stop in our array of potential stops.
		for (Stop stop : potentialStops) {

			// Check to see if the stop is in our array of validated stops. If its not,
			// add it to the array and add 1 to the true index size of stops that have been validated.
			if (!isDuplicate(stop, validatedStops)) {
				validatedStops[validatedSize] = stop;
				validatedSize++;
			}
		}

		// Create an array for our actual stops.
		// Since we now know the number of validated stops we can use that as its size.
		Stop[] actualStops = new Stop[validatedSize];

		// Copy our validated stops into our smaller actual stops array, and return it.
		System.arraycopy(validatedStops, 0, actualStops, 0, actualStops.length);
		return actualStops;
	}

	/**
	 * Checks the provided stop against an array of stops to check if its already contained in the array
	 * (and is therefor a would-be duplicate).
	 *
	 * @param stop      The Stop object to check for.
	 * @param stopArray The stop array to compare the Stop object against.
	 * @return Returns true if the Stop object was found within the array - otherwise it returns false.
	 */
	public static boolean isDuplicate(Stop stop, Stop[] stopArray) {

		// If the provided stop array is null just return false.
		if (stopArray == null) {
			return false;
		}

		for (Stop stopArrayItem : stopArray) {

			// If the array item is null at this point return false since we are at the technical end.
			if (stopArrayItem == null) {
				return false;
			}

			// Check if the following match.
			boolean nameMatch = stop.name.equals(stopArrayItem.name),
					routeMatch = stop.route.routeName.equals(stopArrayItem.route.routeName),
					latitudeMatch = stop.circleOptions.getCenter().latitude == stopArrayItem.circleOptions.getCenter().latitude,
					longitudeMatch = stop.circleOptions.getCenter().longitude == stopArrayItem.circleOptions.getCenter().longitude;

			// If all of the following match, return true.
			if (nameMatch && routeMatch && latitudeMatch && longitudeMatch) {
				return true;
			}
		}

		// Since nothing matched, return false.
		return false;
	}

	/**
	 * Checks if the two provided stops match (have the same name and location).
	 *
	 * @param stop1 The first stop to compare.
	 * @param stop2 The second stop to compare.
	 * @return Whether the first and second stops are the same.
	 * @throws NullPointerException Thrown if stop locations are null.
	 */
	public static boolean doStopsMatch(Stop stop1, Stop stop2) throws NullPointerException {

		// If either stops are null return false.
		if (stop1 == null || stop2 == null) {
			return false;
		}

		// Get the two stop locations.
		LatLng loc1 = stop1.circleOptions.getCenter(), loc2 = stop2.circleOptions.getCenter();

		// Latitude comparison.
		boolean latMatch = loc1.latitude == loc2.latitude,

				// Longitude comparison.
				longMatch = loc1.longitude == loc2.longitude,

				// Name comparison.
				nameMatch = stop1.name.equals(stop2.name);

		// Return whether name and locations are the same.
		return latMatch && longMatch && nameMatch;
	}

	/**
	 * Removes the stop's circle from the map.
	 * This also sets the circle to null so it can be recreated later.
	 * This must be run on the UI Thread.
	 */
	@UiThread
	public void removeStopCircle() {

		// Remove stop circles (if it has them).
		if (this.circle != null) {
			this.circle.remove();
			this.circle = null;
		}
	}
}
