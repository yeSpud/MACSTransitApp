package fnsb.macstransit.activities.splashactivity.splashscreenrunnables

import android.util.Log
import fnsb.macstransit.R
import fnsb.macstransit.activities.splashactivity.SplashActivity
import fnsb.macstransit.routematch.Route
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * Created by Spud on 8/16/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
class DownloadMasterSchedule(private val splashActivity: SplashActivity) {

	/**
	 * The method that will attempt to start the download of the master schedule.
	 * If a volley exception is thrown then then it is assumed that the RouteMatch server has timed out,
	 * and the appropriate action will be taken.
	 */
	suspend fun download(): JSONObject = kotlin.coroutines.suspendCoroutine { // TODO Replace returned value with Array of Routes (from callback).

		// Make the network request for the master route.
		this.splashActivity.viewModel.routeMatch.callMasterSchedule(MasterScheduleCallback(it), {
			error: com.android.volley.VolleyError ->

			// Since there was an error retrieving the master route be sure to log it,
			// and show the retry message and button.
			Log.w("initializeApp", "MasterSchedule callback error", error)
			Log.w("initializeApp", "Error: ${error.message}\n${error.cause.toString()}")
			this.splashActivity.viewModel.setMessage(R.string.routematch_timeout)
			this.splashActivity.showRetryButton()
		})
	}

	/**
	 * Callback used to parse the master route JSON Object that was retrieved from the RouteMatch server.
	 *
	 * @param continuation The continuation coroutine to resume once the callback finishes.
	 */
	internal inner class MasterScheduleCallback(private val continuation: kotlin.coroutines.
	Continuation<JSONObject>) : com.android.volley.Response.Listener<JSONObject> {

		override fun onResponse(response: JSONObject) {

			// Set the progress and message.
			this@DownloadMasterSchedule.splashActivity.viewModel.setProgressBar(SplashActivity.
			DOWNLOAD_MASTER_SCHEDULE_PROGRESS.toDouble())
			this@DownloadMasterSchedule.splashActivity.viewModel.setMessage(R.string.loading_bus_routes)

			// Get the routes from the JSONObject
			val routes = fnsb.macstransit.routematch.RouteMatch.parseData(response)

			// If the routes length is 0, say that there are no buses for the day.
			val count = routes.length()
			if (count == 0) {
				this@DownloadMasterSchedule.splashActivity.viewModel.setMessage(R.string.its_sunday)

				// Also add a chance for the user to retry.
				this@DownloadMasterSchedule.splashActivity.showRetryButton()
				this@DownloadMasterSchedule.splashActivity.loaded = true
				return
			}

			// Create an array to store all the generated routes.
			val potentialRoutes = arrayOfNulls<Route>(count)
			var routeCount = 0

			// Update the progress and message.
			val step = SplashActivity.PARSE_MASTER_SCHEDULE.toDouble() / count
			var progress = SplashActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS.toDouble()
			this@DownloadMasterSchedule.splashActivity.viewModel.setMessage(R.string.parsing_master_schedule)

			// Iterate though each route in the master schedule.
			for (index in 0 until count) {
				Log.d("MasterScheduleCallback", "Parsing route ${index + 1}/$count")

				// Try to get the route data from the array.
				// If there's an issue parsing the data simply continue to the next iteration of the loop.
				val routeData: JSONObject = try {
					routes.getJSONObject(index)
				} catch (e: org.json.JSONException) {
					Log.w("MasterScheduleCallback", "Issue retrieving the route data", e)
					continue
				}

				// Try to create the route using the route data obtained above.
				// If there was a route exception thrown simply log it.
				try {
					val route = Route.generateRoute(routeData)
					potentialRoutes[routeCount] = route
					routeCount++
				} catch (Exception: Exception) {
					Log.e("MasterScheduleCallback", "Issue creating route from route data",
					     Exception)
				}
				this@DownloadMasterSchedule.splashActivity.viewModel.setProgressBar(progress + step)
				progress += step
			}

			// Down size our potential routes array to fit the actual number of routes.
			val finalRoutes: Array<Route?> = arrayOfNulls(routeCount)
			System.arraycopy(potentialRoutes, 0, finalRoutes, 0, routeCount)
			fnsb.macstransit.activities.mapsactivity.MapsActivity.allRoutes = finalRoutes as Array<Route>

			// Continue with the rest of the coroutine.
			Log.d("MasterScheduleCallback", "End of MasterScheduleCallback")
			this.continuation.resume(response)
		}
	}
}