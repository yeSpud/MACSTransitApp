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
 * @version 1.2
 * @since Beta 6.
 */
public class Network {

	/**
	 * Timeouts (in milliseconds) used by various methods.
	 */
	private static final int CONNECTION_TIMEOUT = 5000, READ_TIMEOUT = 5000, PROCESSING_TIMEOUT = 500;

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
		Log.i("getJsonFromUrl", "Url provided: " + url);

		// Create a StringBuilder object that can be accessed within the thread.
		// This will later be used to create the returned JSONObject.
		final StringBuilder jsonString = new StringBuilder();

		// Run the following on a new thread (because android hates networking on the UI thread).
		Thread t = new Thread(() -> {
			try {
				// Get the read result from the network thread.
				String result = Network.readFromUrl(url, useTimeouts);
				Log.v("getJsonFromUrl", "Returned string: " + result);

				// Append the resulting string to the StringBuilder.
				jsonString.append(result);
			} catch (SocketTimeoutException e) {
				// If the socket timed out while trying to get data from the url,
				// simply return from the thread.
				Log.w("getJsonFromUrl", "Connection timed out");
			}
		});

		// Set the name of the network thread, and start it.
		t.setName("Network thread");
		t.start();

		try {
			// Make sure the thread waits for the appropriate amount of time before continuing.
			t.join(useTimeouts ? Network.CONNECTION_TIMEOUT + Network.READ_TIMEOUT + Network.PROCESSING_TIMEOUT : 0);
		} catch (InterruptedException e) {
			// If this got interrupted at all, simply print the stacktrace.
			e.printStackTrace();
		}

		// Check if the string builder is empty.
		if (jsonString.length() == 0) {
			// Since its empty, return a new Json object.
			return new JSONObject();
		}

		try {
			// Try to create a new Json object using the string builder, and return the resulting Json object.
			return new JSONObject(jsonString.toString());
		} catch (org.json.JSONException e) {
			// Log if the Json object couldn't be created, and return an empty object.
			Log.e("getJsonFromUrl", "Couldn't convert string to JSON", e);
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
	 * @return The string containing the Json string, which can then be parsed into a JSONObject.
	 * @throws SocketTimeoutException Thrown if the specified timeout has been reached.
	 */
	private static String readFromUrl(String url, boolean useTimeouts) throws SocketTimeoutException {

		// Try connecting to the provided url.
		java.net.URLConnection connection;
		try {
			connection = new java.net.URL(url).openConnection();
		} catch (IOException e) {
			// If the connection was unsuccessful, log it and return an empty string.
			Log.e("readFromUrl", "Could not connect to provided URL: " + url);
			return "";
		}

		// Add timeouts for the connection.
		connection.setConnectTimeout(useTimeouts ? Network.CONNECTION_TIMEOUT : 0);
		connection.setReadTimeout(useTimeouts ? Network.READ_TIMEOUT : 0);

		// Try to get the input stream from the connection.
		java.io.InputStream inputStream;
		try {
			inputStream = connection.getInputStream();
		} catch (IOException e) {
			// If there was an exception thrown simply log it and return an empty string.
			Log.e("readFromUrl", "Could not get an input stream from the connection");
			return "";
		}

		// Create a buffered reader for the input stream,
		BufferedReader bufferedReader = new BufferedReader(new java.io.InputStreamReader(inputStream,
				java.nio.charset.StandardCharsets.UTF_8));

		// Store the inputted text into a string variable,
		String output = Network.readAll(bufferedReader);


		// Try to close the buffered reader.
		try {
			bufferedReader.close();
		} catch (IOException e) {
			// If there was an exception thrown while closing the reader, log it.
			Log.e("readFromUrl", "Could not close buffered reader", e);
		}

		// Try to close the input stream.
		try {
			inputStream.close();
		} catch (IOException e) {
			// If there was an exception thrown while closing the stream, log it.
			Log.e("readFromUrl", "Could not close input stream", e);
		}

		// Return the output string which should contain the json.
		// It can be parsed in a different method in the event that its malformed,
		// and can thus be handled better.
		return output;
	}

	/**
	 * Parse characters from a reader or buffered reader into a character stream. Once parsed,
	 * the character stream will be converted into a string and then returned.
	 *
	 * @param reader The Reader or BufferedReader object that will be used to read the character stream.
	 * @return The final string from the String builder containing what was read by the Reader.
	 */
	private static String readAll(java.io.Reader reader) {
		// Create a string to store what is read by the reader
		StringBuilder string = new StringBuilder();

		// Create a variable for character parsing
		int character;

		// Try continuously reading characters from the reader until there is either an exception,
		// or -1 is returned.
		try {
			// Make sure there are still characters to read from the reader.
			while ((character = reader.read()) != -1) {
				// Append the character to the string
				string.append((char) character);
			}
		} catch (IOException e) {
			// Log if an exception was thrown.
			Log.e("readAll", "An exception was thrown while reading from the reader", e);
		}

		// Finally return the string
		return string.toString();
	}

}
