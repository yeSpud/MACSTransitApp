package fnsb.macstransit.Settings;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONException;

/**
 * Created by Spud on 3/13/21 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.2.
 */
public abstract class BaseSettings {

	/**
	 * The name of the settings file.
	 */
	public final String FILENAME;

	/**
	 * The version of the settings.
	 */
	public final int VERSION;

	/**
	 * Constructor for BaseSettings class.
	 * All that's strictly requires is the filename and version of the settings file.
	 *
	 * @param filename The file name of the settings file.
	 * @param version  The version of the settings file.
	 */
	public BaseSettings(String filename, int version) {
		this.FILENAME = filename;
		this.VERSION = version;
	}

	/**
	 * Writes the provided string to the settings file.
	 *
	 * @param string  The string to be written to the settings file.
	 * @param context The app context (for determining where the file is).
	 */
	public abstract void writeSettingsToFile(String string, Context context);

	/**
	 * Creates a new settings file with default values.
	 *
	 * @param context The app context used to determine the file location.
	 */
	public abstract void createSettingsFile(Context context);

	/**
	 * Parses the settings from the settings file into booleans based on the corresponding key(s).
	 * <p>
	 * In the overridden version this should actually execute instructions instead of just returning.
	 *
	 * @param strings The string array containing the various key:value pairs read from the settings file.
	 */
	public void parseSettings(String[] strings) {
		Log.w("BaseSettings", "This should have been overridden!");
	}

	/**
	 * Parses settings to be applied to the static variables from the provided JSON object.
	 * <p>
	 * In the overridden version this should actually execute instructions instead of just returning.
	 *
	 * @param jsonObject The JSON object containing the values to be parsed into settings.
	 * @throws JSONException Thrown if there was an issue with parsing any values.
	 */
	public void parseSettings(org.json.JSONObject jsonObject) throws JSONException {
		Log.w("BaseSettings", "This should have been overridden!");
	}

	/**
	 * Reads the object from the settings file.
	 * <p>
	 * In the overridden version the object should be specified.
	 *
	 * @param file The settings file.
	 * @return The object read from the settings file.
	 */
	@Nullable
	public Object readFromSettingsFile(java.io.File file) {
		Log.w("BaseSettings", "This should have been overridden!");
		return null;
	}

	/**
	 * Reads the object from the settings file.
	 * <p>
	 * In the overridden version the object should be specified.
	 *
	 * @param context The context to get the settings file by.
	 * @return The object read from the settings file.
	 */
	@Nullable
	public Object readFromSettingsFile(Context context) {
		Log.w("BaseSettings", "This should have been overridden!");
		return null;
	}
}
