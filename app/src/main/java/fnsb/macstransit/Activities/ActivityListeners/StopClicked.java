package fnsb.macstransit.Activities.ActivityListeners;

import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.Marker;

import fnsb.macstransit.Activities.InfoWindowAdapter;
import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.Activities.PopupWindow;
import fnsb.macstransit.RouteMatch.SharedStop;
import fnsb.macstransit.RouteMatch.Stop;

/**
 * Created by Spud on 2019-10-30 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.1
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
	 * @param stop                    The stop (either an actual Stop object, or a SharedStop).
	 * @param json                    The json object retrieved from the RouteMatch server.
	 * @param is24Hour                Whether or not the user is using 24 hour time.
	 * @param expectedArrivalString   The expected arrival string resource.
	 * @param expectedDepartureString The expected departure string resource.
	 * @param overflowString          The string resource used to notify the user to click on the info window when there is too much content to be displayed.
	 * @return The string containing either all the arrival and departure times for the stop, or the overflowString if there is too much data.
	 */
	public static String postStopTimes(Object stop, org.json.JSONObject json, boolean is24Hour, String expectedArrivalString, String expectedDepartureString, String overflowString) {

		// Get the stop data from the retrieved json.
		org.json.JSONArray stopData = fnsb.macstransit.RouteMatch.RouteMatch.parseData(json);
		int count = stopData.length();

		try {
			// Get the times for the stop.
			// Since the method arguments are slightly different for a shared stop compared to a regular stop,
			// check if the marker is an instance of a Stop or SharedStop.
			String string = Helpers.generateTimeString(stopData, count, expectedArrivalString,
					expectedDepartureString, is24Hour, (stop instanceof Stop) ?
							new fnsb.macstransit.RouteMatch.Route[]{((Stop) stop).route} :
							((SharedStop) stop).routes, stop instanceof SharedStop);

			// Load the times string into a popup window for when its clicked on.
			PopupWindow.body = string;

			// Check to see how many new lines there are in the display.
			// If there are more than the maximum lines allowed bu the info window adapter,
			// display "Click to view all the arrival and departure times.".
			if (Helpers.getCharacterOccurrence('\n', string) <= InfoWindowAdapter.MAX_LINES) {
				return string;
			} else {
				return overflowString;
			}
		} catch (org.json.JSONException e) {
			// If there was an error, just print a stack trace, and return an empty string.
			e.printStackTrace();
			return "";
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
	private void showStop(Stop stop, Marker marker, String stopId) {
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
