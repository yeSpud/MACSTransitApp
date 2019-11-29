package fnsb.macstransit.Activities.ActivityListeners.Async;

import com.google.android.gms.maps.GoogleMap;

import fnsb.macstransit.RouteMatch.Bus;
import fnsb.macstransit.RouteMatch.Route;

/**
 * Created by Spud on 2019-11-23 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Beta 8
 */
public class UpdateBuses extends android.os.AsyncTask<Route, Void, Route[]> {

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
	 * @param routes The parameters of the task. In our case its the routes for the buses.
	 * @return A result, defined by the subclass of this task.
	 * In our case it's the same routes as before, just with updated bus positions.
	 */
	@Override
	protected Route[] doInBackground(Route... routes) {
		// For each of the routes provided, execute the following:
		for (Route route : routes) {

			// First, make sure this wasn't canceled. If it was, simply break from the loop now.
			if (this.isCancelled()) {
				break;
			}

			try {

				// Store the old buses from the route into its own array,
				// and create another bus array containing the newly queried buses.
				Bus[] oldBuses = route.buses, newBuses = Bus.getBuses(route);

				// Check to see if the length of the old buses does not match that of the new buses.
				// If they're different, simply replace the buses in the route with the new buses.
				if (oldBuses.length != newBuses.length) {
					route.buses = newBuses;
				} else {

					// Since the buses in the route were the same length, check the individual buses.
					// Start by iterating through the old buses.
					for (int index = 0; index < oldBuses.length; index++) {

						// Get an old bus.
						Bus oldBus = oldBuses[index];

						// Iterate trough the new buses, and check of the new bus ID equals that of the old bus ID.
						for (Bus newBus : newBuses) {

							// If the IDs match, update the route color, as well as the coordinates.
							if (newBus.busID.equals(oldBus.busID)) {
								oldBus.color = newBus.color;
								oldBus.latitude = newBus.latitude;
								oldBus.longitude = newBus.longitude;
							}
						}

						// Finally, update the old bus at the current index to that of the newly updated bus.
						oldBuses[index] = oldBus;
					}
				}
			} catch (org.json.JSONException e) {
				e.printStackTrace();
			}
		}

		// Finally, return the initial routes, but with the updated buses.
		return routes;
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
	protected void onPostExecute(Route[] result) {
		Bus.drawBuses(result, this.map);
	}
}
