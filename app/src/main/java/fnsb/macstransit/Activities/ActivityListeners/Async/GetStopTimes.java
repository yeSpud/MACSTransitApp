package fnsb.macstransit.Activities.ActivityListeners.Async;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.model.Marker;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

import fnsb.macstransit.RouteMatch.Stop;

/**
 * Created by Spud on 2019-11-23 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.1
 * @since Beta 8.
 */
public class GetStopTimes extends android.os.AsyncTask<Stop, Void, JSONObject> {

	/**
	 * The marker object that will be updated.
	 * <br>
	 * <i>It's the the info window of the marker that is going to be updated if you want to be more technical.</i>
	 */
	private Marker marker;

	/**
	 * A weak reference for the context object.
	 * This is used in order to pass a context object between async tasks without having a memory leak.
	 * <p>
	 * https://stackoverflow.com/questions/45653121/passing-context-from-service-to-asynctask-without-leaking-it
	 */
	private WeakReference<Context> context;

	/**
	 * Constructor for the Asynchronous method.
	 * This is used to initialize a few variables that will be used later in the background,
	 * or once execution has completed.
	 *
	 * @param marker  The marker object that will be updated once the background async process has finished.
	 * @param context The context from which this is being called from.
	 */
	public GetStopTimes(Marker marker, Context context) {
		this.marker = marker;
		this.context = new WeakReference<>(context);
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
		return fnsb.macstransit.Activities.MapsActivity.routeMatch.getStop(stops[0]);
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
					.postStopTimes(marker.getTag(), result, this.context.get()));

			// Refresh the info window by calling showInfoWindow().
			Log.d("onPostExecute", "Refreshing info window");
			this.marker.showInfoWindow();
		}
	}
}