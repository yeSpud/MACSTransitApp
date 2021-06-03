package fnsb.macstransit.RouteMatch;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.regex.Pattern;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 4.0.
 * @since Beta 1.
 */
public class RouteMatch {

	/**
	 * Retry policy used by the Volley networking queue.
	 */
	private static final com.android.volley.RetryPolicy RETRY_POLICY = new com.android.volley.
			DefaultRetryPolicy(90000, 3, 1);

	/**
	 * Volley network queue used to make network and API requests.
	 */
	public final com.android.volley.RequestQueue networkQueue;

	/**
	 * The feed url to pull all the route data, bus data, and stop data from.
	 */
	private final String url;

	/**
	 * Constructor for the RouteMatch object.
	 * Be sure that this is a valid url starting with {@code http(s):}, and ends with a {@code /}.
	 *
	 * @param url     The feed url to pull data from (IE: https://fnsb.routematch.com/feed/).
	 * @param context The app context used to make network and API requests.
	 * @throws MalformedURLException Thrown if the url entered is not in a valid url format.
	 */
	public RouteMatch(String url, android.content.Context context) throws MalformedURLException {

		// Create a regex pattern matcher to match the URL to.
		final Pattern pattern = Pattern.compile("^https?://\\S+/$");

		// Make sure the provided url matches the specific pattern.
		if (pattern.matcher(url).matches()) {
			this.url = url;
		} else {
			throw new MalformedURLException("Url must either be http, or https, and MUST end with /");
		}

		// Initialize the Volley network queue.
		this.networkQueue = com.android.volley.toolbox.Volley.newRequestQueue(context);
	}

	/**
	 * Parses the {@code data} portion of the provided JSONObject, and returns the JSONArray.
	 * If there was no section called {@code data}, then an empty JSONArray will be returned instead.
	 *
	 * @param object The JSONObject to parse.
	 * @return The JSONArray, or an empty JSONArray if no data section was found.
	 */
	@NonNull
	public static JSONArray parseData(@NonNull JSONObject object) {

		// Try to parse the json data array. If unsuccessful just return an empty json array.
		try {
			return object.getJSONArray("data");
		} catch (org.json.JSONException e) {
			Log.w("parseData", "Unable to parse data!");
			return new JSONArray();
		}
	}

	/**
	 * Gets the master schedule from the RouteMatch server.
	 *
	 * @param successCallback The callback function to run once a response has been received.
	 *                        This cannot be null.
	 * @param onError         The function to run if an error occurs with the request.
	 * @param tag             The tag of the request (used to identify requests for canceling).
	 */
	public void callMasterSchedule(Response.Listener<JSONObject> successCallback, @Nullable Response.ErrorListener onError, Object tag) {
		this.executeNetworkRequest(this.url + "masterRoute/", successCallback, onError, tag);
	}

	/**
	 * Gets all the stops for the specified route from the RouteMatch server.
	 *
	 * @param route           The route to get all the stops for. This cannot be null.
	 * @param successCallback The callback function to run once a response has been received.
	 *                        This cannot be null.
	 * @param onError         The function to run if an error occurs with the request.
	 * @param tag             The tag of the request (used to identify requests for canceling).
	 */
	public void callAllStops(@NonNull Route route, Response.Listener<JSONObject> successCallback, @Nullable Response.ErrorListener onError, Object tag) {
		this.executeNetworkRequest(this.url + "stops/" + route.urlFormattedName, successCallback, onError, tag);
	}

	/**
	 * Gets the departure (and arrival) information from the specified stop from the RouteMatch server.
	 *
	 * @param stopName        The name of the stop to get the departure information for. This cannot be null.
	 * @param successCallback The callback function to run once a response has been received.
	 *                        This cannot be null.
	 * @param onError         The function to run if an error occurs with the request.
	 * @param tag             The tag of the request (used to identify requests for canceling).
	 */
	public void callDeparturesByStop(@NonNull String stopName, Response.Listener<JSONObject> successCallback, @Nullable Response.ErrorListener onError, Object tag) {

		// Try formatting the departure URL. If there was an exception, return early.
		String url;
		try {
			url = this.url + "departures/byStop/" + Pattern.compile("\\+").
					matcher(java.net.URLEncoder.encode(stopName, "UTF-8")).replaceAll("%20");
		} catch (java.io.UnsupportedEncodingException e) {
			Log.e("callDeparturesByStop", "Cannot encode URL", e);
			return;
		}

		this.executeNetworkRequest(url, successCallback, onError, tag);
	}

	/**
	 * Gets all the vehicles by an array of routes.
	 *
	 * @param successCallback The callback function to run once a response has been received.
	 *                        This cannot be null.
	 * @param onError         The function to run if an error occurs with the request.
	 * @param tag             The tag of the request (used to identify requests for canceling).
	 * @param routes          The routes to get the buses for. This cannot be null.
	 */
	public void callVehiclesByRoutes(Response.Listener<JSONObject> successCallback, @Nullable Response.ErrorListener onError, Object tag, @NonNull Route... routes) {
		final String separator = "%2C"; // Route separator.

		// Build the initial URL size based off of the size of the routes plus the separator.
		StringBuilder routesString = new StringBuilder(separator.length() * routes.length);
		for (Route route : routes) {

			// Add the route URL to the URL string.
			routesString.append(route.urlFormattedName).append(separator);
		}
		this.executeNetworkRequest(this.url + "vehicle/byRoutes/" + routesString, successCallback, onError, tag);
	}

	/**
	 * Gets the land route (the route the buses will take) of a particular route from the RouteMatch server.
	 *
	 * @param route           The route to make the request the for. This cannot be null.
	 * @param successCallback The callback function to run once a response has been received.
	 *                        This cannot be null.
	 * @param onError         The function to run if an error occurs with the request.
	 * @param tag             The tag of the request (used to identify requests for canceling).
	 */
	public void callLandRoute(@NonNull Route route, Response.Listener<JSONObject> successCallback, @Nullable Response.ErrorListener onError, Object tag) {
		this.executeNetworkRequest(this.url + "landRoute/byRoute/" + route.urlFormattedName, successCallback, onError, tag);
	}

	/**
	 * Executes a GET request on the provided URL. If successful the successCallback is called,
	 * if unsuccessful the onError listener is called.
	 *
	 * @param url             The URL to query in a GET request. This cannot be null.
	 * @param successCallback The callback function to run once a response has been received.
	 *                        This cannot be null.
	 * @param onError         The function to run if an error occurs with the request.
	 * @param tag             The tag of the request (used to identify requests for canceling).
	 */
	private void executeNetworkRequest(@NonNull String url, @NonNull Response.Listener<JSONObject> successCallback, @Nullable Response.ErrorListener onError, Object tag) {
		Log.d("executeNetworkRequest", "Querying url: " + url);
		JsonObjectRequest request = new JsonObjectRequest(url, null, successCallback, onError);
		request.setRetryPolicy(RouteMatch.RETRY_POLICY);
		request.setTag(tag);
		this.networkQueue.add(request);
	}
}
