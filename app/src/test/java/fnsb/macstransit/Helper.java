package fnsb.macstransit;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Created by Spud on 6/27/20 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0
 * @since Release 1.2.
 */
class Helper {

	/**
	 * TODO Documentation
	 */
	static final File ALL_VEHICLES_JSON = new File("src/test/java/fnsb/macstransit/testfiles/all vehicles.json"),
			ALL_VEHICLES_EMPTY_JSON = new File("src/test/java/fnsb/macstransit/testfiles/all vehicles empty.json"),
			MASTERROUTE_JSON = new File("src/test/java/fnsb/macstransit/testfiles/masterRoute.json"),
			OLD_SETTINGS_TXT = new File("src/test/java/fnsb/macstransit/testfiles/old settings.txt"),
			SETTINGS_JSON = new File("src/test/java/fnsb/macstransit/testfiles/settings.json");

	/**
	 * TODO Documentation and comments
	 * @param file
	 * @return
	 */
	static String getText(File file) {
		// Try to create a file input stream in order to read the data from the file.
		FileInputStream input = null;
		try {
			input = new FileInputStream(file);
		} catch (java.io.FileNotFoundException e) {
			Log.e("readFile", "No file found for path: " + file.getAbsolutePath());
			return null;
		}

		// If the file input stream was created successfully, execute the following:
		StringBuilder stringBuilder = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line = reader.readLine();
			while (line != null) {
				stringBuilder.append(line).append("\n");
				line = reader.readLine();
			}
		} catch (IOException e) {
			// Error occurred when opening raw file for reading.
			e.printStackTrace();
		}
		return stringBuilder.toString();
	}

	/**
	 * TODO Documentation and comments
	 * @param file
	 * @return
	 * @throws JSONException
	 */
	static JSONObject getJSON(File file) throws JSONException{
		String text = Helper.getText(file);

		if (text == null) {
			throw new JSONException("Text is null");
		}

		return new JSONObject(text);
	}

}
