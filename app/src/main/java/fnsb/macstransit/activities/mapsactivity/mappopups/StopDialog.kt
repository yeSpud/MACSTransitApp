package fnsb.macstransit.activities.mapsactivity.mappopups

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
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

class StopDialog: LinearLayout {

	/**
	 * The RouteMatch object used to make calls to the RouteMatch server in order to update the bus positions,
	 * and to determine the arrival and departure times for stops.
	 */
	private val routeMatch: RouteMatch = RouteMatch(context.getString(R.string.routematch_url), context)

	constructor(stopName: String, stopRoutes: Array<Route>, context: Context): this(stopName, stopRoutes,
	                                                                                context, null)
	constructor(stopName: String, stopRoutes: Array<Route>, context: Context, attributeSet: AttributeSet?):
			this(stopName, stopRoutes, context, attributeSet, 0)
	constructor(stopName: String, stopRoutes: Array<Route>, context: Context, attributeSet: AttributeSet?,
	            defStyleAttr: Int) : super(context, attributeSet, defStyleAttr) {
		inflate(context, R.layout.stop_dialog, this)

		val stopNameText: TextView = findViewById(R.id.stop_name)
		val timesContainer: LinearLayout = findViewById(R.id.times_container)
		val progressBar: ProgressBar = findViewById(R.id.progress)

		stopNameText.text = stopName

		routeMatch.callDeparturesByStop(stopName, { json: JSONObject ->

			// Get the stop data from the retrieved json.
			val stopData = RouteMatch.parseData(json)
			generateStopEntries(stopData, stopRoutes, timesContainer)
			timesContainer.visibility = View.VISIBLE
			progressBar.visibility = View.GONE

		}, { error: VolleyError? -> Log.e("showMarker", "Unable to get departure times", error) },
		                                this)
	}

	private fun generateStopEntries(stopArray: org.json.JSONArray, activeRoutes: Array<Route>,
	                                view: LinearLayout) {
		val tag = "generateStopEntries"

		// Get the number of entries in our json array.
		val count = stopArray.length()

		// Iterate though each entry in our json array.
		for (index in 0 until count) {
			Log.d(tag, "Parsing stop times for stop $index/$count")

			// Get the json object from the json array.
			val jsonObject: JSONObject = try {
				stopArray.getJSONObject(index)
			} catch (e: JSONException) {
				Log.e(tag, "Could not get json object from json array", e)
				continue
			}

			// Get the route name from the json object.
			// This is not to be confused with the route name from the route.
			val routeId: String = try {
				jsonObject.getString("routeId")
			} catch (e: JSONException) {
				Log.e(tag, "Could not get route name from json array", e)
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
						Log.d(tag, "Converting time to 12 hour time")
						arrivalTime = formatTime(arrivalTime)
						departureTime = formatTime(departureTime)
					}

					val stopEntry = StopEntry(context)
					val params = view.layoutParams
					params.height = ViewGroup.LayoutParams.WRAP_CONTENT
					params.width = ViewGroup.LayoutParams.MATCH_PARENT
					stopEntry.layoutParams = params

					stopEntry.routeName.text = routeId
					stopEntry.routeName.backgroundTintList = ColorStateList.valueOf(activeRoute.color)
					stopEntry.stopTime.text = departureTime

					Log.v(tag, "Adding ${stopEntry.routeName.text} ${stopEntry.stopTime.text} to dialog")
					view.addView(stopEntry)
				}
			}
		}
	}
}