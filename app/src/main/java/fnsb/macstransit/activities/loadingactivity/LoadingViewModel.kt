package fnsb.macstransit.activities.loadingactivity

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.AnyThread
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fnsb.macstransit.R
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.routematch.RouteMatch
import fnsb.macstransit.routematch.SharedStop
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Created by Spud on 8/16/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.1.
 * @since Release 1.3.
 */
class LoadingViewModel(application: Application) : androidx.lifecycle.AndroidViewModel(application) {


	/**
	 * The RouteMatch object used to retrieve data from the RouteMatch servers.
	 */
	val routeMatch: RouteMatch = RouteMatch(this.getApplication<Application>().getString(fnsb.macstransit.R.string.routematch_url), this.getApplication())

	/**
	 * All of the routes that can be tracked by the app. This will be determined by the master schedule.
	 */
	val routes: HashMap<String, fnsb.macstransit.routematch.Route> = HashMap()

	/**
	 * The current (adjustable) progress.
	 * This is private as we only want to adjust it the values in the view model.
	 */
	private val _currentProgress: MutableLiveData<Int> = MutableLiveData(0)

	/**
	 * The (unmodifiable) progress of the progressbar.
	 */
	val currentProgress: LiveData<Int>
		get() = this._currentProgress

	/**
	 * The current (adjustable) visibility of the progressbar.
	 * This is private as we only want to adjust the visibility in the view model.
	 */
	private val _progressBarVisible: MutableLiveData<Boolean> = MutableLiveData(true)

	/**
	 * The (unmodifiable) visibility of the progressbar.
	 */
	val progressBarVisible: LiveData<Boolean>
		get() = this._progressBarVisible

	/**
	 * The current (adjustable) text of the textview.
	 * This is private as we only want to adjust the text in the view model.
	 */
	private val _textviewText: MutableLiveData<String> = MutableLiveData()

	/**
	 * The (unmodifiable) text in the text data.
	 */
	val textviewText: LiveData<String>
		get() = this._textviewText

	/**
	 * Documentation
	 */
	private val _buttonVisible: MutableLiveData<Boolean> = MutableLiveData(false)

	/**
	 * Documentation
	 */
	val buttonVisible: LiveData<Boolean>
		get() = this._buttonVisible

	/**
	 * Documentation
	 */
	private val _buttonText: MutableLiveData<String> = MutableLiveData(this.getApplication<Application>().getString(R.string.retry))

	/**
	 * Documentation
	 */
	val buttonText: LiveData<String>
		get() = this._buttonText

	/**
	 * Documentation
	 */
	private val _buttonRunnable: MutableLiveData<View.OnClickListener> = MutableLiveData()

	/**
	 * Documentation
	 */
	val buttonRunnable: LiveData<View.OnClickListener>
		get() = this._buttonRunnable


	/**
	 * Update the progress bar to the current progress.
	 *
	 * @param progress The current progress out of SplashActivity.maxProgress.
	 */
	@AnyThread
	fun setProgressBar(progress: Double) {
		Log.v("setProgressBar", "Provided progress: $progress")

		// Convert the progress to be an int out of 100.
		var p: Int = (progress / LoadingActivity.MAX_PROGRESS * 100).toInt()

		// Validate that that the progress is between 0 and 100.
		p = if (p > 100) 100 else kotlin.math.max(p, 0)

		// Set the current progress to the int out of 100.
		this._currentProgress.postValue(p)
	}

	/**
	 * Documentation
	 */
	@AnyThread
	fun resetVisibilities() {
		this._progressBarVisible.postValue(true)
		this._buttonVisible.postValue(false)
	}

	/**
	 * Sets the message content to be displayed to the user on the splash screen.
	 *
	 * @param resID The string ID of the message. This can be retrieved by calling R.string.STRING_ID.
	 */
	@AnyThread
	fun setMessage(@StringRes resID: Int) {

		// Set the textview value to the provided string.
		// Because this is a live data object it will then call any observers.
		this._textviewText.postValue(this.getApplication<Application>().getString(resID))
	}

	/**
	 * Checks if the device has a current internet connection.
	 *
	 * @return Whether or not the device has an internet connection.
	 */
	@AnyThread
	fun hasInternet(): Boolean {

		Log.d("hasInternet", "Checking internet...")

		// Get the connectivity manager for the device.
		val connectivityManager: ConnectivityManager = this.getApplication<Application>()
				.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager

		// Check the current API version (as behavior changes in later APIs).
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

			// Newer API.
			// Get the network, and its capabilities for the app.
			val network: android.net.Network? = connectivityManager.activeNetwork
			val networkCapabilities: NetworkCapabilities =
					connectivityManager.getNetworkCapabilities(network) ?: return false

			// Return true if the app has access to any of the network capabilities:
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
			// Get the current network info available to the app.
			@Suppress("Deprecation")
			val networkInfo: android.net.NetworkInfo = connectivityManager.activeNetworkInfo ?: return false

			@Suppress("Deprecation")
			// Return if we are connected or not.
			return networkInfo.isConnected

		}
	}

	/**
	 * Changes the splash screen display when there is no internet.
	 * This method involves making the progress bar invisible,
	 * and setting the button to launch the wireless settings.
	 * It will also close the application when the button is clicked (as to force a restart of the app).
	 */
	@AnyThread
	fun noInternet(buttonRunnable: View.OnClickListener) {

		// Then, set the message of the text view to notify the user that there is no internet connection.
		this.setMessage(R.string.cannot_connect_internet)

		// Then setup the button to open the internet settings when clicked on, and make it visible.
		this._buttonText.postValue(this.getApplication<Application>().getString(R.string.open_network_settings))
		this._buttonRunnable.postValue(buttonRunnable)

		// Hide the progress bar.
		this._progressBarVisible.postValue(false)

		// Set the button to invisible.
		this._buttonVisible.postValue(true)
	}

	/**
	 * Shows the retry button by setting the view to visible, hiding the progress bar,
	 * and by setting the click action of the button to launch the onResume() method once again.
	 */
	@AnyThread
	fun showRetryButton(retryRunnable: View.OnClickListener) {

		// First hide the progress bar since it is no longer of use.
		this._progressBarVisible.postValue(false)

		// Then setup the button to relaunch the activity, and make it visible.
		this._buttonText.postValue(this.getApplication<Application>().getString(R.string.retry))
		this._buttonRunnable.postValue(retryRunnable)
		this._buttonVisible.postValue(true)

		// Comments
		this._progressBarVisible.postValue(false)
	}


	/**
	 * Runs the download runnable on a coroutine and updates the progress while doing so.
	 *
	 * @param loadProgress The load progress value that will be added once the downloadable has been parsed.
	 * @param downloadProgress The download progress value that will be added once the download has finished.
	 * @param progressSoFar The progress that has currently elapsed out of the MAX_PROGRESS
	 * @param runnable The download runnable to run.
	 */
	suspend fun downloadCoroutine(loadProgress: Double, downloadProgress: Double,
	                                      progressSoFar: Double, runnable: fnsb.macstransit.
			activities.loadingactivity.loadingscreenrunnables.DownloadRouteObjects<Unit>) = coroutineScope {

		// Create a variable to store the current state of our current downloads.
		// When the download is queued this value decreases.
		// When the download has completed this value increases.
		var downloadQueue = 0

		// Get the progress step.
		val step: Double = loadProgress / this@LoadingViewModel.routes.size

		// Get the current progress.
		val progress: Double = progressSoFar + downloadProgress

		// Iterate though all the indices of all the routes that can be tracked.
		var i = 0
		for ((_, route) in this@LoadingViewModel.routes) {

			// Decrease the download queue (as we are queueing a new downloadable).
			downloadQueue--

			// Run the download function of our DownloadRoute object, and pass any necessary parameters.
			this.launch(Dispatchers.IO, CoroutineStart.DEFAULT) {

				// Run the downloadable.
				runnable.download(route, downloadProgress, progressSoFar, i)

				// Update the current progress.
				this@LoadingViewModel.setProgressBar(progress + step + downloadQueue +
				                                     this@LoadingViewModel.routes.size)

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
	fun mapSharedStops() {

		// Let the user know that we are checking for shared bus stops at this point.
		this.setMessage(R.string.shared_bus_stop_check)

		// Set the current progress.
		val step = LoadingActivity.LOAD_SHARED_STOPS.toDouble() / this.routes.size
		var currentProgress = (LoadingActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS + LoadingActivity.
		PARSE_MASTER_SCHEDULE + LoadingActivity.DOWNLOAD_BUS_STOPS + LoadingActivity.LOAD_BUS_STOPS).toDouble()

		// Iterate though each route in all our trackable routes.
		for ((_, route) in this.routes) {

			// If there are no stops to iterate over in our route just continue with the next iteration.
			if (route.stops.isEmpty()) { continue }

			// Iterate through all the stops in our first comparison route.
			for ((name, stop) in route.stops) {

				// Make sure the stop is not already in the route's shared stops.
				// If the stop was found as a shared stop then skip this iteration of the loop by continuing.
				if (route.sharedStops[name] != null) { continue }

				// Get an array of shared routes.
				val sharedRoutes: Array<Route> = SharedStop.getSharedRoutes(route, stop, this.routes)

				// If the shared routes array has more than one entry, create a new shared stop object.
				if (sharedRoutes.size > 1) {
					val sharedStop = SharedStop(name, stop.location, sharedRoutes)

					// Iterate though all the routes in the shared route,
					// and add our newly created shared stop.
					sharedRoutes.forEach {
						Log.d("mapSharedStops", "Adding shared stop to route: ${this
							.routes[it.name]!!.name}")

						this.routes[it.name]!!.sharedStops[name] = sharedStop
					}
				}
			}

			// Update the progress.
			currentProgress += step
			this.setProgressBar(currentProgress)
		}

		Log.d("mapSharedStops", "Reached end of mapSharedStops")
	}
}