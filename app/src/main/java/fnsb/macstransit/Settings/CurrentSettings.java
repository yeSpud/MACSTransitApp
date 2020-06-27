package fnsb.macstransit.Settings;

import android.content.Context;
import android.util.Log;

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
 * @version 1.0
 * @since Release 1.2.
 */
public class CurrentSettings {

	/**
	 * TODO Documentation
	 */
	public static v2 settings = new v2();

	/**
	 * TODO Documentation
	 *
	 * @param context
	 * @return
	 */
	public static File findSettingsFile(Context context) {
		// Get the files directory for the app.
		File directory = context.getFilesDir();

		// Get a list of all the files in the directory.
		String[] files = directory.list();

		// Make sure there are files to iterate over.
		if (files != null && files.length != 0) {
			// Iterate through the files in the directory.
			for (String name : files) {
				Log.d("findSettingsFile", "Checking file: " + name);

				// Check if the name matches the current settings file.
				if (name.equals(v2.FILENAME)) {
					// Since it matches, create a new file object using that name.
					Log.v("findSettingsFile", "Current file found!");
					return new File(directory, v2.FILENAME);
				}

				// Check if the name matches an older settings file.
				if (name.equals(v1.FILENAME)) {
					// Since it matches the old file name, create a new file object using the name.
					Log.v("findSettingsFile", "Old file found!");
					return new File(directory, v1.FILENAME);
				}

			}
		}

		// If the file was never found, or the files directory was blank, return null.
		Log.w("findSettingsFile", "No file was found");
		return null;
	}

	/**
	 * TODO Documentation
	 *
	 * @param context
	 * @throws JSONException
	 */
	public static void loadSettings(Context context) throws JSONException {
		// First, find the file.
		File settingsFile = CurrentSettings.findSettingsFile(context);

		// If the file doesn't exist (the result is null), create a new file.
		if (settingsFile == null) {
			CurrentSettings.settings.createSettingsFile(context);
		} else {
			// Make sure the settings file actually exists.
			if (settingsFile.exists()) {
				// Determine the settings version. If its an older version, convert it.
				if (!settingsFile.getName().equals(v2.FILENAME)) {
					// Convert the settings file, and then parse the result.
					JSONObject newSettings = CurrentSettings.convertSettings(settingsFile, context);
					Log.d("loadSettings", "Loading settings: " + newSettings.toString(4));
					CurrentSettings.settings.parseSettings(newSettings);
				} else {
					// Load the settings from the settings file.
					JSONObject settingsValues = CurrentSettings.settings.readFromSettingsFile(context);
					Log.d("loadSettings", "Loading settings: " + settingsValues.toString(4));
					CurrentSettings.settings.parseSettings(settingsValues);
				}
			} else {
				CurrentSettings.settings.createSettingsFile(context);
			}

		}
	}


	/**
	 * TODO Documentation
	 *
	 * @param oldFile
	 * @param context
	 * @return
	 */
	public static JSONObject convertSettings(File oldFile, Context context) {
		// Check if the old file name is that of v1
		if (oldFile.getName().equals(v1.FILENAME)) {
			// Load the old settings
			Log.v("convertSettings", "Converting from v1");
			v1 oldSettings = new v1();
			String[] oldSettingsValues = oldSettings.readFromSettingsFile(context);
			oldSettings.parseSettings(oldSettingsValues);

			try {
				// Carry over the old settings to the new format, and load in the defaults for unknown values.
				JSONObject newSettings = CurrentSettings.settings.formatSettingsToJsonString(v1.ENABLE_TRAFFIC_VIEW,
						v1.DEFAULT_NIGHT_MODE, v1.SHOW_POLYLINES, v1.ENABLE_VR_OPTIONS,
						com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL);

				// Write those settings to the new settings file
				CurrentSettings.settings.writeStringToFile(newSettings.toString(), context);

				// Remove the old v1 file, and return the JSON object that was written to it.
				if (oldFile.delete()) {
					Log.v("convertSettings", "Old file deleted successfully!");
					return newSettings;
				} else {
					Log.w("convertSettings", "Old file was unable to be deleted");
					return new JSONObject();
				}
			} catch (JSONException e) {
				// If anything went wrong, just return an empty JSON object
				e.printStackTrace();
				return new JSONObject();
			}
		} else {
			Log.w("convertSettings", "File version unknown!\n"+oldFile.getName());
			return new JSONObject();
		}
	}


	/**
	 * TODO Documentation
	 *
	 * @param file
	 * @return
	 */
	static String readFile(File file) {
		// Try to create a file input stream in order to read the data from the file.
		java.io.FileInputStream input = null;
		try {
			input = new FileInputStream(file);
		} catch (java.io.FileNotFoundException e) {
			Log.e("readFile", "No file found for path: " + file.getAbsolutePath());
			return null;
		}

		// If the file input stream was created successfully, execute the following:
		StringBuilder stringBuilder = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(input,
				java.nio.charset.StandardCharsets.UTF_8))) {
			String line = reader.readLine();
			while (line != null) {
				stringBuilder.append(line).append("\n");
				line = reader.readLine();
			}
		} catch (java.io.IOException e) {
			// Error occurred when opening raw file for reading.
			e.printStackTrace();
		}
		return stringBuilder.toString();
	}

}
