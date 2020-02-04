package fnsb.macstransit.Activities.ActivityListeners.Async;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

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
	 * The route that this asynchronous task will update.
	 */
	private Route route;

	/**
	 * Constructor for the Async UpdateBuses class.
	 * All that's needed is the map that will be used once the background processes have finished.
	 *
	 * @param route The route that corresponds to this task.
	 */
	public UpdateBuses(Route route) {
		this.route = route;
	}

	@Override
	protected Bus[] doInBackground(Void... no) {

		// Get all the buses used by the selected routes.
		Bus[] buses = new Bus[0];

		// First, make sure this wasn't canceled. If it was, simply continue and return the empty array list.
		if (!this.isCancelled() && this.route != null) {
			// Then, try to get the buses used by the route.
			try {
				// Get the buses used in the selected routes from the RouteMatch server.
				buses = Bus.getBuses(this.route);
				Log.d("doInBackground", "Total number of buses for route: " + buses.length);
			} catch (org.json.JSONException e) {
				e.printStackTrace();
			}
		}

		// Finally, return all the buses that were retrieved from the server.
		return buses;
	}

	@Override
	protected void onPostExecute(Bus[] newBuses) {

		// Make sure the task wasn't canceled.
		if (this.isCancelled()) {
			return;
		}

		// Add new buses if they weren't on the map
		Log.d("onPostExecute", "Adding new buses to map");
		ArrayList<Bus> buses = new ArrayList<>(Arrays.asList(Bus.addNewBuses(this.route.buses, newBuses)));

		// Update the old buses with the new bus locations if IDs and Routes are shared
		Log.d("onPostExecute", "Updating existing buses on map");
		buses.addAll(Arrays.asList(Bus.updateCurrentBuses(this.route.buses, newBuses)));

		// Remove the old buses that are no longer applicable
		Log.d("onPostExecuted", "Removing old buses from map");
		Bus.removeOldBuses(this.route.buses, newBuses);

		// Reapply the buses
		Log.d("onPostExecuted", "Applying buses to route");
		this.route.buses = buses.toArray(new Bus[0]);

		// Double check whether or not this was canceled. If it was, then run the canceled method.
		if (this.isCancelled()) {
			this.onCancelled(newBuses);
		}
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();

		// Remove ALL buses for the route.
		Log.d("onCancelled", "Removing buses from route");
		Bus.removeOldBuses(this.route.buses, new Bus[0]);
		this.route.buses = new Bus[0];
	}
}
