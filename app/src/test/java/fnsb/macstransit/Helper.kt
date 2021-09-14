package fnsb.macstransit

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

/**
 * Created by Spud on 6/27/20 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Release 1.2.
 */
internal object Helper {

	/**
	 * TODO Documentation
	 */
	private const val ROOT = "src/test/java/fnsb/macstransit/testfiles/"

	/**
	 * TODO Documentation
	 */
	val ALL_VEHICLES_JSON = File(ROOT + "all vehicles.json")
	val ALL_VEHICLES_EMPTY_JSON = File(ROOT + "all vehicles empty.json")
	val MASTERROUTE_JSON = File(ROOT + "masterRoute.json")
	val OLD_SETTINGS_TXT = File(ROOT + "old settings.txt")
	val SETTINGS_JSON = File(ROOT + "settings.json")
	val BLUE_STOPS = File(ROOT + "stops/Blue.json")
	val BROWN_STOPS = File(ROOT + "stops/Brown.json")
	val GOLD_STOPS = File(ROOT + "stops/Gold.json")
	val GREEN_STOPS = File(ROOT + "stops/Green.json")
	val PURPLE_STOPS = File(ROOT + "stops/Purple.json")
	val RED_STOPS = File(ROOT + "stops/Red.json")
	val YELLOW_STOPS = File(ROOT + "stops/Yellow.json")

	/**
	 * TODO Documentation
	 */
	val TRANSIT_CENTER = File(ROOT + "stopTimes/Transit Center.json")
	val GEIST_MCDS = File(ROOT + "stopTimes/Geist McDs.json")
	val WOOD_CENTER = File(ROOT + "stopTimes/Wood Center.json")

	/**
	 * TODO Documentation and comments
	 *
	 * @param file
	 * @return
	 */
	@JvmStatic
	fun getText(file: File): String {

		// Try to create a file input stream in order to read the data from the file.
		val input: FileInputStream = try {
			FileInputStream(file)
		} catch (e: java.io.FileNotFoundException) {
			Log.e("readFile", "No file found for path: " + file.absolutePath)
			return ""
		}

		// If the file input stream was created successfully, execute the following:
		val stringBuilder = StringBuilder()
		try {
			java.io.BufferedReader(
					java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8))
					.use { reader ->
						var line = reader.readLine()
						while (line != null) {
							stringBuilder.append(line).append("\n")
							line = reader.readLine()
						}
					}
		} catch (e: java.io.IOException) {
			// Error occurred when opening raw file for reading.
			e.printStackTrace()
		}
		return stringBuilder.toString()
	}

	/**
	 * TODO Documentation and comments
	 *
	 * @param file
	 * @return
	 * @throws JSONException
	 */
	@JvmStatic
	@Throws(JSONException::class)
	fun getJSON(file: File): JSONObject {
		val text: String = getText(file)
		return JSONObject(text)
	}

	/**
	 * TODO Documentation
	 *
	 * @param startTime
	 * @param endTime
	 */
	@JvmStatic
	fun printTime(startTime: Long, endTime: Long): Long {
		val total = endTime - startTime
		var microseconds = 0.0
		var milliseconds = 0.0
		var seconds = 0.0
		if (total / 1000 >= 1) {
			microseconds = total / 1000.0
		}
		if (microseconds / 1000 >= 1) {
			milliseconds = total / 1000.0 / 1000.0
			microseconds = 0.0
		}
		if (milliseconds / 1000 >= 1) {
			seconds = total / 1000.0 / 1000.0 / 1000.0
			microseconds = 0.0
		}
		print("Time taken to execute: ")
		when {
			seconds != 0.0 -> {
				println("$seconds seconds\t($total nanoseconds)")
			}
			milliseconds != 0.0 -> {
				println("$milliseconds milliseconds\t($total nanoseconds)")
			}
			microseconds != 0.0 -> {
				println("$microseconds microseconds\t($total nanoseconds)")
			}
			else -> {
				println("$total nanoseconds")
			}
		}

		return total
	}
}