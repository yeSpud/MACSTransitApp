package fnsb.macstransit.activities.mapsactivity

import android.util.Log
import com.google.android.gms.maps.GoogleMap
import fnsb.macstransit.routematch.Bus
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
	 * Documentation
	 */
	var run: Boolean = false

	/**
	 * Documentation
	 */
	var isRunning: Boolean = false
		private set

	/**
	 * Documentation
	 */
	suspend fun start() {

		// Comments
		Log.i("UpdateCoroutine", "Starting up...")
		this.isRunning = true

		// Comments
		while(this.run) {
			Log.d("UpdateCoroutine", "Looping...")

			// Comments
			this.mapsViewModel.routeMatch.networkQueue.cancelAll(this)

			// Comments
			this.mapsViewModel.routeMatch.callVehiclesByRoutes(this.callback, {
				error: com.android.volley.VolleyError ->
				Log.w("UpdateCoroutine", "Unable to fetch buses", error)
			}, this, *MapsActivity.allRoutes)

			Log.v("UpdateCoroutine", "Waiting for ${this.updateFrequency} milliseconds")
			kotlinx.coroutines.delay(this.updateFrequency)
		}

		// Comments
		this.isRunning = false
		Log.i("UpdateCoroutine", "Shutting down...")
	}

	/**
	 * Documentation
	 */
	internal inner class Callback(private val map: GoogleMap): com.android.volley.Response.Listener<JSONObject> {

		override fun onResponse(response: JSONObject) {

			// Comments
			val vehiclesJson: org.json.JSONArray = fnsb.macstransit.routematch.RouteMatch.parseData(response)

			// Comments
			val buses: Array<Bus> = try {
				Bus.getBuses(vehiclesJson)
			} catch (exception: org.json.JSONException) {
				Log.e("Callback", "Could not parse bus json", exception)
				return
			}

			Log.v("Callback", "Updating buses on map")

			// Get the array of new buses.
			// These buses are buses that were not previously on the map until now.
			Log.d("Callback", "Adding new buses to map")
			val newBuses: Array<Bus> = Bus.addNewBuses(this@UpdateCoroutine.mapsViewModel.buses,
			                                           buses, this.map)

			// Update the current position of our current buses.
			// This also removes old buses from the array, but they still have markers on the map.
			Log.d("Callback", "Updating current buses on map")
			val currentBuses: Array<Bus> = Bus.updateCurrentBuses(this@UpdateCoroutine.mapsViewModel.
			buses, buses)

			// Remove the markers of the old buses that are no longer on the map.
			Log.d("Callback", "Removing old buses from map")
			Bus.removeOldBuses(this@UpdateCoroutine.mapsViewModel.buses, buses)

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
			this@UpdateCoroutine.mapsViewModel.buses = finalBusArray as Array<Bus>
		}
	}
}