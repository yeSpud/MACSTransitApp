package fnsb.macstransit.activities.loadingactivity

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.annotation.AnyThread
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import fnsb.macstransit.R
import fnsb.macstransit.activities.loadingactivity.loadingscreenrunnables.DownloadBusStops
import fnsb.macstransit.activities.loadingactivity.loadingscreenrunnables.DownloadMasterSchedule
import fnsb.macstransit.activities.loadingactivity.loadingscreenrunnables.DownloadRouteObjects
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.routematch.RouteMatch
import fnsb.macstransit.routematch.SharedStop
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Created by Spud on 8/16/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.2.
 * @since Release 1.3.
 */
class LoadingViewModel(application: Application) : AndroidViewModel(application) {


	/**
	 * The RouteMatch object used to retrieve data from the RouteMatch servers.
	 */
	val routeMatch: RouteMatch = RouteMatch(getApplication<Application>().getString(R.string.routematch_url), getApplication())

	/**
	 * All of the routes that can be tracked by the app. This will be determined by the master schedule.
	 */
	val routes: HashMap<String, Route> = HashMap()

	/**
	 * The current (adjustable) progress.
	 * This is private as we only want to adjust it the values in the view model.
	 */
	private val _currentProgress: MutableLiveData<Int> = MutableLiveData(0)

	/**
	 * The (unmodifiable) progress of the progressbar.
	 */
	val currentProgress: LiveData<Int>
		get() = _currentProgress

	/**
	 * The current (adjustable) visibility of the progressbar.
	 * This is private as we only want to adjust the visibility in the view model.
	 */
	private val _progressBarVisible: MutableLiveData<Boolean> = MutableLiveData(true)

	/**
	 * The (unmodifiable) visibility of the progressbar.
	 */
	val progressBarVisible: LiveData<Boolean>
		get() = _progressBarVisible

	/**
	 * The current (adjustable) text of the textview.
	 * This is private as we only want to adjust the text in the view model.
	 */
	private val _textviewText: MutableLiveData<String> = MutableLiveData()

	/**
	 * The (unmodifiable) text in the text data.
	 */
	val textviewText: LiveData<String>
		get() = _textviewText

	/**
	 * The current (adjustable) visibility of the view's button.
	 * This is private as we only want to adjust the visibility in the view model.
	 */
	private val _buttonVisible: MutableLiveData<Boolean> = MutableLiveData(false)

	/**
	 * The (unmodifiable) view of the view's button.
	 */
	val buttonVisible: LiveData<Boolean>
		get() = _buttonVisible

	/**
	 * The current (adjustable) text of the view's button.
	 * This is private as we only want to set the text from the view model.
	 */
	private val _buttonText: MutableLiveData<String> = MutableLiveData(getApplication<Application>().getString(R.string.retry))

	/**
	 * The (unmodifiable) text of the view's button.
	 */
	val buttonText: LiveData<String>
		get() = _buttonText

	/**
	 * The current (adjustable) runnable for the view's button.
	 * This is private as we only want to set the runnable from the view model.
	 */
	private val _buttonRunnable: MutableLiveData<View.OnClickListener> = MutableLiveData()

	/**
	 * The (unmodifiable) runnable of the view's button.
	 */
	val buttonRunnable: LiveData<View.OnClickListener>
		get() = _buttonRunnable


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
		p = if (p > 100) 100 else max(p, 0)

		// Set the current progress to the int out of 100.
		_currentProgress.postValue(p)
	}

	/**
	 * Resets the visibility of the progress bar and the view's button.
	 */
	@AnyThread
	fun resetVisibilities() {
		_progressBarVisible.postValue(true)
		_buttonVisible.postValue(false)
	}

	/**
	 * Sets the message content to be displayed to the user on the splash screen.
	 *
	 * @param resID The string ID of the message. This can be retrieved by calling R.string.STRING_ID.
	 */
	@AnyThread
	fun setMessage(@androidx.annotation.StringRes resID: Int) {

		// Set the textview value to the provided string.
		// Because this is a live data object it will then call any observers.
		_textviewText.postValue(getApplication<Application>().getString(resID))
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
		val connectivityManager: ConnectivityManager = getApplication<Application>()
			.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

		// Check the current API version (as behavior changes in later APIs).
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			hasNetworkCapabilities(connectivityManager)
		} else {
			isConnected(connectivityManager)
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
		setMessage(R.string.cannot_connect_internet)

		// Then setup the button to open the internet settings when clicked on, and make it visible.
		_buttonText.postValue(getApplication<Application>().getString(R.string.open_network_settings))
		_buttonRunnable.postValue(buttonRunnable)

		// Hide the progress bar.
		_progressBarVisible.postValue(false)

		// Set the button to invisible.
		_buttonVisible.postValue(true)
	}

	/**
	 * Shows the retry button by setting the view to visible, hiding the progress bar,
	 * and by setting the click action of the button to launch the onResume() method once again.
	 */
	@AnyThread
	fun showRetryButton(retryRunnable: View.OnClickListener) {

		// First hide the progress bar since it is no longer of use.
		_progressBarVisible.postValue(false)

		// Then setup the button to relaunch the activity, and make it visible.
		_buttonText.postValue(getApplication<Application>().getString(R.string.retry))
		_buttonRunnable.postValue(retryRunnable)
		_buttonVisible.postValue(true)

		// Be sure to hide the progress bar.
		_progressBarVisible.postValue(false)
	}

	fun startupCoroutine(activity: LoadingActivity): kotlinx.coroutines.Job {

		// As there are a lot of operations to run to get the app started be sure to run all of them on a coroutine.
		return viewModelScope.launch(Dispatchers.IO, CoroutineStart.LAZY) {

			// Run various initial checks and download the master schedule.
			val passedInit: Boolean = masterScheduleCoroutine(activity)

			// If the initial checks fail then just exit early by returning.
			Log.d("StartupCoroutine", "Passed init: $passedInit")
			if (!passedInit) { return@launch }

			// Check if there are routes available for the day.
			if (routes.isEmpty()) {
				setMessage(R.string.its_sunday)
				activity.allowForRetry()
				return@launch
			}

			// Download and load the bus stops.
			withContext(coroutineContext) {
				downloadCoroutine(LoadingActivity.LOAD_BUS_STOPS.toDouble(), LoadingActivity.DOWNLOAD_BUS_STOPS.toDouble(),
				                  (LoadingActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS + LoadingActivity.PARSE_MASTER_SCHEDULE).toDouble(),
				                  DownloadBusStops(this@LoadingViewModel))
			}

			// Map the shared stops on a coroutine.
			mapSharedStops()

			// Validate the stops.
			// Let the user know that we are validating the stops (and shared stop) for each route.
			setMessage(R.string.stop_validation)

			// Update the progress bar.
			setProgressBar((LoadingActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS + LoadingActivity.PARSE_MASTER_SCHEDULE
			                + LoadingActivity.DOWNLOAD_BUS_STOPS + LoadingActivity.LOAD_BUS_STOPS +
			                LoadingActivity.LOAD_SHARED_STOPS).toDouble())

			// Iterate though all the routes and recreate the stops for each route.
			// Purge the stops that have shared stops.
			for (route in routes.values) { route.purgeStops() }


			// Update the progress bar to the maximum value since we've reached the end.
			setProgressBar(LoadingActivity.MAX_PROGRESS.toDouble())

			// Finally, launch the maps activity.
			Log.d("onResume", "End of lifecycle")
			activity.launchMapsActivity()
		}
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
	                                      progressSoFar: Double, runnable: DownloadRouteObjects<Unit>) = coroutineScope {

		// Create a variable to store the current state of our current downloads.
		// When the download is queued this value decreases.
		// When the download has completed this value increases.
		var downloadQueue = 0

		// Get the progress step.
		val step: Double = loadProgress / routes.size

		// Get the current progress.
		val progress: Double = progressSoFar + downloadProgress

		// Iterate though all the indices of all the routes that can be tracked.
		for ((i, route) in routes.values.withIndex()) {

			// Decrease the download queue (as we are queueing a new downloadable).
			downloadQueue--

			// Run the download function of our DownloadRoute object, and pass any necessary parameters.
			launch(Dispatchers.IO, CoroutineStart.DEFAULT) {

				// Run the downloadable.
				runnable.download(route, downloadProgress, progressSoFar, i)

				// Update the current progress.
				setProgressBar(progress + step + downloadQueue + routes.size)

				// Increase the downloaded queue as our downloadable has finished downloading.
				downloadQueue++

				// If the download queue has returned back to 0 log that downloading has been completed.
				if (downloadQueue == 0) { Log.d("downloadCoroutine", "Done mapping downloadable!") }
			}
		}
	}

	/**
	 * Runs various initial checks such as checking for internet and downloading (and parsing) the master schedule.
	 */
	private suspend fun masterScheduleCoroutine(activity: LoadingActivity): Boolean {
		Log.d("initialCoroutine", "Starting initialCoroutine")

		// Check if the user has internet before continuing.
		setMessage(R.string.internet_check)

		// Initialize the progress bar to 0.
		setProgressBar(0.0)

		// If there is no internet access then run the noInternet method and return false.
		if (!hasInternet()) {
			noInternet {

				// Open the WiFi settings.
				activity.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))

				// Also, close this application when the button clicked.
				// (Like closing the door on its way out).
				activity.finish()
			}

			// Since technically everything (which is nothing) has been loaded, set the variable as so.
			activity.loaded = true
			return false
		}

		// Get the master schedule from the RouteMatch server.
		setProgressBar(-1.0)
		setMessage(R.string.downloading_master_schedule)

		// Download and parse the master schedule. Use a filler route as the first parameter.
		val fillerRoute = Route("filler")
		DownloadMasterSchedule(activity).download(fillerRoute, LoadingActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS.toDouble(),
		                                          0.0, 0)

		// If we've made it to the end without interruption or error return true (success).
		Log.d("initialCoroutine", "Reached end of initialCoroutine")
		return true
	}

	/**
	 * Adds the shared stops to the map.
	 * This is done by iterating through all the stops in each route and checking for duplicates.
	 * If there are any found they will be added to all the routes the stop belongs to as a shared stop.
	 * At this point the original stop is still present in the route.
	 */
	private fun mapSharedStops() {

		// Let the user know that we are checking for shared bus stops at this point.
		setMessage(R.string.shared_bus_stop_check)

		// Set the current progress.
		val step = LoadingActivity.LOAD_SHARED_STOPS.toDouble() / routes.size
		var currentProgress = (LoadingActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS + LoadingActivity.
		PARSE_MASTER_SCHEDULE + LoadingActivity.DOWNLOAD_BUS_STOPS + LoadingActivity.LOAD_BUS_STOPS).toDouble()

		// Iterate though each route in all our trackable routes.
		for (route in routes.values) {

			// If there are no stops to iterate over in our route just continue with the next iteration.
			if (route.stops.isEmpty()) { continue }

			// Iterate through all the stops in our first comparison route.
			for ((name, stop) in route.stops) {

				// Make sure the stop is not already in the route's shared stops.
				// If the stop was found as a shared stop then skip this iteration of the loop by continuing.
				if (route.sharedStops[name] != null) { continue }

				// Get an array of shared routes.
				val sharedRoutes: Array<Route> = SharedStop.getSharedRoutes(route, stop, routes)

				// If the shared routes array has more than one entry, create a new shared stop object.
				if (sharedRoutes.size > 1) {
					val sharedStop = SharedStop(name, stop.location, sharedRoutes)

					// Iterate though all the routes in the shared route,
					// and add our newly created shared stop.
					for (sharedStopRoute: Route in sharedRoutes) {
						Log.d("mapSharedStops", "Adding shared stop to route: ${routes[sharedStopRoute.name]!!.name}")
						routes[sharedStopRoute.name]!!.sharedStops[name] = sharedStop
					}
				}
			}

			// Update the progress.
			currentProgress += step
			setProgressBar(currentProgress)
		}

		Log.d("mapSharedStops", "Reached end of mapSharedStops")
	}

	companion object {

		/**
		 * Newer API for getting internet connectivity. Requires Android M or newer
		 */
		@RequiresApi(Build.VERSION_CODES.M)
		internal fun hasNetworkCapabilities(connectivityManager: ConnectivityManager): Boolean {

			// Newer API.
			// Get the network, and its capabilities for the app.
			val network: Network? = connectivityManager.activeNetwork
			val networkCapabilities: NetworkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

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
		}

		/**
		 * Older API for getting internet connectivity. Used for versions older than Android M
		 */
		@Suppress("DEPRECATION") // Suppressed because the new way does not exist in this version of android
		internal fun isConnected(connectivityManager: ConnectivityManager): Boolean {

			// Older API.
			// Get the current network info available to the app.
			val networkInfo: NetworkInfo = connectivityManager.activeNetworkInfo ?: return false

			// Return if we are connected or not.
			return networkInfo.isConnected
		}
	}
}