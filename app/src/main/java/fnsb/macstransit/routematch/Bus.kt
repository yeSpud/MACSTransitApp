package fnsb.macstransit.routematch

import android.util.Log
import androidx.annotation.UiThread
import com.google.android.gms.maps.model.LatLng
import fnsb.macstransit.activities.mapsactivity.MapsActivity
import fnsb.macstransit.routematch.Route.RouteException
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 3.0.
 * @since Beta 3.
 */
class Bus : MarkedObject {

	/**
	 * Documentation
	 */
	val route: Route

	/**
	 * The current bus's color.
	 * This is more representative of the route its on (ie what is its route's color),
	 * and thus is optional.
	 *
	 * This is an int instead of a Color object because for whatever reason android stores its colors as ints.
	 */
	val color: Int

	/**
	 * String used to store the buses current heading.
	 * This is optional, and may just be an empty string as a result.
	 */
	var heading: String? = null

	/**
	 * Variables to store the current bus speed in mph.
	 */
	var speed = 0

	/**
	 * Documentation
	 * FIXME Change 2 double parameters to LatLng
	 */
	constructor(vehicleId: String, route: Route, latitude: Double, longitude: Double):
			super("Bus $vehicleId", LatLng(latitude, longitude)) {
		this.route = route
		this.color = route.color
	}

	/**
	 * Documentation
	 */
	@Throws(JSONException::class, RouteException::class)
	constructor(jsonObject: JSONObject) : super("Bus ${jsonObject.getString("vehicleId")}",
	                                            LatLng(jsonObject.getDouble("latitude"),
	                                                   jsonObject.getDouble("longitude"))) {

		// Ge the route name of the bus.
		// This will be used to determine the route object of the bus from our array of all routes.
		val routeName = jsonObject.getString("masterRouteId")

		// Since we have the route name we need to now find the actual route that belongs to it.
		// First make sure all the routes have been loaded before continuing.
		if (MapsActivity.allRoutes.isEmpty()) {
			throw RouteException("There are no loaded routes!")
		}

		// Now iterate through all the routes.
		var route: Route? = null
		for (r in MapsActivity.allRoutes) {

			// If the route name matches that of our bus route, then that's our route object.
			if (r.routeName == routeName) {
				route = r
				break
			}
		}

		// Comments
		if (route == null) {
			throw RouteException("Bus route not found in all routes")
		}

		// Try to get the heading of the bus. This value isn't necessary, but is nice to have.
		val heading = jsonObject.optString("headingName", "")

		// Try to get the current speed of the bus. This value isn't necessary, but is nice to have.
		val speed = jsonObject.optInt("speed", 0)

		// Create a new bus object using the determined information.
		this.route = route
		this.color = route.color
		this.heading = heading
		this.speed = speed
	}

	/**
	 * Searches a given bus array for this bus, and returns if it was not found.
	 *
	 * @param buses The bus array to search for the given bus.
	 * @return Whether the bus was NOT found.
	 */
	fun isBusNotInArray(buses: Array<Bus>): Boolean {

		// Iterate though each bus.
		for (bus: Bus in buses) {

			// Check of the bus we are searching for matches our current bus.
			Log.v("isBusNotInArray", "Comparing ${this.name} to ${bus.name}")
			if (this.name == bus.name) {
				Log.d("isBusNotInArray", "Vehicle IDs match")

				// Check if the routes for the bus also match. If they do, return false (found).
				if (this.route.routeName == bus.route.routeName) {
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
		 * @return An array of buses created from the information in the json array.
		 * @throws JSONException Thrown if there are no routes to track FIXME
		 */
		@JvmStatic
		@Throws(JSONException::class)
		fun getBuses(vehiclesJson: org.json.JSONArray): Array<Bus> {

			// Return the bus array from the following:
			return Array(vehiclesJson.length()) {

				// Get the json object corresponding to the bus.
				val busObject: JSONObject = vehiclesJson.getJSONObject(it)

				// Create a new bus object using the content in the json object.
				Bus(busObject)
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
		fun removeOldBuses(oldBuses: Array<Bus>, newBuses: Array<Bus>) { // FIXME Buses not being removed!

			// Iterate through the oldBuses
			for (oldBus in oldBuses) {

				// Check if the new buses match the old bus.
				// If it doesn't, then remove it from the map.
				if (oldBus.isBusNotInArray(newBuses)) {
					Log.d("removeOldBuses", "Removing bus ${oldBus.name} from map")
					oldBus.removeMarker()
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
				for (oldBus in oldBuses) {
					if (newBus.name == oldBus.name) {

						// Update the buses position, heading, and speed.
						oldBus.updateLocation(newBus.location)
						oldBus.heading = newBus.heading
						oldBus.speed = newBus.speed
						try {
							// Add the bus to the potential bus array.
							potentialBuses[busSize] = oldBus
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
			return buses.requireNoNulls()
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
					newBus.addMarker(map, newBus.color)
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
			return buses.requireNoNulls()
		}
	}
}