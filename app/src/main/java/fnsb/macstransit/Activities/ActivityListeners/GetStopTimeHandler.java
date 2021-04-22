package fnsb.macstransit.Activities.ActivityListeners;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.maps.model.Marker;

/**
 * Created by Spud on 2021-02-10 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0.
 * @since Release 1.2.
 */
public class GetStopTimeHandler implements fnsb.macstransit.Threads.StopTimeCallback.AsyncCallback {

	/**
	 * The marker that belongs to the stop which was clicked.
	 */
	private final Marker marker;

	/**
	 * The parent activity that this handler was called from.
	 */
	private final Activity activity;

	/**
	 * Constructor for the StopTimeHandler.
	 * This class manages the callback from retrieving the departure and arrival times for a given stop.
	 *
	 * @param marker   The marker that belongs to the stop that we are retrieving times for.
	 * @param activity The activity this handler belongs to (we need this to make sure what UI thread to run on later).
	 */
	public GetStopTimeHandler(Marker marker, Activity activity) {
		this.marker = marker;
		this.activity = activity;
	}

	/**
	 * Called when the stop time has been received from the remote server.
	 * While this callback does not return anything it does take the JSONObject of the departures as an argument.
	 *
	 * @param departures The raw departure json retrieved from the url.
	 *                   No processing has been done to it at this point.
	 */
	@Override
	public void receivedStopTime(org.json.JSONObject departures) {

		// Only execute if the marker isn't null.
		if (this.marker != null) {

			// Be sure to run the following on the UI thread to avoid a crash.
			this.activity.runOnUiThread(() -> {

				// Update the snippet text of the marker's info window.
				Log.v("receivedStopTime", "Updating snippet");
				this.marker.setSnippet(StopClicked.postStopTimes((fnsb.macstransit.RouteMatch.MarkedObject)
						marker.getTag(), departures, this.activity));

				// Refresh the info window by calling showInfoWindow().
				Log.v("receivedStopTime", "Refreshing info window");
				this.marker.showInfoWindow();
			});
		}
	}
}
