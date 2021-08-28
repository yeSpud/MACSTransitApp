package fnsb.macstransit.settings

import android.content.Context
import android.util.Log
import fnsb.macstransit.settings.CurrentSettings.readFile
import java.io.File

/**
 * Created by Spud on 6/19/20 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Release 1.2.
 */
@Deprecated("V1 is no longer supported")
object V1 : BaseSettings<Collection<String>>("settings.txt", 1) {

	/**
	 * Strings used to parse the key:value pairs in the old settings file.
	 */
	private const val TRAFFIC_KEY = "Enable Traffic View"
	private const val NIGHT_MODE_KEY = "Enable Dark Theme"
	private const val POLYLINES_KEY = "Show Polylines"
	private const val VR_KEY = "Show VR Options"

	/**
	 * Booleans used by the application to determine what settings should be enabled during initialization.
	 */
	var ENABLE_TRAFFIC_VIEW = false
	private set

	var DEFAULT_NIGHT_MODE = false
	private set

	var SHOW_POLYLINES = false
	private set

	var ENABLE_VR_OPTIONS = false
	private set

	/**
	 * Reads a string array from the settings file.
	 * The array is determined by splitting the content of the settings file on the new line.
	 *
	 * @param file The settings file.
	 * @return The string array read from the settings file.
	 */
	override fun readFromSettingsFile(file: File): Collection<String> {

		// Read the file content
		val content = readFile(file)
		Log.d("readFromSettingsFile", "Content: $content")

		// If the content of the file isn't null or empty then return the array of strings that was split
		// along the new line.
		val strings: MutableList<String> = content.split("\n").toMutableList()
		strings.remove("")
		return strings
	}

	/**
	 * Reads a string array from the settings file.
	 * The array is determined by splitting the content of the settings file on the new line.
	 *
	 * @param context The context to get the settings file by.
	 * @return The string array read from the settings file.
	 */
	fun readFromSettingsFile(context: Context): Collection<String> {

		// First get the settings file.
		val file = File(context.filesDir, FILENAME)
		Log.i("readFromSettingsFile", "Supposed file location: " + file.absolutePath)

		// Return the string array from the file determined by the context.
		return this.readFromSettingsFile(file)
	}

	/**
	 * Parses the settings from the settings file into booleans based on the corresponding key(s).
	 *
	 * @param input The string array containing the various key:value pairs read from the settings file.
	 */
	override fun parseSettings(input: Collection<String>) {

		// Parse the settings into the static global variables above.
		for (string in input) {
			val line = string.split(":")
			when (line[0]) {
				TRAFFIC_KEY -> {
					Log.d("loadSettings", "Updating traffic view setting")
					ENABLE_TRAFFIC_VIEW = line[1].toBoolean()
				}
				NIGHT_MODE_KEY -> {
					Log.d("loadSettings", "Updating dark mode setting")
					DEFAULT_NIGHT_MODE = line[1].toBoolean()
				}
				POLYLINES_KEY -> {
					Log.d("loadSettings", "Updating polyline setting")
					SHOW_POLYLINES = line[1].toBoolean()
				}
				VR_KEY -> {
					Log.d("loadSettings", "Updating VR setting")
					ENABLE_VR_OPTIONS = line[1].toBoolean()
				}
				else -> Log.w("loadSettings", "Line unaccounted for!\n$string")
			}
		}
	}

	/**
	 * Writes the provided string to the settings file. Since this version is deprecated just return.
	 *
	 * @param string  The string to be written to the settings file.
	 * @param context The app context (for determining where the file is).
	 */
	override fun writeSettingsToFile(string: String, context: Context) {}

	/**
	 * Creates a new settings file with default values. Since this version is deprecated just return.
	 *
	 * @param context The app context used to determine the file location.
	 */
	override fun createSettingsFile(context: Context) {}
}