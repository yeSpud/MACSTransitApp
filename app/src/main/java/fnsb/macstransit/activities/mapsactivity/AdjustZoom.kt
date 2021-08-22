package fnsb.macstransit.activities.mapsactivity

import android.util.Log
import com.google.android.gms.maps.GoogleMap
import kotlin.math.pow

/**
 * Created by Spud on 2019-10-28 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 3.0.
 * @since Beta 7.
 */
@androidx.annotation.UiThread
class AdjustZoom(private val map: GoogleMap) : GoogleMap.OnCameraIdleListener {

	/**
	 * Called when camera movement has ended,
	 * there are no pending animations and the user has stopped interacting with the map.
	 *
	 * This is called on the Android UI thread.
	 */
	override fun onCameraIdle() {

		// Simply call the resize function.
		resizeStops(this.map)
	}

	companion object {

		/**
		 * Resizes the stop and shared stop circles on the map.
		 * This works regardless of whether or not a particular route is enabled or disabled.
		 *
		 * @param map TODO
		 */
		@JvmStatic
		fun resizeStops(map: GoogleMap) {

			/*
			 * Calculate meters per pixel.
			 * This will be used to determine the circle size as we want it it be 4 meters in size.
			 * To calculate this we will need the current zoom as well as the cameras latitude.
			 */
			val zoom = map.cameraPosition.zoom
			val lat = map.cameraPosition.target.latitude

			// With the zoom and latitude determined we can then calculate meters per pixel.
			val metersPerPixel =
					156543.03392 * kotlin.math.cos(lat * Math.PI / 180.0) / 2.0.pow(zoom.toDouble())
			Log.v("resizeStops", "Meters / Pixel: $metersPerPixel")

			// Get the size of the circle to resize to.
			val size = metersPerPixel * 4
			Log.d("resizeStops", "Setting circle size to: ${metersPerPixel * 4}")

			// Iterate though each route.
			for (route in MapsActivity.allRoutes) {

				// Start by resizing the stop circles first.
				for (stop in route.stops) {
					if (stop.circle != null) {
						stop.circle!!.radius = size
					}
				}

				// Then resize the route's shared stop circles.
				val sharedStops = route.sharedStops
				if (sharedStops.isNotEmpty()) {
					for (sharedStop in sharedStops) {
						sharedStop.setCircleSizes(size)
					}
				}
			}
		}
	}
}