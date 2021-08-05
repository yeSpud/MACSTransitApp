package fnsb.macstransit.Settings

import android.content.Context
import java.io.File

/**
 * Created by Spud on 3/13/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Release 1.2.
 */
abstract class BaseSettings<R>(val FILENAME: String, val VERSION: Int) {

	/*
	 * The name of the settings file.
	 */

	/*
	 * The version of the settings.
	 */

	/**
	 * Writes the provided string to the settings file.
	 *
	 * @param string  The string to be written to the settings file.
	 * @param context The app context (for determining where the file is).
	 */
	abstract fun writeSettingsToFile(string: String, context: Context)

	/**
	 * Creates a new settings file with default values.
	 *
	 * @param context The app context used to determine the file location.
	 */
	abstract fun createSettingsFile(context: Context)

	/**
	 * Parses the settings from the settings file into booleans and ints.
	 *
	 * @param input TODO
	 */
	abstract fun parseSettings(input: R)

	/**
	 *  Reads the object from the settings file.
	 *
	 *  @param file The settings file.
	 *  @return TODO
	 */
	abstract fun readFromSettingsFile(file: File): R

}