package fnsb.macstransit.routematch

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.UiThread
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng

/**
 * Created by Spud on 2019-11-01 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 4.0.
 * @since Beta 7.
 */
class SharedStop: MarkedObject, Parcelable {

	/**
	 * Documentation
	 */
	val routeNames: Array<String>

	/**
	 * Documentation
	 */
	private val routeColors: IntArray

	/**
	 * Array of circle options for each circle that represents a route.
	 */
	@Transient
	private val circleOptions: Array<CircleOptions>

	/**
	 * Array of circles that represent a route that shares this one stop.
	 */
	@Transient
	private val circles: Array<Circle?>

	/**
	 * Documentation
	 *
	 * @param parcel
	 */
	constructor(parcel: Parcel): super(parcel.readString()!!, parcel.
	readParcelable<LatLng>(LatLng::class.java.classLoader)!!, parcel.readString()!!, parcel.readInt()) {
		this.routeNames = parcel.createStringArray()!!
		this.routeColors = parcel.createIntArray()!!

		this.circleOptions = Array(this.routeNames.size) {
			if (this.routeColors[it] != 0) {
				CircleOptions().center(this.location)
					.fillColor(this.routeColors[it])
					.strokeColor(this.routeColors[it])
			} else {
				CircleOptions().center(this.location)
			}
		}

		this.circles = arrayOfNulls(this.routeNames.size)

		// Set the initial circle size.
		setCircleSizes(INITIAL_CIRCLE_SIZE)
	}

	/**
	 * Documentation
	 *
	 * @param name
	 * @param location
	 * @param routes
	 */
	constructor(name: String, location: LatLng, routes: Array<Route>): super(name, location,
	                                                                         routes[0].name, routes[0].color) {
		this.routeNames = Array(routes.size) { routes[it].name }
		this.routeColors = IntArray(routes.size) { routes[it].color }

		this.circleOptions = Array(routes.size) {
			val route = routes[it]
			val color = route.color
			if (color != 0) {
				CircleOptions().center(this.location).fillColor(color).strokeColor(color)
			} else {
				CircleOptions().center(this.location)
			}
		}

		this.circles = arrayOfNulls(routes.size)

		// Set the initial circle size.
		setCircleSizes(INITIAL_CIRCLE_SIZE)
	}

	/**
	 * Sets the shared stop circles to be visible.
	 * Circles will be created at this point if they were non-existent before (null).
	 *
	 * This must be run on the UI thread.
	 *
	 * @param map The map to put create the circles on.
	 */
	@UiThread
	fun showSharedStop(map: GoogleMap) {

		// Iterate though each of the circles.
		for (i in this.circles.indices) {

			// If the circle is null, create a new shared stop circle.
			if (this.circles[i] == null) {

				// Since the stop circle is null try creating a new one.
				this.createSharedStopCircle(map, i)
			} else {

				// Only set the circle to be clickable if its the 0th index circle (the biggest one).
				this.circles[i]!!.isClickable = i == 0

				// Set the circle to be visible.
				this.circles[i]!!.isVisible = true
			}
		}
	}

	/**
	 * Hides the shared stop.
	 * If there are any routes that are still enabled that belong to this shared stop then the stop will not be hidden.
	 *
	 * This must be run on the UI thread.
	 */
	@UiThread
	fun hideStop() {

		// Iterate though each circle in the shared stop.
		this.circles.forEach {

			// If the circle is not null set it to not be clickable, and hide it.
			if (it != null) {
				it.isClickable = false
				it.isVisible = false
			}
		}
	}

	/**
	 * Sets the circles to the specified size.
	 * Each subsequent circle is set to a smaller size than the initial circle.
	 *
	 * This must be run on the UI thread.
	 *
	 * @param size The size to set the circles to.
	 */
	@UiThread
	fun setCircleSizes(size: Double) {

		// Iterate though each circle option (and circle if its not null) and reset its radius.
		for (i in this.circles.indices) {

			// Get the size of the circle based on its radius.
			val radiusSize = size * (1.0 / (i + 1))

			// Set the circle size.
			this.circleOptions[i].radius(radiusSize)
			if (this.circles[i] != null) {
				this.circles[i]!!.radius = radiusSize
			}
		}
	}

	/**
	 * Removes all the shared stop circles from the map.
	 * This also set the circles to null (so the circles can be recreated later).
	 *
	 * This must run on the UI Thread.
	 */
	@UiThread
	fun removeSharedStopCircles() {

		// Iterate though each circle in the shared stop.
		for (i in circles.indices) {

			// Get the circle from the shared stop, and remove it.
			this.circles[i]?.remove()

			// Set all the circles to null.
			this.circles[i] = null
		}
	}

	/**
	 * Creates a new circle with the specified circle options that is immediately visible.
	 *
	 * This should be run on the UI thread.
	 *
	 * @param map   The map to add the circle to.
	 * @param index The index of the circle.
	 *              Used for determining if the circle should be clickable or not,
	 *              as well as what index to set it to.
	 */
	@UiThread
	fun createSharedStopCircle(map: GoogleMap, index: Int) {

		// Get the circle that was added to the map with the provided circle options.
		val circle: Circle = map.addCircle(this.circleOptions[index])

		// Set the tag of the circle to the provided shared stop object.
		circle.tag = this

		// Set the circle to be clickable depending on the clickable argument.
		circle.isClickable = index == 0

		// At this point set the circle to be visible.
		circle.isVisible = true

		// Return our newly created circle.
		this.circles[index] = circle
	}

	companion object {

		/**
		 * The initial size to set the largest circle to.
		 * Each of the subsequent circles are set to a smaller size dependent on this constant.
		 */
		const val INITIAL_CIRCLE_SIZE = 12.0

		/**
		 * Gets the routes that share the provided stop by iterating through all routes,
		 * and comparing each route's stop to the provided stop to see if they match.
		 *
		 * If there are no matches then the size of the returned route array will be one.
		 * This is because its the only route to have that stop in its stop array.
		 *
		 * @param route      The route to compare against all other routes.
		 * @param stop       The stop to compare against all stops in all other routes.
		 *
		 * @return Documentation
		 */
		@JvmStatic
		fun getSharedRoutes(route: Route, stop: Stop, allRoutes: HashMap<String, Route>): Array<Route> {

			// Comments
			val hashMap: HashMap<String, Route> = HashMap(1)
			hashMap[route.name] = route

			// Make sure all routes isn't empty.
			if (allRoutes.isEmpty()) {

				// Comments
				return hashMap.values.toTypedArray()
			}

			// Comments
			for ((hashName, hashRoute) in allRoutes) {

				// If the routes are the same then continue to the next iteration of the loop.
				if (route == hashRoute) {
					continue
				}

				// If there are no stops to iterate over just continue like above.
				if (hashRoute.stops.isEmpty()) {
					continue
				}

				// Try to get our provided stop from the has route.
				// If the stop isn't null (was found) add the hash route to our hashmap.
				if (hashRoute.stops[stop.name] != null) {
					hashMap[hashName] = hashRoute
				}
			}

			// Comments
			return hashMap.values.toTypedArray()
		}

		@JvmField
		val CREATOR = object : Parcelable.Creator<SharedStop> {

			override fun createFromParcel(parcel: Parcel): SharedStop {
				return SharedStop(parcel)
			}

			override fun newArray(size: Int): Array<SharedStop?> {
				return arrayOfNulls(size)
			}
		}

	}

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeString(this.name)
		parcel.writeParcelable(this.location, flags)
		parcel.writeString(this.routeNames[0])
		parcel.writeInt(this.routeColors[0])
		parcel.writeStringArray(this.routeNames)
		parcel.writeIntArray(this.routeColors)
	}

	override fun describeContents(): Int {
		return this.hashCode()
	}
}