package fnsb.macstransit.Activities.ActivityListeners;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.SharedStop;
import fnsb.macstransit.RouteMatch.Stop;

/**
 * Created by Spud on 2019-10-30 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.2
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
	 * Posts the time string for the selected stop,
	 * which contains when the (selected) buses for that stop will be arriving and departing.
	 * This method also posts this string to the body of the popup window when the info window is clicked on.
	 *
	 * @param stop    The stop (either an actual Stop object, or a SharedStop).
	 * @param json    The json object retrieved from the RouteMatch server.
	 * @param context TODO Documentation
	 * @return The string containing either all the arrival and departure times for the stop, or the overflowString if there is too much data.
	 */
	public static String postStopTimes(Object stop, org.json.JSONObject json, Context context) {

		// Get the stop data from the retrieved json.
		org.json.JSONArray stopData = fnsb.macstransit.RouteMatch.RouteMatch.parseData(json);
		int count = stopData.length();

		try {
			// Get the times for the stop.
			// Since the method arguments are slightly different for a shared stop compared to a regular stop,
			// check if the marker is an instance of a Stop or SharedStop.
			String string = StopClicked.generateTimeString(stopData, count, context, (stop instanceof Stop) ? new Route[]{((Stop) stop).route} : ((SharedStop) stop).routes, stop instanceof SharedStop);

			// Load the times string into a popup window for when its clicked on.
			fnsb.macstransit.Activities.PopupWindow.body = string;

			// Check to see how many new lines there are in the display.
			// If there are more than the maximum lines allowed bu the info window adapter,
			// display "Click to view all the arrival and departure times.".
			return Helpers.getCharacterOccurrence('\n', string) <= fnsb.macstransit.Activities.InfoWindowAdapter.MAX_LINES ? string : context.getString(R.string.click_to_view_all_the_arrival_and_departure_times);

		} catch (JSONException e) {
			// If there was an error, just print a stack trace, and return an empty string.
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * TODO Documentation
	 *
	 * @param stopArray
	 * @param count
	 * @param context
	 * @param routes
	 * @param includeRouteName
	 * @return
	 * @throws JSONException
	 */
	private static String generateTimeString(org.json.JSONArray stopArray, int count, Context context, Route[] routes, boolean includeRouteName) throws JSONException {

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
					String arrivalTime = StopClicked.getTime(object, "predictedArrivalTime"),
							departureTime = StopClicked.getTime(object, "predictedDepartureTime");

					// If the user doesn't use 24-hour time, convert to 12-hour time.
					if (!android.text.format.DateFormat.is24HourFormat(context)) {
						Log.d("generateTimeString", "Converting time to 12 hour time");
						arrivalTime = StopClicked.formatTime(arrivalTime);
						departureTime = StopClicked.formatTime(departureTime);
					}

					// Append the route name if there is one
					if (includeRouteName) {
						Log.d("generateTimeString", "Adding route " + route.routeName);
						snippetText.append(String.format("Route: %s\n", route.routeName));
					}

					// Append the arrival and departure times to the snippet text.
					snippetText.append(String.format("%s %s\n%s %s\n\n", context.getString(R.string.expected_arrival), arrivalTime, context.getString(R.string.expected_departure), departureTime));
				}
			}
		}

		return snippetText.toString();
	}

	/**
	 * TODO Documentation
	 *
	 * @param json
	 * @param tag
	 * @return
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
	 * TODO Documentation
	 *
	 * @param time
	 * @return
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
	 * Called when a circle is clicked.
	 * <p>
	 * This is called on the Android UI thread.
	 *
	 * @param circle The circle that is clicked.
	 */
	@Override
	public void onCircleClick(com.google.android.gms.maps.model.Circle circle) {

		// Check if the circle is a stop or a shared stop.
		if (circle.getTag() instanceof Stop) {
			Log.d("onCircleClick", "Showing stop info window");

			// Get the stop, and show its marker.
			Stop stop = (Stop) circle.getTag();
			this.showStop(stop, stop.getMarker(), stop.stopID);
		} else if (circle.getTag() instanceof SharedStop) {
			Log.d("onCircleClick", "Showing SharedStop info window");

			// Get the shared stop, and show its marker.
			SharedStop sharedStop = (SharedStop) circle.getTag();
			this.showStop(Helpers.findStopInSharedStop(sharedStop), sharedStop.getMarker(), sharedStop.stopID);
		} else {
			// If it was neither a stop or a shared stop, warn that there was an unaccounted for object.
			Log.w("onCircleClick", String.format("Circle object (%s) unaccounted for!",
					java.util.Objects.requireNonNull(circle.getTag()).toString()));
		}
	}

	/**
	 * Shows the stop marker (or stop ID via toast if there is no marker) when tapped on.
	 *
	 * @param stop   The stop that is to be shown.
	 * @param marker The marker corresponding to the stop (this may be null).
	 * @param stopId The Id if the stop if the marker is null. This may NOT be null.
	 */
	private void showStop(Stop stop, com.google.android.gms.maps.model.Marker marker, String stopId) {
		if (marker == null) {
			// If the stop doesn't have a marker, just use toast to display the stop ID.
			Toast.makeText(this.activity, stopId, Toast.LENGTH_SHORT).show();
			Log.w("showStop", String.format("Stop %s has no marker!", stopId));
		} else {
			// Since the marker is not null, show it the marker,
			// and set the snippet to the times when the bus is expected to arrive / depart.
			marker.setVisible(true);
			marker.setSnippet(this.activity.getString(fnsb.macstransit.R.string.retrieving_stop_times));
			new fnsb.macstransit.Activities.ActivityListeners.Async.GetStopTimes(marker, this.activity).execute(stop);
			marker.showInfoWindow();
		}
	}
}
