package fnsb.macstransit.threads

import android.util.Log
import com.android.volley.Response
import com.android.volley.VolleyError
import com.google.android.gms.maps.GoogleMap
import fnsb.macstransit.activities.MapsActivity
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
class UpdateCoroutine(private val updateFrequency: Long, private val routeMatch: RouteMatch, map: GoogleMap) {

	/**
	 * Documentation
	 */
	private val callback: Callback = Callback(map)

	/**
	 * Documentation
	 */
	var state: STATE = STATE.RUN

	/**
	 * Documentation
	 */
	suspend fun main() {

		Log.i("UpdateCoroutine", "Starting up...")

		while(this.state != STATE.STOP) {

			Log.d("UpdateCoroutine", "Looping...")

			// Comments
			this.routeMatch.networkQueue.cancelAll(this)

			when(this.state) {

				STATE.RUN -> {

					if (MapsActivity.allRoutes == null) {
						Log.w("UpdateCoroutine", "No routes to work with!")
						return
					}

					this.routeMatch.callVehiclesByRoutes(this.callback, { error: VolleyError ->
						Log.w("UpdateCoroutine", "Unable to fetch buses", error) }, this, *MapsActivity.allRoutes!!)

					Log.v("UpdateCoroutine", "Waiting for ${this.updateFrequency} milliseconds")
					delay(this.updateFrequency)
				}

				STATE.PAUSE -> {

					Log.i("UpdateCoroutine", "Waiting for thread to resumed...")

					// TODO Pause update thread

					// TODO Wait

					// TODO Resume
				}

				STATE.STOP -> {
					// TODO Stop
				}

			}

		}

		Log.i("UpdateCoroutine", "Shutting down...")

	}

	/**
	 * Documentation
	 */
	enum class STATE {

		/**
		 * Documentation
		 */
		PAUSE,

		/**
		 * Documentation
		 */
		RUN,

		/**
		 * Documentation
		 */
		STOP
	}

	/**
	 * Documentation
	 */
	internal inner class Callback(private val map: GoogleMap): Response.Listener<JSONObject> {

		override fun onResponse(response: JSONObject) {

			// Comments
			val vehiclesJson: JSONArray = RouteMatch.parseData(response)

			// Comments
			val buses: Array<Bus> = try {
				Bus.getBuses(vehiclesJson)
			} catch (exception: JSONException) {
				Log.e("Callback", "Could not parse bus json", exception)
				return
			}

			Log.v("Callback", "Updating buses on map")

			// Get the array of new buses.
			// These buses are buses that were not previously on the map until now.
			Log.d("UpdateBuses", "Adding new buses to map")
			val newBuses: Array<Bus> = Bus.addNewBuses(MapsActivity.buses, buses, this.map)

			// Update the current position of our current buses.
			// This also removes old buses from the array, but they still have markers on the map.
			Log.d("UpdateBuses", "Updating current buses on map")
			val currentBuses: Array<Bus> = Bus.updateCurrentBuses(MapsActivity.buses, buses)

			// Remove the markers of the old buses that are no longer on the map.
			Log.d("UpdateBuses", "Removing old buses from map")
			Bus.removeOldBuses(MapsActivity.buses, buses)

			// Create a new bus array that will store our new and updated buses.
			val finalBusArray: Array<Bus?> = arrayOfNulls(newBuses.size + currentBuses.size)

			// Populate our bus array.
			System.arraycopy(newBuses, 0, finalBusArray, 0, newBuses.size)
			System.arraycopy(currentBuses, 0, finalBusArray, newBuses.size, currentBuses.size)

			// Make sure our entire array was filled.
			if (finalBusArray.isNotEmpty() && finalBusArray[finalBusArray.size - 1] == null) {
				Log.w("UpdateBuses", "Bus array was populated incorrectly!")
			}

			// Set our bus array.
			MapsActivity.buses = finalBusArray
		}
	}

}