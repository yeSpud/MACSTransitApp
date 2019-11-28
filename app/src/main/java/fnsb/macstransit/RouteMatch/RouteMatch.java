package fnsb.macstransit.RouteMatch;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.MalformedURLException;

import fnsb.macstransit.Threads.Network;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 3.2
 * @since Beta 1
 */
public class RouteMatch {

	/**
	 * The feed url to pull route data from.
	 */
	private String url;

	/**
	 * Constructor for the RouteMatch object.
	 * Be sure that this is a valid url starting with {@code http(s):}, and ends with a {@code /}.
	 *
	 * @param url The feed url to pull data from (IE: https://fnsb.routematch.com/feed/).
	 * @throws MalformedURLException Thrown if the url entered is not in a valid url format.
	 */
	public RouteMatch(String url) throws MalformedURLException {
		if (url.matches("^https?://\\S+/$")) {
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
	public static JSONArray parseData(JSONObject object) {
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
	 * Gets all the stops for the specified route from the RouteMatch server.
	 *
	 * @param route The route pertaining to the stops.
	 * @return The JSONObject pertaining to all the stops for the specified route.
	 */
	public JSONObject getAllStops(Route route) {
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
	public JSONObject getStop(Stop stop) {
		try {
			return Network.getJsonFromUrl(this.url + "departures/byStop/" +
					java.net.URLEncoder.encode(stop.stopID, "UTF-8")
							.replaceAll("\\+", "%20"), false);
		} catch (java.io.UnsupportedEncodingException e) {
			Log.e("getStop", "The encoded stop was malformed! Returning an empty JSONObject instead");
			return new JSONObject();
		}
	}

	/**
	 * Gets the route data from the RouteMatch server.
	 *
	 * @param route The specific route to be fetched.
	 * @return The JSONObject pertaining to that specific route's data.
	 */
	public JSONObject getBuses(Route route) {
		return Network.getJsonFromUrl(this.url + "vehicle/byRoutes/" + route.routeName, false);
	}

	/**
	 * Gets the land route (the route the buses will take) of a particular route from the RouteMatch server.
	 *
	 * @param route The route to be fetched.
	 * @return The JSONObject pertaining to the specific route's route
	 * (what route it will take as a series of latitude and longitude coordinates).
	 */
	public JSONObject getLandRoute(Route route) {
		return Network.getJsonFromUrl(this.url + "landRoute/byRoute/" + route.routeName, true);
	}
}
