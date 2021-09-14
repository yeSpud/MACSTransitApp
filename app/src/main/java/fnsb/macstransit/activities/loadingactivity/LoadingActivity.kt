package fnsb.macstransit.activities.loadingactivity

import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import fnsb.macstransit.R
import fnsb.macstransit.activities.loadingactivity.loadingscreenrunnables.DownloadBusStops
import fnsb.macstransit.activities.loadingactivity.loadingscreenrunnables.DownloadMasterSchedule
import fnsb.macstransit.activities.mapsactivity.MapsActivity
import fnsb.macstransit.databinding.LoadingscreenBinding
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.routematch.SharedStop
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Spud on 2019-11-04 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 3.2.
 * @since Beta 7.
 */
class LoadingActivity : androidx.appcompat.app.AppCompatActivity() {

	/**
	 * The view model for the Splash Activity.
	 */
	lateinit var viewModel: LoadingViewModel
		private set

	/**
	 * The binding used to get elements from the activity xml.
	 */
	lateinit var binding: LoadingscreenBinding

	/**
	 * Create a variable to check if the splash activity has already been loaded
	 * (as to determine if the app needs to close when the back button is clicked,
	 * or just needs to refresh the activity)
	 */
	var loaded: Boolean = false

	override fun onCreate(savedInstanceState: android.os.Bundle?) {
		super.onCreate(savedInstanceState)

		// Setup view model.
		this.viewModel = androidx.lifecycle.ViewModelProvider(this).get(LoadingViewModel::class.java)

		// Setup data binding.
		this.binding = DataBindingUtil.setContentView(this, R.layout.loadingscreen)
		this.binding.viewmodel = this.viewModel
		this.binding.lifecycleOwner = this

		// Psst. Hey. Wanna know a secret?
		// In the debug build you can click on the logo to launch right into the maps activity.
		// This is mainly for a bypass on Sundays. :D
		if (fnsb.macstransit.BuildConfig.DEBUG) {
			this.binding.logo.setOnClickListener { this.launchMapsActivity() }
		}

		// Set the button widget to have no current onClickListener, and set it to be invisible for now.
		this.binding.button.setOnClickListener(null)

		// If the SDK supports it, assign the progress minimum.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			this.binding.progressBar.min = 0
		}

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
		this.lifecycleScope.launch(Dispatchers.Main, CoroutineStart.LAZY) {

			// Run various initial checks and download the master schedule.
			val passedInit: Boolean = this@LoadingActivity.initialCoroutine()

			// If the initial checks fail then just exit early by returning.
			Log.d("StartupCoroutine", "Passed init: $passedInit")
			if (!passedInit) { return@launch }

			// Check if there are routes available for the day.
			if (this@LoadingActivity.viewModel.routes.isEmpty()) {
				this@LoadingActivity.viewModel.setMessage(R.string.its_sunday)
				this@LoadingActivity.showRetryButton()
				return@launch
			}

			// Download and load the bus stops.
			withContext(this.coroutineContext) {
				this@LoadingActivity.
				downloadCoroutine(LOAD_BUS_STOPS.toDouble(), DOWNLOAD_BUS_STOPS.toDouble(),
				                  (DOWNLOAD_MASTER_SCHEDULE_PROGRESS + PARSE_MASTER_SCHEDULE).toDouble(),
				                  DownloadBusStops(this@LoadingActivity.viewModel))
			}

			// Map the shared stops on a coroutine.
			this@LoadingActivity.mapSharedStops()

			// Validate the stops.
			this@LoadingActivity.validateStops()

			// Finally, launch the maps activity.
			Log.d("onResume", "End of lifecycle")
			this@LoadingActivity.launchMapsActivity()
		}.start()

		Log.d("onResume", "End of onResume")
	}

	/**
	 * Runs various initial checks such as checking for internet and downloading (and parsing) the master schedule.
	 */
	private suspend fun initialCoroutine(): Boolean {
		Log.d("initialCoroutine", "Starting initialCoroutine")

		// Check if the user has internet before continuing.
		this.viewModel.setMessage(R.string.internet_check)

		// Initialize the progress bar to 0.
		this.viewModel.setProgressBar(0.0)
		this.viewModel.showProgressBar()

		// Make sure the dynamic button is invisible.
		this.binding.button.visibility = View.INVISIBLE

		Log.d("initialCoroutine", "Waiting for internet check...")

		// If there is no internet access then run the noInternet method and return false.
		if (!this.viewModel.hasInternet()) {
			this.viewModel.noInternet(this)
			return false
		}

		// Get the master schedule from the RouteMatch server
		Log.d("initialCoroutine", "Has internet!")
		this.viewModel.setProgressBar(-1.0)
		this.viewModel.setMessage(R.string.downloading_master_schedule)

		// Download and parse the master schedule. Use a filler route as the first parameter.
		val fillerRoute = Route("filler")
		DownloadMasterSchedule(this@LoadingActivity).download(fillerRoute, DOWNLOAD_MASTER_SCHEDULE_PROGRESS.toDouble(), 0.0, 0)

		// If we've made it to the end without interruption or error return true (success).
		Log.d("initialCoroutine", "Reached end of initialCoroutine")
		return true
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
	                                          progressSoFar: Double, runnable: fnsb.macstransit.
			activities.loadingactivity.loadingscreenrunnables.DownloadRouteObjects<Unit>) = coroutineScope {

		// Create a variable to store the current state of our current downloads.
		// When the download is queued this value decreases.
		// When the download has completed this value increases.
		var downloadQueue = 0

		// Get the progress step.
		val step: Double = loadProgress / this@LoadingActivity.viewModel.routes.size

		// Get the current progress.
		val progress: Double = progressSoFar + downloadProgress

		// Iterate though all the indices of all the routes that can be tracked.
		var i = 0
		for ((_, route) in this@LoadingActivity.viewModel.routes) {

			// Decrease the download queue (as we are queueing a new downloadable).
			downloadQueue--

			// Run the download function of our DownloadRoute object, and pass any necessary parameters.
			this.launch(start = CoroutineStart.UNDISPATCHED) {

				// Run the downloadable.
				runnable.download(route, downloadProgress, progressSoFar, i)

				// Update the current progress.
				this@LoadingActivity.viewModel.setProgressBar(progress + step + downloadQueue +
				                                              this@LoadingActivity.viewModel.routes.size)

				// Increase the downloaded queue as our downloadable has finished downloading.
				downloadQueue++

				// If the download queue has returned back to 0 log that downloading has been completed.
				if (downloadQueue == 0) {
					Log.d("downloadCoroutine", "Done mapping downloadable!")
				}
			}
			i++
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
		this.viewModel.setMessage(R.string.shared_bus_stop_check)

		// Set the current progress.
		val step = LOAD_SHARED_STOPS.toDouble() / this.viewModel.routes.size
		var currentProgress = (DOWNLOAD_MASTER_SCHEDULE_PROGRESS + PARSE_MASTER_SCHEDULE +
		                       DOWNLOAD_BUS_STOPS + LOAD_BUS_STOPS).toDouble()

		// Iterate though each route in all our trackable routes.
		for ((_, route) in this.viewModel.routes) {

			// If there are no stops to iterate over in our route just continue with the next iteration.
			if (route.stops.isEmpty()) { continue }

			// Iterate through all the stops in our first comparison route.
			for ((name, stop) in route.stops) {

				// Make sure the stop is not already in the route's shared stops.
				// If the stop was found as a shared stop then skip this iteration of the loop by continuing.
				if (route.sharedStops[name] != null) { continue }

				// Get an array of shared routes.
				val sharedRoutes: Array<Route> = SharedStop.getSharedRoutes(route, stop, this.viewModel.routes)

				// If the shared routes array has more than one entry, create a new shared stop object.
				if (sharedRoutes.size > 1) {
					val sharedStop = SharedStop(name, stop.location, sharedRoutes)

					// Iterate though all the routes in the shared route,
					// and add our newly created shared stop.
					sharedRoutes.forEach {
						Log.d("mapSharedStops", "Adding shared stop to route: ${this.
						viewModel.routes[it.name]!!.name}")

						this.viewModel.routes[it.name]!!.sharedStops[name] = sharedStop
					}
				}
			}

			// Update the progress.
			currentProgress += step
			this.viewModel.setProgressBar(currentProgress)
		}

		Log.d("mapSharedStops", "Reached end of mapSharedStops")
	}

	/**
	 * Validates the stops and shared stops.
	 * Meaning this method removes the stops that are shared stops as to not duplicate the stop.
	 */
	private fun validateStops() {

		// Let the user know that we are validating the stops (and shared stop) for each route.
		this@LoadingActivity.viewModel.setMessage(R.string.stop_validation)

		// Determine the progress step.
		val step = VALIDATE_STOPS.toDouble() / this.viewModel.routes.size
		var currentProgress =
				(DOWNLOAD_MASTER_SCHEDULE_PROGRESS + PARSE_MASTER_SCHEDULE + DOWNLOAD_BUS_STOPS +
				 LOAD_BUS_STOPS + LOAD_SHARED_STOPS).toDouble()

		// Iterate though all the routes and recreate the stops for each route.
		for ((name, route) in this.viewModel.routes) {

			// Purge the stops that have shared stops (and get the final count for debugging).
			route.purgeStops()
			Log.d("validateStops", "Final stop count for route $name: ${route.stops.size}")
			Log.d("validateStops", "Final shared stop count for route $name: ${route.sharedStops.size}")

			// Update the progress.
			currentProgress += step
			this.viewModel.setProgressBar(currentProgress)
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

		// Get the routes as parcelables.
		mapsIntent.putExtra("Routes", this.viewModel.routes.values.toTypedArray())

		// Start the MapsActivity, and close this splash activity.
		Log.d("launchMapsActivity", "Starting maps activity")
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
		const val VALIDATE_STOPS: Short = 1

		/**
		 * The max progress for the progress bar.
		 * The max progress is determined by adding all of the const values.
		 */
		const val MAX_PROGRESS: Short = (DOWNLOAD_MASTER_SCHEDULE_PROGRESS + PARSE_MASTER_SCHEDULE +
		                                 DOWNLOAD_BUS_STOPS + LOAD_BUS_STOPS + LOAD_SHARED_STOPS +
		                                 VALIDATE_STOPS).toShort()
	}

}