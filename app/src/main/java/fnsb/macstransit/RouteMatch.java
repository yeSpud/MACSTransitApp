package fnsb.macstransit;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 */
public class RouteMatch {

	/**
	 * Name of the route
	 */
	String name;

	/**
	 * The url to pull route data from
	 * <p>
	 * https://fnsb.routematch.com/feed/vehicle/byRoutes/
	 */
	String url;

	/**
	 * The bus lines in the route
	 */
	String[] lines;

	public RouteMatch(String name, String url, String[] lines) {
		this.name = name;
		this.url = url;
		this.lines = lines;
	}

	/**
	 * Gets the route data from the url provided in the constructor.
	 *
	 * @param route The route to get the pertaining data from.
	 * @return The JSONObject pertaining to that specific route's data.
	 */
	public JSONObject getRoute(String route) {
		try {
			// JSONObject blue = readJsonFromUrl("https://fnsb.routematch.com/feed/vehicle/byRoutes/Red");
			return this.readJsonFromUrl(this.url + route);
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

		// Create the JSONObject array, and make it the size of the total lines provided in this object
		JSONObject[] jsonObjects = new JSONObject[this.lines.length];

		// Iterate through each line, and be sure to keep track of the current index
		for (int index = 0; index < jsonObjects.length; index++) {

			// Retrieve the current lines route, and store it into the JSONObject array
			jsonObjects[index] = this.getRoute(this.lines[index]);
		}

		// Return the final JSONObject array
		return jsonObjects;
	}

	/**
	 * Reads the JSON from the provided URL, and formats it into a JSONObject.
	 *
	 * @param url The URL to retrieve the JSON data from.
	 * @return The JSONObject containing the data.
	 * @throws IOException Throws an IOException if anything goes wrong.
	 */
	private JSONObject readJsonFromUrl(String url) throws IOException, JSONException {

		// Open an input stream using the URL
		try (InputStream inputStream = new URL(url).openStream()) {

			// Create a buffered reader for the input stream
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

			// Store the inputted text into a string variable
			String jsonString = this.readAll(bufferedReader);

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
	private String readAll(Reader reader) throws IOException {

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

}
