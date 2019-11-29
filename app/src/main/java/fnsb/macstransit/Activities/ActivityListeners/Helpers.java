package fnsb.macstransit.Activities.ActivityListeners;

import android.util.Log;
import android.widget.CheckBox;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

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

	/**
	 * Helper function that formats a string containing the arrival and departure times that is to be used in the body section of a stop info window.
	 *
	 * @param stopArray               The JSONArray containing the times to be formatted.
	 * @param count                   The number of stops to parse the time for.
	 * @param expectedArrivalString   The expected arrival place holder string.
	 * @param expectedDepartureString THe expected departure placeholder string.
	 * @param is24Hour                Whether or not the user is using 24 hour time or not.
	 * @param routes                  The routes that correspond to the times that need to be parsed.
	 * @param includeRouteName        Whether or not to include the route name in the final string corresponding to the times.
	 * @return The formatted time string (to be used in the body section of an info window).
	 * @throws JSONException Thrown if there are any exceptions when parsing the JSONObjects.
	 */
	public static String generateTimeString(org.json.JSONArray stopArray, int count, String expectedArrivalString,
	                                        String expectedDepartureString, boolean is24Hour,
	                                        Route[] routes, boolean includeRouteName) throws JSONException {

		StringBuilder snippetText = new StringBuilder();

		// Iterate through the stops in the json object to get the time for.
		for (int index = 0; index < count; index++) {
			Log.d("generateTimeString", String.format("Parsing stop times for stop %d/%d", index, count));

			// Get the stop time from the current stop.
			JSONObject object = stopArray.getJSONObject(index);

			// First, check if the current time does belong to the desired route
			for (Route route : routes) {
				if (route.routeName.equals(object.getString("routeId"))) {

					// Set the arrival and departure time to the arrival and departure time in the jsonObject.
					// At this point this is stored in 24-hour time.
					String arrivalTime = Helpers.getTime(object, "predictedArrivalTime"),
							departureTime = Helpers.getTime(object, "predictedDepartureTime");

					// If the user doesn't use 24-hour time, convert to 12-hour time.
					if (!is24Hour) {
						Log.d("generateTimeString", "Converting time to 12 hour time");
						arrivalTime = Helpers.formatTime(arrivalTime);
						departureTime = Helpers.formatTime(departureTime);
					}

					// Append the route name if there is one
					if (includeRouteName) {
						Log.d("generateTimeString", "Adding route " + route.routeName);
						snippetText.append(String.format("Route: %s\n", route.routeName));
					}

					// Append the arrival and departure times to the snippet text.
					snippetText.append(String.format("%s %s\n%s %s\n\n", expectedArrivalString, arrivalTime,
							expectedDepartureString, departureTime));
				}
			}
		}

		// Get the length of the original snippet text.
		int length = snippetText.length();

		// Replace the last 2 new lines
		if (length > 2) {
			snippetText.deleteCharAt(length - 1);
			snippetText.deleteCharAt(length - 2);
		}

		return snippetText.toString();
	}

	/**
	 * Helper function that returns the time (in 24-hour form) that is found in the provided JSONObject via a regex.
	 *
	 * @param json The JSONObject to search.
	 * @param tag  The tag in the JSONObject to search.
	 * @return The time (in 24-hour form) as a String that was found in the JSONObject.
	 * This may be null if no such string was able to be found, or if there was a JSONException.
	 */
	public static String getTime(JSONObject json, String tag) {
		try {
			// Get a matcher object from the time regex, and have it match the tag.
			java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d\\d:\\d\\d").matcher(json.getString(tag));

			// If the match was found, return it, if not return null.
			return matcher.find() ? matcher.group(0) : null;

		} catch (JSONException jsonException) {
			// If there was an error, print a stack trace, and return null.
			jsonException.printStackTrace();
			return null;
		}
	}

	/**
	 * Helper function that returns the time from 24-hour time to 12-hour time (and even includes AM and PM).
	 *
	 * @param time The time to format as a string.
	 * @return The formatted 12-hour time.
	 * This may return the original 12 hour time if there was an exception parsing the time.
	 */
	public static String formatTime(String time) {
		try {
			// Try to format the time from 24 hours to 12 hours (including AM and PM).
			return new SimpleDateFormat("K:mm a", Locale.US).format(new SimpleDateFormat("H:mm", Locale.US).parse(time));
		} catch (java.text.ParseException parseException) {
			// If there was a parsing exception simply return the old time.
			parseException.printStackTrace();
			return time;
		}
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
	 * Creates a checkbox for the settings popup window.
	 * This also sets up a listener for when the checkbox is checked to update the apply button value based on the checkboxes changed value.
	 *
	 * @param view                The view to get the checkbox from.
	 * @param id                  The id of the check box.
	 * @param checked             Whether or not the checkbox should be checked by default.
	 * @param button              The apply button for the settings popup window.
	 * @param settingsPopupWindow The settings popup window class.
	 * @return The newly created checkbox.
	 */
	public static CheckBox createSettingsPopupCheckbox(android.view.View view, int id, boolean checked,
	                                                   android.widget.Button button,
	                                                   fnsb.macstransit.Activities.SettingsPopupWindow settingsPopupWindow,
	                                                   String tag) {
		// Find the checkbox within the view.
		CheckBox checkBox = view.findViewById(id);

		// Set the checkbox to be checked based on the checked value.
		checkBox.setChecked(checked);

		// Add an onCheckChanged listener to update the apply button.
		checkBox.setOnCheckedChangeListener((a, checkedValue) -> settingsPopupWindow.changeApplyButton(checkedValue, checked, button));

		// Set the tag of the checkbox.
		checkBox.setTag(tag);

		// Return the newly created checkbox.
		return checkBox;
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
