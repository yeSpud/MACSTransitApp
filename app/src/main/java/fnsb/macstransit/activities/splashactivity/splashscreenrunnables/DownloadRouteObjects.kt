package fnsb.macstransit.activities.splashactivity.splashscreenrunnables

import fnsb.macstransit.activities.splashactivity.SplashViewModel
import fnsb.macstransit.routematch.Route
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * Created by Spud on 9/1/21 for the project: MACS Transit.
 *
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0
 * @since Release 1.3.1.
 */
abstract class DownloadRouteObjects(val viewModel: SplashViewModel) {

	/**
	 * Documentation
	 * @param route
	 * @param index
	 * @return
	 */
	abstract suspend fun download(route: Route, downloadProgress: Double, progressSoFar: Double,
	                              index: Int): JSONObject

	/**
	 * Documentation
	 * @param continuation
	 * @param route
	 * @param message
	 */
	internal abstract inner class Callback(
			private val continuation: kotlin.coroutines.Continuation<JSONObject>, val route: Route,
			private val message: Int): com.android.volley.Response.Listener<JSONObject> {

		/**
		 * Documentation
		 * @param jsonArray
		 */
		abstract fun function(jsonArray: JSONArray)

		override fun onResponse(response: JSONObject) {

			// TODO Comment
			this@DownloadRouteObjects.viewModel.setMessage(this.message)

			// Get the data from all the stops and store it in a JSONArray.
			val data: JSONArray = fnsb.macstransit.routematch.RouteMatch.parseData(response)

			// Pass it to our function
			this.function(data)

			// TODO Log and comment
			this.continuation.resume(response)

		}
	}
}