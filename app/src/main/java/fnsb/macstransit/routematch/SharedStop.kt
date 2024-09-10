package fnsb.macstransit.routematch

import androidx.annotation.UiThread
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions

/**
 * Created by Spud on 2019-11-01 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 4.1.
 * @since Beta 7.
 */
class SharedStop(val name: String, val location: com.google.android.gms.maps.model.LatLng,
                 val routes: Array<Route>): java.io.Closeable {

	/**
	 * Array of circle options for each circle that represents a route.
	 */
	private val circleOptions: Array<CircleOptions> = Array(routes.size) {
		val route = routes[it]
		val color = route.color

		// Set the circle location.
		val options: CircleOptions = CircleOptions().center(location)

		// If the route has color then set the circle color.
		if (color != 0) {
			options.fillColor(color).strokeColor(color)
		}

		// Apply the options to the array.
		options
	}

	/**
	 * Array of circles that represent a route that shares this one stop.
	 */
	private var circles: Array<Circle> = emptyArray()

	init { setCircleSizes(INITIAL_CIRCLE_SIZE) }

	@UiThread
	private fun addCirclesToMap(map: GoogleMap) {
		circles = Array(routes.size) {

			// Get the circle that was added to the map with the provided circle options.
			val circle: Circle = map.addCircle(circleOptions[it])

			// Set the circle to be clickable depending on the clickable argument.
			circle.isClickable = (it == 0)

			// At this point set the circle to be visible.
			circle.isVisible = true

			// Set the tag of the circle to the provided shared stop object.
			circle.tag = this

			// Return our newly created circle.
			circle
		}
	}

	/**
	 * Sets the shared stop circles to be visible.
	 * Circles will be created at this point if they were non-existent before (null).
	 */
	@UiThread
	fun showSharedStop(map: GoogleMap) {

		if (circles.isEmpty()) { addCirclesToMap(map) }

		// Iterate though each of the circles.
		for (i in circles.indices) {

			// Only set the circle to be clickable if its the 0th index circle (the biggest one).
			circles[i].isClickable = i == 0

			// Set the circle to be visible.
			circles[i].isVisible = true
		}
	}

	/**
	 * Hides the shared stop.
	 * If there are any routes that are still enabled that belong to this shared stop then the stop will not be hidden.
	 *
	 * This must be run on the UI thread.
	 */
	fun hideStop() {

		// Iterate though each circle in the shared stop.
		for (circle in circles) {

			// If the circle is not null set it to not be clickable, and hide it.
			circle.isClickable = false
			circle.isVisible = false
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
	fun setCircleSizes(size: Double) {

		// Iterate though each circle option (and circle if its not null) and reset its radius.
		for (i in circles.indices) {

			// Get the size of the circle based on its radius.
			val radiusSize: Double = size * (1.0 / (i + 1))

			// Set the circle size.
			circleOptions[i].radius(radiusSize)
			circles[i].radius = radiusSize
		}
	}

	companion object {

		/**
		 * The initial size to set the largest circle to.
		 * Each of the subsequent circles are set to a smaller size dependent on this constant.
		 */
		const val INITIAL_CIRCLE_SIZE: Double = 12.0

		/**
		 * Gets the routes that share the provided stop by iterating through all routes,
		 * and comparing each route's stop to the provided stop to see if they match.
		 *
		 * If there are no matches then the size of the returned route array will be one.
		 * This is because its the only route to have that stop in its stop array.
		 *
		 * @param route      The route to compare against all other routes.
		 * @param stop       The stop to compare against all stops in all other routes.
		 * @param allRoutes  A hashmap of all the trackable routes.
		 *
		 * @return Returns all the shared routes for the stop as an array.
		 */
		@JvmStatic
		fun getSharedRoutes(route: Route, stop: Stop, allRoutes: HashMap<String, Route>): Array<Route> {

			// Make sure all routes isn't empty.
			if (allRoutes.isEmpty()) { return arrayOf(route) }

			// Create a hashmap that will contain all our shared routes.
			val hashMap: HashMap<String, Route> = HashMap(1)

			// Add the first provided route to the hashmap.
			hashMap[route.name] = route

			// Iterate though all the provided routes.
			for ((hashName, hashRoute) in allRoutes) {

				// If the routes are the same then continue to the next iteration of the loop.
				if (route == hashRoute) { continue }

				// If there are no stops to iterate over just continue like above.
				if (hashRoute.stops.isEmpty()) { continue }

				// Try to get our provided stop from the has route.
				// If the stop isn't null (was found) add the hash route to our hashmap.
				if (hashRoute.stops[stop.name] != null) { hashMap[hashName] = hashRoute }
			}

			// Return our hashmap of all the shared routes as an array.
			return hashMap.values.toTypedArray()
		}
	}

	override fun close() {
		for (circle in circles) { circle.remove() }
		circles = emptyArray()
	}
}