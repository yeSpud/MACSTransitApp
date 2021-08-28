package fnsb.macstransit.activities.splashactivity.splashscreenrunnables

import android.util.Log
import fnsb.macstransit.activities.mapsactivity.MapsActivity
import fnsb.macstransit.activities.splashactivity.SplashActivity
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.routematch.Stop
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * Created by Spud on 8/6/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
class DownloadBusStops(private val activity: SplashActivity) {

	/**
	 * Documentation
	 * Comments
	 * @param route
	 */
	suspend fun downloadBusStops(route: Route, index: Int): JSONObject =
			kotlin.coroutines.suspendCoroutine{ continuation ->

				val step: Double =
						SplashActivity.DOWNLOAD_BUS_STOPS.toDouble() / MapsActivity.allRoutes.size
				val progress: Double =
						(SplashActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS + SplashActivity.PARSE_MASTER_SCHEDULE
						 + SplashActivity.DOWNLOAD_BUS_ROUTES + SplashActivity.LOAD_BUS_ROUTES).toDouble()
				Log.d("getBusStops", "Step value: $step")


				this.activity.routeMatch.callAllStops(route, BusStopCallback(continuation, route), {
					error: com.android.volley.VolleyError ->
					Log.w("loadStops", "Unable to get stops from RouteMatch server", error)
				})
				this.activity.viewModel.setProgressBar(progress + step + index)
			}

	internal inner class BusStopCallback(private val continuation: kotlin.coroutines.Continuation<JSONObject>,
	                                     private val route: Route) :
			com.android.volley.Response.Listener<JSONObject> {

		override fun onResponse(response: JSONObject) {

			// Display that we are mapping bus stops to the user.
			this@DownloadBusStops.activity.viewModel.setMessage(fnsb.macstransit.R.string.mapping_bus_stops)

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
			for (stop: Stop in potentialStops) {

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

			// Comments
			continuation.resume(response)
		}
	}
}