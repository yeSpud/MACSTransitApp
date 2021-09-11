package fnsb.macstransit.settings

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.maps.GoogleMap
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.settings.CurrentSettings.readFile
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Created by Spud on 6/19/20 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.1.
 * @since Release 1.2.
 */
object V2 : BaseSettings<JSONObject>("Settings.json", 2) {

	/**
	 * Settings variables used by the app.
	 */
	var traffic = false
		private set

	/**
	 * The dark theme boolean (if dark theme should be set on launch).
	 */
	var darktheme = false
		private set

	/**
	 * The polyline boolean (if they should be shown or not).
	 */
	var polylines = false
		private set

	/**
	 * @return The streetview boolean.
	 */
	var streetView = false
		private set


	/**
	 * Settings variable used by the app. This variable corresponds with what map type should be used.
	 */
	var maptype = GoogleMap.MAP_TYPE_NORMAL
		private set

	/**
	 * Favorite routes set by the user.
	 * These routes should be enabled / selected as soon as the app has finished initialization.
	 * This is null as it should not be set by any other method except ones defined by this class.
	 */
	var favoriteRouteNames: Array<String> = emptyArray()
		private set

	/**
	 * Reads the JSON object from the settings file.
	 *
	 * @param file The settings file.
	 * @return The JSON object read from the settings file.
	 */
	override fun readFromSettingsFile(file: File): JSONObject {

		// Load the content from the file via a call to readFile.
		val content = readFile(file)
		Log.d("readFromSettingsFile", "Content: $content")

		// If the content isn't null, create a new JSON object from the content.
		return try {
			JSONObject(content)
		} catch (e: JSONException) {

			// If there was an issue with parsing the JSON, return an empty object.
			Log.e("readFromSettingsFile", "Cannot parse JSON")
			JSONObject()
		}
	}

	/**
	 * Reads the JSON object from the settings file.
	 *
	 * @param context The context to get the settings file by.
	 * @return The JSON object read from the settings file.
	 */
	fun readFromSettingsFile(context: Context): JSONObject {

		// Get the file from the context.
		val file = File(context.filesDir, FILENAME)
		Log.i("readFromSettingsFile", "Supposed file location: " + file.absolutePath)

		// Return the JSON object from the file determined by the context.
		return this.readFromSettingsFile(file)
	}

	/**
	 * Formats the given arguments into a JSON object that can be written to the settings file.
	 *
	 * @param bTraffic        Whether or not to show the traffic overlay.
	 * @param bDarktheme      Whether or not to launch with the dark theme.
	 * @param bPolylines      Whether to not to show polylines.
	 * @param bStreetview     Whether or not to enable the (deprecated) streetview feature.
	 * @param iMapType        What type of map to use.
	 * @param rFavoriteRoutes An array of favorited routes defined by the user.
	 * @return The formatted JSON object.
	 * @throws JSONException Thrown if there are any issues parsing the arguments provided.
	 */
	@Throws(JSONException::class)
	fun formatSettingsToJsonString(bTraffic: Boolean, bDarktheme: Boolean, bPolylines: Boolean,
	                               bStreetview: Boolean, iMapType: Int,
	                               vararg rFavoriteRoutes: Route): JSONObject {

		// Create a new JSON object to hold all the setting values.
		val parent = JSONObject()

		// Add all the the simple key value pairs to the parent JSON object.
		parent.putOpt("version", VERSION)
				.putOpt("enable traffic view", bTraffic)
				.putOpt("enable dark theme", bDarktheme)
				.putOpt("enable polylines", bPolylines)
				.putOpt("enable streetview", bStreetview)
				.putOpt("map type", iMapType)

		// Create a JSON array for all the favorite routes.
		val favoritedRoutes = JSONArray()
		for (route in rFavoriteRoutes) {
			favoritedRoutes.put(route.name)
		}

		// Add the favorite routes JSON array to the parent JSON object.
		parent.putOpt("favorited routes", favoritedRoutes)
		Log.d("formatSettingsJsnString", "Formatted Json: $parent")
		return parent
	}

	/**
	 * Writes the provided string to the settings file.
	 *
	 * @param string  The string to be written to the settings file. This cannot be null.
	 * @param context The app context (for determining where the file is). This cannot be null.
	 */
	override fun writeSettingsToFile(string: String, context: Context) {

		// Try opening the settings file.
		try {
			context.openFileOutput(FILENAME, Context.MODE_PRIVATE).use { outputStream ->

				// Write the string to the file.
				Log.d("writeStringToFile", "Writing string: $string")
				outputStream.write(string.toByteArray())
				outputStream.flush()
			}
		} catch (e: FileNotFoundException) {

			// Log that the file wasn't found, and notify the user.
			Log.e("writeSettingsToFile", "File was not found", e)
			Toast.makeText(context, "Unable to find settings file", Toast.LENGTH_LONG).show()
		} catch (e: IOException) {

			// Notify of any issues writing the file, and log it.
			Log.e("writeSettingsToFile", "Unable to write to file", e)
			Toast.makeText(context, "Unable write settings to file", Toast.LENGTH_LONG).show()
		}
	}

	/**
	 * Creates a new settings file with default values.
	 *
	 * @param context The app context used to determine the file location.
	 */
	override fun createSettingsFile(context: Context) {
		Log.i("createSettingsFile", "Creating new settings file")
		try {
			// Create a new JSON object with all the default settings.
			val json = formatSettingsToJsonString(bTraffic = false, bDarktheme = false,
			                                      bPolylines = false, bStreetview = false,
			                                      iMapType = GoogleMap.MAP_TYPE_NORMAL)

			// Write those settings to the file.
			writeSettingsToFile(json.toString(), context)
		} catch (e: JSONException) {
			e.printStackTrace()
		}
	}

	/**
	 * Parses settings to be applied to the static variables from the provided JSON object.
	 *
	 * @param input The JSON object containing the values to be parsed into settings.
	 */
	override fun parseSettings(input: JSONObject) {

		// Parse the simpler JSON objects from the settings file first.
		try {
			this.traffic = input.getBoolean("enable traffic view")
			this.darktheme = input.getBoolean("enable dark theme")
			this.polylines = input.getBoolean("enable polylines")
			this.streetView = input.getBoolean("enable streetview")
			this.maptype = input.getInt("map type")

			// Now try to parse the more dynamic content (favorited routes array).
			val favoritedRoutes: JSONArray = input.getJSONArray("favorited routes")
			val count: Int = favoritedRoutes.length()

			// Iterate though the JSON Array and retrieve the favorite route names.
			val favoriteRouteNames: Array<String?> = arrayOfNulls(count)
			for (i in 0 until count) {
				favoriteRouteNames[i] = favoritedRoutes.getString(i)
			}
			this.favoriteRouteNames = favoriteRouteNames as Array<String>

		} catch (e: JSONException) {
			Log.e("parseSettings", "Cannot parse settings", e)
		}
	}
}