package fnsb.macstransit.Activities.ActivityListeners.Async;

import android.util.Log;

import java.util.ArrayList;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.RouteMatch.Bus;
import fnsb.macstransit.RouteMatch.Route;

/**
 * Created by Spud on 2019-11-23 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.0
 * @since Beta 8.
 */
public class UpdateBuses extends android.os.AsyncTask<Void, Void, Bus[]> {

	/**
	 * TODO
	 */
	private Route route;

	/**
	 * Constructor for the Async UpdateBuses class.
	 * All that's needed is the map that will be used once the background processes have finished.
	 *
	 * @param route TODO
	 */
	public UpdateBuses(Route route) {
		this.route = route;
	}

	/**
	 * Override this method to perform a computation on a background thread.
	 * The specified parameters are the parameters passed to execute(Params...) by the caller of this task.
	 * This will normally run on a background thread. But to better support testing frameworks,
	 * it is recommended that this also tolerates direct execution on the foreground thread,
	 * as part of the execute(Params...) call.
	 * This method can call publishProgress(Progress...) to publish updates on the UI thread.
	 * This method may take several seconds to complete,
	 * so it should only be called from a worker thread.
	 *
	 * @param no The parameters of the task. Since its void in our case don't use it.
	 * @return A result, defined by the subclass of this task.
	 * In our case it's the same childRoutes as before, just with updated bus positions.
	 */
	@Override
	protected Bus[] doInBackground(Void... no) {

		// Get all the buses used by the selected routes.
		ArrayList<Bus> buses = new ArrayList<>();

		// First, make sure this wasn't canceled. If it was, simply continue and return the empty array list.
		if (!this.isCancelled() && this.route != null) {
			// Then, try to get the buses used by the route.
			try {
				// Get the buses used in the selected routes from the RouteMatch server.
				buses.addAll(java.util.Arrays.asList(Bus.getBuses(this.route)));
			} catch (org.json.JSONException e) {
				e.printStackTrace();
			}
		}

		// Finally, return all the buses that were retrieved from the server
		return buses.toArray(new Bus[0]);
	}

	/**
	 * Runs on the UI thread after doInBackground(Params...).
	 * The specified result is the value returned by doInBackground(Params...).
	 * To better support testing frameworks,
	 * it is recommended that this be written to tolerate direct execution as part of the execute() call.
	 * The default version does nothing.
	 * <p>
	 * This method won't be invoked if the task was cancelled.
	 * <p>
	 * <p>
	 * This method must be called from the Looper#getMainLooper() of your app.
	 * <p>
	 * Essentially this should draw the buses to the map once complete.
	 *
	 * @param result The result of the operation computed by doInBackground(Params...).
	 */
	@Override
	protected void onPostExecute(Bus[] result) {

		// TODO Check for matching buses!
		// Iterate through each of the resulting buses and execute the following:
		for (int i = 0; i < result.length; i++) {
			Bus bus = result[i];

			// TODO Comments
			boolean found = false;
			for (Bus preExistingBus : this.route.buses) {
				if (bus.busID.equals(preExistingBus.busID)) {
					bus.setMarker(preExistingBus.getMarker());
					found = true;
					break;
				}
			}

			// If the activity was canceled by this point, just return.
			if (this.isCancelled()) {
				return;
			}

			// Get the bus marker
			com.google.android.gms.maps.model.Marker marker;

			if (found) {
				Log.d("onPostExecute", "Updating position for bus " + bus.busID);

				marker = bus.getMarker();

				// If the marker already exists (is not null), just update the buses position.
				if (marker != null) {
					Log.d("onPostExecute", "Bus marker already exists. Updating position...");
					marker.setPosition(new com.google.android.gms.maps.model.LatLng(bus.latitude,
							bus.longitude));
				} else {
					// Since the buses marker does not exist, add it to the map.
					Log.d("onPostExecute", "Bus marker does not yet exits. Creating marker...");
					marker = bus.addMarker(MapsActivity.map, bus.latitude, bus.longitude, bus.color,
							"Bus " + bus.busID);
				}
			} else {
				// Since the buses marker does not exist, add it to the map.
				Log.d("onPostExecute", "Bus marker does not yet exits. Creating marker...");
				marker = bus.addMarker(MapsActivity.map, bus.latitude, bus.longitude, bus.color,
						"Bus " + bus.busID);
			}

			// Apply the bus marker.
			Log.d("onPostExecute", "Applying bus marker");
			bus.setMarker(marker);

			// Be sure to set the marker as visible.
			Log.d("onPostExecute", "Setting marker to visible");
			marker.setVisible(true);

			// Apply the bus to the result.
			result[i] = bus;
		}

		// Apply the buses to the route
		this.route.buses = result;
	}
}
