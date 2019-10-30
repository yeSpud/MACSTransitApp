package fnsb.macstransit.RouteMatch;

import org.json.JSONArray;
import org.json.JSONObject;

import fnsb.macstransit.Network;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 3.1
 * @since Beta 1
 */
public class RouteMatch {

	/**
	 * The feed url to pull route data from.
	 * <p>
	 * Example: https://fnsb.routematch.com/feed/
	 * TODO Make sure this follows a standard form when created
	 */
	private String url;

	/**
	 * Constructor for the RouteMatch object.
	 *
	 * @param url The feed url to pull data from (IE: https://fnsb.routematch.com/feed/)
	 */
	public RouteMatch(String url) {
		this.url = url;
	}

	/**
	 * TODO Documentation
	 *
	 * @param object
	 * @return
	 */
	public static JSONArray parseData(JSONObject object) {
		try {
			return object.getJSONArray("data");
		} catch (org.json.JSONException e) {
			android.util.Log.w("parseData", "Unable to parse data!");
			return new JSONArray();
		}
	}

	/**
	 * TODO Documentation
	 *
	 * @return
	 */
	public JSONObject getMasterSchedule() {
		return Network.getJsonFromUrl(this.url + "masterRoute/");
	}

	/**
	 * TODO Documentation
	 *
	 * @param route
	 * @return
	 */
	public JSONObject getAllStops(Route route) {
		return Network.getJsonFromUrl(this.url + "stops/" + route.routeName);
	}

	/**
	 * Gets the route data from the url provided in the constructor.
	 * TODO Documentation
	 *
	 * @param route
	 * @return The JSONObject pertaining to that specific route's data.
	 */
	public JSONObject getRoute(Route route) {
		return Network.getJsonFromUrl(this.url + "vehicle/byRoutes/" + route.routeName);
	}
}
