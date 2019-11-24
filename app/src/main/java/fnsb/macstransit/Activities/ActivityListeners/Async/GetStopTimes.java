package fnsb.macstransit.Activities.ActivityListeners.Async;

import android.util.Log;

import com.google.android.gms.maps.model.Marker;

import org.json.JSONObject;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.Stop;

/**
 * Created by Spud on 2019-11-23 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Beta 8
 */
public class GetStopTimes extends android.os.AsyncTask<Stop, Void, JSONObject> {

	/**
	 * The marker object that will be updated.
	 * <br>
	 * <i>It's the the info window of the marker that is going to be updated if you want to be more technical.</i>
	 */
	private Marker marker;

	/**
	 * The expected arrival, departure, and overflow strings (retrieved from the string value file).
	 */
	private String expectedArrival, expectedDeparture, overflowString;

	/**
	 * Boolean that will be used later to check if the current device is in 24 hour time.
	 */
	private boolean is24Hour;

	/**
	 * Constructor for the Asynchronous method.
	 * This is used to initialize a few variables that will be used later in the background,
	 * or once execution has completed.
	 *
	 * @param marker   The marker object that will be updated once the background async process has finished.
	 * @param activity The activity of the application.
	 *                 This is used to determine whether the user is using 24 time or not,
	 *                 and to retrieve the expected arrival, departure, and overflow strings.
	 */
	public GetStopTimes(Marker marker, MapsActivity activity) {
		this.marker = marker;
		this.is24Hour = android.text.format.DateFormat.is24HourFormat(activity);
		this.expectedArrival = activity.getString(R.string.expected_arrival);
		this.expectedDeparture = activity.getString(R.string.expected_departure);
		this.overflowString = activity.getString(R.string.click_to_view_all_the_arrival_and_departure_times);
	}

	/**
	 * Override this method to perform a computation on a background thread.
	 * The specified parameters are the parameters passed to execute(Params...) by the caller of this task.
	 * This will normally run on a background thread. But to better support testing frameworks,
	 * it is recommended that this also tolerates direct execution on the foreground thread,
	 * as part of the execute(Params...) call.
	 * This method can call publishProgress(Progress...) to publish updates on the UI thread.
	 * This method may take several seconds to complete, so it should only be called from a worker thread.
	 *
	 * @param stops The parameters of the task.
	 *              This should only be the stop that needs to retrieved by the RouteMatch object from the RouteMatch server.
	 * @return A result, defined by the subclass of this task.
	 * This should be the corresponding JSON from the RouteMatch server.
	 */
	@Override
	protected JSONObject doInBackground(Stop... stops) {
		Log.d("doInBackground", "Retrieving stop data...");
		return MapsActivity.routeMatch.getStop(stops[0]);
	}

	/**
	 * Runs on the UI thread after doInBackground(Params...).
	 * The specified result is the value returned by doInBackground(Params...).
	 * To better support testing frameworks,
	 * it is recommended that this be written to tolerate direct execution as part of the execute() call.
	 * This current implementation will update the snippet of the stop's marker.
	 * <p>
	 * This method won't be invoked if the task was cancelled.
	 * <p>
	 * <p>
	 * This method must be called from the Looper#getMainLooper() of your app.
	 *
	 * @param result The result of the operation computed by doInBackground(Params...).
	 */
	@Override
	protected void onPostExecute(JSONObject result) {
		// Only execute if the marker isn't null
		if (this.marker != null) {

			// Update the snippet text of the marker's info window
			Log.d("onPostExecute", "Updating snippet");
			this.marker.setSnippet(fnsb.macstransit.Activities.ActivityListeners.StopClicked
					.postStopTimes(marker.getTag(), result, is24Hour, expectedArrival, expectedDeparture, overflowString));

			// Refresh the info window by calling showInfoWindow().
			Log.d("onPostExecute", "Refreshing info window");
			this.marker.showInfoWindow();
		}
	}
}
