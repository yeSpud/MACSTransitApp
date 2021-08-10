package fnsb.macstransit.Activities.splashscreenrunnables

import android.util.Log
import android.util.Pair
import fnsb.macstransit.Activities.MapsActivity
import fnsb.macstransit.Activities.SplashActivity
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.routematch.Stop
import org.json.JSONObject
import java.util.*

/**
 * Created by Spud on 8/6/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
class MapBusStops {

	/**
	 * Documentation
	 */
	private val pairs: MutableCollection<Pair<Route, SplashListener>> = ArrayList() // TODO Don't use arrayList

	/**
	 * Documentation
	 * @param pair TODO
	 */
	fun addListener(pair: Pair<Route, SplashListener>) {
		pairs.add(pair)
	}

	/**
	 * Documentation
	 * Comments
	 * @param activity TODO
	 */
	fun getBusStops(activity: SplashActivity) {

		// Verify that allRoutes is not null. If it is then log and return early.
		if (MapsActivity.allRoutes == null) {
			Log.w("getBusRoutes", "All routes is null!")
			return
		}

		val step: Double = SplashActivity.DOWNLOAD_BUS_STOPS.toDouble() / MapsActivity.allRoutes!!.size
		var progress: Double = (SplashActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS +
				SplashActivity.PARSE_MASTER_SCHEDULE + SplashActivity.DOWNLOAD_BUS_ROUTES +
				SplashActivity.LOAD_BUS_ROUTES).toDouble()
		Log.d("getBusStops", "Step value: $step")

		for (pair: Pair<Route, SplashListener> in pairs) {
			val callback = BusStopCallback(pair.second, pair.first, activity)
			MapsActivity.routeMatch.callAllStops(pair.first, callback, { error: com.android.volley.VolleyError ->
				Log.w("loadStops", "Unable to get stops from RouteMatch server", error)
			})
			progress += step
			activity.setProgressBar(progress)
		}
		activity.setProgressBar(progress)
	}

	internal inner class BusStopCallback(private val listener: SplashListener, private val route: Route,
		private val activity: SplashActivity) : com.android.volley.Response.Listener<JSONObject> {

		override fun onResponse(response: JSONObject) {

			// Display that we are mapping bus stops to the user.
			this.activity.setMessage(fnsb.macstransit.R.string.mapping_bus_stops)

			// Get the data from all the stops and store it in a JSONArray.
			val data: org.json.JSONArray = fnsb.macstransit.routematch.RouteMatch.parseData(response)

			// Load in all the potential stops for the route.
			// The reason why this is considered potential stops is because at this stage duplicate
			// stops have not yet been handled.
			val potentialStops: Array<Stop> = Stop.generateStops(data, route)

			// Create a variable to store the true size of the stops that have been validated.
			var validatedSize = 0

			// Create an array to store the validated stops.
			// While we don't know the specific size of this array until done, we do know the maximum size,
			// so use that for setting the array size.
			val validatedStops = arrayOfNulls<Stop>(potentialStops.size)

			// Iterate through each stop in our array of potential stops.
			for (stop: Stop? in potentialStops) {

				if (stop == null) {
					continue
				}

				// Check to see if the stop is in our array of validated stops. If its not,
				// add it to the array and add 1 to the true index size of stops that have been validated.
				if (!Stop.isDuplicate(stop, validatedStops)) {
					validatedStops[validatedSize] = stop
					validatedSize++
				}
			}

			// Create an array for our actual stops.
			// Since we now know the number of validated stops we can use that as its size.
			val actualStops = arrayOfNulls<Stop>(validatedSize)

			// Copy our validated stops into our smaller actual stops array, and return it.
			System.arraycopy(validatedStops, 0, actualStops, 0, actualStops.size)

			// At this point duplicate stops have now been handled and removed.
			this.route.stops = actualStops.requireNoNulls()
			this.listener.splashRunnableFinished()
		}
	}
}