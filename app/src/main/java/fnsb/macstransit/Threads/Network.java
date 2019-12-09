package fnsb.macstransit.Threads;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * Created by Spud on 2019-10-21 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 * <p>
 * <i><b>Yo dawg. I heard you like static methods. So I got you some static methods,
 * so you could static method while you static method :D</b></i>
 * <p>
 * <p>
 * <i>Ok, I've had my fun.</i>
 * <p>
 * But in all seriousness, all of these methods are static, but only one is public,
 * and is to be used outside this class as a result.
 * The reason why all the other methods exist is simply for more easy of use within the primary method.
 * The reason for this is because this class is used to actually connect to a server via the provided url.
 * Since android doesn't like url connections to be run on the UI thread,
 * this needs to be done on its own thread.
 * Hence the complexity :(
 *
 * @version 1.1
 * @since Beta 6.
 */
public class Network {

	/**
	 * Timeouts (in milliseconds) used by various methods.
	 */
	private static final int CONNECTION_TIMEOUT = 5000, READ_TIMEOUT = 5000, PROCESSING_TIMEOUT = 500;

	/**
	 * The maximum number of retries allowed for the method, stored as a short
	 * (as it's not meant to be a large number).
	 */
	private static final short MAX_ATTEMPTS = 3;

	/**
	 * Create a private variable that will track the number of attempts made to connect.
	 * This needs to remain private as it should only be accessed and used in this class.
	 */
	private static int attempts = 0;

	/**
	 * Reads the JSON from the provided URL, and formats it into a JSONObject.
	 * This may be an empty JSONObject if the page times out and the method can no longer retry,
	 * or if any other error occurs.
	 *
	 * @param url         The URL to retrieve the JSON data from.
	 * @param useTimeouts Whether or not to use the builtin timeouts for this method.
	 * @return The JSONObject containing the data, or an empty JSONObject if there was an error,
	 * or the page timed out.
	 */
	public static JSONObject getJsonFromUrl(String url, boolean useTimeouts) {

		// Log the url that was passed as an argument for debugging purposes
		Log.d("getJsonFromUrl", url);

		// Create a new string builder for the Json value.
		StringBuilder jsonString = new StringBuilder();

		// Run the following on a new thread (because android hates networking on the UI thread.
		Thread t = new Thread(() -> {
			try {
				// Append the Json read from the URL to the string builder.
				jsonString.append(Network.readFromUrl(url, useTimeouts));
			} catch (java.io.FileNotFoundException | SocketTimeoutException e) {
				// In the event that it took too long to process, throw a runtime exception
				// This will be cause by setUncaughtExceptionHandler();
				throw new RuntimeException();
			} catch (IOException e) {
				// If a different type of error occurred (IOException, print the stack trace instead).
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
			t.join(useTimeouts ? Network.CONNECTION_TIMEOUT + Network.READ_TIMEOUT + Network.PROCESSING_TIMEOUT : 0);

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
	 * Reads the data from the url and returns a string containing said data.
	 * A connection must be established withing the time determined by {@code CONNECTION_TIMEOUT},
	 * and must read within the time determined by {@code READ_TIMEOUT},
	 * otherwise it will throw a {@code SocketTimeoutException}.
	 *
	 * @param url         The url to read from.
	 * @param useTimeouts Whether or not to use the builtin timeouts for this method.
	 * @return The string (hopefully) containing the Json data, which can then be parsed into a JSONObject.
	 * @throws IOException            Thrown if there is an issue with the connection, buffered reader, or input stream.
	 * @throws SocketTimeoutException Thrown if the connection time surpasses the allotted time in the connection timeout.
	 *                                Same with the read timeout.
	 */
	private static String readFromUrl(String url, boolean useTimeouts) throws IOException, SocketTimeoutException {

		// Specify the URL connection, and try to open a connection. If unsuccessful, just return null.
		java.net.URLConnection connection;
		try {
			connection = new java.net.URL(url).openConnection();
		} catch (java.net.MalformedURLException e) {

			// If the url provided was malformed, simply print a stacktrace, and return a null string.
			e.printStackTrace();
			return null;
		}

		// Since we made it this far (didn't return null). meaning connection was established, log that in the debugger.
		Log.d("readFromUrl", "Connection established");

		if (useTimeouts) {
			// Add timeouts for the connection (1 second to connect, 1 second to read, 2 seconds total)
			connection.setConnectTimeout(Network.CONNECTION_TIMEOUT);
			connection.setReadTimeout(Network.READ_TIMEOUT);
		} else {
			connection.setConnectTimeout(0);
			connection.setReadTimeout(0);
		}

		// Get the input stream from the connection
		java.io.InputStream inputStream = connection.getInputStream();

		// Create a buffered reader for the input stream
		BufferedReader bufferedReader = new BufferedReader(new java.io.InputStreamReader(inputStream,
				java.nio.charset.StandardCharsets.UTF_8));

		// Store the inputted text into a string variable
		String output = Network.readAll(bufferedReader);

		// Close the reader and input stream (also log this to the debugger).
		Log.d("readFromUrl", "Closing reader and stream");
		bufferedReader.close();
		inputStream.close();

		// Return the output string which should contain the json.
		// It can be parsed in a different method in the event that its malformed, and can thus be handled better.
		return output;
	}

	/**
	 * This method tried to validate the Json that is provided as a StringBuilder object
	 * by first checking if its not of length 0,
	 * and then by attempting to parse it to a JSONObject.
	 *
	 * @param stringBuilder The StringBuilder that will be validated.
	 *                      Mainly used for checking if the length is 0.
	 *                      If it's not the StringBuilder is then attempted to be parsed into a JSONObject.
	 * @param url           The url in the event that this needs to be retried.
	 * @return The JSONObject containing the parsed data from the string,
	 * or an empty JSONObject if there was an error while parsing.
	 */
	private static JSONObject validateJson(StringBuilder stringBuilder, String url) {

		// Check if the string builder object is empty (has a length of 0). If it does,
		// then that means that there was no JSON returned from the URL (likely due to a connection error),
		// so retry a maximum of 3 times.
		if (stringBuilder.length() == 0) {

			// If there have been less than 3 retries, keep retrying.
			if (Network.attempts < Network.MAX_ATTEMPTS) {

				// Sleep for a second to alleviate some stress from the receiving servers
				// (in the event that this was the issue).
				try {
					Thread.sleep(500);
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}

				// Up the number of network attempts here, and log the current attempt number
				Network.attempts++;
				Log.w("validateJson", String.format("Url didn't respond, going to retry! (%d/%d)",
						Network.attempts, Network.MAX_ATTEMPTS));

				// Try to return the JsonObject from the new attempt.
				return Network.getJsonFromUrl(url, true);
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
				return new JSONObject(stringBuilder.toString());
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
	 * @throws IOException Thrown if there is an error while reading from the reader.
	 */
	private static String readAll(java.io.Reader reader) throws IOException {

		Log.d("readAll", "Reading from stream...");

		// Create a string to store what is read by the reader
		StringBuilder string = new StringBuilder();

		// Create a variable for character parsing
		int character;

		// Loop through all the characters until there are no more characters to run through
		// (returns -1).
		while ((character = reader.read()) != -1) {

			// Append the character to the string
			string.append((char) character);
		}

		// Finally return the string
		return string.toString();
	}

}
