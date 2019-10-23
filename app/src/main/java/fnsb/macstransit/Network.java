package fnsb.macstransit;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Created by Spud on 2019-10-21 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Beta 6.
 */
public class Network {

	/**
	 * TODO Documentation
	 */
	private static final int CONNECTION_TIMEOUT = 2000, READ_TIMEOUT = 500, PROCESSING_TIMEOUT = 250;

	/**
	 * TODO Documentation
	 */
	private static final short MAX_ATTEMPTS = 3;

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
	public static JSONObject getJsonFromUrl(String url) {

		// Log the url that was passed as an argument for debugging purposes
		Log.d("getJsonFromUrl", url);

		// Create a new string builder for the Json value.
		StringBuilder jsonString = new StringBuilder();

		// Run the following on a new thread (because android hates networking on the UI thread.
		Thread t = new Thread(() -> {
			try {
				// Append the Json read from the URL to the string builder.
				jsonString.append(Network.readFromUrl(url));
			} catch (java.io.FileNotFoundException | java.net.SocketTimeoutException e) {
				// In the event that it took too long to process, throw a runtime exception
				// This will be cause by setUncaughtExceptionHandler();
				throw new RuntimeException();
			} catch (IOException e) {
				// If a different type of error occurred (IOException, print the stack trace instead)
				e.printStackTrace();
			}
		});

		// If a RuntimeException was thrown, it will be caught here.
		// Simply set the jsonString to a length of 0.
		// The validateJson will then interpret this as invalid, and will attempt to retry.
		t.setUncaughtExceptionHandler((t1, e) -> jsonString.setLength(0));

		// Set the name of the network thread, and start it.
		t.setName("Network thread");
		t.start();

		try {
			// All the thread to run for the combined time of the CONNECTION_TIMEOUT, READ_TIMEOUT,
			// and PROCESSING_TIMEOUT.
			t.join(Network.CONNECTION_TIMEOUT + Network.READ_TIMEOUT + Network.PROCESSING_TIMEOUT);

			// Run the jsonString and the url through the validateJson() method to make sure this doesn't need to be retried,
			// and return the newly formatted Json.
			return Network.validateJson(jsonString, url);
		} catch (InterruptedException e) {
			// If this got interrupted at all, simply print the stacktrace, and return a blank JSONObject.
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
		connection.setConnectTimeout(Network.CONNECTION_TIMEOUT);
		connection.setReadTimeout(Network.READ_TIMEOUT);

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
			if (Network.attempts < Network.MAX_ATTEMPTS) {

				// Sleep for a second to alleviate some stress from the receiving servers (in the event that this was the issue).
				try {
					Thread.sleep(500);
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}

				// Up the number of network attempts here, and log the current attempt number
				Network.attempts++;
				Log.w("validateJson", String.format("Url didn't respond, going to retry! (%d/%d)", Network.attempts, Network.MAX_ATTEMPTS));

				// Try to return the JsonObject from the new attempt.
				return Network.getJsonFromUrl(url);
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
