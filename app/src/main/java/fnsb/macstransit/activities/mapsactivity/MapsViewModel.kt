package fnsb.macstransit.activities.mapsactivity

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import fnsb.macstransit.activities.FarePopupWindow
import fnsb.macstransit.activities.mapsactivity.maplisteners.AdjustZoom
import fnsb.macstransit.routematch.Bus
import fnsb.macstransit.routematch.RouteMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.ConcurrentModificationException

/**
 * Created by Spud on 8/21/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
class MapsViewModel(application: Application) : AndroidViewModel(application) {

	/**
	 * Create an array of all the buses that will be used
	 * (either shown or hidden) on the map at any given time.
	 * For now just initialize this array to 0.
	 */
	var buses: Array<Bus> = emptyArray()

	/**
	 * Create a variable to store our fare popup window instance.
	 */
	val farePopupWindow: FarePopupWindow = FarePopupWindow(this.getApplication())

	/**
	 * Documentation
	 */
	lateinit var routeMatch: RouteMatch
	private set

	/**
	 * Documentation
	 */
	private var loadedRouteMatch: Boolean = false

	/**
	 * Documentation
	 */
	fun setRouteMatch(bundle: Bundle) {
		if (!this.loadedRouteMatch) {
			val url: String = try {
				bundle.getString("RouteMatch")!!
			} catch (exception: NullPointerException) {
				Log.e("setRouteMatch", "Could not find URL for routematch", exception)
				return
			}
			this.routeMatch = RouteMatch(url, this.getApplication())
			this.loadedRouteMatch = true
		}
	}

	/**
	 * Draws the stops and shared stops onto the map,
	 * and adjusts the stop sizes based on the zoom level.
	 */
	fun drawStops(map: GoogleMap) {

		viewModelScope.launch(Dispatchers.Main) {

			// Iterate though all the routes as we know at this point that they are not null.
			for (route in MapsActivity.allRoutes) {

				// Iterate though the stops in the route before getting to the shared stops.
				for (stop in route.stops) {

					if (route.enabled) {

						// Show the stop on the map.
						stop.showStop(map)
					} else {

						// Hide the stop from the map.
						// Be sure its only hidden and not actually destroying the object.
						stop.hideStop()
					}
				}

				// Check that there are shared stops to hide in the route.
				val sharedStops = route.sharedStops

				// Iterate though the shared stops in the route.
				for (sharedStop in sharedStops) {
					if (route.enabled) {

						// Show the shared stops.
						sharedStop.showSharedStop(map)
					} else {

						// Hide the shared stops on the map.
						// Note that the stops should be hidden - not destroyed.
						sharedStop.hideStop()
					}
				}
			}

			// Adjust the circle sizes of the stops on the map given the current zoom.
			AdjustZoom.resizeStops(map)

		}
	}

	/**
	 * Draws the route's polylines to the map.
	 * While all polylines are drawn they will only be shown if the route that they belong to is enabled.
	 */
	fun drawRoutes(map: GoogleMap) {

		viewModelScope.launch(Dispatchers.Main) {

			// Start by iterating through all the routes.
			for (route in MapsActivity.allRoutes) {
				try {

					// Check if the route has a polyline to set visible.
					if (route.polyline == null) {

						// Create a new polyline for the route since it didn't have one before.
						route.createPolyline(map)
					}

					// Set the polyline's visibility to whether the route is enabled or not.
					route.polyline!!.isVisible = route.enabled
				} catch (e: java.lang.NullPointerException) {

					// If the polyline was still null after being created, log it as a warning.
					Log.w("drawRoutes", "Polyline for route ${route.routeName} " +
					                    "was not created successfully!")
				}
			}
		}
	}

	/**
	 * (Re)draws the buses on the map.
	 * While all buses are drawn they will only be shown if the route that they belong to is enabled.
	 *
	 * @throws ConcurrentModificationException Concurrent exception may be thrown
	 * as it iterates through the bus list,
	 * which may be modified at the time of iteration.
	 */
	@Throws(ConcurrentModificationException::class)
	fun drawBuses(map: GoogleMap) {

		viewModelScope.launch(Dispatchers.Main) {

			// Start by iterating though all the buses on the map.
			for (bus in this@MapsViewModel.buses) {

				// Comments
				val route = bus.route
				if (bus.marker != null) {

					// Set the bus marker visibility based on if the bus's route is enabled or not.
					bus.marker!!.isVisible = route.enabled
				} else {
					if (route.enabled) {

						// Try creating a new marker for the bus (if its enabled).
						bus.addMarker(map, bus.color)
						bus.marker!!.isVisible = true

					} else {

						// If the marker was null simply log it as a warning.
						Log.w("drawBuses", "Bus doesn't have a marker for route "+
						                   "${route.routeName}!")
					}
				}
			}
		}
	}

}