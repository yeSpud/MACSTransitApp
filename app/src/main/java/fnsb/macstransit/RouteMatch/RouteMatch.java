package fnsb.macstransit.RouteMatch;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.regex.Pattern;

import fnsb.macstransit.Threads.Network;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 3.3
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
	@NotNull
	public static JSONArray parseData(@NotNull JSONObject object) {
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
		return Network.getJsonFromUrl(this.url + "masterRoute/", true);
	}

	/**
	 * Gets all the stops for the specified parentRoute from the RouteMatch server.
	 *
	 * @param route The parentRoute pertaining to the stops.
	 * @return The JSONObject pertaining to all the stops for the specified parentRoute.
	 */
	public JSONObject getAllStops(@NotNull Route route) {
		return Network.getJsonFromUrl(this.url + "stops/" + route.routeName, true);
	}

	/**
	 * Gets the stop data from the RouteMatch server.
	 * If the final encoded url is somehow malformed (most likely through a bad stop id)
	 * then an empty JSONObject will be returned instead.
	 *
	 * @param stop The stop to get the data for.
	 * @return The data as a JSONObject for the pertaining stop.
	 */
	public JSONObject getDeparturesByStop(@NotNull Stop stop) {
		try {
			// Create a pattern to match special URL characters.
			final Pattern pattern = Pattern.compile("\\+");

			// Create the url that will be used to retrieve the stop data.
			String url = this.url + "departures/byStop/" +
					pattern.matcher(java.net.URLEncoder.encode(stop.stopID, "UTF-8"))
							.replaceAll("%20");
			Log.d("getDeparturesByStop", "URL: " + url);

			// Get the stop data from the URL.
			return Network.getJsonFromUrl(url, false);
		} catch (java.io.UnsupportedEncodingException e) {
			Log.e("getStop", "The encoded stop was malformed! Returning an empty JSONObject instead");
			return new JSONObject();
		}
	}

	/**
	 * Gets the parentRoute data from the RouteMatch server.
	 *
	 * @param route The specific parentRoute to be fetched.
	 * @return The JSONObject pertaining to that specific parentRoute's data.
	 * @deprecated Use getVehiclesByRoutes(...)
	 */
	@Deprecated
	public JSONObject getBuses(Route route) {
		return Network.getJsonFromUrl(this.url + "vehicle/byRoutes/" + route.routeName, false);
	}

	/**
	 * Gets all the vehicles by an array of routes.
	 *
	 * @param routes The routes of the vehicles to be queried from the RouteMatch server.
	 * @return The json object containing the data for all the vehicles that were retrieved by their respective routes.
	 */
	public JSONObject getVehiclesByRoutes(@NotNull Route... routes) { // TODO Test
		final String separator = "%2C";
		StringBuilder routesString = new StringBuilder(separator.length());
		for (Route route : routes) {
			routesString.append(route.routeName).append(separator);
		}
		// TODO Experiment with templates
		return Network.getJsonFromUrl(this.url + "vehicle/byRoutes/" + routesString, false);
	}

	/**
	 * Gets the land route
	 * (the route the buses will take) of a particular route from the RouteMatch server.
	 *
	 * @param route The route to be fetched.
	 * @return The JSONObject pertaining to the specific parentRoute's parentRoute
	 * (what route it will take as a series of latitude and longitude coordinates).
	 */
	public JSONObject getLandRoute(@NotNull Route route) {
		return Network.getJsonFromUrl(this.url + "landRoute/byRoute/" + route.routeName, true);
	}
}
