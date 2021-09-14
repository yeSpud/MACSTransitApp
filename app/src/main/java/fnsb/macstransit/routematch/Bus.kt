package fnsb.macstransit.routematch

import android.util.Log
import androidx.annotation.UiThread
import com.google.android.gms.maps.model.LatLng
import org.json.JSONException
import kotlin.RuntimeException

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 3.1.
 * @since Beta 3.
 */
class Bus(

		/**
		 * The ID (name) of the bus.
		 * This must not have any prefix (as that will be added later).
		 */
		name: String,

		/**
		 * The current location of the bus (as a LatLng object)
		 */
		location: LatLng,

		/**
		 * The bus's route.
		 */
		val route: Route,

		/**
		 * String used to store the buses current heading.
		 * This is optional, and may just be an empty string as a result.
		 */
		var heading: String = "",

		/**
		 * Variables to store the current bus speed in mph.
		 */
		var speed: Int = 0) : MarkedObject("Bus $name", location, route.name, route.color) {

	/**
	 * Searches a given bus array for this bus, and returns if it was not found.
	 *
	 * @param buses The bus array to search for the given bus.
	 * @return Whether the bus was NOT found.
	 */
	fun isBusNotInArray(buses: Array<Bus>): Boolean {

		// Iterate though each bus.
		buses.forEach {

			// Check of the bus we are searching for matches our current bus.
			Log.v("isBusNotInArray", "Comparing ${this.name} to ${it.name}")
			if (this.name == it.name) {
				Log.d("isBusNotInArray", "Vehicle IDs match")

				// Check if the routes for the bus also match. If they do, return false (found).
				if (this.route.name == it.route.name) {
					Log.d("isBusNotInArray", "Objects match!")
					return false
				}
			}
		}

		// Since the bus was not found in our array, return true.
		Log.d("isBusNotInArray", "No objects match")
		return true
	}

	companion object {

		/**
		 * Creates an array of bus objects using the information in the provided json array.
		 * The size of the return array is the size of the json array.
		 *
		 * @param vehiclesJson The json array containing the bus information.
		 * @param routes
		 *
		 * @return An array of buses created from the information in the json array.
		 * @throws RuntimeException Thrown if the bus route is not in our trackable routes.
		 * @throws JSONException Thrown if there is an error parsing the JSON values.
		 */
		@JvmStatic
		@Throws(RuntimeException::class, JSONException::class)
		fun getBuses(vehiclesJson: org.json.JSONArray, routes: HashMap<String, Route>): Array<Bus> {

			// Return the bus array from the following:
			return Array(vehiclesJson.length()) {

				// Get the json object corresponding to the bus.
				val busObject: org.json.JSONObject = vehiclesJson.getJSONObject(it)

				// Get the bus ID.
				val name: String = busObject.getString("vehicleId")

				// Get the location of the bus as a LatLng object.
				val location = LatLng(busObject.getDouble("latitude"), busObject.getDouble("longitude"))

				// Since we have the route name we need to now find the actual route that belongs to it.
				// First make sure all the routes have been loaded before continuing.
				if (routes.isEmpty()) {
					throw RuntimeException("There are no loaded routes!")
				}

				// Try to get the bus's route via the route name.
				val route: Route = try {
					routes[busObject.getString("masterRouteId")]!!
				} catch (NullPointerException: NullPointerException) {

					// If the bus route was not found in all of our trackable routes throw a RuntimeException.
					throw RuntimeException("Bus route not found in route map!")
				}

				// Try to get the heading of the bus. This value isn't necessary, but is nice to have.
				val heading: String = busObject.optString("headingName", "")

				// Try to get the current speed of the bus. This value isn't necessary, but is nice to have.
				val speed: Int = busObject.optInt("speed", 0)

				// Create a new bus object using the content in the json object.
				Bus(name, location, route, heading = heading, speed = speed)
			}
		}

		/**
		 * Compares two bus arrays and removes any buses that were in the old bus array that are not in the new bus array.
		 *
		 * @param oldBuses The original buses.
		 * @param newBuses The new buses retrieved from the server.
		 */
		@JvmStatic
		@UiThread
		fun removeOldBuses(oldBuses: Array<Bus>, newBuses: Array<Bus>) {

			// Iterate through the oldBuses
			oldBuses.forEach {

				// Check if the new buses match the old bus.
				// If it doesn't, then remove it from the map.
				if (it.isBusNotInArray(newBuses)) {
					Log.d("removeOldBuses", "Removing bus ${it.name} from map")
					it.removeMarker()
				}
			}
		}

		/**
		 * Compares two bus arrays and updates the positions, heading,
		 * and speed of the buses with matching IDs.
		 *
		 * @param oldBuses The original buses.
		 * @param newBuses The new buses retrieved from the server.
		 * @return An array of buses which have been updated.
		 */
		@JvmStatic
		@UiThread
		fun updateCurrentBuses(oldBuses: Array<Bus>, newBuses: Array<Bus>): Array<Bus> {

			// Create an array with the maximum size of the size of our new buses.
			// We will resize the array later,
			// this is just to make sure we can fit at most all the new buses into the potential array.
			val potentialBuses = arrayOfNulls<Bus>(newBuses.size)
			var busSize = 0

			// Iterate through the new buses.
			for (newBus in newBuses) {

				// Compare the new bus to the oldBuses.
				// If they match, then add it to the potential bus array and update its position.
				for (i in oldBuses.indices) {
					if (newBus.name == oldBuses[i].name) {

						// Update the buses position, heading, and speed.
						oldBuses[i].updateLocation(newBus.location)
						oldBuses[i].heading = newBus.heading
						oldBuses[i].speed = newBus.speed
						try {
							// Add the bus to the potential bus array.
							potentialBuses[busSize] = oldBuses[i]
							busSize++
						} catch (e: ArrayIndexOutOfBoundsException) {

							// If there was an ArrayIndexOutOfBoundsException log it and break out of the for loop because of suspicious behavior.
							Log.w("updateCurrentBuses", "Potential bus array out of bounds... how?")
							break
						}
					}
				}
			}

			// Down size the array to its actual size and return it.
			val buses = arrayOfNulls<Bus>(busSize)
			System.arraycopy(potentialBuses, 0, buses, 0, busSize)
			return buses as Array<Bus>
		}

		/**
		 * Compares two bus arrays and determines which buses are new.
		 *
		 * @param oldBuses The original buses.
		 * @param newBuses The new buses retrieved from the server.
		 * @return An array of all the new buses.
		 */
		@JvmStatic
		@UiThread
		fun addNewBuses(oldBuses: Array<Bus>, newBuses: Array<Bus>,
		                map: com.google.android.gms.maps.GoogleMap): Array<Bus> {

			// Create an array with the maximum size of the size of our new buses.
			// We will resize the array later,
			// this is just to make sure we can fit at most all the new buses into the potential array.
			val potentialBuses = arrayOfNulls<Bus>(newBuses.size)
			var busSize = 0

			// Iterate through the new buses.
			for (newBus in newBuses) {

				// Compare the new bus to the oldBuses.
				// If they don't match, then it has not been added to the map yet,
				// so add it to the array and map.
				if (newBus.isBusNotInArray(oldBuses)) {
					Log.d("addNewBuses", "Adding new bus to map: ${newBus.name}")

					// Create the bus marker.
					newBus.addMarker(map)
					if (newBus.marker != null) {

						// Determine whether or not to show the bus marker.
						newBus.marker!!.isVisible = newBus.route.enabled
					} else {
						Log.w("addNewBus", "Unable to add bus marker!")
					}

					// Add the bus to the bus array.
					potentialBuses[busSize] = newBus
					busSize++
				}
			}

			// Down size the array to its actual size and return it.
			val buses = arrayOfNulls<Bus>(busSize)
			System.arraycopy(potentialBuses, 0, buses, 0, busSize)
			return buses as Array<Bus>
		}
	}
}