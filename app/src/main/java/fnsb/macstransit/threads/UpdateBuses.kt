package fnsb.macstransit.threads

import android.util.Log
import fnsb.macstransit.routematch.Bus
import fnsb.macstransit.activities.MapsActivity

/**
 * Created by Spud on 2021-04-01 for the project: MACS Transit.
 *
 *
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Release 1.2.
 */
@androidx.annotation.UiThread
@Deprecated("Use UpdateCoroutine")
class UpdateBuses (private val map: com.google.android.gms.maps.GoogleMap) : Runnable {

	/**
	 * Array of potential buses that needs to be parsed onto the map.
	 * Be sure to update this before calling run, but don't update it on a blocking thread.
	 */
	var potentialNewBuses: Array<Bus> = emptyArray()

	/**
	 * The runnable method that updates the buses on the map. This must be run on the UI thread.
	 * Be sure to update potentialNewBuses before calling this method,
	 * or else it will iterate though the old bus array - thus not changing anything.
	 */
	override fun run() {
		Log.v("UpdateBuses", "Updating buses on map")

		// Get the array of new buses.
		// These buses are buses that were not previously on the map until now.
		Log.d("UpdateBuses", "Adding new buses to map")
		val newBuses: Array<Bus> = Bus.addNewBuses(MapsActivity.buses, potentialNewBuses, this.map)

		// Update the current position of our current buses.
		// This also removes old buses from the array, but they still have markers on the map.
		Log.d("UpdateBuses", "Updating current buses on map")
		val currentBuses: Array<Bus> = Bus.updateCurrentBuses(MapsActivity.buses, potentialNewBuses)

		// Remove the markers of the old buses that are no longer on the map.
		Log.d("UpdateBuses", "Removing old buses from map")
		Bus.removeOldBuses(MapsActivity.buses, potentialNewBuses)

		// Create a new bus array that will store our new and updated buses.
		val buses: Array<Bus?> = arrayOfNulls(newBuses.size + currentBuses.size)

		// Populate our bus array.
		System.arraycopy(newBuses, 0, buses, 0, newBuses.size)
		System.arraycopy(currentBuses, 0, buses, newBuses.size, currentBuses.size)

		// Make sure our entire array was filled.
		if (buses.isNotEmpty() && buses[buses.size - 1] == null) {
			Log.w("UpdateBuses", "Bus array was populated incorrectly!")
		}

		// Set our bus array.
		MapsActivity.buses = buses
	}
}