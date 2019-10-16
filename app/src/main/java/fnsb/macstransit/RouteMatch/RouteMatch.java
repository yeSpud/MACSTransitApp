package fnsb.macstransit.RouteMatch;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;

import fnsb.macstransit.Exceptions.RouteMatchException;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 */
public class RouteMatch {

	/**
	 * Name of the route
	 */
	@Deprecated
	private String name;

	/**
	 * The url to pull route data from
	 * <p>
	 * https://fnsb.routematch.com/feed/
	 */
	private String url;

	/**
	 * The bus routes in the route
	 */
	private Route[] routes;

	/**
	 * TODO Documentaiton
	 *
	 * @param name
	 * @param url
	 * @param routes
	 */
	public RouteMatch(String name, String url, Route[] routes) {
		this.name = name;
		this.url = url;
		this.routes = routes;
	}

	/**
	 * TODO Documentation
	 *
	 * @param name
	 * @param url
	 */
	public RouteMatch(String name, String url) throws RouteMatchException {
		// Since the routes were passed by defalt, attempt to get them dynamically
		Route[] r = Route.generateRoutes(url);

		// Now continue with the standard construction
		this.name = name;
		this.url = url;
		this.routes = r;
	}

	/**
	 * Reads the JSON from the provided URL, and formats it into a JSONObject.
	 *
	 * @param url The URL to retrieve the JSON data from.
	 * @return The JSONObject containing the data.
	 * @throws IOException Throws an IOException if anything goes wrong.
	 */
	static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {

		// Open an input stream using the URL
		try (java.io.InputStream inputStream = new java.net.URL(url).openStream()) {

			// Create a buffered reader for the input stream
			BufferedReader bufferedReader = new BufferedReader(new java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8));

			// Store the inputted text into a string variable
			String jsonString = RouteMatch.readAll(bufferedReader);

			// Create and return a new JSONObject from the jsonString variable
			return new JSONObject(jsonString);
		}
	}

	/**
	 * Parse characters from a reader or buffered reader into a stream.
	 *
	 * @param reader The Reader or BufferedReader object.
	 * @return The final string from the String builder containing what was read by the Reader.
	 * @throws IOException Throws an IOException if anything goes wrong.
	 */
	private static String readAll(java.io.Reader reader) throws IOException {

		// Create a string to store what is read by the reader
		StringBuilder string = new StringBuilder();

		// Create a variable for character parsing
		int character;

		// Loop through all the characters until there are no more characters to run through (returns -1)
		while ((character = reader.read()) != -1) {

			// Append the character to the string
			string.append((char) character);
		}

		// Finally return the string
		return string.toString();
	}

	/**
	 * Gets the route data from the url provided in the constructor.
	 *
	 * @param routeName The name of the route to get the pertaining data from.
	 * @return The JSONObject pertaining to that specific route's data.
	 */
	public JSONObject getRoute(String routeName) {
		try {
			// Example usage: readJsonFromUrl("https://fnsb.routematch.com/feed/vehicle/byRoutes/Red");
			return RouteMatch.readJsonFromUrl(this.url + "/vehicle/byRoutes/" + routeName);
		} catch (IOException | JSONException e) {
			// If there is an error, be sure to include the stacktrace
			e.printStackTrace();
		}

		// If there is an error, just return an empty JSON object
		return new JSONObject();
	}

	/**
	 * Gets all the routes that were provided in the constructor.
	 *
	 * @return A JSONObject array that contains all the individual routes's data.
	 */
	public JSONObject[] getAllRoutes() {

		// Create the JSONObject array, and make it the size of the total routes provided in this object
		JSONObject[] jsonObjects = new JSONObject[this.routes.length];

		// Iterate through each line, and be sure to keep track of the current index
		for (int index = 0; index < jsonObjects.length; index++) {

			// Retrieve the current routes route, and store it into the JSONObject array
			jsonObjects[index] = this.getRoute(this.routes[index].routeName);
		}

		// Return the final JSONObject array
		return jsonObjects;
	}

}
