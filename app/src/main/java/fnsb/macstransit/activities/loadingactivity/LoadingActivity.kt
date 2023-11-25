package fnsb.macstransit.activities.loadingactivity

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import fnsb.macstransit.BuildConfig
import fnsb.macstransit.R
import fnsb.macstransit.activities.mapsactivity.MapsActivity
import fnsb.macstransit.databinding.LoadingscreenBinding

/**
 * Created by Spud on 2019-11-04 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 3.3.
 * @since Beta 7.
 */
class LoadingActivity : AppCompatActivity() {

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

		// Setup data binding.
		binding = DataBindingUtil.setContentView(this, R.layout.loadingscreen)
		binding.viewmodel = ViewModelProvider(this)[LoadingViewModel::class.java]
		binding.lifecycleOwner = this

		// Psst. Hey. Wanna know a secret?
		// In the debug build you can click on the logo to launch right into the maps activity.
		// This is mainly for a bypass on Sundays. :D
		if (BuildConfig.DEBUG) {
			binding.logo.setOnClickListener { launchMapsActivity() }
		}

		// If the SDK supports it, assign the progress minimum.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			binding.progressBar.min = 0
		}

		// Set how the progress bar updates.
		binding.viewmodel!!.currentProgress.observe(this) {

			// Set the progress to indeterminate if its less than 1.
			binding.progressBar.isIndeterminate = it <= 0.0

			// Animate the progress bar.
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				binding.progressBar.setProgress(it, true)
			} else {

				// Set the progress bar to the current progress.
				binding.progressBar.progress = it
			}
		}
	}

	override fun onPause() {
		super.onPause()

		// Simply close the application, since it hasn't finished loading.
		if (!loaded) { finishAffinity() }
	}

	override fun onResume() {
		Log.v("onResume", "Start of onResume")
		super.onResume()

		// Reset the button and progress bar visibilities.
		binding.viewmodel!!.resetVisibilities()

		// Run all the startup actions on a coroutine.
		binding.viewmodel!!.startupCoroutine(this).start()

		// Log that the end of onResume has been reached.
		Log.d("onResume", "End of onResume")
	}

	/**
	 * Shows the retry button and sets the runnable action to the onResume method.
	 */
	fun allowForRetry() {

		// Set the button to launch the onResume method.
		binding.viewmodel!!.showRetryButton { onResume() }
	}

	/**
	 * Launches the maps activity.
	 */
	fun launchMapsActivity() {

		// Set the loaded state to true as everything was loaded (or should have been loaded).
		loaded = true

		// Set the selected favorites routes to be false for the maps activity.
		MapsActivity.firstRun = true

		// Get the intent to start the MapsActivity.
		val mapsIntent = Intent(this, MapsActivity::class.java)

		// Get the routes as parcelables.
		mapsIntent.putExtra("Routes", binding.viewmodel!!.routes.values.toTypedArray())

		// Start the MapsActivity, and close this splash activity.
		Log.d("launchMapsActivity", "Starting maps activity")
		startActivity(mapsIntent)
		finishAfterTransition()
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