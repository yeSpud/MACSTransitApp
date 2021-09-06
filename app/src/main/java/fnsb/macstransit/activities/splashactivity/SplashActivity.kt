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
	 * The view model for the Splash Activity.
	 */
	lateinit var viewModel: SplashViewModel
		private set

	/**
	 * The binding used to get elements from the activity xml.
	 */
	private lateinit var binding: SplashscreenBinding

	/**
	 * Create a variable to check if the splash activity has already been loaded
	 * (as to determine if the app needs to close when the back button is clicked,
	 * or just needs to refresh the activity)
	 */
	var loaded: Boolean = false

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

				// Set the progress bar to the current progress.
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
		if (!this.loaded) {
			this.finishAffinity()
		}
	}

	override fun onResume() {
		super.onResume()

		// As there are a lot of operations to run to get the app started be sure to run all of them on a coroutine.
		this.lifecycleScope.launch {

			// Run various initial checks and download the master schedule.
			// Do all this work on a coroutine.
			var noContinue = true
			this.launch(CoroutineName("InitialCoroutine"), start = CoroutineStart.UNDISPATCHED) {
				noContinue = !this@SplashActivity.initialCoroutine()
			}.join()

			// If the initial checks fail then just exit early by returning.
			if (noContinue) {
				return@launch
			}

			// Download and load the bus routes (on a coroutine of course).
			this.launch(CoroutineName("RouteCoroutine"), start = CoroutineStart.UNDISPATCHED) {
				this@SplashActivity.downloadCoroutine(LOAD_BUS_ROUTES.toDouble(), DOWNLOAD_BUS_ROUTES.toDouble(),
				                                      (DOWNLOAD_MASTER_SCHEDULE_PROGRESS + 
				                                       PARSE_MASTER_SCHEDULE).toDouble(), fnsb.
				macstransit.activities.splashactivity.splashscreenrunnables.DownloadBusRoutes(this@SplashActivity.viewModel))
			}.join()

			// Download and load the bus stops (on a coroutine of course).
			this.launch(CoroutineName("StopCoroutine"), start = CoroutineStart.UNDISPATCHED) {
				this@SplashActivity.downloadCoroutine(LOAD_BUS_STOPS.toDouble(), DOWNLOAD_BUS_STOPS.toDouble(),
				                                      (DOWNLOAD_MASTER_SCHEDULE_PROGRESS +
				                                       PARSE_MASTER_SCHEDULE + DOWNLOAD_BUS_ROUTES +
				                                       LOAD_BUS_ROUTES).toDouble(), fnsb.macstransit.
				activities.splashactivity.splashscreenrunnables.DownloadBusStops(this@SplashActivity.viewModel))
			}.join()

			// Map the shared stops on a coroutine (as this is work intensive).
			this.launch(CoroutineName("SharedStopCoroutine"), start = CoroutineStart.UNDISPATCHED) {
				this@SplashActivity.mapSharedStops() }.join()


			// Validate stops on a coroutine (as it can be somewhat intensive).
			launch(CoroutineName("ValidateStopCoroutine"), start = CoroutineStart.UNDISPATCHED) {
				this@SplashActivity.validateStops() }.join()

			// Finally, launch the maps activity.
			Log.d("onResume", "End of lifecycle")
			this@SplashActivity.launchMapsActivity()
		}

		Log.d("onResume", "End of onResume")
	}

	/**
	 * Runs various initial checks such as checking for internet and downloading (and parsing) the master schedule.
	 */
	private suspend fun initialCoroutine() = coroutineScope {
		Log.d("initialCoroutine", "Starting initialCoroutine")

		// Check if there is an internet connection.
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

		// If there is no internet access then run the noInternet method and return false.
		if (!hasInternet.await()) {
			this@SplashActivity.noInternet()
			return@coroutineScope false
		}

		// Get the master schedule from the RouteMatch server
		Log.d("initialCoroutine", "Has internet!")
		this@SplashActivity.viewModel.setProgressBar(-1.0)
		this@SplashActivity.viewModel.setMessage(R.string.downloading_master_schedule)

		// Download the master schedule.
		val downloadMasterSchedule = fnsb.macstransit.activities.splashactivity.splashscreenrunnables.
		DownloadMasterSchedule(this@SplashActivity)
		downloadMasterSchedule.download()

		// If we've made it to the end without interruption or error return true (success).
		Log.d("initialCoroutine", "Reached end of initialCoroutine")
		return@coroutineScope true
	}

	/**
	 * Runs the download runnable on a coroutine and updates the progress while doing so.
	 *
	 * @param loadProgress The load progress value that will be added once the downloadable has been parsed.
	 * @param downloadProgress The download progress value that will be added once the download has finished.
	 * @param progressSoFar The progress that has currently elapsed out of the MAX_PROGRESS
	 * @param runnable The download runnable to run.
	 */
	private suspend fun downloadCoroutine(loadProgress: Double, downloadProgress: Double, 
	                                      progressSoFar: Double, runnable: fnsb.macstransit.activities.
			splashactivity.splashscreenrunnables.DownloadRouteObjects) = coroutineScope {

		// Create a variable to store the current state of our current downloads.
		// When the download is queued this value decreases.
		// When the download has completed this value increases.
		var downloadQueue = 0

		// Get the progress step.
		val step: Double = loadProgress / MapsActivity.allRoutes.size

		// Get the current progress.
		val progress: Double = progressSoFar + downloadProgress

		// Iterate though all the indices of all the routes that can be tracked.
		MapsActivity.allRoutes.indices.forEach {

			// Decrease the download queue (as we are queueing a new downloadable).
			downloadQueue--

			// Run the download function of our DownloadRoute object, and pass any necessary parameters.
			this.launch(start = CoroutineStart.UNDISPATCHED) {
				runnable.download(MapsActivity.allRoutes[it], downloadProgress, progressSoFar, it)

				// Update the current progress.
				this@SplashActivity.viewModel.setProgressBar(progress + step +
				                                             MapsActivity.allRoutes.size + downloadQueue)

				// Increase the downloaded queue as our downloadable has finished downloading.
				downloadQueue++

				// If the download queue has returned back to 0 log that downloading has been completed.
				if (downloadQueue == 0) {
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

			// Open the WiFi settings.
			this.startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))

			// Also, close this application when the button clicked.
			// (Like closing the door on its way out).
			this.finish()
		}

		// Hide the progress bar.
		this.viewModel.hideProgressBar()

		// Set the button to invisible.
		this.binding.button.visibility = View.VISIBLE

		// Since technically everything (which is nothing) has been loaded, set the variable as so
		this.loaded = true
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
		for (routeIndex: Int in MapsActivity.allRoutes.indices) {

			// Get a first comparison route.
			val route = MapsActivity.allRoutes[routeIndex]

			// If there are no stops to iterate over just continue with the next iteration.
			if (route.stops.isEmpty()) {
				continue
			}

			// Iterate through all the stops in our first comparison route.
			for (stop in route.stops) {

				// Make sure our stop is not already in our shared stop.
				// Do this by iterating though the shared stops in the route.
				var found = false
				for (sharedStop: SharedStop in route.sharedStops) {

					// If the route was found in the shared stop then skip this iteration of the loop by continuing.
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
		MapsActivity.allRoutes.forEach {

			// Get the final stop count for each route by removing stops that are taken care of by the shared route object.
			val finalStops = SharedStop.removeStopsWithSharedStops(it.stops, it.sharedStops)
			Log.d("validateStops", "Final stop count: ${finalStops.size}")

			// Set the stops array for the route to the final determined stop array.
			// This array no longer contains the stops that are shared stops.
			it.stops = finalStops

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
		this.loaded = true

		// Set the selected favorites routes to be false for the maps activity.
		MapsActivity.firstRun = true

		// Get the intent to start the MapsActivity.
		val mapsIntent = Intent(this, MapsActivity::class.java)

		// Start the MapsActivity, and close this splash activity.
		this.startActivity(mapsIntent)
		this.finishAfterTransition()
	}

	companion object {

		/**
		 * Number of iterations it takes to download the master schedule.
		 */
		const val DOWNLOAD_MASTER_SCHEDULE_PROGRESS: Short = 1

		/**
		 *  Number of iterations it takes to parse the master schedule.
		 */
		const val PARSE_MASTER_SCHEDULE: Short = 8

		/**
		 * Number of iterations (on average) it takes to download the bus routes.
		 */
		const val DOWNLOAD_BUS_ROUTES: Short = 8

		/**
		 * Number of iterations (on average) it takes to load the bus routes.
		 */
		const val LOAD_BUS_ROUTES: Short = 8

		/**
		 * Number of iterations (on average) it takes to download the bus stops.
		 */
		const val DOWNLOAD_BUS_STOPS: Short = 8

		/**
		 * Number of iterations (on average) it takes to load the bus stops.
		 */
		const val LOAD_BUS_STOPS: Short = 8

		/**
		 * Number of iterations (on average) it takes to load the shared bus stops.
		 */
		const val LOAD_SHARED_STOPS: Short = 8

		/**
		 * Number of iterations (on average) it takes to validate the stops and shared stops.
		 */
		const val VALIDATE_STOPS: Short = 8

		/**
		 * The max progress for the progress bar.
		 * The max progress is determined by adding all of the const values.
		 */
		const val MAX_PROGRESS: Short = (DOWNLOAD_MASTER_SCHEDULE_PROGRESS + PARSE_MASTER_SCHEDULE +
		                                 DOWNLOAD_BUS_ROUTES + LOAD_BUS_ROUTES + DOWNLOAD_BUS_STOPS +
		                                 LOAD_BUS_STOPS + LOAD_SHARED_STOPS + VALIDATE_STOPS).toShort()
	}

}