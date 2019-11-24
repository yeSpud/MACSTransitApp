package fnsb.macstransit.Activities.ActivityListeners;

import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.Marker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;

import fnsb.macstransit.Activities.ActivityListeners.Async.GetSharedStopTimes;
import fnsb.macstransit.Activities.ActivityListeners.Async.GetStopTimes;
import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.RouteMatch;
import fnsb.macstransit.RouteMatch.SharedStop;
import fnsb.macstransit.RouteMatch.Stop;

/**
 * Created by Spud on 2019-10-30 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Beta 7
 */
public class StopClicked implements com.google.android.gms.maps.GoogleMap.OnCircleClickListener {

	/**
	 * The maps activity that this listener corresponds to.
	 */
	private MapsActivity activity;

	/**
	 * Constructor the the StopClicked listener.
	 *
	 * @param activity The activity that this listener corresponds to.
	 */
	public StopClicked(MapsActivity activity) {
		this.activity = activity;
	}

	/**
	 * Returns the time (in 24-hour form) that is found in the provided JSONObject via a regex.
	 *
	 * @param json The JSONObject to search.
	 * @param tag  The tag in the JSONObject to search.
	 * @return The time (in 24-hour form) as a String that was found in the JSONObject.
	 * This may be null if no such string was able to be found, or if there was a JSONException.
	 */
	private static String getTime(JSONObject json, String tag) {
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
	 * Formats the time from 24-hour time to 12-hour time (and even includes AM and PM).
	 *
	 * @param time The time to format as a string.
	 * @return The formatted 12-hour time.
	 * This may return the original 12 hour time if there was an exception parsing the time.
	 */
	private static String formatTime(String time) {
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
	 * TODO Documentation
	 *
	 * @param json
	 * @param expectedArrivalString
	 * @param expectedDepartureString
	 * @param is24Hour
	 * @return
	 */
	public static String getStopTime(JSONObject json, boolean is24Hour, String expectedArrivalString, String expectedDepartureString) {
		StringBuilder snippetText = new StringBuilder();
		JSONArray stopData = RouteMatch.parseData(json);
		int count = stopData.length();

		// Iterate through the following stops
		for (int index = 0; index < count; index++) {
			Log.d("getStopTime", String.format("Parsing stop times for stop %d/%d", index, count));

			// Try to get the stop time from the current stop.
			JSONObject object;
			try {
				object = stopData.getJSONObject(index);
			} catch (JSONException e) {
				// If that fails, just print the stack trace, and break from the for loop.
				e.printStackTrace();
				break;
			}

			// Set the arrival and departure time to the arrival and departure time in the jsonObject.
			// At this point this is stored in 24-hour time.
			String arrivalTime = StopClicked.getTime(object, "predictedArrivalTime"),
					departureTime = StopClicked.getTime(object, "predictedDepartureTime");

			// If the user doesn't use 24-hour time, convert to 12-hour time.
			if (!is24Hour) {
				Log.d("getStopTime", "Converting time to 12 hour time");
				arrivalTime = StopClicked.formatTime(arrivalTime);
				departureTime = StopClicked.formatTime(departureTime);
			}

			// Append the arrival and departure times to the snippet text.
			snippetText.append(String.format("%s %s\n%s %s\n\n", expectedArrivalString, arrivalTime,
					expectedDepartureString, departureTime));
		}

		// Get the length of the original snippet text.
		int length = snippetText.length();

		// Replace the last 2 new lines
		snippetText.deleteCharAt(length - 1);
		snippetText.deleteCharAt(length - 2);

		String string = snippetText.toString();

		// Load the string into a popup window for when its clicked on.
		StopPopupWindow.body = string;

		// Check to see how many times there are to display. If there are 3 or less,
		// just display all the times. If there are more than 3, display "Click to view all times".
		if (count <= 3) {
			return string;
		} else {
			return "Click to view all the arrival and departure times.";
		}
	}

	/**
	 * TODO Documentation
	 *
	 * @param sharedStop
	 * @param json
	 * @param is24Hour
	 * @param expectedArrivalString
	 * @param expectedDepartureString
	 * @return
	 */
	public static String getSharedStopTimes(SharedStop sharedStop, JSONObject[] json, boolean is24Hour, String expectedArrivalString, String expectedDepartureString) {
		StringBuilder snippetText = new StringBuilder();
		int count = 0;
		for (int jsonIndex = 0; jsonIndex < json.length; jsonIndex++) {
			JSONObject jsonObject = json[jsonIndex];
			JSONArray stopData = RouteMatch.parseData(jsonObject);
			count = stopData.length();

			// Iterate through the stops.
			for (int index = 0; index < count; index++) {
				Log.d("showSharedStopInfo", String.format("Parsing stop times for stop %d/%d",
						index, count));

				// Try to get the stop time from the current stop.
				JSONObject object;
				try {
					object = stopData.getJSONObject(index);
				} catch (JSONException e) {
					// If that fails, just print the stack trace, and break from the for loop.
					e.printStackTrace();
					break;
				}


				// Set the arrival and departure time to the arrival and departure time in the jsonObject.
				// At this point this is stored in 24-hour time.
				String arrivalTime = StopClicked.getTime(object, "predictedArrivalTime"),
						departureTime = StopClicked.getTime(object, "predictedDepartureTime");

				// If the user doesn't use 24-hour time, convert to 12-hour time.
				if (!is24Hour) {
					arrivalTime = StopClicked.formatTime(arrivalTime);
					departureTime = StopClicked.formatTime(departureTime);
				}

				// Append the arrival and departure times to the snippet text.
				snippetText.append(String.format("Route: %s\n%s %s\n%s %s\n\n",
						sharedStop.routes[jsonIndex].routeName, expectedArrivalString, arrivalTime,
						expectedDepartureString, departureTime));


			}
		}

		// Get the length of the original snippet text.
		int length = snippetText.length();

		// Replace the last 2 new lines
		snippetText.deleteCharAt(length - 1);
		snippetText.deleteCharAt(length - 2);

		String string = snippetText.toString();

		// Load the string into a popup window for when its clicked on.
		StopPopupWindow.body = string;

		// Check to see how many times there are to display. If there are 3 or less after being multiplied by the json length,
		// just display all the times. If there are more than 3, display "Click to view all times".
		if ((count * json.length) <= 3) {
			return string;
		} else {
			return "Click to view all the arrival and departure times.";
		}
	}

	/**
	 * Called when a circle is clicked.
	 * <p>
	 * This is called on the Android UI thread.
	 *
	 * @param circle The circle that is clicked.
	 */
	@Override
	public void onCircleClick(com.google.android.gms.maps.model.Circle circle) {
		if (circle.getTag() instanceof Stop) { // Check if the circle is a stop
			Log.d("onCircleClick", "Showing stop info window");
			Stop stop = (Stop) circle.getTag();
			Marker marker = stop.getMarker();
			if (marker == null) {
				// If the stop doesn't have a marker, just use toast to display the stop ID.
				Toast.makeText(this.activity, stop.stopID, Toast.LENGTH_SHORT).show();
			} else {
				// If the stop does have a marker, set the marker to be visible, and show the info window corresponding to that marker.
				marker.setVisible(true);

				// Set the snippet text to loading
				marker.setSnippet(this.activity.getString(R.string.retrieving_stop_times));

				// Retrieve the stops asynchronously
				new GetStopTimes(marker, this.activity).execute(stop);

				// Show the info window
				marker.showInfoWindow();
			}
		} else if (circle.getTag() instanceof SharedStop) { // Check if the circle is a shared stop
			Log.d("onCircleClick", "Showing sharedStop info window");
			SharedStop sharedStop = (SharedStop) circle.getTag();
			Marker marker = sharedStop.getMarker();
			if (marker == null) {
				// If the shared stop doesn't have a marker, just use toast to display the stop ID.
				Toast.makeText(this.activity, sharedStop.stopID, Toast.LENGTH_SHORT).show();
			} else {
				// If the stop does have a marker, set the marker to be visible, and show the info window corresponding to that marker.
				marker.setVisible(true);

				// Set the snippet text to loading
				marker.setSnippet(this.activity.getString(R.string.retrieving_stop_times));

				// Retrieve the stops asynchronously
				new GetSharedStopTimes(marker, this.activity).execute(sharedStop);

				// Show the info window
				marker.showInfoWindow();
			}
		} else {
			// If it was neither a stop or a shared stop, warn that there was an unaccounted for object.
			Log.w("onCircleClick", String.format("Circle object (%s) unaccounted for!",
					java.util.Objects.requireNonNull(circle.getTag()).toString()));
		}
	}
}
