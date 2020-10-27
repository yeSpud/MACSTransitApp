package fnsb.macstransit.RouteMatch;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by Spud on 2019-10-18 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.3
 * @since Beta 6.
 */
public class Stop extends MarkedObject {

	/**
	 * The starting radius size of the circle for the stop on the map (in meters).
	 */
	private static final double STARTING_RADIUS = 50.0d;

	/**
	 * This is the route that the stop corresponds to.
	 */
	public final Route route;

	/**
	 * The name (or ID) of the stop.
	 */
	public final String stopName;

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

		// Set our stop name and route.
		this.route = route;
		this.stopName = stopName;

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
	public Stop(@NotNull org.json.JSONObject json, Route route) throws JSONException {
		this(json.getString("stopId"), json.getDouble("latitude"),
				json.getDouble("longitude"), route);
	}

	/**
	 * Shows the stops for the given route.
	 * If the stops weren't previously added to the map then this method will also see fit to add them to the map.
	 *
	 * @param map The google maps object that the stops will be drawn onto.
	 *            Be sure this object has been initialized first.
	 */
	public void showStop(GoogleMap map) {
		// Check if the circle for the stop needs to be created,
		// or just set to visible if it already exists.
		if (this.circle == null) {

			// Create a new circle object.
			Log.d("showStop", "Creating new stop for " + this.stopName);
			this.circle = Stop.createStopCircle(map, this.circleOptions, this);
		} else {

			// Since the circle already exists simply set it to visible.
			Log.d("showStop", "Showing stop " + this.stopName);
			this.circle.setClickable(true);
			this.circle.setVisible(true);
		}
	}

	/**
	 * Hides the objects on the map.
	 * This doesn't dispose of the circle object, but rather sets it to invisible
	 * (and also sets it to not be clickable in an attempt to disable its hit box from overriding other circles).
	 */
	public void hideStop() {
		// If the circle is null this will simply return.
		if (this.circle != null) {

			// Since it exists, hide the circle.
			Log.d("hideStop", "Hiding stop " + this.stopName);
			this.circle.setClickable(false);
			this.circle.setVisible(false);
		}
	}

	/**
	 * Creates a new circle object for new Stops.
	 * This method does not set the circle itself, but rather returns the newly created circle.
	 *
	 * @param map     The google maps object that this newly created circle will be added to.
	 * @param options The options to apply to the circle.
	 * @param stop
	 * @return The newly created circle.
	 */
	private static @NotNull Circle createStopCircle(@NotNull GoogleMap map, CircleOptions options, Stop stop) {
		// Add the circle to the map.
		Circle circle = map.addCircle(options);

		// Set the tag of the circle to Stop so that it can differentiate between this class and other stop-like classes (such as shared stops).
		circle.setTag(stop);

		// Set the stop to be visible and clickable.
		circle.setClickable(true);
		circle.setVisible(true);

		// Return the newly created circle.
		return circle;
	}

	/**
	 * TODO Documentation
	 * @param array
	 * @param route
	 * @return
	 */
	public static @NotNull Stop[] generateStops(@NotNull JSONArray array, Route route) {
		int count = array.length();
		Stop[] uncheckedStops = new Stop[count];

		for (int i = 0; i < count; i++) {
			Stop stop;
			try {
				stop = new Stop(array.getJSONObject(i), route);
			} catch (JSONException e) {
				Log.e("generateStops", "Exception occurred while creating stop!", e);
				continue;
			}
			uncheckedStops[i] = stop;
		}

		return uncheckedStops;
	}

	/**
	 * TODO Documentation
	 * @param potentialStops
	 * @return
	 */
	public static @NotNull Stop[] validateGeneratedStops(@NotNull Stop[] potentialStops) {
		int validatedSize = 0;
		Stop[] validatedStops = new Stop[potentialStops.length];

		for (Stop stop : potentialStops) {
			if (!isDuplicate(stop, validatedStops)) {
				validatedStops[validatedSize] = stop;
				validatedSize++;
			}
		}

		Stop[] actualStops = new Stop[validatedSize];
		System.arraycopy(validatedStops, 0, actualStops, 0, actualStops.length);
		return actualStops;
	}

	/**
	 * Checks the provided stop against an array of stops to check if its already contained in the array
	 * (and is therefor a would-be duplicate).
	 *
	 * @param stop The Stop object to check for.
	 * @param stopArray The stop array to compare the Stop object against.
	 * @return Returns true if the Stop object was found within the array - otherwise it returns false.
	 */
	public static boolean isDuplicate(Stop stop, Stop[] stopArray) {
		if (stopArray == null) {
			return false;
		}

		for (Stop stopArrayItem : stopArray) {

			// If the array item is null at this point return false since we are at the technical end
			if (stopArrayItem == null) {
				return false;
			}

			// Check if the following match.
			boolean nameMatch = stop.stopName.equals(stopArrayItem.stopName),
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
}
