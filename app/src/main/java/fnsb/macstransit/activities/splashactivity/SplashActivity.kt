package fnsb.macstransit.activities.splashactivity

import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import fnsb.macstransit.R
import fnsb.macstransit.activities.mapsactivity.MapsActivity
import fnsb.macstransit.databinding.SplashscreenBinding
import fnsb.macstransit.routematch.SharedStop
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Created by Spud on 2019-11-04 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 3.0.
 * @since Beta 7.
 */
class SplashActivity : androidx.appcompat.app.AppCompatActivity() {

	/**
	 * Documentation
	 */
	lateinit var viewModel: SplashViewModel
		private set

	/**
	 * Documentation
	 */
	private lateinit var binding: SplashscreenBinding

	override fun onCreate(savedInstanceState: android.os.Bundle?) {
		super.onCreate(savedInstanceState)

		// Setup view model.
		this.viewModel = androidx.lifecycle.ViewModelProvider(this).get(SplashViewModel::class.java)

		// Setup data binding.
		this.binding = SplashscreenBinding.inflate(this.layoutInflater)
		this.binding.viewmodel = this.viewModel
		this.binding.lifecycleOwner = this

		// Set the view to that of the splash screen.
		this.setContentView(this.binding.root)

		// Psst. Hey. Wanna know a secret?
		// In the debug build you can click on the logo to launch right into the maps activity.
		// This is mainly for a bypass on Sundays. :D
		if (fnsb.macstransit.BuildConfig.DEBUG) {
			this.binding.logo.setOnClickListener { this.launchMapsActivity() }
		}

		// Set the button widget to have no current onClickListener, and set it to be invisible for now.
		this.binding.button.setOnClickListener(null)
		this.binding.button.visibility = View.INVISIBLE

		// If the SDK supports it, assign the progress minimum.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			this.binding.progressBar.min = 0
		}

		// Make sure the progress bar is visible to the user.
		this.binding.progressBar.visibility = View.VISIBLE

		// Set how the progress bar updates.
		this.viewModel.currentProgress.observe(this, {

			// Set the progress to indeterminate if its less than 1.
			this.binding.progressBar.isIndeterminate = it <= 0.0

			// Animate the progress bar.
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				this.binding.progressBar.setProgress(it, true)
			} else {

				// Comments
				this.binding.progressBar.progress = it
			}
		})

		// Set how the progress bar appears and disappears.
		this.viewModel.progressBarVisible.observe(this, {
			if (it) {
				this.binding.progressBar.visibility = View.VISIBLE
			} else {
				this.binding.progressBar.visibility = View.INVISIBLE
			}
		})
	}

	override fun onPause() {
		super.onPause()

		// Simply close the application, since it hasn't finished loading.
		if (!loaded) {
			this.finishAffinity()
		}
	}

	override fun onResume() { // Comments
		super.onResume()

		this.lifecycleScope.launch {

			var noContinue = true
			this.launch(CoroutineName("InitialCoroutine"), start = CoroutineStart.UNDISPATCHED) {
				noContinue = !this@SplashActivity.initialCoroutine()
			}.join()

			if (noContinue) {
				return@launch
			}

			this.launch(CoroutineName("RouteCoroutine"), start = CoroutineStart.UNDISPATCHED) {
				this@SplashActivity.downloadCoroutine(LOAD_BUS_ROUTES.toDouble(), DOWNLOAD_BUS_ROUTES.toDouble(),
				                                      (DOWNLOAD_MASTER_SCHEDULE_PROGRESS + 
				                                       PARSE_MASTER_SCHEDULE).toDouble(), fnsb.
				macstransit.activities.splashactivity.splashscreenrunnables.DownloadBusRoutes(this@SplashActivity.viewModel))
			}.join()

			this.launch(CoroutineName("StopCoroutine"), start = CoroutineStart.UNDISPATCHED) {
				this@SplashActivity.downloadCoroutine(LOAD_BUS_STOPS.toDouble(), DOWNLOAD_BUS_STOPS.toDouble(),
				                                      (DOWNLOAD_MASTER_SCHEDULE_PROGRESS +
				                                       PARSE_MASTER_SCHEDULE + DOWNLOAD_BUS_ROUTES +
				                                       LOAD_BUS_ROUTES).toDouble(), fnsb.macstransit.
				activities.splashactivity.splashscreenrunnables.DownloadBusStops(this@SplashActivity.viewModel))
			}.join()

			this.launch(CoroutineName("SharedStopCoroutine"), start = CoroutineStart.UNDISPATCHED) {

				// Map shared stops.
				this@SplashActivity.mapSharedStops()
			}.join()

			launch(CoroutineName("ValidateStopCoroutine"), start = CoroutineStart.UNDISPATCHED) {

				// Validate stops.
				this@SplashActivity.validateStops()

			}.join()


			// Finally, launch the maps activity.
			Log.d("onResume", "End of lifecycle")
			this@SplashActivity.launchMapsActivity()
		}

		Log.d("onResume", "End of onResume")
	}

	/**
	 * Documentation
	 * Comments
	 */
	private suspend fun initialCoroutine() = coroutineScope {
		Log.d("initialCoroutine", "Starting initialCoroutine")
		val hasInternet = async(start = CoroutineStart.UNDISPATCHED) {

			// Check if the user has internet before continuing.
			this@SplashActivity.viewModel.setMessage(R.string.internet_check)
			kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { 
				this@SplashActivity.viewModel.hasInternet() 
			}
		}

		// Initialize the progress bar to 0.
		this@SplashActivity.viewModel.setProgressBar(0.0)
		this@SplashActivity.viewModel.showProgressBar()

		// Make sure the dynamic button is invisible.
		this@SplashActivity.binding.button.visibility = View.INVISIBLE

		Log.d("initialCoroutine", "Waiting for internet check...")
		if (!hasInternet.await()) {
			this@SplashActivity.noInternet()
			return@coroutineScope false
		}

		// Get the master schedule from the RouteMatch server
		Log.d("initialCoroutine", "Has internet!")
		this@SplashActivity.viewModel.setProgressBar(-1.0)
		this@SplashActivity.viewModel.setMessage(R.string.downloading_master_schedule)

		val downloadMasterSchedule = fnsb.macstransit.activities.splashactivity.splashscreenrunnables.
		DownloadMasterSchedule(this@SplashActivity)
		downloadMasterSchedule.download()
		Log.d("initialCoroutine", "Reached end of initialCoroutine")
		return@coroutineScope true
	}

	/**
	 * Documentation
	 * Comments
	 * @param loadProgress
	 * @param downloadProgress
	 * @param progressSoFar
	 * @param runnable
	 */
	private suspend fun downloadCoroutine(loadProgress: Double, downloadProgress: Double, 
	                                      progressSoFar: Double, runnable: fnsb.macstransit.activities.
			splashactivity.splashscreenrunnables.DownloadRouteObjects) = coroutineScope {

				var p = 0

				val step: Double = loadProgress / MapsActivity.allRoutes.size
				val progress: Double = progressSoFar + downloadProgress

				for (i in MapsActivity.allRoutes.indices) {
					p--

					this.launch(start = CoroutineStart.UNDISPATCHED) {
						runnable.download(MapsActivity.allRoutes[i], downloadProgress,
						                  progressSoFar, i)

						this@SplashActivity.viewModel.setProgressBar(progress + step +
						                                             MapsActivity.allRoutes.size + p)

						p++

						if (p == 0) {
							Log.d("downloadCoroutine", "Done mapping downloadable!")
						}
					}
				}

			}

	/**
	 * Changes the splash screen display when there is no internet.
	 * This method involves making the progress bar invisible,
	 * and setting the button to launch the wireless settings.
	 * It will also close the application when the button is clicked (as to force a restart of the app).
	 */
	@androidx.annotation.UiThread
	private fun noInternet() {

		// Then, set the message of the text view to notify the user that there is no internet connection.
		this.viewModel.setMessage(R.string.cannot_connect_internet)

		// Then setup the button to open the internet settings when clicked on, and make it visible.
		this.binding.button.setText(R.string.open_network_settings)
		this.binding.button.setOnClickListener {
			this.startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))

			// Also, close this application when clicked
			this.finish()
		}

		// Hide the progress bar.
		this.viewModel.hideProgressBar()

		// Set the button to invisible.
		this.binding.button.visibility = View.VISIBLE

		// Since technically everything (which is nothing) has been loaded, set the variable as so
		loaded = true
	}

	/**
	 * Adds the shared stops to the map.
	 * This is done by iterating through all the stops in each route and checking for duplicates.
	 * If there are any found they will be added to all the routes the stop belongs to as a shared stop.
	 * At this point the original stop is still present in the route.
	 */
	private suspend fun mapSharedStops() = coroutineScope {

		// Let the user know that we are checking for shared bus stops at this point.
		this@SplashActivity.viewModel.setMessage(R.string.shared_bus_stop_check)

		// Set the current progress.
		val step = LOAD_SHARED_STOPS.toDouble() / MapsActivity.allRoutes.size
		var currentProgress = (DOWNLOAD_MASTER_SCHEDULE_PROGRESS + PARSE_MASTER_SCHEDULE +
		                       DOWNLOAD_BUS_ROUTES + LOAD_BUS_ROUTES + DOWNLOAD_BUS_STOPS +
		                       LOAD_BUS_STOPS).toDouble()

		// Iterate though all the routes.
		for (routeIndex in MapsActivity.allRoutes.indices) {

			// Get a first comparison route.
			val route = MapsActivity.allRoutes[routeIndex]

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
				val sharedRoutes: Array<fnsb.macstransit.routematch.Route> = SharedStop.
				getSharedRoutes(route, routeIndex, stop)

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
			this@SplashActivity.viewModel.setProgressBar(currentProgress)
		}

		Log.d("mapSharedStops", "Reached end of mapSharedStops")
	}

	/**
	 * Validates the stops and shared stops.
	 * Meaning this method removes the stops that are shared stops as to not duplicate the stop.
	 */
	private suspend fun validateStops() = coroutineScope {

		// Let the user know that we are validating the stops (and shared stop) for each route.
		this@SplashActivity.viewModel.setMessage(R.string.stop_validation)

		// Determine the progress step.
		val step = VALIDATE_STOPS.toDouble() / MapsActivity.allRoutes.size
		var currentProgress =
				(DOWNLOAD_MASTER_SCHEDULE_PROGRESS + PARSE_MASTER_SCHEDULE + DOWNLOAD_BUS_ROUTES +
				 LOAD_BUS_ROUTES + DOWNLOAD_BUS_STOPS + LOAD_BUS_STOPS + LOAD_SHARED_STOPS).toDouble()

		// Iterate though all the routes and recreate the stops for each route.
		for (route in MapsActivity.allRoutes) {

			// Get the final stop count for each route by removing stops that are taken care of by the shared route object.
			val finalStops = SharedStop.removeStopsWithSharedStops(route.stops, route.sharedStops)
			Log.d("validateStops", "Final stop count: ${finalStops.size}")

			// Set the stops array for the route to the final determined stop array.
			// This array no longer contains the stops that are shared stops.
			route.stops = finalStops

			// Update the progress.
			currentProgress += step
			this@SplashActivity.viewModel.setProgressBar(currentProgress)
		}
	}

	/**
	 * Shows the retry button by setting the view to visible, hiding the progress bar,
	 * and by setting the click action of the button to launch the onResume() method once again.
	 */
	@androidx.annotation.AnyThread
	fun showRetryButton() {

		// Since we are updating UI elements, run the following on the UI thread.
		this.runOnUiThread {

			// First hide the progress bar since it is no longer of use.
			this.viewModel.hideProgressBar()

			// Then setup the button to relaunch the activity, and make it visible.
			this.binding.button.setText(R.string.retry)
			this.binding.button.setOnClickListener { this.onResume() }
			this.binding.button.visibility = View.VISIBLE
		}
	}

	/**
	 * Launches the maps activity.
	 */
	private fun launchMapsActivity() {

		// Set the loaded state to true as everything was loaded (or should have been loaded).
		loaded = true

		// Set the selected favorites routes to be false for the maps activity.
		MapsActivity.firstRun = true

		// Comments
		val mapsIntent = Intent(this, MapsActivity::class.java)

		// Start the MapsActivity, and close this splash activity.
		this.startActivity(mapsIntent)
		this.finishAfterTransition()
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
		const val VALIDATE_STOPS: Short = 8

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
		const val MAX_PROGRESS: Short = (DOWNLOAD_MASTER_SCHEDULE_PROGRESS + PARSE_MASTER_SCHEDULE +
		                                 DOWNLOAD_BUS_ROUTES + LOAD_BUS_ROUTES + DOWNLOAD_BUS_STOPS +
		                                 LOAD_BUS_STOPS + LOAD_SHARED_STOPS + VALIDATE_STOPS).toShort()

		/**
		 * Create a variable to check if the map activity has already been loaded
		 * (as to determine if the app needs to close when the back button is clicked,
		 * or just needs to refresh the activity)
		 */
		var loaded = false
	}

}