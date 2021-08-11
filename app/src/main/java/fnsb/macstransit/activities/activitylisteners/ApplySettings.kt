package fnsb.macstransit.activities.activitylisteners

import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import com.google.android.gms.maps.GoogleMap
import fnsb.macstransit.R
import fnsb.macstransit.routematch.Route

/**
 * Created by Spud on 6/23/20 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Release 1.2.
 */
@androidx.annotation.UiThread
class ApplySettings(private val activity: fnsb.macstransit.activities.SettingsActivity) :
		View.OnClickListener {

	/**
	 * Documentation
	 */
	private val settings = fnsb.macstransit.settings.CurrentSettings.settingsImplementation

	/**
	 * Called when a view has been clicked.
	 *
	 * @param v The view that was clicked.
	 */
	override fun onClick(v: View) {

		// Get the favorite routes from the activity.
		val favoritedRoutes: Array<Route> = favoritedRoutes()

		// Determine the map type.
		val mapId: Int = when (this.activity.mapType.checkedRadioButtonId) {
			R.id.satellite_map -> {
				GoogleMap.MAP_TYPE_SATELLITE
			}
			R.id.terrain_map -> {
				GoogleMap.MAP_TYPE_TERRAIN
			}
			else -> {
				GoogleMap.MAP_TYPE_NORMAL
			}
		}

		// Format the options into a Json string.
		val json: org.json.JSONObject = try {
			(this.settings as fnsb.macstransit.settings.V2).formatSettingsToJsonString(
					this.activity.trafficBox.isChecked, this.activity.darkthemeBox.isChecked,
					this.activity.polyBox.isChecked, this.activity.streetviewBox.isChecked, mapId,
					*favoritedRoutes)
		} catch (e: org.json.JSONException) {
			Log.e("ApplySettings", "Exception on settings button click", e)
			Toast.makeText(v.context, "An exception occurred while applying settings",
			               Toast.LENGTH_LONG).show()
			return
		}

		// Write that string to the file
		this.settings.writeSettingsToFile(json.toString(), activity)

		// Reload the settings.
		this.settings.parseSettings(json)

		// Close the activity.
		this.activity.finish()
	}

	/**
	 * Gets the favorited routes from the favorited routes container.
	 *
	 * @return The array of selected favorited routes.
	 */
	private fun favoritedRoutes(): Array<Route> {

		// Get the number of potential favorite routes.
		val potentialRoutesCount: Int = this.activity.favoriteContainer.childCount
		Log.d("getFavoritedRoutes", "Potential count: $potentialRoutesCount")

		// Create an array of potential routes.
		// Since we know the maximum just use that as its starting size.
		val potentialRoutes = arrayOfNulls<Route>(potentialRoutesCount)
		var routesPosition = 0

		// Iterate though each radio button in the favorites container.
		for (i in 0 until potentialRoutesCount) {

			// Get a specific checkbox from the favorites container.
			val box: CheckBox = this.activity.favoriteContainer.getChildAt(i) as CheckBox

			// Add the route to the array if its checked.
			if (box.isChecked) {
				potentialRoutes[routesPosition] = box.tag as Route
				Log.d("getFavoritedRoutes",
				      "Adding route ${potentialRoutes[routesPosition]!!.routeName}")

				// Add one to a tally of verified favorite routes.
				routesPosition++
			}
		}

		// Create a new route array, and be sure to fit it to its actual size.
		val routes = arrayOfNulls<Route>(routesPosition)
		System.arraycopy(potentialRoutes, 0, routes, 0, routesPosition)

		// Return the newly created array.
		return routes.requireNoNulls()
	}
}