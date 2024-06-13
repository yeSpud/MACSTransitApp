package fnsb.macstransit.routematch

import androidx.annotation.UiThread
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.ktx.addCircle

/**
 * Created by Spud on 2019-10-18 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 4.0.
 * @since Beta 6.
 */
class Stop(val name: String, val location: LatLng, val route: Route): java.io.Closeable {

	/**
	 * The circle marking the bus stop on the map
	 * (be sure to check if this exists first as it may be null).
	 */
	var circle: com.google.android.gms.maps.model.Circle? = null

	/**
	 * Lazy creation of a new Stop object using the provided JSON and the route.
	 *
	 * @param json  The JSONObject containing the bus stop data.
	 * @param route The route this newly created Stop object will apply to.
	 */
	@Throws(org.json.JSONException::class)
	constructor(json: org.json.JSONObject, route: Route): this(json.getString("stopId"),
	                                                                  LatLng(json.getDouble("latitude"),
	                                                                  json.getDouble("longitude")),
	                                                                  route)

	@UiThread
	private fun addCircleToMap(map: GoogleMap) {
		circle = map.addCircle {

			// Set the location of the circle to the location of the stop.
			center(location)

			// Set the initial size of the circle to the STARTING_RADIUS constant.
			radius(STARTING_RADIUS)

			// Set the colors.
			fillColor(route.color)
			strokeColor(route.color)

			// Set the stop to be clickable and visible based on the visibility boolean.
			clickable(true)
			visible(true)
		}

		// Set the tag of the circle to Stop so that it can differentiate between this class
		// and other stop-like classes (such as shared stops).
		circle!!.tag = this
	}

	/**
	 * Shows the stops for the given route.
	 * If the stops weren't previously added to the map then this method will also see fit to add them to the map.
	 * This must be run on the UI thread.
	 */
	@UiThread
	fun toggleStopVisibility(map: GoogleMap, visible: Boolean) {

		if (circle == null) { addCircleToMap(map) }

		// Since the circle already exists simply update its visibility.
		circle!!.isClickable = visible
		circle!!.isVisible = visible
	}

	companion object {

		/**
		 * The starting radius size of the circle for the stop on the map (in meters).
		 */
		private const val STARTING_RADIUS = 50.0

		/**
		 * Validates the provided array of potential stops and returned the actual stops in the route,
		 * removing any duplicate or invalid stops.
		 *
		 * @param potentialStops The potential stops that may contain duplicate or invalid stops.
		 * @return The validated stops array (or an empty stop array if the provided potential stops is null).
		 */
		@JvmStatic
		fun validateGeneratedStops(potentialStops: Array<Stop>): Array<Stop> {

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

			// Suppressed because we are asserting that none of the coordinates are null
			@Suppress("UNCHECKED_CAST")
			return actualStops as Array<Stop>
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

			// Iterate though each potential stop in the stop array.
			for (stopArrayItem: Stop? in stopArray) {

				// If the array item is null just return false.
				if (stopArrayItem == null) {
					return false
				}

				// Check if the following match.
				val routeNameMatch = stop.route.name == stopArrayItem.route.name
				val colorMatch = stop.route.color == stopArrayItem.route.color

				// If all of the following match, return true.
				if (routeNameMatch && colorMatch && stop == stopArrayItem) {
					return true
				}
			}

			// Since nothing matched, return false.
			return false
		}
	}

	override fun close() {
		if (circle != null) {
			circle!!.remove()
			circle = null
		}
	}
}