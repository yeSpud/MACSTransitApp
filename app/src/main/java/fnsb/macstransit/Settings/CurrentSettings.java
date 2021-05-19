package fnsb.macstransit.Settings;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;

/**
 * Created by Spud on 6/22/20 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.2.
 */
@SuppressWarnings("deprecation")
public class CurrentSettings {

	/**
	 * The most current implementation of the settings class.
	 * This is the class that you likely want to call when retrieving settings.
	 */
	public static BaseSettings settingsImplementation = new v2();

	/**
	 * Attempts to find the settings file on the device.
	 * This will try to search for the most current file first, and then retrieve older files.
	 * If the file is not able to be found then null is returned instead.
	 *
	 * @param context The app context (for accessing the folder location). This cannot be null.
	 * @return The settings file as a file object if found. Otherwise null.
	 */
	@Nullable
	public static File findSettingsFile(@NonNull Context context) {

		// Get the files directory for the app.
		File directory = context.getFilesDir();

		// Get a list of all the files in the directory.
		String[] files = directory.list();

		// Make sure there are files to iterate over.
		if (files != null && files.length != 0) {

			// Iterate through the files in the directory.
			for (String name : files) {
				Log.d("findSettingsFile", String.format("Checking file: %s", name));

				// Check if the name matches the current settings file.
				if (name.equals(CurrentSettings.settingsImplementation.FILENAME)) {

					// Since it matches, create a new file object using that name.
					Log.v("findSettingsFile", "Current file found!");
					return new File(directory, CurrentSettings.settingsImplementation.FILENAME);
				}

				// Check if the name matches an older settings file.
				v1 oldSettings = new v1();
				if (name.equals(oldSettings.FILENAME)) {

					// Since it matches the old file name, create a new file object using the name.
					Log.v("findSettingsFile", "Old file found!");
					return new File(directory, oldSettings.FILENAME);
				}
			}
		}

		// If the file was never found, or the files directory was blank, return null.
		Log.w("findSettingsFile", "No file was found");
		return null;
	}

	/**
	 * Loads the settings from the settings file into {@link #settingsImplementation}.
	 *
	 * @param context The app context for finding or recreating the settings file.
	 * @throws JSONException Thrown if there was an issue parsing the settings data.
	 */
	public static void loadSettings(Context context) throws JSONException {

		// First, find the file.
		File settingsFile = CurrentSettings.findSettingsFile(context);

		// If the file doesn't exist (the result is null), create a new file.
		if (settingsFile == null) {
			CurrentSettings.settingsImplementation.createSettingsFile(context);
		} else {

			// Make sure the settings file actually exists.
			if (settingsFile.exists()) {

				// Determine the settings version. If its an older version, convert it.
				if (settingsFile.getName().equals(CurrentSettings.settingsImplementation.FILENAME)) {

					// Load the settings from the settings file.
					JSONObject settingsValues = ((v2) CurrentSettings.settingsImplementation).readFromSettingsFile(context);
					if (settingsValues != null) {
						Log.d("loadSettings", String.format("Loading settings: %s", settingsValues.toString(4)));
						CurrentSettings.settingsImplementation.parseSettings(settingsValues);
					} else {
						Log.w("loadSettings", "Settings values are null!");
					}
				} else {

					// Convert the settings file, and then parse the result.
					JSONObject newSettings = CurrentSettings.convertSettings(settingsFile, context);
					Log.d("loadSettings", "Loading settings: " + newSettings.toString(4));
					CurrentSettings.settingsImplementation.parseSettings(newSettings);
				}
			} else {

				// Since the settings file does not exist create a new file.
				CurrentSettings.settingsImplementation.createSettingsFile(context);
			}
		}
	}

	/**
	 * Converts older settings to the new standard as a json object.
	 * This will also write the converted settings to the current settings file standard.
	 *
	 * @param oldFile The old settings file.
	 * @param context The app context (for creating a new current settings file).
	 * @return The converted settings as a json object.
	 */
	@NonNull
	public static JSONObject convertSettings(@NonNull File oldFile, @NonNull Context context) {

		// Check if the old file name is that of v1.
		v1 oldVersion = new v1();
		if (oldFile.getName().equals(oldVersion.FILENAME)) {

			// Load the old settings.
			Log.v("convertSettings", "Converting from v1");
			v1 oldSettings = new v1();
			String[] oldSettingsValues = oldSettings.readFromSettingsFile(context);
			if (oldSettingsValues != null) {
				oldSettings.parseSettings(oldSettingsValues);
			} else {
				Log.w("convertSettings", "Old settings values were null!");
			}

			try {
				// Carry over the old settings to the new format, and load in the defaults for unknown values.
				JSONObject newSettings = ((v2) CurrentSettings.settingsImplementation)
						.formatSettingsToJsonString(v1.ENABLE_TRAFFIC_VIEW, v1.DEFAULT_NIGHT_MODE,
								v1.SHOW_POLYLINES, v1.ENABLE_VR_OPTIONS, com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL);

				// Write those settings to the new settings file.
				CurrentSettings.settingsImplementation.writeSettingsToFile(newSettings.toString(), context);

				// Remove the old v1 file, and return the JSON object that was written to it.
				if (oldFile.delete()) {
					Log.v("convertSettings", "Old file deleted successfully!");
					return newSettings;
				} else {
					Log.w("convertSettings", "Old file was unable to be deleted");
					return new JSONObject();
				}
			} catch (JSONException e) {

				// If anything went wrong just return an empty JSON object after logging the exception.
				Log.e("convertSettings", "Could not parse json", e);
				e.printStackTrace();
				return new JSONObject();
			}
		} else {
			Log.w("convertSettings", "File version unknown!\n" + oldFile.getName());
			return new JSONObject();
		}
	}

	/**
	 * Reads the content of the file to a string.
	 * The returned string may be null if there was an exception thrown while reading,
	 * or if there is simply nothing to read.
	 *
	 * @param file The file to read from.
	 * @return The content of the file as a string. This may be null.
	 */
	@Nullable
	static String readFile(File file) {

		// Try to create a file input stream in order to read the data from the file.
		java.io.FileInputStream input;
		try {
			input = new FileInputStream(file);
		} catch (java.io.FileNotFoundException e) {
			Log.e("readFile", "No file found for path: " + file.getAbsolutePath());
			return null;
		}

		// If the file input stream was created successfully, execute the following:
		StringBuilder stringBuilder = new StringBuilder(0);
		try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(input,
				java.nio.charset.StandardCharsets.UTF_8))) {
			String line = reader.readLine();
			while (line != null) {
				stringBuilder.append(line).append("\n");
				line = reader.readLine();
			}
		} catch (java.io.IOException e) {

			// Error occurred when opening raw file for reading.
			Log.e("readFile", "Could not read from file!", e);
		}
		return stringBuilder.toString();
	}
}
