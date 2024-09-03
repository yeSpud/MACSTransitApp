package fnsb.macstransit.activities.mapsactivity.mappopups

import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.ProgressBar
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
		val progressBar: ProgressBar = view.findViewById(R.id.progress)

		stopNameText.text = stopName

		routeMatch.callDeparturesByStop(stopName, { json: JSONObject ->

			// Get the stop data from the retrieved json.
			val stopData = RouteMatch.parseData(json)
			generateStopEntries(stopData, stopRoutes, timesContainer)
			timesContainer.visibility = View.VISIBLE
			progressBar.visibility = View.GONE

		}, { error: VolleyError? -> Log.e("showMarker", "Unable to get departure times", error) },
		                                this)

		return view
	}

	private fun generateStopEntries(stopArray: org.json.JSONArray, activeRoutes: Array<Route>,
	                                view: LinearLayout) {

		// Get the number of entries in our json array.
		val count = stopArray.length()

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

					val stopEntry = StopEntry(context)
					stopEntry.routeName?.text = routeId
					stopEntry.routeName?.backgroundTintList = ColorStateList.valueOf(activeRoute.color)
					stopEntry.stopTime?.text = departureTime

					view.addView(stopEntry)
				}
			}
		}
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