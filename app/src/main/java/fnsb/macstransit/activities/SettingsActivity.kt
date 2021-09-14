package fnsb.macstransit.activities

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import fnsb.macstransit.R
import com.google.android.gms.maps.GoogleMap
import fnsb.macstransit.databinding.SettingsBinding
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.settings.CurrentSettings

/**
 * Created by Spud on 2019-11-24 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 3.1.
 * @since Beta 8.
 */
class SettingsActivity : androidx.appcompat.app.AppCompatActivity() {

	/**
	 * The binding used to retrieve elements from the activity layout.
	 */
	private lateinit var binding: SettingsBinding

	/**
	 * The current settings implementation.
	 */
	val settings = CurrentSettings.settingsImplementation as fnsb.macstransit.settings.V2

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Setup the binder.
		this.binding = SettingsBinding.inflate(this.layoutInflater)

		// Set the layout view to the settings view.
		this.setContentView(this.binding.root)

		// Setup the buttons.
		// The apply settings button should run the apply settings listener.
		this.binding.apply.setOnClickListener(ApplySettings())

		// The cancel button should just finish the class and return.
		this.binding.cancel.setOnClickListener { this.finish() }

		// Get the routes from the intent extra.
		val extraBundle: Bundle = this.intent.extras ?: return
		val routeParcel: Array<android.os.Parcelable> = extraBundle.getParcelableArray("Routes") ?: return

		// Setup the favorites container.
		// Begin by iterating though all the routes.
		routeParcel.forEach {

			// Get the route from the parcelable.
			val route: Route = it as Route

			// Create a new checkbox.
			val checkBox = CheckBox(this)

			// Set the checkbox's text to the route name.
			checkBox.text = route.name

			// Set the minimum height for the checkbox.
			checkBox.minHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CHECKBOX_MIN_HEIGHT,
			                                               this.resources.displayMetrics).toInt()

			// Set the color and size of the text to constants.
			checkBox.textSize = CHECKBOX_TEXT_SIZE
			val color = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
				@Suppress("Deprecation")
				this.resources.getColor(R.color.white)
			} else {
				this.resources.getColor(R.color.white, null)
			}
			checkBox.setTextColor(color)

			// Add button tint if the sdk supports it.
			checkBox.buttonTintList = androidx.appcompat.content.res.AppCompatResources.
			getColorStateList(this, R.color.white)

			// Set the checkbox tag to the route object.
			checkBox.tag = route

			// Set the checkbox to be checked if its route is a favorited route.
			checkBox.isChecked = this.settings.favoriteRouteNames.contains(route.name)

			// Add the box to the favorites container.
			this.binding.favoriteRouteContainer.addView(checkBox)
		}
	}

	companion object {

		/**
		 * Constant used to set the initial size of the text for the favorite routes checkbox.
		 */
		private const val CHECKBOX_TEXT_SIZE: Float = 20.0F

		/**
		 * Constant used to set the minimum height of the checkbox for the favorite routes checkbox.
		 */
		private const val CHECKBOX_MIN_HEIGHT: Float = 48.0F
	}

	/**
	 * Listener used to apply the settings entered once the apply button has been clicked.
	 */
	internal inner class ApplySettings : View.OnClickListener {

		override fun onClick(v: View) {

			// Get the favorite routes from the activity.
			val favoritedRoutes: Array<Route> = favoritedRoutes()

			// Determine the map type.
			val mapId: Int = when (this@SettingsActivity.binding.mapGroup.checkedRadioButtonId) {
				R.id.normal_map -> GoogleMap.MAP_TYPE_NORMAL
				R.id.satellite_map -> GoogleMap.MAP_TYPE_SATELLITE
				R.id.hybrid_map -> GoogleMap.MAP_TYPE_HYBRID
				R.id.terrain_map -> GoogleMap.MAP_TYPE_TERRAIN
				else -> GoogleMap.MAP_TYPE_NORMAL
			}

			// Format the options into a Json string.
			val json: org.json.JSONObject = try {
				this@SettingsActivity.settings.formatSettingsToJsonString(
						this@SettingsActivity.binding.traffic.isChecked,
						this@SettingsActivity.binding.nightMode.isChecked,
						this@SettingsActivity.binding.polylines.isChecked,
						this@SettingsActivity.binding.VR.isChecked, mapId, *favoritedRoutes)
			} catch (e: org.json.JSONException) {
				Log.e("ApplySettings", "Exception on settings button click", e)
				Toast.makeText(v.context, "An exception occurred while applying settings",
				               Toast.LENGTH_LONG).show()
				return
			}

			// Write that string to the file
			CurrentSettings.settingsImplementation.writeSettingsToFile(json.toString(),
			                                                           this@SettingsActivity)

			// Reload the settings.
			CurrentSettings.settingsImplementation.parseSettings(json)

			// Close the activity.
			this@SettingsActivity.finish()
		}

		/**
		 * Gets the favorited routes from the favorited routes container.
		 *
		 * @return The array of selected favorited routes.
		 */
		private fun favoritedRoutes(): Array<Route> {

			// Get the number of potential favorite routes.
			val potentialRoutesCount: Int =
					this@SettingsActivity.binding.favoriteRouteContainer.childCount
			Log.d("getFavoritedRoutes", "Potential count: $potentialRoutesCount")

			// Create an array of potential routes.
			// Since we know the maximum just use that as its starting size.
			val potentialRoutes = arrayOfNulls<Route>(potentialRoutesCount)
			var routesPosition = 0

			// Iterate though each radio button in the favorites container.
			for (i in 0 until potentialRoutesCount) {

				// Get a specific checkbox from the favorites container.
				val box: CheckBox = this@SettingsActivity.binding.favoriteRouteContainer.
				getChildAt(i) as CheckBox

				// Add the route to the array if its checked.
				if (box.isChecked) {
					potentialRoutes[routesPosition] = box.tag as Route
					Log.d("getFavoritedRoutes",
					      "Adding route ${potentialRoutes[routesPosition]!!.name}")

					// Add one to a tally of verified favorite routes.
					routesPosition++
				}
			}

			// Create a new route array, and be sure to fit it to its actual size.
			val routes = arrayOfNulls<Route>(routesPosition)
			System.arraycopy(potentialRoutes, 0, routes, 0, routesPosition)

			// Return the newly created array.
			return routes as Array<Route>
		}
	}
}