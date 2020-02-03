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

		if (isCancelled()) {
			return;
		}

		// TODO Add new buses if they weren't on the map
		Log.d("onPostExecute", "Adding new buses to map");
		ArrayList<Bus> buses = new ArrayList<>(Arrays.asList(Bus.addNewBuses(this.route.buses, newBuses)));

		// TODO Update the old buses with the new bus locations if IDs and Routes are shared
		Log.d("onPostExecute", "Updating existing buses on map");
		buses.addAll(Arrays.asList(Bus.updateCurrentBuses(this.route.buses, newBuses)));

		// TODO Remove the old buses that are no longer applicable
		Log.d("onPostExecuted", "Removing old buses from map");
		Bus.removeOldBuses(this.route.buses, newBuses);

		// TODO Reapply the buses
		Log.d("onPostExecuted", "Applying buses to route");
		this.route.buses = buses.toArray(new Bus[0]);

		/*
		// Iterate through each of the resulting buses and execute the following:
		for (int i = 0; i < newBuses.length; i++) {
			Bus bus = newBuses[i];

			// TODO Remove buses that no longer exist for this route.

			// TODO Comments
			boolean found = false;
			for (Bus preExistingBus : this.route.buses) {
				if (bus.busID.equals(preExistingBus.busID)) {
					bus.setMarker(preExistingBus.getMarker());
					bus.color = preExistingBus.color;
					bus.route = preExistingBus.route;
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

					// Be sure to set the marker as visible.
					Log.d("onPostExecute", "Setting marker to visible");
					marker.setVisible(true);

				}
			} else {
				// Since the buses marker does not exist, add it to the map.
				Log.d("onPostExecute", "Bus marker does not yet exits. Creating marker...");
				marker = bus.addMarker(MapsActivity.map, bus.latitude, bus.longitude, bus.color,
						"Bus " + bus.busID);

				// Be sure to set the marker as visible.
				Log.d("onPostExecute", "Setting marker to visible");
				marker.setVisible(true);

			}

			// Apply the bus marker.
			Log.d("onPostExecute", "Applying bus marker");
			bus.setMarker(marker);

			// Apply the bus to the result.
			newBuses[i] = bus;
		}

		// Apply the buses to the route
		this.route.buses = newBuses;
		 */
	}

	@Override
	protected void onCancelled(Bus[] newBuses) {
		super.onCancelled(newBuses);

		// TODO Remove all buses for the route.
		Log.d("onCancelled", "Removing buses from route");
		Bus.removeOldBuses(this.route.buses, newBuses);
		this.route.buses = new Bus[0];
	}
}
