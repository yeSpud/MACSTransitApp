package fnsb.macstransit.Threads;

import android.util.Log;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.RouteMatch.Bus;

/**
 * Created by Spud on 2021-04-01 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.2.
 */
@androidx.annotation.UiThread
public class UpdateBuses implements Runnable {

	/**
	 * Array of potential buses that needs to be parsed onto the map.
	 * Be sure to update this before calling run, but don't update it on a blocking thread.
	 */
	public Bus[] potentialNewBuses;

	/**
	 * The runnable method that updates the buses on the map. This must be run on the UI thread.
	 * Be sure to update potentialNewBuses before calling this method,
	 * or else it will iterate though the old bus array - thus not changing anything.
	 */
	@Override
	public void run() {

		Log.v("UpdateBuses", "Updating buses on map");

		// Get the array of new buses.
		// These buses are buses that were not previously on the map until now.
		Log.d("UpdateBuses", "Adding new buses to map");
		Bus[] newBuses = Bus.addNewBuses(MapsActivity.buses, this.potentialNewBuses);

		// Update the current position of our current buses.
		// This also removes old buses from the array, but they still have markers on the map.
		Log.d("UpdateBuses", "Updating current buses on map");
		Bus[] currentBuses = Bus.updateCurrentBuses(MapsActivity.buses, this.potentialNewBuses);

		// Remove the markers of the old buses that are no longer on the map.
		Log.d("UpdateBuses", "Removing old buses from map");
		Bus.removeOldBuses(MapsActivity.buses, this.potentialNewBuses);

		// Create a new bus array that will store our new and updated buses.
		Bus[] buses = new Bus[newBuses.length + currentBuses.length];

		// Populate our bus array.
		System.arraycopy(newBuses, 0, buses, 0, newBuses.length);
		System.arraycopy(currentBuses, 0, buses, newBuses.length, currentBuses.length);

		// Make sure our entire array was filled.
		if (buses.length != 0 && buses[buses.length - 1] == null) {
			Log.w("UpdateBuses", "Bus array was populated incorrectly!");
		}

		// Set our bus array.
		MapsActivity.buses = buses;
	}
}