package fnsb.macstransit.activities.loadingactivity.loadingscreenrunnables

import android.util.Log
import fnsb.macstransit.R
import fnsb.macstransit.activities.loadingactivity.LoadingActivity
import fnsb.macstransit.routematch.Route

/**
 * Created by Spud on 8/16/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.1.
 * @since Release 1.3.
 */
class DownloadMasterSchedule(private val loadingActivity: LoadingActivity):
		DownloadRouteObjects<Unit>(loadingActivity.viewModel) {

	override suspend fun download(route: Route, downloadProgress: Double, progressSoFar: Double,
	                              index: Int): Unit = kotlin.coroutines.suspendCoroutine {

		// Set the progress.
		this@DownloadMasterSchedule.loadingActivity.viewModel.setProgressBar(downloadProgress)

		// Make the network request for the master route.
		this.loadingActivity.viewModel.routeMatch.callMasterSchedule(ParseMasterSchedule(it,
		                                                                                 this.loadingActivity,
		                                                                                 this.viewModel), {
			error: com.android.volley.VolleyError ->

			// Since there was an error retrieving the master route be sure to log it,
			// and show the retry message and button.
			Log.w("initializeApp", "MasterSchedule callback error", error)
			Log.w("initializeApp", "Error: ${error.message}\n${error.cause.toString()}")
			this.loadingActivity.viewModel.setMessage(R.string.routematch_timeout)
			this.loadingActivity.allowForRetry()
		})
	}

	internal class ParseMasterSchedule(continuation: kotlin.coroutines.Continuation<Unit>,
	                                   private val loadingActivity: LoadingActivity, viewModel: fnsb.
			macstransit.activities.loadingactivity.LoadingViewModel) :
			DownloadableCallback<Unit>(continuation, viewModel, R.string.parsing_master_schedule) {

		override fun parse(jsonArray: org.json.JSONArray) {

			// Get the route count from the JSON Array.
			val count = jsonArray.length()

			// If the routes length is 0, say that there are no buses for the day.
			if (count == 0) {
				this.viewModel.setMessage(R.string.its_sunday)

				// Also add a chance for the user to retry.
				this.loadingActivity.allowForRetry()
				this.loadingActivity.loaded = true
			}

			// Update the progress and message.
			val step = LoadingActivity.PARSE_MASTER_SCHEDULE.toDouble() / count
			var progress = LoadingActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS.toDouble()

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

					// Add the route to the hashmap in the view model.
					this.loadingActivity.viewModel.routes[route.name] = route
				} catch (Exception: Exception) {

					// If there was a route exception thrown simply log it.
					Log.e("MasterScheduleCallback", "Issue creating route from route data",
					      Exception)
				}
				this.loadingActivity.viewModel.setProgressBar(progress + step)
				progress += step
			}
		}
	}
}