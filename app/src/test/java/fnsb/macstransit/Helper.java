package fnsb.macstransit;

import android.util.Log;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
	private static final String ROOT = "src/test/java/fnsb/macstransit/testfiles/";

	/**
	 * TODO Documentation
	 */
	static final File ALL_VEHICLES_JSON = new File(Helper.ROOT + "all vehicles.json"),
			ALL_VEHICLES_EMPTY_JSON = new File(Helper.ROOT + "all vehicles empty.json"),
			MASTERROUTE_JSON = new File(Helper.ROOT + "masterRoute.json"),
			OLD_SETTINGS_TXT = new File(Helper.ROOT + "old settings.txt"),
			SETTINGS_JSON = new File(Helper.ROOT + "settings.json"),
			BLUE_STOPS = new File(Helper.ROOT + "stops/Blue.json"),
			BROWN_STOPS = new File(Helper.ROOT + "stops/Brown.json"),
			GOLD_STOPS = new File(Helper.ROOT + "stops/Gold.json"),
			GREEN_STOPS = new File(Helper.ROOT + "stops/Green.json"),
			PURPLE_STOPS = new File(Helper.ROOT + "stops/Purple.json"),
			RED_STOPS = new File(Helper.ROOT + "stops/Red.json"),
			YELLOW_STOPS = new File(Helper.ROOT + "stops/Yellow.json");

	/**
	 * TODO Documentation and comments
	 * @param file
	 * @return
	 */
	static @Nullable String getText(File file) {
		// Try to create a file input stream in order to read the data from the file.
		FileInputStream input;
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
	static @NotNull JSONObject getJSON(File file) throws JSONException{
		String text = Helper.getText(file);

		if (text == null) {
			throw new JSONException("Text is null");
		}

		return new JSONObject(text);
	}

	/**
	 * TODO Documentation
	 * @param startTime
	 * @param endTime
	 */
	static void printTime(long startTime, long endTime) {
		long total = endTime - startTime;
		double microseconds = 0, milliseconds = 0, seconds = 0;

		if ((total / 1000) >= 1) {
			microseconds = (total / 1000d);
		}

		if ((microseconds / 1000 >= 1)) {
			milliseconds = ((total /1000d) / 1000d);
			microseconds = 0;
		}

		if ((milliseconds / 1000) >= 1) {
			seconds = (((total /1000d) / 1000d) / 1000d);
			microseconds = 0;
		}

		System.out.print("Time taken to execute: ");
		if (seconds != 0) {
			System.out.println(String.format("%f seconds\t(%d nanoseconds)", seconds, total));
		} else if (milliseconds != 0) {
			System.out.println(String.format("%f milliseconds\t(%d nanoseconds)", milliseconds, total));
		} else if (microseconds != 0) {
			System.out.println(String.format("%f microseconds\t(%d nanoseconds)", microseconds, total));
		} else {
			System.out.println(total + " nanoseconds");
		}
	}
}
