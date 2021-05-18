package fnsb.macstransit.RouteMatch;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.regex.Pattern;

import fnsb.macstransit.Threads.Network;

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
	 * The feed url to pull all the route data, bus data, and stop data from.
	 */
	private final String url;

	/**
	 * TODO Documentation
	 */
	public final RequestQueue networkQueue;

	/**
	 * TODO Documentation
	 */
	private static final RetryPolicy RETRY_POLICY = new DefaultRetryPolicy(90000, 3, 1);

	/**
	 * Constructor for the RouteMatch object.
	 * Be sure that this is a valid url starting with {@code http(s):}, and ends with a {@code /}.
	 *
	 * @param url The feed url to pull data from (IE: https://fnsb.routematch.com/feed/).
	 * @param context
	 * @throws MalformedURLException Thrown if the url entered is not in a valid url format.
	 */
	public RouteMatch(String url, Context context) throws MalformedURLException {

		// Create a regex pattern matcher to match the URL to.
		final Pattern pattern = Pattern.compile("^https?://\\S+/$");

		// Make sure the provided url matches the specific pattern.
		if (pattern.matcher(url).matches()) {
			this.url = url;
		} else {
			throw new MalformedURLException("Url must either be http, or https, and MUST end with /");
		}

		// TODO
		this.networkQueue = Volley.newRequestQueue(context);
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
	 * TODO Documentation
	 * @param successCallback
	 * @param onError
	 */
	public void callMasterSchedule(Response.Listener<JSONObject> successCallback, @Nullable Response.ErrorListener onError) {
		JsonObjectRequest request = new JsonObjectRequest(this.url + "masterRoute/", null, successCallback, onError);
		request.setRetryPolicy(RouteMatch.RETRY_POLICY);
		this.networkQueue.add(request);
	}

	/**
	 * TODO Documentation
	 * @param route
	 * @param successCallback
	 * @param onError
	 */
	public void callAllStops(@NonNull Route route, Response.Listener<JSONObject> successCallback, @Nullable Response.ErrorListener onError) {
		JsonObjectRequest request = new JsonObjectRequest(this.url + "stops/" + route.urlFormattedName, null, successCallback, onError);
		request.setRetryPolicy(RouteMatch.RETRY_POLICY);
		this.networkQueue.add(request);
	}

	/**
	 * TODO Documentation
	 * @param stopName
	 * @param successCallback
	 * @param onError
	 */
	public void callDeparturesByStop(@NonNull String stopName, Response.Listener<JSONObject> successCallback, @Nullable Response.ErrorListener onError) {

		String url;
		try {
			url = this.url + "departures/byStop/" + Pattern.compile("\\+").
					matcher(java.net.URLEncoder.encode(stopName, "UTF-8")).replaceAll("%20");
		} catch (UnsupportedEncodingException e) {
			Log.e("callDeparturesByStop", "Cannot encode URL", e);
			return;
		}

		JsonObjectRequest request = new JsonObjectRequest(url, null, successCallback, onError);
		request.setRetryPolicy(RouteMatch.RETRY_POLICY);
		this.networkQueue.add(request);
	}

	/**
	 * Gets all the vehicles by an array of routes.
	 *
	 * @param routes The routes of the vehicles to be queried from the RouteMatch server.
	 * @return The json object containing the data for all the vehicles that were retrieved by their respective routes.
	 */
	@Deprecated
	public JSONObject getVehiclesByRoutes(@NonNull Route... routes) {

		// Get the URL encoded separator for separating different routes.
		final String separator = "%2C";

		// Create a new string builder that will be used to store our final generated string from all the route names.
		// Since we know that all routes will have a separator,
		// set the initial length to the separator times the number of routes.
		StringBuilder routesString = new StringBuilder(separator.length() * routes.length);

		// Iterate through each route and append the route name plus the separator to our string builder.
		for (Route route : routes) {
			routesString.append(route.urlFormattedName).append(separator);
		}

		// Return the bus data from the url.
		return Network.getJsonFromUrl(this.url + "vehicle/byRoutes/" + routesString, false);
	}

	/**
	 * TODO Documentation
	 * @param route
	 * @param successCallback
	 * @param onError
	 */
	public void callLandRoute(@NonNull Route route, Response.Listener<JSONObject> successCallback, @Nullable Response.ErrorListener onError) {
		JsonObjectRequest request = new JsonObjectRequest(this.url + "landRoute/byRoute/" + route.urlFormattedName, null, successCallback, onError);
		request.setRetryPolicy(RouteMatch.RETRY_POLICY);
		this.networkQueue.add(request);
	}
}
