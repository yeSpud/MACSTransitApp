package fnsb.macstransit.routematch

import android.util.Log
import com.android.volley.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 5.0.
 * @since Beta 1.
 */
class RouteMatch(private val url: String, private val context: android.content.Context) {

	/**
	 * Volley network queue used to make network and API requests.
	 */
	val networkQueue: com.android.volley.RequestQueue =
			com.android.volley.toolbox.Volley.newRequestQueue(this.context)

	/**
	 * Used to check if the URL provided matches the URL pattern.
	 * If this is false then no network requests will be made.
	 */
	private val badURL: Boolean = !Pattern.compile("^https?://\\S+/$").matcher(this.url).matches()

	/**
	 * Gets the master schedule from the RouteMatch server.
	 *
	 * @param successCallback The callback function to run once a response has been received.
	 * @param onError         The function to run if an error occurs with the request.
	 */
	fun callMasterSchedule(successCallback: Response.Listener<JSONObject>,
	                       onError: Response.ErrorListener) {
		executeNetworkRequest(this.url + "masterRoute/", successCallback, onError, null)
	}

	/**
	 * Gets all the stops for the specified route from the RouteMatch server.
	 *
	 * @param route           The route to get all the stops for.
	 * @param successCallback The callback function to run once a response has been received.
	 * @param onError         The function to run if an error occurs with the request.
	 */
	fun callAllStops(route: Route, successCallback: Response.Listener<JSONObject>,
	                 onError: Response.ErrorListener) {
		executeNetworkRequest(this.url + "stops/${route.urlFormattedName}", successCallback, onError,
		                      null)
	}

	/**
	 * Gets the departure (and arrival) information from the specified stop from the RouteMatch server.
	 *
	 * @param stopName        The name of the stop to get the departure information for.
	 * @param successCallback The callback function to run once a response has been received.
	 * This cannot be null.
	 * @param onError         The function to run if an error occurs with the request.
	 * @param tag             The tag of the request (used to identify requests for canceling).
	 */
	fun callDeparturesByStop(stopName: String, successCallback: Response.Listener<JSONObject>,
	                         onError: Response.ErrorListener?, tag: Any) {

		// Try formatting the departure URL. If there was an exception, return early.
		val url: String = try {
			this.url + "departures/byStop/" + Pattern.compile("\\+")
					.matcher(java.net.URLEncoder.encode(stopName, "UTF-8")).replaceAll("%20")
		} catch (e: java.io.UnsupportedEncodingException) {
			Log.e("callDeparturesByStop", "Cannot encode URL", e)
			return
		}

		// Execute the network call to retrieve all the arrival and departure times.
		executeNetworkRequest(url, successCallback, onError, tag)
	}

	/**
	 * Gets all the vehicles by an array of routes.
	 *
	 * @param successCallback The callback function to run once a response has been received.
	 * This cannot be null.
	 * @param onError         The function to run if an error occurs with the request.
	 * @param tag             The tag of the request (used to identify requests for canceling).
	 * @param routes          The routes to get the buses for. This cannot be null.
	 */
	fun callVehiclesByRoutes(successCallback: Response.Listener<JSONObject>,
	                         onError: Response.ErrorListener?, tag: Any, vararg routes: Route) {

		// Iterate though all the routes that are going to be queried.
		var routesString = ""
		routes.forEach {

			// Add the route URL to the URL string.
			// %2C marks the separator between routes in terms of the URL string.
			routesString += "${it.urlFormattedName}%2C"
		}

		// Execute the network request to get all the buses for the given routes.
		executeNetworkRequest(this.url + "vehicle/byRoutes/" + routesString, successCallback,
		                      onError, tag)
	}

	/**
	 * Gets the land route (the route the buses will take) of a particular route from the RouteMatch server.
	 *
	 * @param route           The route to make the request the for. This cannot be null.
	 * @param successCallback The callback function to run once a response has been received.
	 * This cannot be null.
	 * @param onError         The function to run if an error occurs with the request.
	 * @param tag             The tag of the request (used to identify requests for canceling).
	 */
	fun callLandRoute(route: Route, successCallback: Response.Listener<JSONObject>,
	                  onError: Response.ErrorListener, tag: Any) {
		executeNetworkRequest(url + "landRoute/byRoute/" + route.urlFormattedName, successCallback,
		                      onError, tag)
	}

	/**
	 * Executes a GET request on the provided URL. If successful the successCallback is called,
	 * if unsuccessful the onError listener is called.
	 *
	 * @param url             The URL to query in a GET request. This cannot be null.
	 * @param successCallback The callback function to run once a response has been received.
	 * This cannot be null.
	 * @param onError         The function to run if an error occurs with the request.
	 * @param tag             The tag of the request (used to identify requests for canceling).
	 */
	private fun executeNetworkRequest(url: String, successCallback: Response.Listener<JSONObject>,
	                                  onError: Response.ErrorListener?, tag: Any?) {

		// Check if teh URL is bad. If it is then log and return early.
		if (this.badURL) {
			Log.e("executeNetworkRequest", "URL to be queried is invalid: $url")
			return
		}

		// Make the JSON Request object.
		Log.d("executeNetworkRequest", "Querying url: $url")
		val request =
				com.android.volley.toolbox.JsonObjectRequest(com.android.volley.Request.Method.GET,
				                                             url, null, successCallback,
				                                             onError)

		// Set the request retry policy.
		request.retryPolicy = RETRY_POLICY

		// Set the request tag (this may be null).
		request.tag = tag

		// Make the network request by adding it to the network queue.
		this.networkQueue.add(request)
	}

	companion object {

		/**
		 * Retry policy used by the Volley networking queue.
		 * The request will time out after 1.5 minutes, and will retry 3 times.
		 */
		private val RETRY_POLICY: com.android.volley.RetryPolicy =
				com.android.volley.DefaultRetryPolicy(90000, 3, 1.0f)

		/**
		 * Parses the `data` portion of the provided JSONObject, and returns the JSONArray.
		 * If there was no section called `data`, then an empty JSONArray will be returned instead.
		 *
		 * @param jsonObject The JSONObject to parse.
		 * @return The JSONArray, or an empty JSONArray if no data section was found.
		 */
		@JvmStatic
		fun parseData(jsonObject: JSONObject): JSONArray {

			// Try to parse the json data array. If unsuccessful just return an empty json array.
			return try {
				jsonObject.getJSONArray("data")
			} catch (e: org.json.JSONException) {
				Log.w("parseData", "Unable to parse data!")
				JSONArray()
			}
		}
	}
}