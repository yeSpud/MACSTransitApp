package fnsb.macstransit.activities.splashactivity.splashscreenrunnables

import android.util.Log
import fnsb.macstransit.R
import fnsb.macstransit.activities.splashactivity.SplashActivity
import fnsb.macstransit.routematch.Route
import org.json.JSONArray
import org.json.JSONObject

/**
 * Created by Spud on 8/16/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
class DownloadMasterSchedule(private val splashActivity: SplashActivity):
		DownloadRouteObjects<Route>(splashActivity.viewModel, R.string.loading_bus_routes) {

	override suspend fun download(route: Route, downloadProgress: Double, progressSoFar: Double,
	                              index: Int): Array<Route> = kotlin.coroutines.suspendCoroutine {

		// Comments
		this.continuation = it

		// Set the progress.
		this@DownloadMasterSchedule.splashActivity.viewModel.setProgressBar(downloadProgress)

		// Make the network request for the master route.
		this.splashActivity.viewModel.routeMatch.callMasterSchedule(this, {
			error: com.android.volley.VolleyError ->

			// Since there was an error retrieving the master route be sure to log it,
			// and show the retry message and button.
			Log.w("initializeApp", "MasterSchedule callback error", error)
			Log.w("initializeApp", "Error: ${error.message}\n${error.cause.toString()}")
			this.splashActivity.viewModel.setMessage(R.string.routematch_timeout)
			this.splashActivity.showRetryButton()
		})
	}

	override fun parse(jsonArray: JSONArray): Array<Route> {

		// If the routes length is 0, say that there are no buses for the day.
		val count = jsonArray.length()
		if (count == 0) {
			this.splashActivity.viewModel.setMessage(R.string.its_sunday)

			// Also add a chance for the user to retry.
			this.splashActivity.showRetryButton()
			this.splashActivity.loaded = true

			// Return an empty array.
			return emptyArray()
		}

		// Create an array to store all the generated routes.
		val potentialRoutes = arrayOfNulls<Route>(count)
		var routeCount = 0

		// Update the progress and message.
		val step = SplashActivity.PARSE_MASTER_SCHEDULE.toDouble() / count
		var progress = SplashActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS.toDouble()
		this.splashActivity.viewModel.setMessage(R.string.parsing_master_schedule)

		// Iterate though each route in the master schedule.
		for (index in 0 until count) {
			Log.d("MasterScheduleCallback", "Parsing route ${index + 1}/$count")

			// Try to get the route data from the array.
			// If there's an issue parsing the data simply continue to the next iteration of the loop.
			val routeData: JSONObject = try {
				jsonArray.getJSONObject(index)
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
			this.splashActivity.viewModel.setProgressBar(progress + step)
			progress += step
		}

		// Down size our potential routes array to fit the actual number of routes.
		val finalRoutes: Array<Route?> = arrayOfNulls(routeCount)
		System.arraycopy(potentialRoutes, 0, finalRoutes, 0, routeCount)

		// Comments
		return finalRoutes as Array<Route>
	}
}