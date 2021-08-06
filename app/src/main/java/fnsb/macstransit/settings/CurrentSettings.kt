package fnsb.macstransit.settings

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.nio.charset.StandardCharsets

/**
 * Created by Spud on 6/22/20 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Release 1.2.
 */
object CurrentSettings {

	/**
	 * The most current implementation of the settings class.
	 * This is the class that you likely want to call when retrieving settings.
	 */
	val settingsImplementation: BaseSettings<JSONObject> = V2

	/**
	 * Attempts to find the settings file on the device.
	 * This will try to search for the most current file first, and then retrieve older files.
	 * If the file is not able to be found then null is returned instead.
	 *
	 * @param context The app context (for accessing the folder location). This cannot be null.
	 * @return The settings file as a file object if found. Otherwise null.
	 */
	private fun findSettingsFile(context: Context): File? {

		// Get the files directory for the app.
		val directory = context.filesDir

		// Get a list of all the files in the directory.
		val files = directory.list()

		// Make sure there are files to iterate over.
		if (files != null && files.isNotEmpty()) {

			// Iterate through the files in the directory.
			for (name in files) {
				Log.d("findSettingsFile", "Checking file: $name")

				// Check if the name matches the current settings file.
				if (name == settingsImplementation.FILENAME) {

					// Since it matches, create a new file object using that name.
					Log.v("findSettingsFile", "Current file found!")
					return File(directory, settingsImplementation.FILENAME)
				}

				// Check if the name matches an older settings file.
				val oldSettings = V1
				if (name == oldSettings.FILENAME) {

					// Since it matches the old file name, create a new file object using the name.
					Log.v("findSettingsFile", "Old file found!")
					return File(directory, oldSettings.FILENAME)
				}
			}
		}

		// If the file was never found, or the files directory was blank, return null.
		Log.w("findSettingsFile", "No file was found")
		return null
	}

	/**
	 * Loads the settings from the settings file into [.settingsImplementation].
	 *
	 * @param context The app context for finding or recreating the settings file.
	 * @throws JSONException Thrown if there was an issue parsing the settings data.
	 */
	@Throws(JSONException::class)
	fun loadSettings(context: Context) {

		// First, find the file.
		val settingsFile = findSettingsFile(context)

		// If the file doesn't exist (the result is null), create a new file.
		if (settingsFile == null) {
			settingsImplementation.createSettingsFile(context)
		} else {

			// Make sure the settings file actually exists.
			if (settingsFile.exists()) {

				// Determine the settings version. If its an older version, convert it.
				if (settingsFile.name == settingsImplementation.FILENAME) {

					// Load the settings from the settings file.
					val settingsValues = (settingsImplementation as V2).readFromSettingsFile(context)
					Log.d("loadSettings", "Loading settings: ${settingsValues.toString(4)}")
					settingsImplementation.parseSettings(settingsValues)
				} else {

					// Convert the settings file, and then parse the result.
					val newSettings = convertSettings(settingsFile, context)
					Log.d("loadSettings", "Loading settings: ${newSettings.toString(4)}")
					settingsImplementation.parseSettings(newSettings)
				}
			} else {

				// Since the settings file does not exist create a new file.
				settingsImplementation.createSettingsFile(context)
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
	private fun convertSettings(oldFile: File, context: Context): JSONObject {

		// Check if the old file name is that of v1.
		val oldVersion = V1
		return if (oldFile.name == oldVersion.FILENAME) {

			// Load the old settings.
			Log.v("convertSettings", "Converting from v1")
			val oldSettings = V1
			val oldSettingsValues = oldSettings.readFromSettingsFile(context)
			oldSettings.parseSettings(oldSettingsValues)
			try {
				// Carry over the old settings to the new format, and load in the defaults for unknown values.
				val newSettings = (settingsImplementation as V2).formatSettingsToJsonString(V1.ENABLE_TRAFFIC_VIEW,
						V1.DEFAULT_NIGHT_MODE, V1.SHOW_POLYLINES, V1.ENABLE_VR_OPTIONS, GoogleMap.MAP_TYPE_NORMAL)

				// Write those settings to the new settings file.
				settingsImplementation.writeSettingsToFile(newSettings.toString(), context)

				// Remove the old v1 file, and return the JSON object that was written to it.
				if (oldFile.delete()) {
					Log.v("convertSettings", "Old file deleted successfully!")
					newSettings
				} else {
					Log.w("convertSettings", "Old file was unable to be deleted")
					JSONObject()
				}
			} catch (e: JSONException) {

				// If anything went wrong just return an empty JSON object after logging the exception.
				Log.e("convertSettings", "Could not parse json", e)
				e.printStackTrace()
				JSONObject()
			}
		} else {
			Log.w("convertSettings", "File version unknown! ${oldFile.name}")
			JSONObject()
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
	fun readFile(file: File): String {

		// TODO
		val stringBuilder = StringBuilder(0)

		// Try to create a file input stream in order to read the data from the file.
		val input: FileInputStream = try {
			FileInputStream(file)
		} catch (e: FileNotFoundException) {
			Log.e("readFile", "No file found for path: " + file.absolutePath)
			return stringBuilder.toString()
		}

		// If the file input stream was created successfully, execute the following:
		try {
			BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
				var line = reader.readLine()
				while (line != null) {
					stringBuilder.append(line).append("\n")
					line = reader.readLine()
				}
			}
		} catch (e: IOException) {

			// Error occurred when opening raw file for reading.
			Log.e("readFile", "Could not read from file!", e)
		}
		return stringBuilder.toString()
	}
}