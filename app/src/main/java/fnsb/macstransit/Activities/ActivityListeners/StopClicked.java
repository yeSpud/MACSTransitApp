package fnsb.macstransit.Activities.ActivityListeners;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.Marker;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;

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
	public static String postStopTimes(Object stop, org.json.JSONObject json, Context context) {
		// Get the stop data from the retrieved json.
		org.json.JSONArray stopData = fnsb.macstransit.RouteMatch.RouteMatch.parseData(json);
		int count = stopData.length();

		try {
			// Get the times for the stop.
			// Since the method arguments are slightly different for a shared stop compared to a regular stop,
			// check if the marker is an instance of a Stop or SharedStop.
			String string=  "";

			// If the stop is a shared stop, execute the following:
			if (stop instanceof SharedStop) {
				// FIXME
			} else if (stop instanceof Stop) {
				// Since the stop is just a stop, just go straight into generating the time string,
				// without the route name.
				string = StopClicked.generateTimeString(stopData, count, context,
						new Route[]{((Stop) stop).route}, false);
			} else {
				// If the instance of the stop was undetermined, warn the developer.
				Log.w("postStopTimes", "Object unaccounted for!");
				return "";
			}

			// Load the times string into a popup window for when its clicked on.
			fnsb.macstransit.Activities.PopupWindow.body = string;

			// Check to see how many new lines there are in the display.
			// If there are more than the maximum lines allowed bu the info window adapter,
			// display "Click to view all the arrival and departure times.".
			return StopClicked.getNewlineOccurrence(string) <= fnsb.macstransit.Activities.InfoWindowAdapter.MAX_LINES
					? string : context.getString(R.string.click_to_view_all_the_arrival_and_departure_times);

		} catch (JSONException e) {
			// If there was an error, just print a stack trace, and return an empty string.
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * Generates the large string that is used to display the departure and arrival times of a
	 * particular stop when clicked on.
	 *
	 * @param stopArray        The JSONArray that contains all the stops for the route.
	 * @param count            The number of stops within the JSONArray.
	 * @param context          The context from which this method is being called
	 *                         (used for string lookup).
	 * @param routes           The routes to get the times for.
	 * @param includeRouteName Whether or not to include the route name in the final string.
	 * @return The string containing all the departure and arrival times for the particular stop.
	 * @throws JSONException Thrown if there is a JSONException while parsing the data for the stop.
	 */
	@NotNull
	private static String generateTimeString(org.json.JSONArray stopArray, int count, Context context,
	                                         Route[] routes, boolean includeRouteName) throws JSONException {

		StringBuilder snippetText = new StringBuilder(count);

		// Iterate through the stops in the json object to get the time for.
		for (int index = 0; index < count; index++) {
			Log.d("generateTimeString", String.format("Parsing stop times for stop %d/%d",
					index, count));

			// Get the stop time from the current stop.
			JSONObject object = stopArray.getJSONObject(index);

			// First, check if the current time does belong to the desired parentRoute
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
						Log.d("generateTimeString", "Adding route: " + route.routeName);
						snippetText.append(String.format("Route: %s\n", route.routeName));
					}

					// Append the arrival and departure times to the snippet text.
					snippetText.append(String.format("%s %s\n%s %s\n\n", context.getString(R.string.expected_arrival),
							arrivalTime, context.getString(R.string.expected_departure), departureTime));
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
		java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d\\d:\\d\\d")
				.matcher(timeString);

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
	public static int getNewlineOccurrence(@NotNull CharSequence string) { // TODO Unit test
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

		MarkedObject potentialStop = (MarkedObject) circle.getTag();

		if (potentialStop instanceof Stop) {
			Log.d("onCircleClick", "Showing stop info window");

			// Get the stop, and show its marker.
			Stop stop = (Stop) potentialStop;

			if (stop.getMarker() == null) {
				try {
					Marker marker = stop.addMarker(MapsActivity.map, stop.circleOptions.getCenter(), stop.route.color, stop.stopName);
					stop.setMarker(marker);
				} catch (Exception e) {
					e.printStackTrace();
					// If the stop doesn't have a marker, just use toast to display the stop ID.

					Toast.makeText(this.activity, stop.stopName, Toast.LENGTH_SHORT).show();
					Log.w("showStop", String.format("Stop %s has no marker!", stop.stopName));
				}
			}

			//this.showMarker(stop.getMarker());
		} else if (potentialStop instanceof SharedStop) {
			Log.d("onCircleClick", "Showing shared stop info window");

			SharedStop sharedStop = (SharedStop) potentialStop;

			if (sharedStop.getMarker() == null) {
				try {
					for (int i = 0; i < sharedStop.routes.length; i++) {
						Route route = sharedStop.routes[i];
						if (route.enabled) {
							Marker marker = sharedStop.addMarker(MapsActivity.map, sharedStop.circleOptions[i].getCenter(), route.color, sharedStop.stopName);
							sharedStop.setMarker(marker);
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					// If the stop doesn't have a marker, just use toast to display the stop ID.
					Toast.makeText(this.activity, sharedStop.stopName, Toast.LENGTH_SHORT).show();
					Log.w("showStop", String.format("Stop %s has no marker!", sharedStop.stopName));
				}
			}
		} else {
			// If it was neither a stop or a shared stop, warn that there was an unaccounted for object.
			Log.w("onCircleClick", String.format("Circle object (%s) unaccounted for!", circle.getTag()));
			return;
		}

		this.showMarker(potentialStop.getMarker());
	}


	/**
	 * TODO Documentation
	 * @param marker
	 */
	private void showMarker(@NotNull Marker marker) {
		// Since the marker is not null, show it the marker,
		// and set the snippet to the times when the bus is expected to arrive / depart.
		marker.setVisible(true);
		marker.setSnippet(this.activity.getString(fnsb.macstransit.R.string.retrieving_stop_times));
		// FIXME
		// new fnsb.macstransit.Activities.ActivityListeners.Async.GetStopTimes(marker, this.activity).execute(marker);
		marker.showInfoWindow();
	}
}
