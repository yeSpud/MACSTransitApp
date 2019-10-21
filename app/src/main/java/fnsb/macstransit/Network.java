package fnsb.macstransit;

import android.util.Log;

import org.json.JSONException;
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
	 * TODO Documentation
	 */
	private static int retryattempts = 0;


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
				jsonString.append(Network.readAll(bufferedReader));

				bufferedReader.close();
				inputStream.close();

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
			t.join(4000);
			return Network.validateJson(jsonString, url);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return new JSONObject();
		}
	}

	/**
	 * TODO Documentation
	 *
	 * @param string
	 * @param url
	 * @return
	 */
	private static JSONObject validateJson(StringBuilder string, String url) {
		if (string.length() == 0) {
			if (Network.retryattempts < 3) {
				// Keep trying!
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
				Network.retryattempts++;
				android.util.Log.w("readJsonFromUrl", String.format("Page didn't respond, going to retry! (%d/3)", Network.retryattempts));
				return Network.readJsonFromUrl(url);
			} else {
				Network.retryattempts = 0;
				return new JSONObject();
			}
		} else {
			Network.retryattempts = 0;
			try {
				// Create and return a new JSONObject from the jsonString variable
				return new JSONObject(string.toString());
			} catch (JSONException e) {
				e.printStackTrace();
				return new JSONObject();
			}
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

}
