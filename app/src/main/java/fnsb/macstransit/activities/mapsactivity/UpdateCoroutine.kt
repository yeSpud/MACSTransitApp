package fnsb.macstransit.activities.mapsactivity

import android.util.Log
import com.android.volley.Response
import com.android.volley.VolleyError
import com.google.android.gms.maps.GoogleMap
import fnsb.macstransit.routematch.Bus
import fnsb.macstransit.routematch.RouteMatch
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by Spud on 8/20/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
class UpdateCoroutine(private val updateFrequency: Long, private val mapsViewModel: MapsViewModel,
                      map: GoogleMap) {

	/**
	 * The callback to execute once the vehicles json has been returned from the server.
	 */
	private val callback: Callback = Callback(map)

	/**
	 * Boolean to determine whether or not to continue running the coroutine's update loop.
	 */
	var run: Boolean = false

	/**
	 * Whether or not the coroutine loop (the while loop) is currently running.
	 */
	var isRunning: Boolean = false
		private set

	/**
	 * Starts the coroutine's update loop.
	 */
	suspend fun start() {

		// Set the running variable to true.
		Log.i("UpdateCoroutine", "Starting up...")
		isRunning = true

		// Continue looping while the run variable is true.
		while(run) {
			Log.d("UpdateCoroutine", "Looping...")

			// Cancel any network requests with this as the tag as they have essentially timed out.
			mapsViewModel.routeMatch.networkQueue.cancelAll(this)

			// Get all the buses that can be tracked (even the ones that are disabled).
			mapsViewModel.routeMatch.callVehiclesByRoutes(callback, {
				error: VolleyError -> Log.w("UpdateCoroutine", "Unable to fetch buses", error)
			}, this, *mapsViewModel.routes.values.toTypedArray())

			// Wait for the specified update frequency.
			Log.v("UpdateCoroutine", "Waiting for $updateFrequency milliseconds")
			delay(updateFrequency)
		}

		// Since we have broken out of the while loop,
		// and are about to exist set the running variable to false.
		isRunning = false
		Log.i("UpdateCoroutine", "Shutting down...")
	}

	/**
	 * Callback used once the bus network call has finished.
	 * This is the function that updates the buses on the map.
	 */
	internal inner class Callback(private val map: GoogleMap): Response.Listener<JSONObject> {

		override fun onResponse(response: JSONObject) {

			// Get all the vehicles from the JSON Object as a JSON Array.
			val vehiclesJson: JSONArray = RouteMatch.parseData(response)

			// Convert the JSON Array of Buses into an Array of Buses.
			val buses: Array<Bus> = try {
				Bus.getBuses(vehiclesJson, mapsViewModel.routes)
			} catch (exception: JSONException) {
				Log.e("Callback", "Could not parse bus json", exception)
				return
			}

			// Get the array of new buses.
			// These buses are buses that were not previously on the map until now.
			Log.d("Callback", "Adding new buses to map")
			val newBuses: Array<Bus> = Bus.addNewBuses(mapsViewModel.buses, buses, map)

			// Update the current position of our current buses.
			// This also removes old buses from the array, but they still have markers on the map.
			Log.d("Callback", "Updating current buses on map")
			val currentBuses: Array<Bus> = Bus.updateCurrentBuses(mapsViewModel.buses, buses)

			// Remove the markers of the old buses that are no longer on the map.
			Log.d("Callback", "Removing old buses from map")
			Bus.removeOldBuses(mapsViewModel.buses, buses)

			// Create a new bus array that will store our new and updated buses.
			val finalBusArray: Array<Bus?> = arrayOfNulls(newBuses.size + currentBuses.size)

			// Populate our bus array.
			System.arraycopy(newBuses, 0, finalBusArray, 0, newBuses.size)
			System.arraycopy(currentBuses, 0, finalBusArray, newBuses.size, currentBuses.size)

			// Make sure our entire array was filled.
			if (finalBusArray.isNotEmpty() && finalBusArray[finalBusArray.size - 1] == null) {
				Log.w("Callback", "Bus array was populated incorrectly!")
			}

			// Set our bus array.
			@Suppress("UNCHECKED_CAST") // Suppressed because we are asserting that none of the buses are null
			mapsViewModel.buses = finalBusArray as Array<Bus>
		}
	}
}