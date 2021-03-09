package fnsb.macstransit.Activities.ActivityListeners;

import android.content.Context;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;

import fnsb.macstransit.Threads.StopTimeCallback;
import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.MarkedObject;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.SharedStop;
import fnsb.macstransit.RouteMatch.Stop;

/**
 * Created by Spud on 2019-10-30 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.4
 * @since Beta 7.
 */
public class StopClicked implements com.google.android.gms.maps.GoogleMap.OnCircleClickListener {

	/**
	 * The maps activity that this listener corresponds to.
	 *
	 * @deprecated Because this is a potential for a memory leak try to move away from passing activities as arguments.
	 */
	@Deprecated
	private final MapsActivity activity;

	/**
	 * Constructor the the StopClicked listener.
	 *
	 * @param activity The activity that this listener corresponds to.
	 */
	public StopClicked(MapsActivity activity) {
		this.activity = activity;
	}

	/**
	 * Posts the time string for the selected stop,
	 * which contains when the (selected) buses for that stop will be arriving and departing.
	 * This method also posts this string to the body of the popup window when the info window is clicked on.
	 *
	 * @param stop    The stop (either an actual Stop object, or a SharedStop).
	 * @param json    The json object retrieved from the RouteMatch server.
	 * @param context The context from which this method is being called from (for string lookup).
	 * @return The string containing either all the arrival and departure times for the stop,
	 * or the overflowString if there is too much data.
	 */
	public static String postStopTimes(MarkedObject stop, JSONObject json, Context context) {

		// Get the stop data from the retrieved json.
		JSONArray stopData = fnsb.macstransit.RouteMatch.RouteMatch.parseData(json);

		// Get the times for the stop.
		// Since the method arguments are slightly different for a shared stop compared to a regular stop,
		// check if the marker is an instance of a Stop or SharedStop.
		String string = "";

		// Check if our marked object is a shared stop (for future formatting reasons).
		boolean isSharedStop = stop instanceof SharedStop;

		// TODO Test me
		// Try setting the routes array to either enabled routes (shared stop) or our single route (stop).
		Route[] routes;
		try {
			routes = isSharedStop ? StopClicked.getEnabledRoutesForStop(((SharedStop) stop).routes) : new Route[]{((Stop) stop).route};
		} catch (ClassCastException e) {

			// If there was an issue casting from classes log the error and return the current content of the string.
			Log.e("postStopTimes", String.format("Unaccounted object class: %s", stop.getClass()), e);
			return string;
		}

		// Try to get the formatted time string for the marked object.
		try {
			string = StopClicked.generateTimeString(stopData, context, routes, isSharedStop);
		} catch (JSONException e) {

			// If there was an exception thrown while parsing the json simply log it and return the current content of the string.
			Log.e("postStopTimes", "Could not get stop time from json", e);
			return string;
		}

		// Load the times string into a popup window for when its clicked on.
		fnsb.macstransit.Activities.PopupWindow.body = string;

		// Check to see how many new lines there are in the display.
		// If there are more than the maximum lines allowed bu the info window adapter,
		// display "Click to view all the arrival and departure times.".
		return StopClicked.getNewlineOccurrence(string) <= fnsb.macstransit.Activities.InfoWindowAdapter.MAX_LINES
				? string : context.getString(R.string.click_to_view_all_the_arrival_and_departure_times);
	}

	/**
	 * Returns an array of routes that are enabled from all the routes in the shared stop.
	 *
	 * @param allRoutesForStop The routes in the shared stop.
	 * @return The routes in the shared stop that are enabled.
	 */
	@NotNull
	public static Route[] getEnabledRoutesForStop(@NotNull Route[] allRoutesForStop) {

		// Create a new routes array to store routes that have been verified to be enabled.
		Route[] potentialRoutes = new Route[allRoutesForStop.length];
		int routeCount = 0;

		// Iterate though all the routes in our shared stop.
		for (Route route : allRoutesForStop) {

			// If the route is enabled add it to our verified routes array,
			// and increase the verified count.
			if (route.enabled) {
				potentialRoutes[routeCount] = route;
				routeCount++;
			}
		}

		// Create a new routes array of selected routes that has the size of our verified count.
		Route[] selectedRoutes = new Route[routeCount];

		// Fill the selected routes array.
		System.arraycopy(potentialRoutes, 0, selectedRoutes, 0, routeCount);

		// Return our selected routes.
		return selectedRoutes;
	}

	/**
	 * Generates the large string that is used to display the departure and arrival times of a
	 * particular stop when clicked on.
	 *
	 * @param stopArray        The JSONArray that contains all the stops for the route.
	 * @param context          The context from which this method is being called (used for string lookup).
	 * @param routes           The active (enabled) routes to get the times for.
	 * @param includeRouteName Whether or not to include the route name in the final string.
	 * @return The string containing all the departure and arrival times for the particular stop.
	 * @throws JSONException Thrown if there is a JSONException while parsing the data for the stop.
	 */
	@NotNull
	private static String generateTimeString(@NotNull JSONArray stopArray, Context context, Route[] routes,
	                                         boolean includeRouteName) throws JSONException {

		// Get the number of entries in our json array.
		int count = stopArray.length();

		// Create a new string with the size of our capacity times 5 (0:00\n).
		StringBuilder snippetText = new StringBuilder(count * 5);

		// Iterate though each entry in our json array.
		for (int index = 0; index < count; index++) {
			Log.d("generateTimeString", String.format("Parsing stop times for stop %d/%d", index,
					count));

			// Get the stop time from the current stop.
			JSONObject object = stopArray.getJSONObject(index);

			// Iterate though each of our active routes. If the route is one that is listed,
			// append the time to the string builder.
			for (Route route : routes) {
				if (route.routeName.equals(object.getString("routeId"))) {

					// Set the arrival and departure time to the arrival and departure time in the JSONObject.
					// At this point this is stored in 24-hour time.
					String arrivalTime = StopClicked.getTime(object, "predictedArrivalTime"),
							departureTime = StopClicked.getTime(object, "predictedDepartureTime");

					// If the user doesn't use 24-hour time, convert to 12-hour time.
					if (!android.text.format.DateFormat.is24HourFormat(context)) {
						Log.d("generateTimeString", "Converting time to 12 hour time");
						arrivalTime = StopClicked.formatTime(arrivalTime);
						departureTime = StopClicked.formatTime(departureTime);
					}

					// Append the route name if there is one.
					if (includeRouteName) {
						Log.d("generateTimeString", String.format("Adding route: %s", route.routeName));
						snippetText.append(String.format("Route: %s\n", route.routeName));
					}

					// Append the arrival and departure times to the snippet text.
					snippetText.append(String.format("%s %s\n%s %s\n\n", context.getString(R.string.expected_arrival),
							arrivalTime, context.getString(R.string.expected_departure), departureTime));
				}
			}
		}

		// Be sure to trim the snippet text at this point.
		snippetText.trimToSize();

		// Get the length of the original snippet text.
		int length = snippetText.length();

		// Replace the last 2 new lines (this is to mitigate a side effect of the final append).
		if (length > 2) {
			snippetText.deleteCharAt(length - 1);
			snippetText.deleteCharAt(length - 2);
		}

		// Finally, build the text and return it.
		return snippetText.toString();
	}

	/**
	 * Gets the the time (predicted arrival or predicted departure depending on the tag)
	 * for the stop via its JSONObject.
	 *
	 * @param json The JSONObject containing the time for the stop.
	 * @param tag  The specific tag to search for within the JSONObject.
	 * @return The time found within the JSONObject.
	 */
	public static String getTime(@NotNull JSONObject json, String tag) { // TODO Unit test
		String timeString;
		try {
			// Try to get the time string from the json object based on the tag.
			timeString = json.getString(tag);
		} catch (JSONException e) {
			// Log any errors and return an empty string if unsuccessful.
			Log.e("getTime", "Unable to get stop times.", e);
			return "";
		}

		// Get a matcher object from the time regex (example: 00:00), and have it match the tag.
		java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d\\d:\\d\\d").matcher(timeString);

		// If the match was found, return it, if not return null.
		return matcher.find() ? matcher.group(0) : "";
	}

	/**
	 * Formats a given 24 hour time string into a 12 hour time string complete with AM/PM characters.
	 *
	 * @param time The time string (ie 13:15 for what should be 1:15 PM).
	 * @return The formatted time string with the AM/PM characters included.
	 */
	public static String formatTime(String time) {

		// Create a date format for parsing 24 hour time.
		SimpleDateFormat fullTime = new SimpleDateFormat("H:mm", Locale.US);

		// Create another date format for formatting 12 hour time.
		SimpleDateFormat halfTime = new SimpleDateFormat("h:mm a", Locale.US);

		java.util.Date fullTimeDate;
		try {

			// Try to get the 24 hour time as a date.
			fullTimeDate = fullTime.parse(time);
		} catch (java.text.ParseException e) {

			// If there was a parsing exception simply return the old time.
			Log.e("formatTime", "Could not parse full 24 hour time", e);
			return time;
		}

		// Check if the 24 hour time date is null.
		if (fullTimeDate == null) {

			// Since the 24 hour time date is null return the old time.
			Log.e("formatTime", "24 hour time date is null");
			return time;
		}

		// Format the 24 hour time date into 12 hour time and return it.
		String formattedTime = halfTime.format(fullTimeDate);
		Log.d("formatTime", "Formatted time: " + formattedTime);
		return formattedTime;
	}

	/**
	 * Function that finds the number of times a character occurs within a given string.
	 *
	 * @param string The string to search.
	 * @return The number of times the character occurs within the string.
	 */
	public static int getNewlineOccurrence(CharSequence string) {

		// Check to make sure the string is not null. If it is return 0.
		if (string == null) {
			return 0;
		}

		// Create a variable to store the occurrence.
		int count = 0;

		// Iterate through the string.
		for (int i = 0; i < string.length(); i++) {

			// If the character at the current index matches our character, increase the count.
			if (string.charAt(i) == '\n') {
				count++;
			}
		}

		// Finally, return the count.
		return count;
	}

	/**
	 * Called when a circle is clicked.
	 * <p>
	 * This is called on the Android UI thread.
	 *
	 * @param circle The circle that is clicked.
	 */
	@Override
	public void onCircleClick(@NotNull com.google.android.gms.maps.model.Circle circle) {

		// Get the marked object from our circle.
		MarkedObject markedObject = (MarkedObject) circle.getTag();

		// Make sure our marked object is not null.
		if (markedObject == null) {
			Log.w("onCircleClick", "Circle tag is null!");
			return;
		}

		// If the marker for our marked object is null, create a new marker.
		if (markedObject.marker == null) {

			// Get the location and color of the object
			// (this is different depending on whether or not its a shared stop or a regular stop).
			com.google.android.gms.maps.model.LatLng location;
			int color;

			if (markedObject instanceof SharedStop) {

				// Get the location and color of the largest circle of our shared stop.
				SharedStop sharedStop = (SharedStop) markedObject;
				location = sharedStop.location;
				color = sharedStop.routes[0].color;
			} else if (markedObject instanceof Stop) {

				// Get the location and color of our stop.
				Stop stop = (Stop) markedObject;
				location = stop.circleOptions.getCenter();
				color = stop.route.color;
			} else {

				// Since our marked object was neither a shared stop nor a regular stop log it as a warning,
				// and return early.
				Log.w("onCircleClick", String.format("Object unaccounted for: %s", markedObject.getClass()));
				return;
			}

			// Create a new marker for our marked object using the newly determined location and color.
			markedObject.marker = markedObject.addMarker(MapsActivity.map, location, color, markedObject.name);
		}

		// Show our marker.
		this.showMarker(markedObject.marker);
	}

	/**
	 * Shows the given marker on the map by setting it to visible.
	 * The title of the marker is set to the name of the stop.
	 * The marker snippet is set to a pending message as a callback method retrieves the stop times for the stop.
	 *
	 * @param marker The marker to be shown. This cannot be null.
	 */
	private void showMarker(@NotNull com.google.android.gms.maps.model.Marker marker) {

		// Since the marker is not null, show it the marker by setting it to visible.
		marker.setVisible(true);

		// Get the name of the stop.
		String name = marker.getTitle();

		// For now just set the snippet text to "retrieving stop times" as a callback method gets the times.
		marker.setSnippet(this.activity.getString(fnsb.macstransit.R.string.retrieving_stop_times));

		// Create a method on how to handle the stop time once its retrieved.
		StopTimeCallback.AsyncCallback callbackMethod = new GetStopTimeHandler(marker, this.activity);

		// Setup a new stop callback to reset the info window.
		StopTimeCallback callback = new StopTimeCallback(callbackMethod);

		// Retrieve the stop times.
		callback.retrieveStopTime(name);

		// For now though just show the info window.
		marker.showInfoWindow();
	}
}
