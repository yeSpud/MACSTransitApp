package fnsb.macstransit.activities.splashactivity.splashscreenrunnables

import android.util.Log
import fnsb.macstransit.R
import fnsb.macstransit.activities.MapsActivity
import fnsb.macstransit.activities.splashactivity.SplashActivity
import fnsb.macstransit.routematch.Route
import org.json.JSONObject

/**
 * Created by Spud on 8/16/21 for the project: MACS Transit.
 *
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
class DownloadMasterSchedule(private val activity: SplashActivity) {

	/**
	 * Documentation
	 */
	fun download() {
		this.activity.routeMatch.callMasterSchedule(MasterScheduleCallback(), {
			error: com.android.volley.VolleyError ->
			Log.w("initializeApp", "MasterSchedule callback error", error)
			this.activity.viewModel.setMessage(R.string.routematch_timeout)
			this.activity.showRetryButton()
		})
	}

	internal inner class MasterScheduleCallback() : com.android.volley.Response.Listener<JSONObject> {

		override fun onResponse(response: JSONObject) { // Comments

			// Set the progress and message.
			this@DownloadMasterSchedule.activity.viewModel.setProgressBar(SplashActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS.toDouble())
			this@DownloadMasterSchedule.activity.viewModel.setMessage(R.string.loading_bus_routes)

			// Get the routes from the JSONObject
			val routes = fnsb.macstransit.routematch.RouteMatch.parseData(response)

			// If the routes length is 0, say that there are no buses for the day.
			val count = routes.length()
			if (count == 0) {
				this@DownloadMasterSchedule.activity.viewModel.setMessage(R.string.its_sunday)

				// Also add a chance for the user to retry.
				this@DownloadMasterSchedule.activity.showRetryButton()
				SplashActivity.loaded = true
				return
			}

			// Create an array to store all the generated routes.
			val potentialRoutes = arrayOfNulls<Route>(count)
			var routeCount = 0
			val step = SplashActivity.PARSE_MASTER_SCHEDULE.toDouble() / count
			var progress = SplashActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS.toDouble()
			this@DownloadMasterSchedule.activity.viewModel.setMessage(R.string.parsing_master_schedule)

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
				} catch (e: Route.RouteException) {
					Log.w("MasterScheduleCallback", "Issue creating route from route data", e)
				} catch (e: java.io.UnsupportedEncodingException) {
					Log.w("MasterScheduleCallback", "Issue creating route from route data", e)
				}
				this@DownloadMasterSchedule.activity.viewModel.setProgressBar(progress + step)
				progress += step
			}

			// Down size our potential routes array to fit the actual number of routes.
			MapsActivity.allRoutes = arrayOfNulls(routeCount)
			System.arraycopy(potentialRoutes, 0, MapsActivity.allRoutes!!, 0, routeCount)
			Log.d("MasterScheduleCallback", "End of MasterScheduleCallback")
		}
	}
}