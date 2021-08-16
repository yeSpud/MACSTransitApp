package fnsb.macstransit.activities

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.util.Pair
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.AnyThread
import androidx.annotation.UiThread
import fnsb.macstransit.R
import fnsb.macstransit.activities.splashscreenrunnables.SplashListener
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.routematch.RouteMatch
import fnsb.macstransit.routematch.SharedStop
import kotlin.math.roundToInt

/**
 * Created by Spud on 2019-11-04 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 3.0.
 * @since Beta 7.
 */
class SplashActivity : androidx.appcompat.app.AppCompatActivity() {

	/**
	 * The TextView widget in the activity.
	 */
	private var textView: TextView? = null

	/**
	 * The ProgressBar widget in the activity.
	 */
	private var progressBar: ProgressBar? = null

	/**
	 * The Button widget in the activity.
	 */
	private var button: Button? = null

	/**
	 * Documentation
	 */
	private var routeMatch: RouteMatch? = null

	/**
	 * Documentation
	 */
	var mapBusProgress = 0

	/**
	 * Documentation
	 */
	var mapStopProgress = 0

	override fun onCreate(savedInstanceState: android.os.Bundle?) {
		super.onCreate(savedInstanceState)

		// Set the view to that of the splash screen.
		this.setContentView(R.layout.splashscreen)

		// Find the widgets of use in the splash screen, and assign them to their own private variables.
		this.textView = this.findViewById(R.id.textView)
		this.progressBar = this.findViewById(R.id.progressBar)
		this.button = this.findViewById(R.id.button)

		// Comments
		this.routeMatch = RouteMatch("https://fnsb.routematch.com/feed/", this)

		// Psst. Hey. Wanna know a secret?
		// In the debug build you can click on the logo to launch right into the maps activity.
		// This is mainly for a bypass on Sundays. :D
		if (fnsb.macstransit.BuildConfig.DEBUG) {
			this.findViewById<View>(R.id.logo).setOnClickListener { launchMapsActivity() }
		}

		// Comments
		if (this.button == null) {
			return
		}

		// Set the button widget to have no current onClickListener, and set it to be invisible for now.
		this.button!!.setOnClickListener(null)
		this.button!!.visibility = View.INVISIBLE

		// Comments
		if (this.progressBar == null) {
			return
		}

		// Setup the progress bar by defining its max, and if the SDK supports it, assign its min as well.
		this.progressBar!!.max = 100
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			this.progressBar!!.min = 0
		}

		// Make sure the progress bar is visible to the user.
		this.progressBar!!.visibility = View.VISIBLE
	}

	override fun onResume() {
		super.onResume()

		// Comments
		if (this.textView == null || this.progressBar == null || this.button == null) {
			return
		}

		// Initialize the progress bar to 0.
		this.progressBar!!.visibility = View.VISIBLE
		this.setProgressBar(0.0)

		// Make sure the dynamic button is invisible.
		this.button!!.visibility = View.INVISIBLE

		// Check if the user has internet before continuing.
		this.setMessage(R.string.internet_check)
		if (!this.hasInternet()) {
			this.noInternet()
			return
		}

		// Get the master schedule from the RouteMatch server
		this.setProgressBar(-1.0)
		this.setMessage(R.string.downloading_master_schedule)
		this.routeMatch!!.callMasterSchedule(fnsb.macstransit.activities.splashscreenrunnables.MasterScheduleCallback(this), {
			error: com.android.volley.VolleyError ->
			Log.w("initializeApp", "MasterSchedule callback error", error)
			this.setMessage(R.string.routematch_timeout)
			this.showRetryButton()
		})
	}

	override fun onPause() {
		super.onPause()

		// Simply close the application, since it hasn't finished loading.
		if (!loaded) {
			this.finishAffinity()
		}
	}

	/**
	 * Creates a thread that will run the initialization methods.
	 * The reason why this needs to be run on a new thread is if it was run on the UI thread
	 * it would cause the app to hang until all the methods are completed.
	 *
	 * @return The thread that will run all the necessary initialization methods.
	 */
	internal fun cleanupThread(): Thread { // TODO Separate and co-routine
		val thread = Thread {

			// Map shared stops.
			this.mapSharedStops()

			// Validate stops.
			this.validateStops()

			// Finally, launch the maps activity.
			this.launchMapsActivity()
		}

		// Set the name of the thread, and finally return it.
		thread.name = "Cleanup thread"
		return thread
	}

	/**
	 * Documentation
	 * Comments
	 */
	fun downloadBusRoutes() {

		if (MapsActivity.allRoutes == null) {
			return  // TODO Log
		}

		this.setMessage(R.string.loading_bus_routes)
		this.setProgressBar((DOWNLOAD_MASTER_SCHEDULE_PROGRESS + PARSE_MASTER_SCHEDULE).toDouble())
		val mapBusRoutes = fnsb.macstransit.activities.splashscreenrunnables.MapBusRoutes(routeMatch!!)

		for (route in MapsActivity.allRoutes!!) {
			mapBusProgress--
			val pair = Pair<Route, SplashListener>(route, fnsb.macstransit.activities.splashscreenrunnables.DownloadBusRoutes(this))
			mapBusRoutes.addListener(pair)
		}
		mapBusRoutes.getBusRoutes(this)
	}

	/**
	 * Loads the bus stops for every route. At this point shared stops are not implemented,
	 * so stops for separate routes will overlap.
	 *
	 * Documentation
	 * Comments
	 */
	internal fun downloadBusStops() { // TODO Make this a co-routine.

		// Verify that allRoutes is not null. If it is then log and return early.
		if (MapsActivity.allRoutes == null) {
			Log.w("mapBusStops", "All routes is null!")
			return
		}

		this.setMessage(R.string.loading_bus_stops)
		this.setProgressBar(
				(DOWNLOAD_MASTER_SCHEDULE_PROGRESS + PARSE_MASTER_SCHEDULE + DOWNLOAD_BUS_ROUTES + LOAD_BUS_ROUTES).toDouble())
		val mapBusStops = fnsb.macstransit.activities.splashscreenrunnables.MapBusStops(routeMatch!!)

		// Iterate thorough all the routes to load each stop.
		for (route in MapsActivity.allRoutes!!) {
			mapStopProgress--
			val pair = Pair<Route, SplashListener>(route, fnsb.macstransit.activities.splashscreenrunnables.DownloadBusStops(this))
			mapBusStops.addListener(pair)
		}
		mapBusStops.getBusStops(this)
	}

	/**
	 * Changes the splash screen display when there is no internet.
	 * This method involves making the progress bar invisible,
	 * and setting the button to launch the wireless settings.
	 * It will also close the application when the button is clicked (as to force a restart of the app).
	 */
	@UiThread
	private fun noInternet() {

		// First, hide the progress bar.
		this.progressBar!!.visibility = View.INVISIBLE

		// Then, set the message of the text view to notify the user that there is no internet connection.
		this.setMessage(R.string.cannot_connect_internet)

		// Then setup the button to open the internet settings when clicked on, and make it visible.
		this.button!!.setText(R.string.open_network_settings)
		this.button!!.setOnClickListener {
			this.startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))

			// Also, close this application when clicked
			this.finish()
		}

		// Set the button to invisible.
		this.button!!.visibility = View.VISIBLE

		// Since technically everything (which is nothing) has been loaded, set the variable as so
		loaded = true
	}

	/**
	 * Checks if the device has a current internet connection.
	 *
	 * @return Whether or not the device has an internet connection.
	 */
	@UiThread
	private fun hasInternet(): Boolean {

		// Get the connectivity manager for the device.
		val connectivityManager: ConnectivityManager = this.applicationContext
				.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager

		// Check the current API version (as behavior changes in later APIs).
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

			// Newer API.
			// Comments
			val network: android.net.Network? = connectivityManager.activeNetwork
			val networkCapabilities: NetworkCapabilities =
					connectivityManager.getNetworkCapabilities(network) ?: return false

			// Comments
			return when {
				// WiFi
				networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

				// Cellular Data
				networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

				// Ethernet
				networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true

				// Bluetooth
				networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true

				// No connectivity.
				else -> false
			}

		} else {

			// Older API.
			// Comments
			@Suppress("Deprecation")
			val networkInfo: android.net.NetworkInfo = connectivityManager.activeNetworkInfo ?: return false

			@Suppress("Deprecation")
			return networkInfo.isConnected

		}
	}

	/**
	 * Adds the shared stops to the map.
	 * This is done by iterating through all the stops in each route and checking for duplicates.
	 * If there are any found they will be added to all the routes the stop belongs to as a shared stop.
	 * At this point the original stop is still present in the route.
	 */
	private fun mapSharedStops() {

		// Let the user know that we are checking for shared bus stops at this point.
		this.setMessage(R.string.shared_bus_stop_check)

		// Verify that allRoutes is not null. If it is then log and return early.
		if (MapsActivity.allRoutes == null) {
			Log.w("mapSharedStops", "All routes is null!")
			return
		}

		// Set the current progress.
		val step = LOAD_SHARED_STOPS.toDouble() / MapsActivity.allRoutes!!.size
		var currentProgress =
				(DOWNLOAD_MASTER_SCHEDULE_PROGRESS + PARSE_MASTER_SCHEDULE + DOWNLOAD_BUS_ROUTES + LOAD_BUS_ROUTES + DOWNLOAD_BUS_STOPS + LOAD_BUS_STOPS).toDouble()

		// Iterate though all the routes.
		for (routeIndex in MapsActivity.allRoutes!!.indices) {

			// Get a first comparison route.
			val route = MapsActivity.allRoutes!![routeIndex]

			// If there are no stops to iterate over just continue with the next iteration.
			val stops = route.stops
			if (stops.isEmpty()) {
				continue
			}

			// Iterate through all the stops in our first comparison route.
			for (stop in stops) {

				// Make sure our stop is not already in our shared stop.
				val sharedStops = route.sharedStops
				var found = false

				// Iterate though the shared stops in the route.
				for (sharedStop in sharedStops) {

					// If the route was found, continue.
					if (sharedStop.equals(stop)) {
						found = true
						break
					}
				}
				if (found) {
					continue
				}

				// Get an array of shared routes.
				val sharedRoutes = SharedStop.getSharedRoutes(route, routeIndex, stop)

				// If the shared routes array has more than one entry, create a new shared stop object.
				if (sharedRoutes.size > 1) {
					val sharedStop = SharedStop(stop.location, stop.name, sharedRoutes)

					// Iterate though all the routes in the shared route, and add our newly created shared stop.
					for (sharedRoute in sharedRoutes) {
						sharedRoute.addSharedStop(sharedStop)
					}
				}
			}

			// Update the progress.
			currentProgress += step
			setProgressBar(currentProgress)
		}
	}

	/**
	 * Validates the stops and shared stops.
	 * Meaning this method removes the stops that are shared stops as to not duplicate the stop.
	 */
	private fun validateStops() {

		// Let the user know that we are validating the stops (and shared stop) for each route.
		this.setMessage(R.string.stop_validation)

		// Verify that allRoutes is not null. If it is then log and return early.
		if (MapsActivity.allRoutes == null) {
			Log.w("validateStops", "All routes is null!")
			return
		}

		// Determine the progress step.
		val step = VALIDATE_STOPS.toDouble() / MapsActivity.allRoutes!!.size
		var currentProgress =
				(DOWNLOAD_MASTER_SCHEDULE_PROGRESS + PARSE_MASTER_SCHEDULE + DOWNLOAD_BUS_ROUTES +
				 LOAD_BUS_ROUTES + DOWNLOAD_BUS_STOPS + LOAD_BUS_STOPS + LOAD_SHARED_STOPS).toDouble()

		// Iterate though all the routes and recreate the stops for each route.
		for (route in MapsActivity.allRoutes!!) {

			// Get the final stop count for each route by removing stops that are taken care of by the shared route object.
			val finalStops = SharedStop.removeStopsWithSharedStops(route.stops, route.sharedStops)
			Log.d("validateStops", "Final stop count: ${finalStops.size}")

			// Set the stops array for the route to the final determined stop array.
			// This array no longer contains the stops that are shared stops.
			route.stops = finalStops

			// Update the progress.
			currentProgress += step
			setProgressBar(currentProgress)
		}
	}

	/**
	 * Launches the maps activity.
	 */
	private fun launchMapsActivity() {

		// Set the loaded state to true as everything was loaded (or should have been loaded).
		loaded = true

		// Set the selected favorites routes to be false for the maps activity.
		MapsActivity.selectedFavorites = false

		val mapsIntent = Intent(this, MapsActivity::class.java)
		mapsIntent.putExtra("RouteMatch", routeMatch!!.url)

		// Start the MapsActivity, and close this splash activity.
		this.startActivity(mapsIntent)
		this.finishAfterTransition()
	}

	/**
	 * Sets the message content to be displayed to the user on the splash screen.
	 *
	 * @param resID The string ID of the message. This can be retrieved by calling R.string.STRING_ID.
	 */
	@AnyThread
	fun setMessage(@androidx.annotation.StringRes resID: Int) {

		// Since we are changing a TextView element, the following needs to be run on the UI thread.
		this.runOnUiThread {

			// Make sure the text view is not null.
			if (textView != null) {

				// Set the TextView text to that of the message.
				textView!!.setText(resID)
			} else {

				// Since the TextView is null, log that it hasn't been initialized yet.
				Log.w("setMessage", "TextView has not been initialized yet")
			}
		}
	}

	/**
	 * Update the progress bar to the current progress.
	 *
	 * @param progress The current progress out of SplashActivity.maxProgress.
	 */
	@AnyThread
	fun setProgressBar(progress: Double) {
		Log.v("setProgressBar", "Provided progress: $progress")

		// Because we are updating UI elements we need to run the following on the UI thread.
		this.runOnUiThread {

			// Convert the progress to be an int out of 100.
			var p: Int = (progress / MAX_PROGRESS * 100).roundToInt()

			// Validate that that the progress is between 0 and 100.
			p = if (p > 100) 100 else kotlin.math.max(p, 0)

			// Make sure the progress bar is not null.
			if (this.progressBar == null) {

				// Log that the progress bar has not been set up yet.
				Log.w("setProgressBar", "Progressbar has not been initialized yet")
			}

			// Set the progress to indeterminate if its less than 1.
			this.progressBar!!.isIndeterminate = progress < 0.0

			// Apply the progress to the progress bar, and animate it if its supported in the SDK.
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				progressBar!!.setProgress(p, true)
			} else {
				progressBar!!.progress = p
			}
		}
	}

	/**
	 * Shows the retry button by setting the view to visible, hiding the progress bar,
	 * and by setting the click action of the button to launch the onResume() method once again.
	 */
	@AnyThread
	fun showRetryButton() {

		// Since we are updating UI elements, run the following on the UI thread.
		this.runOnUiThread {

			// First hide the progress bar since it is no longer of use.
			this.progressBar!!.visibility = View.INVISIBLE

			// Then setup the button to relaunch the activity, and make it visible.
			this.button!!.setText(R.string.retry)
			this.button!!.setOnClickListener { this.onResume() }
			this.button!!.visibility = View.VISIBLE
		}
	}

	companion object {

		/**
		 * Documentation
		 */
		const val DOWNLOAD_MASTER_SCHEDULE_PROGRESS: Short = 1

		/**
		 * Documentation
		 */
		const val PARSE_MASTER_SCHEDULE: Short = 8

		/**
		 * Documentation
		 */
		const val DOWNLOAD_BUS_ROUTES: Short = 8

		/**
		 * Documentation
		 */
		const val LOAD_BUS_ROUTES: Short = 8

		/**
		 * Documentation
		 */
		const val DOWNLOAD_BUS_STOPS: Short = 8

		/**
		 * Documentation
		 */
		const val LOAD_BUS_STOPS: Short = 8

		/**
		 * Documentation
		 */
		const val LOAD_SHARED_STOPS: Short = 8

		/**
		 * Documentation
		 */
		const val VALIDATE_STOPS: Short = 1

		/**
		 * The max progress for the progress bar.
		 * The progress is determined the following checks:
		 *
		 *  * Downloading the master schedule (1)
		 *  * Load bus routes (Route) (8) - average number of routes
		 *  * Map the bus routes (Polyline) (8)
		 *  * Map the bus stops (1)
		 *  * Map the shared stops (8)
		 *  * Validate the stops (8)
		 *  Documentation
		 */
		const val MAX_PROGRESS: Short =
				(DOWNLOAD_MASTER_SCHEDULE_PROGRESS + PARSE_MASTER_SCHEDULE + DOWNLOAD_BUS_ROUTES + LOAD_BUS_ROUTES + DOWNLOAD_BUS_STOPS + LOAD_BUS_STOPS + LOAD_SHARED_STOPS + VALIDATE_STOPS).toShort()

		/**
		 * Create a variable to check if the map activity has already been loaded
		 * (as to determine if the app needs to close when the back button is clicked,
		 * or just needs to refresh the activity)
		 */
		var loaded = false
	}
}