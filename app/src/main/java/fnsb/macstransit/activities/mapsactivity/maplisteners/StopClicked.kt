package fnsb.macstransit.activities.mapsactivity.maplisteners

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.UiThread
import com.google.android.gms.maps.GoogleMap
import fnsb.macstransit.R
import fnsb.macstransit.activities.mapsactivity.MapsActivity
import fnsb.macstransit.routematch.MarkedObject
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.routematch.SharedStop
import fnsb.macstransit.routematch.Stop
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Spud on 2019-10-30 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Beta 7.
 */
class StopClicked(private val activity: MapsActivity, private val map: GoogleMap) : GoogleMap.OnCircleClickListener {

	/**
	 * Called when a circle is clicked.
	 * This is called on the Android UI thread.
	 *
	 * @param circle The circle that is clicked.
	 */
	@UiThread
	override fun onCircleClick(circle: com.google.android.gms.maps.model.Circle) {

		// Get the marked object from our circle.
		val markedObject = circle.tag as MarkedObject

		// If the marker for our marked object is null, create a new marker.
		if (markedObject.marker == null) {

			// Get the location and color of the object
			// (this is different depending on whether or not its a shared stop or a regular stop).
			val color: Int
			when (markedObject) {
				is SharedStop -> {

					// Get color of the largest circle of our shared stop.
					color = markedObject.routes[0].color
				}
				is Stop -> {

					// Get color of our stop.
					color = markedObject.route.color
				}
				else -> {

					// Since our marked object was neither a shared stop nor a regular stop log it as a warning,
					// and return early.
					Log.w("onCircleClick", "Object unaccounted for: ${markedObject.javaClass}")
					return
				}
			}

			// Create a new marker for our marked object using the newly determined location and color.
			markedObject.addMarker(this.map, color)
		}

		// Comments
		if (markedObject.marker != null) {

			// Show our marker.
			showMarker(markedObject.marker!!)
		} else {

			// Comments
			Toast.makeText(this.activity, markedObject.name, Toast.LENGTH_LONG).show()
		}
	}

	/**
	 * Shows the given marker on the map by setting it to visible.
	 * The title of the marker is set to the name of the stop.
	 * The marker snippet is set to a pending message as a callback method retrieves the stop times for the stop.
	 *
	 * @param marker The marker to be shown. This cannot be null.
	 */
	@UiThread
	private fun showMarker(marker: com.google.android.gms.maps.model.Marker) {

		// Since the marker is not null, show it the marker by setting it to visible.
		marker.isVisible = true

		// Get the name of the stop.
		val name = marker.title ?: return

		// For now just set the snippet text to "retrieving stop times" as a callback method gets the times.
		marker.snippet = this.activity.getString(R.string.retrieving_stop_times)

		// Retrieve the stop times.
		this.activity.viewModel.routeMatch.callDeparturesByStop(name, { result: JSONObject ->

			// Update the snippet text of the marker's info window.
			Log.v("showMarker", "Updating snippet")
			marker.snippet = postStopTimes(marker.tag as MarkedObject, result, this.activity)

			// Refresh the info window by calling showInfoWindow().
			Log.v("showMarker", "Refreshing info window")
			marker.showInfoWindow()
		}, { error: com.android.volley.VolleyError? -> Log.e("showMarker",
		                                                     "Unable to get departure times",
		                                                     error) }, marker)

		// For now though just show the info window.
		marker.showInfoWindow()
	}

	companion object {

		/**
		 * Posts the time string for the selected stop,
		 * which contains when the (selected) buses for that stop will be arriving and departing.
		 * This method also posts this string to the body of the popup window when the info window is clicked on.
		 *
		 * @param stop    The stop (either an actual Stop object, or a SharedStop).
		 * @param json    The json object retrieved from the RouteMatch server.
		 * @param context The context from which this method is being called from (for string lookup).
		 * @return The string containing either all the arrival and departure times for the stop,
		 * or the overflowString if there is too much data.
		 */
		fun postStopTimes(stop: MarkedObject, json: JSONObject, context: Context): String {

			// Get the stop data from the retrieved json.
			val stopData = fnsb.macstransit.routematch.RouteMatch.parseData(json)

			// Get the times for the stop.
			// Since the method arguments are slightly different for a shared stop compared to a regular stop,
			// check if the marker is an instance of a Stop or SharedStop.
			var string = ""

			// Check if our marked object is a shared stop (for future formatting reasons).
			val isSharedStop = stop is SharedStop

			// Try setting the routes array to either enabled routes (shared stop) or our single route (stop).
			val routes: Array<Route> = try {
				if (isSharedStop){
					getEnabledRoutesForStop((stop as SharedStop).routes)
				} else {
					arrayOf((stop as Stop).route)
				}
			} catch (e: ClassCastException) {

				// If there was an issue casting from classes log the error and return the current content of the string.
				Log.e("postStopTimes", "Unaccounted object class: ${stop.javaClass}", e)
				return string
			} catch (nullPointerException: NullPointerException) {

				// Log the null exception, and return the current string content.
				Log.e("postStopTimes", "Null pointer exception thrown!", nullPointerException)
				return string
			}

			// Try to get the formatted time string for the marked object.
			string = try {
				generateTimeString(stopData, context, routes, isSharedStop)
			} catch (e: JSONException) {

				// If there was an exception thrown while parsing the json simply log it and return the current content of the string.
				Log.e("postStopTimes", "Could not get stop time from json", e)
				return string
			}

			// Load the times string into a popup window for when its clicked on.
			fnsb.macstransit.activities.PopupWindow.body = string

			// Check to see how many new lines there are in the display.
			// If there are more than the maximum lines allowed bu the info window adapter,
			// display "Click to view all the arrival and departure times.".
			return if (getNewlineOccurrence(string) <= fnsb.macstransit.activities.InfoWindowAdapter.MAX_LINES) {
				string
			} else {
				context.getString(
						R.string.click_to_view_all_the_arrival_and_departure_times)
			}
		}

		/**
		 * Returns an array of routes that are enabled from all the routes in the shared stop.
		 *
		 * @param allRoutesForStop The routes in the shared stop.
		 * @return The routes in the shared stop that are enabled.
		 */
		private fun getEnabledRoutesForStop(allRoutesForStop: Array<Route>): Array<Route> {

			// Create a new routes array to store routes that have been verified to be enabled.
			val potentialRoutes = arrayOfNulls<Route>(allRoutesForStop.size)
			var routeCount = 0

			// Iterate though all the routes in our shared stop.
			for (route in allRoutesForStop) {

				// If the route is enabled add it to our verified routes array,
				// and increase the verified count.
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
			return selectedRoutes.requireNoNulls()
		}

		/**
		 * Generates the large string that is used to display the departure and arrival times of a
		 * particular stop when clicked on.
		 *
		 * @param stopArray        The JSONArray that contains all the stops for the route.
		 * @param context          The context from which this method is being called (used for string lookup).
		 * @param routes           The active (enabled) routes to get the times for.
		 * @param includeRouteName Whether or not to include the route name in the final string.
		 * @return The string containing all the departure and arrival times for the particular stop.
		 * @throws JSONException Thrown if there is a JSONException while parsing the data for the stop.
		 */
		@Throws(JSONException::class)
		private fun generateTimeString(stopArray: org.json.JSONArray, context: Context, routes: Array<Route>, includeRouteName: Boolean): String {

			// Get the number of entries in our json array.
			val count = stopArray.length()

			// Create a new string with the size of our capacity times 5 (0:00\n).
			val snippetText = StringBuilder(count * 5)

			// Iterate though each entry in our json array.
			for (index in 0 until count) {
				Log.d("generateTimeString", "Parsing stop times for stop $index/$count")

				// Get the stop time from the current stop.
				val jsonObject = stopArray.getJSONObject(index)

				// Iterate though each of our active routes. If the route is one that is listed,
				// append the time to the string builder.
				for (route in routes) {
					val routeName = route.routeName
					if (routeName == jsonObject.getString("routeId")) {

						// Set the arrival and departure time to the arrival and departure time in the JSONObject.
						// At this point this is stored in 24-hour time.
						var arrivalTime = getTime(jsonObject, "predictedArrivalTime")
						var departureTime = getTime(jsonObject, "predictedDepartureTime")

						// If the user doesn't use 24-hour time, convert to 12-hour time.
						if (!android.text.format.DateFormat.is24HourFormat(context)) {
							Log.d("generateTimeString", "Converting time to 12 hour time")
							arrivalTime = formatTime(arrivalTime)
							departureTime = formatTime(departureTime)
						}

						// Append the route name if there is one.
						if (includeRouteName) {
							Log.d("generateTimeString", "Adding route: $routeName")
							snippetText.append("Route: $routeName\n")
						}

						// Append the arrival and departure times to the snippet text.
						snippetText.append("${context.getString(R.string.expected_arrival)} $arrivalTime\n" +
						                   "${context.getString(R.string.expected_departure)} $departureTime\n\n")
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

								// Because the string was probably "Null", return null.
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
				} catch (exception: NullPointerException) {
					// TODO Log the exception
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
			val fullTimeDate: Date = try {

				// Try to get the 24 hour time as a date.
				fullTime.parse(time)!!
			} catch (e: java.text.ParseException) {

				// If there was a parsing exception simply return the old time.
				Log.e("formatTime", "Could not parse full 24 hour time", e)
				return time
			} catch (npe: NullPointerException) {

				// Because time was null return an empty string.
				// We cant return the argument because then we would be returning null.
				Log.e("formatTime", "Provided time was null!", npe)
				return ""
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
			for (element in string) {

				// If the character at the current index matches our character, increase the count.
				if (element == '\n') {
					count++
				}
			}

			// Finally, return the count.
			return count
		}
	}
}