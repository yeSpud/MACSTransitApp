package fnsb.macstransit.Activities.ActivityListeners;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.maps.model.Marker;

import org.json.JSONObject;

import fnsb.macstransit.RouteMatch.MarkedObject;
import fnsb.macstransit.Threads.StopTimeCallback;

/**
 * Created by Spud on 2021-02-10 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0.
 * @since Release 1.2.
 */
public class GetStopTimeHandler implements StopTimeCallback.AsyncCallback {

	/**
	 * TODO Documentation
	 */
	private final Marker marker;

	/**
	 * TODO Documentation
	 */
	private final Activity activity;

	public GetStopTimeHandler(Marker marker, Activity activity) {
		this.marker = marker;
		this.activity = activity;
	}

	@Override
	public void receivedStopTime(JSONObject departures) {

		// Only execute if the marker isn't null
		if (this.marker != null) {

			this.activity.runOnUiThread(() -> {

				// Update the snippet text of the marker's info window
				Log.v("receivedStopTime", "Updating snippet");
				this.marker.setSnippet(StopClicked.postStopTimes((MarkedObject) marker.getTag(), departures, this.activity));

				// Refresh the info window by calling showInfoWindow().
				Log.v("receivedStopTime", "Refreshing info window");
				this.marker.showInfoWindow();

			});
		}
	}
}
