package fnsb.macstransit;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Created by Spud on 2019-10-21 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 */
public class Network {

	/**
	 * Create a private variable that will track the number of attempts made to connect.
	 * This needs to remain private as it should only be accessed and used in this class.
	 */
	private static int attempts = 0;

	/**
	 * TODO Update documentation and comments
	 * Reads the JSON from the provided URL, and formats it into a JSONObject. If the URL times out,
	 * or responds with an error the method will retry.
	 *
	 * @param url The URL to retrieve the JSON data from.
	 * @return The JSONObject containing the data (or a blank JSON Object if there was an error).
	 */
	public static JSONObject readJsonFromUrl(String url) {

		Log.d("readJsonFromUrl", url);
		StringBuilder jsonString = new StringBuilder();

		Thread t = new Thread(() -> {
			try {
				jsonString.append(Network.readFromUrl(url));
			} catch (java.io.FileNotFoundException | java.net.SocketTimeoutException e) {
				throw new RuntimeException();
			} catch (IOException uhoh) {
				uhoh.printStackTrace();
			}
		});
		t.setUncaughtExceptionHandler((t1, e) -> {
			jsonString.setLength(0);
		});
		t.setName("Network thread");
		t.start();
		try {
			t.join(2500);
			return Network.validateJson(jsonString, url);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return new JSONObject();
		}
	}

	/**
	 * TODO Documentation and comments
	 *
	 * @param url
	 * @return
	 */
	private static String readFromUrl(String url) throws IOException {

		// Specify the URL connection
		java.net.URLConnection connection;
		try {
			connection = new java.net.URL(url).openConnection();
		} catch (java.net.MalformedURLException e) {
			// TODO Documentation
			e.printStackTrace();
			return null;
		}

		Log.d("readFromUrl", "Connection established");

		// Add timeouts for the connection (1 second to connect, 1 second to read, 2 seconds total)
		connection.setConnectTimeout(1000);
		connection.setReadTimeout(1000);

		// Get the input stream from the connection
		java.io.InputStream inputStream = connection.getInputStream();

		// Create a buffered reader for the input stream
		BufferedReader bufferedReader = new BufferedReader(new java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8));

		// Store the inputted text into a string variable
		String output = Network.readAll(bufferedReader);

		// TODO Documentation
		Log.d("readFromUrl", "Closing reader and stream");
		bufferedReader.close();
		inputStream.close();

		return output;
	}

	/**
	 * This method tried to validate the Json that is provided as a StringBuilder object by first checking if its not of length 0,
	 * and then by attempting to parse it to a JSONObject.
	 *
	 * @param string
	 * @param url
	 * @return
	 */
	private static JSONObject validateJson(StringBuilder string, String url) {

		// Check if the string builder object is empty (has a length of 0). If it does,
		// then that means that there was no JSON returned from the URL (likely due to a connection error),
		// so retry a maximum of 3 times.
		if (string.length() == 0) {

			// If there have been less than 3 retries, keep retrying.
			if (Network.attempts < 3) {

				// Sleep for a second to alleviate some stress from the receiving servers (in the event that this was the issue).
				try {
					Thread.sleep(500);
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}

				// Up the number of network attempts here, and log the current attempt number
				Network.attempts++;
				Log.w("validateJson", String.format("Url didn't respond, going to retry! (%d/3)", Network.attempts));

				// Try to return the JsonObject from the new attempt.
				return Network.readJsonFromUrl(url);
			} else {
				// Since the maximum number of tries has been attempted, reset the count,
				// and return an empty JsonObject.
				Log.w("validateJson", "Unable to get data from url");
				Network.attempts = 0;
				return new JSONObject();
			}
		} else {

			// Since the string builder wasn't empty it may be a valid Json string.
			// In this case, set the number of attempts to 0,
			// and try to return the string builder as a JSONObject.
			Network.attempts = 0;
			try {
				return new JSONObject(string.toString());
			} catch (org.json.JSONException e) {

				// If it failed to parse the string to a JSONObject, the string was likely malformed.
				// Shame.
				// Simply print the stack trace, and then return an empty JSONObject.
				e.printStackTrace();
				return new JSONObject();
			}
		}
	}

	/**
	 * Parse characters from a reader or buffered reader into a character stream. Once parsed,
	 * the character stream will be converted into a string and then returned.
	 *
	 * @param reader The Reader or BufferedReader object that will be used to read the character stream.
	 * @return The final string from the String builder containing what was read by the Reader.
	 */
	private static String readAll(java.io.Reader reader) throws IOException {

		Log.d("readAll", "Reading from stream...");

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
