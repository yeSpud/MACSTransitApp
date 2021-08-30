package fnsb.macstransit.routematch

import android.util.Log
import androidx.annotation.UiThread
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.ktx.addCircle
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by Spud on 2019-10-18 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 3.0.
 * @since Beta 6.
 */
class Stop(stopName: String, location: LatLng, val route: Route) : MarkedObject(stopName, location) {

	/**
	 * The circle marking the bus stop on the map
	 * (be sure to check if this exists first as it may be null).
	 */
	var circle: Circle? = null
	private set

	/**
	 * Documentation
	 *
	 * @param stopName
	 * @param latitude
	 * @param longitude
	 * @param route
	 */
	constructor(stopName: String, latitude: Double, longitude: Double, route: Route) : this(
			stopName, LatLng(latitude, longitude), route)

	/**
	 * Lazy creation of a new Stop object using the provided JSON and the route.
	 *
	 * @param json  The JSONObject containing the bus stop data.
	 * @param route The route this newly created Stop object will apply to.
	 * @throws JSONException Thrown if there is any issue in parsing the data from the provided JSONObject.
	 */
	constructor(json: JSONObject, route: Route) : this(json.getString("stopId"),
	                                                   json.getDouble("latitude"),
	                                                   json.getDouble("longitude"), route)

	/**
	 * Shows the stops for the given route.
	 * If the stops weren't previously added to the map then this method will also see fit to add them to the map.
	 * This should be run on the UI thread.
	 *
	 * @param map The google maps object that the stops will be drawn onto.
	 * Be sure this object has been initialized first.
	 * @param attempted Comments
	 */
	@UiThread
	fun toggleStopVisibility(map: GoogleMap, attempted: Boolean = false) {

		// Check if the circle for the stop needs to be created,
		// or just set to visible if it already exists.
		if (this.circle == null) {

			if (attempted) {
				Log.w("toggleStopVisibility", "Unable to create circle for stop ${this.name}")
				return
			}

			// Create a new circle object.
			Log.d("toggleStopVisibility", "Creating new stop for ${this.name}")
			this.createStopCircle(map)
			this.toggleStopVisibility(map, true)
		} else {

			// Since the circle already exists simply update its visibility.
			Log.d("toggleStopVisibility", "Setting stop ${this.name} to visible: ${this.route.enabled}")
			this.circle!!.isClickable = this.route.enabled
			this.circle!!.isVisible = this.route.enabled
		}
	}

	/**
	 * Removes the stop's circle from the map.
	 * This also sets the circle to null so it can be recreated later.
	 * This must be run on the UI Thread.
	 */
	@UiThread
	fun removeStopCircle() {

		// Remove stop circles (if it has them).
		if (circle != null) {
			circle!!.remove()
			circle = null
		}
	}

	/**
	 * Creates a new circle object for new Stops.
	 *
	 * @param map     The google maps object that this newly created circle will be added to.
	 * This cannot be null.
	 */
	@UiThread
	fun createStopCircle(map: GoogleMap) {

		// Add our circle to the map.
		this.circle = map.addCircle {

			// Comments
			this.center(this@Stop.location)

			// Comments
			this.radius(STARTING_RADIUS)

			val route: Route = this@Stop.route

			// Set the colors.
			this.fillColor(route.color)
			this.strokeColor(route.color)

			// Set the stop to be visibility to whether or not the route is enabled.
			this.clickable(route.enabled)
			this.visible(route.enabled)
		}

		// Set the tag of the circle to Stop so that it can differentiate between this class
		// and other stop-like classes (such as shared stops).
		this.circle!!.tag = this // TODO Set me after check
	}

	companion object {

		/**
		 * The starting radius size of the circle for the stop on the map (in meters).
		 */
		private const val STARTING_RADIUS = 50.0

		/**
		 * Creates an array of stops from the provided json array.
		 * If the json array is null then the stop array will be 0 in length.
		 *
		 * @param array The json array containing the stop information.
		 * @param route The route these stops belongs to.
		 * @return The stop array created from the json array.
		 */
		@JvmStatic
		fun generateStops(array: JSONArray,
		                  route: Route): Array<Stop> { // TODO Test me with empty array

			// Create an array of stops that will be filled using the information from the json array.
			val count = array.length()
			val uncheckedStops = arrayOfNulls<Stop>(count)

			// Iterate though the json array.
			for (i in 0 until count) {

				// Try to create a new stop object using the information in the json array.
				val stop: Stop = try {
					Stop(array.getJSONObject(i), route)
				} catch (e: JSONException) {

					// If unsuccessful simply log the exception and continue iterating.
					Log.e("generateStops", "Exception occurred while creating stop!", e)
					continue
				}
				uncheckedStops[i] = stop
			}

			// Return the stop array.
			return uncheckedStops.requireNoNulls()
		}

		/**
		 * Validates the provided array of potential stops and returned the actual stops in the route,
		 * removing any duplicate or invalid stops.
		 *
		 * @param potentialStops The potential stops that may contain duplicate or invalid stops.
		 * @return The validated stops array (or an empty stop array if the provided potential stops is null).
		 */
		@JvmStatic
		fun validateGeneratedStops(
				potentialStops: Array<Stop>): Array<Stop> { // TODO Test with empty array

			// Create a variable to store the true size of the stops that have been validated.
			var validatedSize = 0

			// Create an array to store the validated stops.
			// While we don't know the specific size of this array until done, we do know the maximum size,
			// so use that for setting the array size.
			val validatedStops = arrayOfNulls<Stop>(potentialStops.size)

			// Iterate through each stop in our array of potential stops.
			for (stop in potentialStops) {

				// Check to see if the stop is in our array of validated stops. If its not,
				// add it to the array and add 1 to the true index size of stops that have been validated.
				if (!isDuplicate(stop, validatedStops)) {
					validatedStops[validatedSize] = stop
					validatedSize++
				}
			}

			// Create an array for our actual stops.
			// Since we now know the number of validated stops we can use that as its size.
			val actualStops = arrayOfNulls<Stop>(validatedSize)

			// Copy our validated stops into our smaller actual stops array, and return it.
			System.arraycopy(validatedStops, 0, actualStops, 0, actualStops.size)
			return actualStops.requireNoNulls()
		}

		/**
		 * Checks the provided stop against an array of stops to check if its already contained in the array
		 * (and is therefore a would-be duplicate).
		 *
		 * @param stop      The Stop object to check for.
		 * @param stopArray The stop array to compare the Stop object against.
		 * @return Returns true if the Stop object was found within the array - otherwise it returns false.
		 */
		@JvmStatic
		fun isDuplicate(stop: Stop, stopArray: Array<Stop?>): Boolean {

			// If the provided stop array is null just return false.
			if (stopArray.isEmpty()) {
				return false
			}

			for (stopArrayItem in stopArray) {

				if (stopArrayItem == null) {
					return false
				}

				// Check if the following match.
				val routeMatch = stop.route.routeName == stopArrayItem.route.routeName

				// If all of the following match, return true.
				if (routeMatch && stop == stopArrayItem) {
					return true
				}
			}

			// Since nothing matched, return false.
			return false
		}
	}
}