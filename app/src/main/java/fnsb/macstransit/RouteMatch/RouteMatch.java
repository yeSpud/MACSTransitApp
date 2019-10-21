package fnsb.macstransit.RouteMatch;


/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 */
public class RouteMatch {

	/**
	 * The feed url to pull route data from.
	 * <p>
	 * Example: https://fnsb.routematch.com/feed/
	 */
	private String url;

	/**
	 * The array of all the bus routes that are <b><i>able to be</i></b> tracked tracked.
	 */
	private Route[] routes;

	/**
	 * Constructor for the RouteMatch object.
	 *
	 * @param url    The feed url to pull data from (IE: https://fnsb.routematch.com/feed/)
	 * @param routes The array of all the bus routes that are <b><i>able to be</i></b> tracked.
	 */
	public RouteMatch(String url, Route[] routes) {
		this.url = url;
		this.routes = routes;
	}


	/**
	 * Gets the route data from the url provided in the constructor.
	 *
	 * @param routeName The name of the route to get the pertaining data from (IE: Red).
	 * @return The JSONObject pertaining to that specific route's data.
	 */
	public org.json.JSONObject getRoute(String routeName) {
		// Example usage: readJsonFromUrl("https://fnsb.routematch.com/feed/vehicle/byRoutes/Red");
		return fnsb.macstransit.Network.readJsonFromUrl(this.url + "/vehicle/byRoutes/" + routeName);
	}

	/**
	 * Gets all the routes as a Json object that were provided in the constructor.
	 *
	 * @return A JSONObject array that contains all the individual routes's data.
	 */
	@Deprecated
	public org.json.JSONObject[] getAllRoutes() {

		// Create the JSONObject array, and make it the size of the total routes provided in this object
		org.json.JSONObject[] jsonObjects = new org.json.JSONObject[this.routes.length];

		// Iterate through each line, and be sure to keep track of the current index
		for (int index = 0; index < jsonObjects.length; index++) {

			// Retrieve the current routes route, and store it into the JSONObject array
			jsonObjects[index] = this.getRoute(this.routes[index].routeName);
		}

		// Return the final JSONObject array
		return jsonObjects;
	}

}
