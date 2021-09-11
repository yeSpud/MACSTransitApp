package fnsb.macstransit.routematch

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.annotation.UiThread
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.ktx.addCircle
import org.json.JSONException

/**
 * Created by Spud on 2019-10-18 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 3.1.
 * @since Beta 6.
 */
class Stop: MarkedObject, Parcelable {

	/**
	 * The circle marking the bus stop on the map
	 * (be sure to check if this exists first as it may be null).
	 */
	@Transient
	var circle: com.google.android.gms.maps.model.Circle? = null
		private set

	/**
	 * Creation of a stop object from a previously created stop object.
	 *
	 * @param parcel The parcel containing the saved information to create a stop object.
	 */
	constructor(parcel: Parcel): super(parcel.readString()!!, parcel.
	readParcelable<LatLng>(LatLng::class.java.classLoader)!!, parcel.readString()!!, parcel.readInt())

	/**
	 * A stop object.
	 *
	 * @param stopName The name of the stop.
	 * @param location THe location of the stop.
	 * @param route The route the stop belongs to.
	 */
	constructor(stopName: String, location: LatLng, route: Route): super(stopName, location,
	                                                                     route.name, route.color)

	/**
	 * A stop object.
	 *
	 * @param stopName The name of the stop.
	 * @param latitude The latitude of the stop.
	 * @param longitude The longitude of the stop.
	 * @param route The route of the stop.
	 */
	constructor(stopName: String, latitude: Double, longitude: Double, route: Route) :
			this(stopName, LatLng(latitude, longitude), route)

	/**
	 * Lazy creation of a new Stop object using the provided JSON and the route.
	 *
	 * @param json  The JSONObject containing the bus stop data.
	 * @param route The route this newly created Stop object will apply to.
	 */
	@Throws(JSONException::class)
	constructor(json: org.json.JSONObject, route: Route) : this(json.getString("stopId"),
	                                                            json.getDouble("latitude"),
	                                                            json.getDouble("longitude"), route)

	/**
	 * Shows the stops for the given route.
	 * If the stops weren't previously added to the map then this method will also see fit to add them to the map.
	 * This must be run on the UI thread.
	 *
	 * @param map The google maps object that the stops will be drawn onto.
	 *            Be sure this object has been initialized first.
	 * @param visible Whether the stop should be visible or not.
	 * @param attempted Whether or not this function has been attempted before (default is false).
	 */
	@UiThread
	fun toggleStopVisibility(map: GoogleMap, visible: Boolean, attempted: Boolean = false) {

		// Check if the circle for the stop needs to be created,
		// or just set to visible if it already exists.
		if (this.circle == null) {

			// If this function was already attempted return early.
			// There may be a reason why the stop was unable to be created
			if (attempted) {
				Log.w("toggleStopVisibility", "Unable to create circle for stop ${this.name}")
				return
			}

			// Create a new circle object.
			this.createStopCircle(map, visible)
			this.toggleStopVisibility(map, visible,true)
		} else {

			// Since the circle already exists simply update its visibility.
			this.circle!!.isClickable = visible
			this.circle!!.isVisible = visible
		}
	}

	/**
	 * Removes the stop's circle from the map.
	 * This also sets the circle to null so it can be recreated later.
	 * This must be run on the UI Thread.
	 */
	@UiThread
	fun removeStopCircle() {

		// Remove stop circles (if it has them).
		if (circle != null) {
			circle!!.remove()
			circle = null
		}
	}

	/**
	 * Creates a new circle object for new Stops.
	 *
	 * @param map The google maps object that this newly created circle will be added to.
	 * @param visible Whether or not the stop is visible or not.
	 */
	@UiThread
	fun createStopCircle(map: GoogleMap, visible: Boolean) {

		// Add our circle to the map.
		this.circle = map.addCircle {

			// Set the location of the circle to the location of the stop.
			this.center(this@Stop.location)

			// Set the initial size of the circle to the STARTING_RADIUS constant.
			this.radius(STARTING_RADIUS)

			// Set the colors.
			this.fillColor(this@Stop.color)
			this.strokeColor(this@Stop.color)

			// Set the stop to be clickable and visible based on the visiblity boolean.
			this.clickable(visible)
			this.visible(visible)
		}

		// Check if the circle is null at this point (failure to add to map).
		if (this.circle == null) {
			Log.w("createStopCircle", "Failed to add stop circle to map!")
			return
		}

		// Set the tag of the circle to Stop so that it can differentiate between this class
		// and other stop-like classes (such as shared stops).
		this.circle!!.tag = this
	}

	companion object {

		/**
		 * The starting radius size of the circle for the stop on the map (in meters).
		 */
		private const val STARTING_RADIUS = 50.0

		/**
		 * Validates the provided array of potential stops and returned the actual stops in the route,
		 * removing any duplicate or invalid stops.
		 *
		 * @param potentialStops The potential stops that may contain duplicate or invalid stops.
		 * @return The validated stops array (or an empty stop array if the provided potential stops is null).
		 */
		@JvmStatic
		fun validateGeneratedStops(potentialStops: Array<Stop>): Array<Stop> {

			// Create a variable to store the true size of the stops that have been validated.
			var validatedSize = 0

			// Create an array to store the validated stops.
			// While we don't know the specific size of this array until done, we do know the maximum size,
			// so use that for setting the array size.
			val validatedStops = arrayOfNulls<Stop>(potentialStops.size)

			// Iterate through each stop in our array of potential stops.
			for (stop in potentialStops) {

				// Check to see if the stop is in our array of validated stops. If its not,
				// add it to the array and add 1 to the true index size of stops that have been validated.
				if (!isDuplicate(stop, validatedStops)) {
					validatedStops[validatedSize] = stop
					validatedSize++
				}
			}

			// Create an array for our actual stops.
			// Since we now know the number of validated stops we can use that as its size.
			val actualStops = arrayOfNulls<Stop>(validatedSize)

			// Copy our validated stops into our smaller actual stops array, and return it.
			System.arraycopy(validatedStops, 0, actualStops, 0, actualStops.size)
			return actualStops as Array<Stop>
		}

		/**
		 * Checks the provided stop against an array of stops to check if its already contained in the array
		 * (and is therefore a would-be duplicate).
		 *
		 * @param stop      The Stop object to check for.
		 * @param stopArray The stop array to compare the Stop object against.
		 * @return Returns true if the Stop object was found within the array - otherwise it returns false.
		 */
		@JvmStatic
		fun isDuplicate(stop: Stop, stopArray: Array<Stop?>): Boolean {

			// Iterate though each potential stop in the stop array.
			for (stopArrayItem: Stop? in stopArray) {

				// If the array item is null just return false.
				if (stopArrayItem == null) {
					return false
				}

				// Check if the following match.
				val routeNameMatch = stop.routeName == stopArrayItem.routeName
				val colorMatch = stop.color == stop.color

				// If all of the following match, return true.
				if (routeNameMatch && colorMatch && stop == stopArrayItem) {
					return true
				}
			}

			// Since nothing matched, return false.
			return false
		}

		@JvmField
		val CREATOR = object: Parcelable.Creator<Stop> {

			override fun createFromParcel(parcel: Parcel): Stop {
				return Stop(parcel)
			}

			override fun newArray(size: Int): Array<Stop?> {
				return arrayOfNulls(size)
			}
		}
	}

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeString(this.name)
		parcel.writeParcelable(this.location, flags)
		parcel.writeString(this.routeName)
		parcel.writeInt(this.color)
	}

	override fun describeContents(): Int {
		return this.hashCode()
	}
}