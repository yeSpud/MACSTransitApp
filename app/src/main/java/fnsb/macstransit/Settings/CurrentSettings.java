package fnsb.macstransit.Settings;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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
	 * @param context
	 * @return
	 */
	public static File findSettingsFile(Context context) {
		// TODO Find (potentially old) settings file and return it.

		return null;
	}

	/**
	 * TODO Documentation
	 * @param context
	 */
	public static void loadSettings(Context context) {
		// TODO Determine if settings file exists. If not, create new one. If old, convert.

		File settingsFile = CurrentSettings.findSettingsFile(context);
		if (settingsFile != null) {
			if (settingsFile.exists()) {
				// TODO Check if old or new
			} else {
				CurrentSettings.settings.createSettingsFile(context);
			}
		} else {
			CurrentSettings.settings.createSettingsFile(context);
		}

		try {
			CurrentSettings.settings.parseSettings(CurrentSettings.settings.readFromSettingsFile(context));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}


	/**
	 * TODO Documentation
	 * @param oldFile
	 * @param context
	 */
	public static void convertSettings(File oldFile, Context context) {
		// TODO Determine settings file version, and convert it.
		v1 oldSettings = new v1();
		oldSettings.readFromSettingsFile(context);
	}


	/**
	 * TODO Documentation
	 *
	 * @param file
	 * @return
	 */
	protected static String readFile(File file) {
		// Try to create a file input stream in order to read the data from the file.
		java.io.FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
		} catch (java.io.FileNotFoundException e) {
			Log.e("readFile", "No file found for path: " + file.getAbsolutePath());
			return null;
		}

		// If the file input stream was created successfully, execute the following:
		StringBuilder stringBuilder = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(fis,
				java.nio.charset.StandardCharsets.UTF_8))) {
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

}
