package fnsb.macstransit.Activities.ActivityListeners.Async;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

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
public class UpdateBuses extends android.os.AsyncTask<Route, Void, Bus[]> {

	/**
	 * The map used later once the background process has finished.
	 */
	private GoogleMap map;

	/**
	 * Constructor for the Async UpdateBuses class.
	 * All that's needed is the map that will be used once the background processes have finished.
	 *
	 * @param map The map that will be used to update the bus positions.
	 */
	public UpdateBuses(GoogleMap map) {
		this.map = map;
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
	 * @param routes The parameters of the task. In our case its the childRoutes for the buses.
	 * @return A result, defined by the subclass of this task.
	 * In our case it's the same childRoutes as before, just with updated bus positions.
	 */
	@Override
	protected Bus[] doInBackground(Route... routes) {

		// Get all the buses used by the selected routes.
		ArrayList<Bus> buses = new ArrayList<>();

		for (Route route : routes) {

			// First, make sure this wasn't canceled. If it was, simply break from the loop now.
			if (this.isCancelled()) {
				break;
			}

			// Then, try to get the buses used by the route.
			try {
				// Get the buses used in the selected routes
				buses.addAll(java.util.Arrays.asList(Bus.getBuses(route)));
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
	 * @param result he result of the operation computed by doInBackground(Params...).
	 */
	@Override
	protected void onPostExecute(Bus[] result) {
		// TODO Comments
		for (int i = 0; i < result.length; i++) {
			Bus newBus = result[i];
			boolean found = false;
			for (Bus oldBus : MapsActivity.trackedBuses) {
				if (newBus.busID.equals(oldBus.busID)) {
					found = true;
					newBus.setMarker(this.updateMarkerPosition(oldBus.getMarker(), newBus));
					break;
				}
			}

			if (!found) {
				newBus.setMarker(this.updateMarkerPosition(newBus.getMarker(), newBus));
			}
			result[i] = newBus;
		}
		MapsActivity.trackedBuses = result;
	}

	/**
	 * TODO Documentation
	 * TODO Update comments
	 *
	 * @param marker
	 * @param bus
	 * @return
	 */
	private Marker updateMarkerPosition(Marker marker, Bus bus) {

		// If the marker already exists (is not null), just update the buses position
		if (marker != null) {
			marker.setPosition(new com.google.android.gms.maps.model.LatLng(bus.latitude,
					bus.longitude));
		} else {
			// Since the buses marker does not exist, add it to the map.
			marker = bus.addMarker(this.map, bus.latitude, bus.longitude, bus.color,
					"Bus " + bus.busID);
		}

		// Set the bus marker to be visible, and update the bus marker by calling setMarker();
		marker.setVisible(true);
		return marker;
	}
}
