package fnsb.macstransit.Activities.ActivityListeners.Async;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.model.Marker;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.RouteMatch.MarkedObject;
import fnsb.macstransit.RouteMatch.SharedStop;
import fnsb.macstransit.RouteMatch.Stop;

/**
 * Created by Spud on 2019-11-23 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.2.
 * @since Beta 8.
 */
@Deprecated
public class GetStopTimes extends android.os.AsyncTask<String, Void, JSONObject> {

	/**
	 * The marker object that will be updated.
	 * <br>
	 * <i>It's the the info window of the marker that is going to be updated if you want to be more technical.</i>
	 */
	private final Marker marker;

	/**
	 * A weak reference for the context object.
	 * This is used in order to pass a context object between async tasks without having a memory leak.
	 * <p>
	 * https://stackoverflow.com/questions/45653121/passing-context-from-service-to-asynctask-without-leaking-it
	 */
	private final WeakReference<MapsActivity> context;

	/**
	 * Constructor for the Asynchronous method.
	 * This is used to initialize a few variables that will be used later in the background,
	 * or once execution has completed.
	 *
	 * @param marker  The marker object that will be updated once the background async process has finished.
	 * @param context The context from which this is being called from.
	 */
	public GetStopTimes(Marker marker, MapsActivity context) {
		this.marker = marker;
		this.context = new WeakReference<>(context);
	}

	@Override
	protected JSONObject doInBackground(String... stopNames) {
		Log.v("doInBackground", "Retrieving stop data...");

		return MapsActivity.routeMatch.getDeparturesByStop(stopNames[0]);
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
	 * This method must be called from the Looper#getMainLooper() of your app.
	 *
	 * @param result The result of the operation computed by doInBackground(Params...).
	 */
	@Override
	protected void onPostExecute(JSONObject result) {
		// Only execute if the marker isn't null
		if (this.marker != null) {

			// Update the snippet text of the marker's info window
			Log.v("onPostExecute", "Updating snippet");
			this.marker.setSnippet(fnsb.macstransit.Activities.ActivityListeners.StopClicked
					.postStopTimes((MarkedObject) marker.getTag(), result, this.context.get()));

			// Refresh the info window by calling showInfoWindow().
			Log.v("onPostExecute", "Refreshing info window");
			this.marker.showInfoWindow();
		}
	}
}