package fnsb.macstransit.Settings;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;

/**
 * Created by Spud on 6/19/20 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.2.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class v1 extends BaseSettings {

	/**
	 * Strings used to parse the key:value pairs in the old settings file.
	 */
	private static final String TRAFFIC_KEY = "Enable Traffic View",
			NIGHT_MODE_KEY = "Enable Dark Theme", POLYLINES_KEY = "Show Polylines",
			VR_KEY = "Show VR Options";

	/**
	 * Booleans used by the application to determine what settings should be enabled during initialization.
	 */
	public static boolean ENABLE_TRAFFIC_VIEW, DEFAULT_NIGHT_MODE, SHOW_POLYLINES, ENABLE_VR_OPTIONS;


	/**
	 * Constructor for v1.
	 * Because this class extends BaseSettings, the file and version need to be passed.
	 */
	public v1() {
		super("settings.txt", 1);
	}

	/**
	 * Reads a string array from the settings file.
	 * The array is determined by splitting the content of the settings file on the new line.
	 *
	 * @param file The settings file.
	 * @return The string array read from the settings file.
	 */
	@Override
	@androidx.annotation.Nullable
	public String[] readFromSettingsFile(File file) {

		// Read the file content
		String content = CurrentSettings.readFile(file);
		Log.d("readFromSettingsFile", "Content: " + content);

		// If the content of the file isn't null, then return the array of strings that was split
		// along the new line
		if (content != null) {
			return content.split("\n");
		} else {
			return null;
		}
	}

	/**
	 * Reads a string array from the settings file.
	 * The array is determined by splitting the content of the settings file on the new line.
	 *
	 * @param context The context to get the settings file by.
	 * @return The string array read from the settings file.
	 */
	@Override
	public String[] readFromSettingsFile(@NonNull android.content.Context context) {

		// First get the settings file.
		File file = new File(context.getFilesDir(), this.FILENAME);
		Log.i("readFromSettingsFile", "Supposed file location: " + file.getAbsolutePath());

		// Return the string array from the file determined by the context.
		return this.readFromSettingsFile(file);
	}

	/**
	 * Parses the settings from the settings file into booleans based on the corresponding key(s).
	 *
	 * @param strings The string array containing the various key:value pairs read from the settings file.
	 */
	@Override
	public void parseSettings(@NonNull String[] strings) {

		// Parse the settings into the static global variables above.
		for (String string : strings) {
			String[] line = string.split(":");

			// Check what the first line is (to see if its an important key).
			switch (line[0]) {
				case v1.TRAFFIC_KEY:
					Log.d("loadSettings", "Updating traffic view setting");
					v1.ENABLE_TRAFFIC_VIEW = Boolean.parseBoolean(line[1]);
					break;
				case v1.NIGHT_MODE_KEY:
					Log.d("loadSettings", "Updating dark mode setting");
					v1.DEFAULT_NIGHT_MODE = Boolean.parseBoolean(line[1]);
					break;
				case v1.POLYLINES_KEY:
					Log.d("loadSettings", "Updating polyline setting");
					v1.SHOW_POLYLINES = Boolean.parseBoolean(line[1]);
					break;
				case v1.VR_KEY:
					Log.d("loadSettings", "Updating VR setting");
					v1.ENABLE_VR_OPTIONS = Boolean.parseBoolean(line[1]);
					break;
				default:
					Log.w("loadSettings", String.format("Line unaccounted for!\n%s", string));
					break;
			}
		}
	}

	/**
	 * Writes the provided string to the settings file. Since this version is deprecated just return.
	 *
	 * @param string  The string to be written to the settings file.
	 * @param context The app context (for determining where the file is).
	 */
	@Override
	public void writeSettingsToFile(String string, Context context) {
	}

	/**
	 * Creates a new settings file with default values. Since this version is deprecated just return.
	 *
	 * @param context The app context used to determine the file location.
	 */
	@Override
	public void createSettingsFile(Context context) {
	}
}
