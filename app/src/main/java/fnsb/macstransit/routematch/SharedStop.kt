package fnsb.macstransit.routematch

import android.util.Log
import androidx.annotation.UiThread
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import fnsb.macstransit.activities.mapsactivity.MapsActivity

/**
 * Created by Spud on 2019-11-01 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 4.0.
 * @since Beta 7.
 */
class SharedStop(location: LatLng, stopName: String, val routes: Array<Route>):
		MarkedObject(stopName, location, routes[0]) {

	/**
	 * Array of circle options for each circle that represents a route.
	 */
	private val circleOptions: Array<CircleOptions> = Array(this.routes.size) {
		val route = routes[it]
		val color = route.color
		if (color != 0) {
			CircleOptions().center(this.location).fillColor(color).strokeColor(color)
		} else {
			CircleOptions().center(this.location)
		}
	}

	/**
	 * Array of circles that represent a route that shares this one stop.
	 */
	private val circles: Array<Circle?> = arrayOfNulls(this.routes.size)

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
		for (i in circles.indices) {

			// If the circle is null, create a new shared stop circle.
			if (circles[i] == null) {

				// Since the stop circle is null try creating a new one.
				this.createSharedStopCircle(map, i)
			} else {

				// Only set the circle to be clickable if its the 0th index circle (the biggest one).
				circles[i]!!.isClickable = i == 0

				// Set the circle to be visible.
				circles[i]!!.isVisible = true
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

		// Iterate though each route in the shared stop.
		this.routes.forEach {

			// If any route is still enabled, return early.
			if (it.enabled) {
				return
			}
		}

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
		 * @param routeIndex The index of the route that we are comparing in all routes.
		 * @param stop       The stop to compare against all stops in all other routes.
		 * @return Array of routes that share the stop. This always return an array of at least one route.
		 */
		@JvmStatic
		fun getSharedRoutes(route: Route, routeIndex: Int, stop: Stop): Array<Route> {

			// Create an array of potential routes that could share a same stop
			// (the stop that we are iterating over).
			// Set the array size to that of all the routes minus the current index as to make it decrease every iteration.
			val potentialRoutes = arrayOfNulls<Route>(MapsActivity.allRoutes.size - routeIndex)

			// Add the current route to the potential routes, and update the potential route index.
			potentialRoutes[0] = route
			var potentialRouteIndex = 1

			// In order to iterate though all the routes remaining in the allRoutes array we need to get the 2nd route index.
			// This is equal to the first route index + 1 as to not hopefully not compare the same route against itself,
			// but also not compare against previous routes in the array.
			for (route2Index in routeIndex + 1 until MapsActivity.allRoutes.size) {

				// Get the route at the 2nd index for comparison.
				val route2 = MapsActivity.allRoutes[route2Index]

				// If the routes are the same then continue to the next iteration of the loop.
				if (route == route2) {
					continue
				}

				// If there are no stops to iterate over just continue like above.
				val route2Stops = route2.stops
				if (route2Stops.isEmpty()) {
					continue
				}

				// Iterate though each stop in the second route and compare them to the provided stop.
				for (stop2 in route2Stops) {

					// If the stops match, add the route to the potential routes array.
					if (stop == stop2) {
						potentialRoutes[potentialRouteIndex] = route2
						potentialRouteIndex++
					}
				}
			}

			// Create a new array of routes with the actual size of shared routes between the one shared stop.
			val actualRoutes: Array<Route?> = arrayOfNulls(potentialRouteIndex)

			// Copy the content from the potential routes into the actual route, and return the actual route.
			System.arraycopy(potentialRoutes, 0, actualRoutes, 0, potentialRouteIndex)
			return actualRoutes as Array<Route>
		}

		/**
		 * Compares stops against shared stops and only returns the stops that are not shared stops.
		 *
		 * @param stops       The original stops for the route that may be shared with shared stops.
		 * @param sharedStops The shared stops for the route.
		 * @return Returns an array of stops that are unique to the route (not shared by any other routes or shared stops).
		 */
		@JvmStatic
		fun removeStopsWithSharedStops(stops: Array<Stop>, sharedStops: Array<SharedStop>): Array<Stop> {

			// Create an of potential stops with a maximum size of the original stop array.
			val potentialStops = arrayOfNulls<Stop>(stops.size)
			var finalIndex = 0

			// Iterate though each stop in the provided stop array.
			stops.forEach { stop ->

				// Check if the stop matches the shared stop (same name, location).
				var noMatch = true
				for (sharedStop: SharedStop in sharedStops) {
					if (sharedStop.equals(stop)) {
						noMatch = false
						break
					}
				}

				// If the stop doesn't match add it to the potential stops array since its not shared.
				if (noMatch) {
					try {
						potentialStops[finalIndex] = stop
						finalIndex++
					} catch (e: ArrayIndexOutOfBoundsException) {

						// If the array was out of bounds then log it (catastrophic if left unchecked).
						Log.e("remvStpsWthShredStps",
						      "Failed to add stop ${stop.name} " +
						      "from route ${stop.route.routeName} to array\n" +
						      "Final stops array is too small!", e)
					}
				}
			}

			// If the final index does match the stop length minus shared stop length log how much it was off by.
			// This is left over from debugging, but is still useful to know.
			if (finalIndex != stops.size - sharedStops.size) {
				Log.i("remvStpsWthShredStps", "Final index differs from standard number! " +
				      "(${stops.size - sharedStops.size}d vs $finalIndex)")
			}

			// Return our final stop array.
			val finalStops: Array<Stop?> = arrayOfNulls(finalIndex)
			System.arraycopy(potentialStops, 0, finalStops, 0, finalIndex)
			return finalStops as Array<Stop>
		}
	}

	init {

		// Set the initial circle size.
		setCircleSizes(INITIAL_CIRCLE_SIZE)
	}
}