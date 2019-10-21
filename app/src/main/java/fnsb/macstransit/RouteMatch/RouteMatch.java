package fnsb.macstransit.RouteMatch;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 */
public class RouteMatch {

	/**
	 * The url to pull route data from.
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
	 * @param url    The URL to pull data from (IE: https://fnsb.routematch.com/feed/)
	 * @param routes The array of all the bus routes that are <b><i>able to be</i></b> tracked.
	 */
	public RouteMatch(String url, Route[] routes) {
		this.url = url;
		this.routes = routes;
	}

	/**
	 * TODO Update documentation and comments
	 * Reads the JSON from the provided URL, and formats it into a JSONObject. If the URL times out,
	 * or responds with an error the method will retry.
	 *
	 * @param url The URL to retrieve the JSON data from.
	 * @return The JSONObject containing the data (or a blank JSON Object if there was an error).
	 */
	static JSONObject readJsonFromUrl(String url) {

		Log.d("readJsonFromUrl", url);
		StringBuilder jsonString = new StringBuilder();

		Thread t = new Thread(() -> {

			try {
				// Specify the URL connection
				java.net.URLConnection connection = new java.net.URL(url).openConnection();

				// Add timeouts for the connection (1.5 seconds to connect, 2 seconds to read, 3.5 seconds total)
				connection.setConnectTimeout(1500);
				connection.setReadTimeout(2000);

				// Get the input stream from the connection
				java.io.InputStream inputStream = connection.getInputStream();

				// Create a buffered reader for the input stream
				BufferedReader bufferedReader = new BufferedReader(new java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8));

				// Store the inputted text into a string variable
				jsonString.append(RouteMatch.readAll(bufferedReader));

				bufferedReader.close();
				inputStream.close();

			} catch (java.io.FileNotFoundException | java.net.SocketTimeoutException e) {
				throw new RuntimeException();
			} catch (IOException uhoh) {
				uhoh.printStackTrace();
			}

		});
		t.setUncaughtExceptionHandler((t1, e) -> {
			// Keep trying!
			android.util.Log.w("readJsonFromUrl", "Page didn't respond, going to retry!");
			// TODO Clear JSON String
			jsonString.setLength(0);
			jsonString.append(RouteMatch.readJsonFromUrl(url).toString());
		});
		t.setName("Network thread");
		t.start();
		try {
			t.join(4000);
			if (jsonString.length() == 0) {
				return RouteMatch.readJsonFromUrl(url);
			} else {
				// Create and return a new JSONObject from the jsonString variable
				return new JSONObject(jsonString.toString());
			}
		} catch (org.json.JSONException | InterruptedException e) {
			e.printStackTrace();
			return new JSONObject();
		}
	}

	/**
	 * Parse characters from a reader or buffered reader into a stream.
	 *
	 * @param reader The Reader or BufferedReader object.
	 * @return The final string from the String builder containing what was read by the Reader.
	 */
	@Deprecated
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
	 * @param routeName The name of the route to get the pertaining data from (IE: Red).
	 * @return The JSONObject pertaining to that specific route's data.
	 */
	public JSONObject getRoute(String routeName) {
		// Example usage: readJsonFromUrl("https://fnsb.routematch.com/feed/vehicle/byRoutes/Red");
		return RouteMatch.readJsonFromUrl(this.url + "/vehicle/byRoutes/" + routeName);
	}

	/**
	 * Gets all the routes as a Json object that were provided in the constructor.
	 *
	 * @return A JSONObject array that contains all the individual routes's data.
	 */
	@Deprecated
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
