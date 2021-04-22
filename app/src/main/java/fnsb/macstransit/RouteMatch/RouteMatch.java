package fnsb.macstransit.RouteMatch;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.regex.Pattern;

import fnsb.macstransit.Threads.Network;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 3.3.
 * @since Beta 1.
 */
public class RouteMatch {

	/**
	 * The feed url to pull all the route data, bus data, and stop data from.
	 */
	private final String url;

	/**
	 * Constructor for the RouteMatch object.
	 * Be sure that this is a valid url starting with {@code http(s):}, and ends with a {@code /}.
	 *
	 * @param url The feed url to pull data from (IE: https://fnsb.routematch.com/feed/).
	 * @throws MalformedURLException Thrown if the url entered is not in a valid url format.
	 */
	public RouteMatch(String url) throws MalformedURLException {

		// Create a regex pattern matcher to match the URL to.
		final Pattern pattern = Pattern.compile("^https?://\\S+/$");

		// Make sure the provided url matches the specific pattern.
		if (pattern.matcher(url).matches()) {
			this.url = url;
		} else {
			throw new MalformedURLException("Url must either be http, or https, and MUST end with /");
		}
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
	 * @return The master Schedule as a JSONObject.
	 */
	public JSONObject getMasterSchedule() {
		return Network.getJsonFromUrl(this.url + "masterRoute/", false);
	}

	/**
	 * Gets all the stops for the specified route from the RouteMatch server.
	 *
	 * @param route The route pertaining to the stops.
	 * @return The JSONObject pertaining to all the stops for the specified route.
	 */
	public JSONObject getAllStops(@NonNull Route route) {
		return Network.getJsonFromUrl(this.url + "stops/" + route.routeName, true);
	}

	/**
	 * Gets the departure (and arrival) information from the specified stop from the RouteMatch server.
	 *
	 * @param stopName The stop to get the information for.
	 * @return The json object contain the departure (and arrival) information
	 */
	public JSONObject getDeparturesByStop(@NonNull String stopName) {

		// Create a pattern to match special URL characters.
		final Pattern pattern = Pattern.compile("\\+");

		// Try to create the url that will be used to retrieve the stop data.
		try {
			String url = this.url + "departures/byStop/" +
					pattern.matcher(java.net.URLEncoder.encode(stopName, "UTF-8"))
							.replaceAll("%20");
			Log.d("getDeparturesByStop", "URL: " + url);

			// Return the stop data from the URL.
			return Network.getJsonFromUrl(url, false);
		} catch (java.io.UnsupportedEncodingException e) {

			// If there was an encoding exception thrown simply return an empty json object (and log it).
			Log.e("getStop",
					"The encoded stop was malformed! Returning an empty JSONObject instead", e);
			return new JSONObject();
		}
	}

	/**
	 * Gets all the vehicles by an array of routes.
	 *
	 * @param routes The routes of the vehicles to be queried from the RouteMatch server.
	 * @return The json object containing the data for all the vehicles that were retrieved by their respective routes.
	 */
	public JSONObject getVehiclesByRoutes(@NonNull Route... routes) {

		// Get the URL encoded separator for separating different routes.
		final String separator = "%2C";

		// Create a new string builder that will be used to store our final generated string from all the route names.
		// Since we know that all routes will have a separator,
		// set the initial length to the separator times the number of routes.
		StringBuilder routesString = new StringBuilder(separator.length() * routes.length);

		// Iterate through each route and append the route name plus the separator to our string builder.
		for (Route route : routes) {
			routesString.append(route.routeName).append(separator);
		}

		// Return the bus data from the url.
		return Network.getJsonFromUrl(this.url + "vehicle/byRoutes/" + routesString, false);
	}

	/**
	 * Gets the land route
	 * (the route the buses will take) of a particular route from the RouteMatch server.
	 *
	 * @param route The route to be fetched.
	 * @return The JSONObject pertaining to the specific route
	 * (what route it will take as a series of latitude and longitude coordinates).
	 */
	public JSONObject getLandRoute(@NonNull Route route) {
		return Network.getJsonFromUrl(this.url + "landRoute/byRoute/" + route.routeName, true);
	}
}
