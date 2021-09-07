package fnsb.macstransit.activities.splashactivity.splashscreenrunnables

import android.util.Log
import fnsb.macstransit.R
import fnsb.macstransit.activities.splashactivity.SplashActivity
import fnsb.macstransit.routematch.Route

/**
 * Created by Spud on 8/16/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
class DownloadMasterSchedule(private val splashActivity: SplashActivity):
		DownloadRouteObjects<HashMap<String, Route>>(splashActivity.viewModel) {

	override suspend fun download(route: Route, downloadProgress: Double, progressSoFar: Double,
	                              index: Int): HashMap<String, Route> = kotlin.coroutines.suspendCoroutine {

		// Set the progress.
		this@DownloadMasterSchedule.splashActivity.viewModel.setProgressBar(downloadProgress)

		// Make the network request for the master route.
		this.splashActivity.viewModel.routeMatch.callMasterSchedule(ParseMasterSchedule(it,
		                                                                                this.splashActivity,
		                                                                                this.viewModel), {
			error: com.android.volley.VolleyError ->

			// Since there was an error retrieving the master route be sure to log it,
			// and show the retry message and button.
			Log.w("initializeApp", "MasterSchedule callback error", error)
			Log.w("initializeApp", "Error: ${error.message}\n${error.cause.toString()}")
			this.splashActivity.viewModel.setMessage(R.string.routematch_timeout)
			this.splashActivity.showRetryButton()
		})
	}

	internal class ParseMasterSchedule(continuation: kotlin.coroutines.Continuation<HashMap<String, Route>>,
	                                   private val splashActivity: SplashActivity, viewModel: fnsb.
			macstransit.activities.splashactivity.SplashViewModel) :
			DownloadableCallback<HashMap<String, Route>>(continuation, viewModel, R.string.loading_bus_routes) {

		override fun parse(jsonArray: org.json.JSONArray): HashMap<String, Route> {

			// Comments
			val hashMap: HashMap<String, Route> = HashMap()

			// If the routes length is 0, say that there are no buses for the day.
			val count = jsonArray.length()
			if (count == 0) {
				this.viewModel.setMessage(R.string.its_sunday)

				// Also add a chance for the user to retry.
				this.splashActivity.showRetryButton()
				this.splashActivity.loaded = true

				// Return the empty hashmap.
				return hashMap
			}

			// Update the progress and message.
			val step = SplashActivity.PARSE_MASTER_SCHEDULE.toDouble() / count
			var progress = SplashActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS.toDouble()
			this.splashActivity.viewModel.setMessage(R.string.parsing_master_schedule)

			// Iterate though each route in the master schedule.
			for (index in 0 until count) {
				Log.d("MasterScheduleCallback", "Parsing route ${index + 1}/$count")

				// Try to get the route data from the array.
				// If there's an issue parsing the data simply continue to the next iteration of the loop.
				val routeData: org.json.JSONObject = try {
					jsonArray.getJSONObject(index)
				} catch (e: org.json.JSONException) {
					Log.w("MasterScheduleCallback", "Issue retrieving the route data", e)
					continue
				}

				// Try to create the route using the route data obtained above.
				try {
					val route = Route.generateRoute(routeData)

					// Comments
					hashMap[route.name] = route
				} catch (Exception: Exception) {

					// If there was a route exception thrown simply log it.
					Log.e("MasterScheduleCallback", "Issue creating route from route data",
					      Exception)
				}
				this.splashActivity.viewModel.setProgressBar(progress + step)
				progress += step
			}

			// Comments
			return hashMap
		}
	}
}