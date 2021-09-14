package fnsb.macstransit.activities.loadingactivity

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import fnsb.macstransit.R
import fnsb.macstransit.activities.loadingactivity.loadingscreenrunnables.DownloadBusStops
import fnsb.macstransit.activities.mapsactivity.MapsActivity
import fnsb.macstransit.databinding.LoadingscreenBinding
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
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
		//this.binding.button.setOnClickListener(null)

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
		if (!this.loaded) { this.finishAffinity() }
	}

	override fun onResume() {
		super.onResume()

		// Comments
		this.viewModel.resetVisibilities()

		// As there are a lot of operations to run to get the app started be sure to run all of them on a coroutine.
		this.lifecycleScope.launch(Dispatchers.IO, CoroutineStart.LAZY) {

			// Run various initial checks and download the master schedule.
			val passedInit: Boolean = this@LoadingActivity.viewModel.masterScheduleCoroutine(this@LoadingActivity)

			// If the initial checks fail then just exit early by returning.
			Log.d("StartupCoroutine", "Passed init: $passedInit")
			if (!passedInit) { return@launch }

			// Check if there are routes available for the day.
			if (this@LoadingActivity.viewModel.routes.isEmpty()) {
				this@LoadingActivity.viewModel.setMessage(R.string.its_sunday)
				this@LoadingActivity.viewModel.showRetryButton { this@LoadingActivity.onResume() }
				return@launch
			}

			// Download and load the bus stops.
			withContext(this.coroutineContext) {
				this@LoadingActivity.viewModel.downloadCoroutine(LOAD_BUS_STOPS.toDouble(),
				                                                 DOWNLOAD_BUS_STOPS.toDouble(),
				                  (DOWNLOAD_MASTER_SCHEDULE_PROGRESS + PARSE_MASTER_SCHEDULE).toDouble(),
				                  DownloadBusStops(this@LoadingActivity.viewModel))
			}

			// Map the shared stops on a coroutine.
			this@LoadingActivity.viewModel.mapSharedStops()

			// Validate the stops.
			// Let the user know that we are validating the stops (and shared stop) for each route.
			this@LoadingActivity.viewModel.setMessage(R.string.stop_validation)

			// Comments
			this@LoadingActivity.viewModel.setProgressBar((DOWNLOAD_MASTER_SCHEDULE_PROGRESS +
			                                               PARSE_MASTER_SCHEDULE + DOWNLOAD_BUS_STOPS +
			                                               LOAD_BUS_STOPS + LOAD_SHARED_STOPS).toDouble())

			// Iterate though all the routes and recreate the stops for each route.
			// Purge the stops that have shared stops.
			for ((_, route) in this@LoadingActivity.viewModel.routes) { route.purgeStops() }


			// Comments
			this@LoadingActivity.viewModel.setProgressBar(MAX_PROGRESS.toDouble())

			// Finally, launch the maps activity.
			Log.d("onResume", "End of lifecycle")
			this@LoadingActivity.launchMapsActivity()
		}.start()

		Log.d("onResume", "End of onResume")
	}

	/**
	 * Documentation
	 */
	fun allowForRetry() {

		// TODO Log and comment
		this.viewModel.showRetryButton { this.onResume() }
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