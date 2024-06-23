package fnsb.macstransit.activities.mapsactivity.mappopups

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.android.volley.VolleyError
import fnsb.macstransit.R
import fnsb.macstransit.activities.mapsactivity.MapsViewModel.Companion.formatTime
import fnsb.macstransit.activities.mapsactivity.MapsViewModel.Companion.getTime
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.routematch.RouteMatch
import org.json.JSONException
import org.json.JSONObject

class StopDialog(private val context: Context, private val stopName: String,
                 private val stopRoutes: Array<Route>): BaseAdapter() {

	/**
	 * The RouteMatch object used to make calls to the RouteMatch server in order to update the bus positions,
	 * and to determine the arrival and departure times for stops.
	 */
	private val routeMatch: RouteMatch = RouteMatch(context.getString(R.string.routematch_url), context)

	override fun getCount(): Int {
		return 1
	}

	override fun getItem(position: Int): Any {
		return position // Todo properly implement me?
	}

	override fun getItemId(position: Int): Long {
		return position.toLong() // Todo properly implement me?
	}

	override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

		val layoutInflater: LayoutInflater = LayoutInflater.from(context)
		val view = convertView ?: layoutInflater.inflate(R.layout.stop_dialog, parent, false)

		val stopNameText: TextView = view.findViewById(R.id.stop_name)
		val timesContainer: LinearLayout = view.findViewById(R.id.times_container)

		stopNameText.text = stopName

		routeMatch.callDeparturesByStop(stopName, { json: JSONObject ->

			// Get the stop data from the retrieved json.
			val stopData = RouteMatch.parseData(json)

			// Get the formatted time string for the marked object, and load it into the popup window.
			PopupWindow.body = generateTimeString(stopData, stopRoutes)

			// Check to see how many new lines there are in the display.
			// If there are more than the maximum lines allowed bu the info window adapter,
			// display "Click to view all the arrival and departure times.".
			/*
			selectedStop!!.snippet = if (getNewlineOccurrence(PopupWindow.body) <= InfoWindowPopup.MAX_LINES) {
				PopupWindow.body
			} else {
				context.getString(R.string.click_to_view_all_the_arrival_and_departure_times)
			}
			 */

		}, { error: VolleyError? -> Log.e("showMarker", "Unable to get departure times", error) },
		                                this)

		return view
	}

	/**
	 * Generates the large string that is used to display the departure and arrival times of a
	 * particular stop when clicked on.
	 *
	 * @param stopArray        The JSONArray that contains all the stops for the route.
	 * @param activeRoutes           The active (enabled) routes to get the times for.
	 * @return The string containing all the departure and arrival times for the particular stop.
	 */
	private fun generateTimeString(stopArray: org.json.JSONArray, activeRoutes: Array<Route>): String  {

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
			for (activeRoute in activeRoutes) {
				if (activeRoute.name == routeId) {

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
					snippetText.append("Route: ${activeRoute.name}\n")

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

	companion object {

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