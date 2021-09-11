package fnsb.macstransit.activities.mapsactivity

import android.util.Log
import android.widget.Toast
import com.google.android.gms.maps.GoogleMap
import fnsb.macstransit.R
import fnsb.macstransit.activities.mapsactivity.mappopups.PopupWindow
import fnsb.macstransit.routematch.MarkedObject
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.routematch.RouteMatch
import fnsb.macstransit.routematch.SharedStop
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Created by Spud on 2019-10-30 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.1.
 * @since Beta 7.
 */
class StopClicked(private val context: android.content.Context, private val routematch: RouteMatch,
                  private val map: GoogleMap, private val routes: HashMap<String, Route>) :
		GoogleMap.OnCircleClickListener {

	@androidx.annotation.UiThread
	override fun onCircleClick(circle: com.google.android.gms.maps.model.Circle) {

		// Get the marked object from our circle.
		val markedObject = circle.tag as MarkedObject

		// If the marker for our marked object is null, create a new marker.
		if (markedObject.marker == null) {

			// Create a new marker for our marked object using the newly determined location and color.
			markedObject.addMarker(this.map)

			// Check if the marker was added successfully.
			if (markedObject.marker == null) {
				Log.w("onCircleClick", "Unable to add marker to map!")

				// Since the marker was unable to be added to the map just default to toast.
				Toast.makeText(this.context, markedObject.name, Toast.LENGTH_LONG).show()
				return
			}
		}

		// Since the marker is not null, show it the marker by setting it to visible.
		markedObject.marker!!.isVisible = true

		// Get the name of the stop.
		val name = markedObject.marker!!.title ?: return

		// Set the snippet text to "retrieving stop times".
		// Once the stop times for this stop are retrieved and parsed
		// the callback function will set the snippet text to the actual times.
		markedObject.marker!!.snippet = this.context.getString(R.string.retrieving_stop_times)

		// Start the callback to retrieve the actual stop times.
		this.routematch.callDeparturesByStop(name, {

			// Update the snippet text of the marker's info window.
			markedObject.marker!!.snippet = postStopTimes(markedObject.marker!!.tag as MarkedObject, it)

			// Refresh the info window by calling showInfoWindow().
			Log.v("showMarker", "Refreshing info window")
			markedObject.marker!!.showInfoWindow()
		}, { error: com.android.volley.VolleyError? ->

			// Log that we are unable to get the departure times, and provide the error.
			Log.e("showMarker", "Unable to get departure times", error)

			// Be sure to update the stop snippet to let the user know there was an error.
			markedObject.marker!!.snippet = this.context.getString(R.string.stop_times_retrieval_error, markedObject.name)
			markedObject.marker!!.showInfoWindow()
		}, markedObject.marker!!)

		// Sow the stop info window.
		markedObject.marker!!.showInfoWindow()
	}

	/**
	 * Posts the time string for the selected stop,
	 * which contains when the (selected) buses for that stop will be arriving and departing.
	 * This method also posts this string to the body of the popup window when the info window is clicked on.
	 *
	 * @param stop    The stop (either an actual Stop object, or a SharedStop).
	 * @param json    The json object retrieved from the RouteMatch server.
	 * @return The string containing either all the arrival and departure times for the stop,
	 *         or the overflow string if there is too much data.
	 */
	private fun postStopTimes(stop: MarkedObject, json: JSONObject): String {

		// Get the stop data from the retrieved json.
		val stopData = RouteMatch.parseData(json)

		// Check if our marked object is a shared stop (for future formatting reasons).
		val isSharedStop = stop is SharedStop

		// Try setting the routes array to either enabled routes (shared stop) or our single route (stop).
		val routes: Array<Route> = try {
			if (isSharedStop){
				this.getEnabledRoutesForStop(stop as SharedStop)
			} else {
				arrayOf(this.routes[stop.routeName]!!)
			}
		} catch (e: ClassCastException) {

			// If there was an issue casting from classes log the error and return the current content of the string.
			Log.e("postStopTimes", "Unaccounted object class: ${stop.javaClass}", e)
			return ""
		} catch (NullPointerException : NullPointerException) {

			// Since the route was not found in the hashmap log it as an error,
				// and return an empty string.
			Log.e("postStopTimes", "Could not find stop route!", NullPointerException)
			return ""
		}

		// Get the formatted time string for the marked object, and load it into the popup window.
		PopupWindow.body = this.generateTimeString(stopData, routes, isSharedStop)

		// Check to see how many new lines there are in the display.
		// If there are more than the maximum lines allowed bu the info window adapter,
		// display "Click to view all the arrival and departure times.".
		return if (getNewlineOccurrence(PopupWindow.body) <= fnsb.macstransit.activities.mapsactivity
					.mappopups.InfoWindowPopup.MAX_LINES) {
			PopupWindow.body
		} else {
			this.context.getString(R.string.click_to_view_all_the_arrival_and_departure_times)
		}
	}

	/**
	 * Generates the large string that is used to display the departure and arrival times of a
	 * particular stop when clicked on.
	 *
	 * @param stopArray        The JSONArray that contains all the stops for the route.
	 * @param routes           The active (enabled) routes to get the times for.
	 * @param includeRouteName Whether or not to include the route name in the final string.
	 * @return The string containing all the departure and arrival times for the particular stop.
	 */
	private fun generateTimeString(stopArray: org.json.JSONArray, routes: Array<Route>,
	                               includeRouteName: Boolean): String {

		// Get the number of entries in our json array.
		val count = stopArray.length()

		// Create a new string with the size of our capacity times 5 (0:00\n).
		val snippetText = StringBuilder(count * 5)

		// Iterate though each entry in our json array.
		for (index in 0 until count) {
			Log.d("generateTimeString", "Parsing stop times for stop $index/$count")

			// Get the json object from the json array.
			val jsonObject: JSONObject = try {
				stopArray.getJSONObject(index)
			} catch (e: JSONException) {
				Log.e("generateTimeString", "Could not get json object from json array", e)
				continue
			}

			// Get the route name from the json object.
			// This is not to be confused with the route name from the route.
			val routeId: String = try {
				jsonObject.getString("routeId")
			} catch (e: JSONException) {
				Log.e("generateTimeString", "Could not get route name from json array", e)
				continue
			}

			// Iterate though each of our active routes. If the route is one that is listed,
			// append the time to the string builder.
			routes.forEach {

				if (it.name == routeId) {

					// Set the arrival and departure time to the arrival and departure time in the JSONObject.
					// At this point this is stored in 24-hour time.
					var arrivalTime = getTime(jsonObject, "predictedArrivalTime")
					var departureTime = getTime(jsonObject, "predictedDepartureTime")

					// If the user doesn't use 24-hour time, convert to 12-hour time.
					if (!android.text.format.DateFormat.is24HourFormat(this.context)) {
						Log.d("generateTimeString", "Converting time to 12 hour time")
						arrivalTime = formatTime(arrivalTime)
						departureTime = formatTime(departureTime)
					}

					// Append the route name if there is one.
					if (includeRouteName) {
						Log.d("generateTimeString", "Adding route: ${it.name}")
						snippetText.append("Route: ${it.name}\n")
					}

					// Append the arrival and departure times to the snippet text.
					snippetText.append("${this.context.getString(R.string.expected_arrival)} $arrivalTime\n" +
					                   "${this.context.getString(R.string.expected_departure)} $departureTime\n\n")
				}
			}
		}

		// Be sure to trim the snippet text at this point.
		snippetText.trimToSize()

		// Get the length of the original snippet text.
		val length = snippetText.length

		// Replace the last 2 new lines (this is to mitigate a side effect of the final append).
		if (length > 2) {
			snippetText.deleteCharAt(length - 1)
			snippetText.deleteCharAt(length - 2)
		}

		// Finally, build the text and return it.
		return snippetText.toString()
	}

	/**
	 * Returns an array of routes that are enabled from all the routes in the shared stop.
	 *
	 * @param sharedStop The shared stop to get the times for.
	 * @return The routes in the shared stop that are enabled.
	 */
	private fun getEnabledRoutesForStop(sharedStop: SharedStop): Array<Route> {

		// Create a new routes array to store routes that have been verified to be enabled.
		val potentialRoutes = arrayOfNulls<Route>(sharedStop.routeNames.size)
		var routeCount = 0

		// Iterate though all the shared stop names.
		for (routeName : String in sharedStop.routeNames) {

			// Try to get the route with the shared stop route name from the hashmap of routes.
			val route: Route = try {
				this.routes[routeName]!!
			} catch (NullPointerException: NullPointerException) {
				Log.e("getEnabledRoutes",
				      "Route for shared stop ${sharedStop.name} is invalid: $routeName}",
				      NullPointerException)
				continue
			}

			// Since the route was found only add it to the route count if the potentials if its enabled.
			if (route.enabled) {
				potentialRoutes[routeCount] = route
				routeCount++
			}
		}

		// Create a new routes array of selected routes that has the size of our verified count.
		val selectedRoutes = arrayOfNulls<Route>(routeCount)

		// Fill the selected routes array.
		System.arraycopy(potentialRoutes, 0, selectedRoutes, 0, routeCount)

		// Return our selected routes.
		return selectedRoutes as Array<Route>
	}

	companion object {

		/**
		 * Gets the the time (predicted arrival or predicted departure depending on the key)
		 * for the stop via its JSONObject.
		 *
		 * @param json The JSONObject containing the time for the stop.
		 * @param key  The specific key to search for within the JSONObject.
		 * @return The time found within the JSONObject.
		 */
		fun getTime(json: JSONObject, key: String): String {

			// Try to get the time string from the json object based on the key.
			val timeString: String = try {
						json.getString(key)
					} catch (e: JSONException) {

						// Try to manage the exception, as it may be thrown if the value is actually null.
						val message = e.message
						if (message != null) {
							if (message == "JSONObject[\"$key\"] is not a string.") {
								Log.w("getTime", "$key has the wrong type (not a string - probably null)")

								// Because the string was probably "Null", return an empty string.
								return ""
							}
						}

						// Log any errors and return empty if unsuccessful.
						Log.e("getTime", "Unable to get stop times.", e)
						return ""
					}

			// Get a matcher object from the time regex (example: 00:00), and have it match the key.
			val matcher = java.util.regex.Pattern.compile("\\d\\d:\\d\\d").matcher(timeString)

			// If the match was found, return it, if not return midnight.
			return if (matcher.find()) {
				try {
					matcher.group(0)!!
				} catch (NullPointerException: NullPointerException) {
					Log.e("getTime", "Regex returned null!", NullPointerException)
					""
				}
			} else {
				""
			}
		}

		/**
		 * Formats a given 24 hour time string into a 12 hour time string complete with AM/PM characters.
		 *
		 * @param time The time string (ie 13:15 for what should be 1:15 PM).
		 * @return The formatted time string with the AM/PM characters included.
		 */
		fun formatTime(time: String): String {

			// Create a date format for parsing 24 hour time.
			val fullTime = SimpleDateFormat("H:mm", Locale.US)

			// Create another date format for formatting 12 hour time.
			val halfTime = SimpleDateFormat("h:mm a", Locale.US)

			// Get the time in 24 hours.
			val fullTimeDate: java.util.Date = try {

				// Try to get the 24 hour time as a date.
				fullTime.parse(time)!!
			} catch (Exception: Exception) {

				// If there was an exception raised during parsing simply return the parameter.
				Log.e("formatTime", "Could not parse full 24 hour time", Exception)
				return time
			}

			// Format the 24 hour time date into 12 hour time and return it.
			val formattedTime = halfTime.format(fullTimeDate)
			Log.d("formatTime", "Formatted time: $formattedTime")
			return formattedTime
		}

		/**
		 * Function that finds the number of times a character occurs within a given string.
		 *
		 * @param string The string to search.
		 * @return The number of times the character occurs within the string.
		 */
		fun getNewlineOccurrence(string: CharSequence): Int {

			// Create a variable to store the occurrence.
			var count = 0

			// Iterate through the string.
			string.forEach {

				// If the character at the current index matches our character, increase the count.
				if (it == '\n') {
					count++
				}
			}

			// Finally, return the count.
			return count
		}
	}
}