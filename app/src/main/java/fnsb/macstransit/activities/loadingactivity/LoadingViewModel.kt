package fnsb.macstransit.activities.loadingactivity

import android.app.Application
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fnsb.macstransit.R
import fnsb.macstransit.routematch.RouteMatch

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
	 * Update the progress bar to the current progress.
	 *
	 * @param progress The current progress out of SplashActivity.maxProgress.
	 */
	@MainThread
	fun setProgressBar(progress: Double) {
		Log.v("setProgressBar", "Provided progress: $progress")

		// Convert the progress to be an int out of 100.
		var p: Int = (progress / LoadingActivity.MAX_PROGRESS * 100).toInt()

		// Validate that that the progress is between 0 and 100.
		p = if (p > 100) 100 else kotlin.math.max(p, 0)

		// Set the current progress to the int out of 100.
		this._currentProgress.value = p
	}

	/**
	 * Shows the progress bar.
	 */
	@MainThread
	@Deprecated("Move to inline sets")
	fun showProgressBar() {
		this._progressBarVisible.value = true
	}

	/**
	 * Hides the progress bar.
	 */
	@MainThread
	@Deprecated("Move to inline sets")
	fun hideProgressBar() {
		this._progressBarVisible.value = false
	}

	/**
	 * Sets the message content to be displayed to the user on the splash screen.
	 *
	 * @param resID The string ID of the message. This can be retrieved by calling R.string.STRING_ID.
	 */
	@MainThread
	fun setMessage(@androidx.annotation.StringRes resID: Int) {

		// Set the textview value to the provided string.
		// Because this is a live data object it will then call any observers.
		this._textviewText.value = this.getApplication<Application>().getString(resID)
	}

	/**
	 * Checks if the device has a current internet connection.
	 *
	 * @return Whether or not the device has an internet connection.
	 */
	@androidx.annotation.AnyThread
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
	@UiThread
	fun noInternet(activity: LoadingActivity) {

		// Then, set the message of the text view to notify the user that there is no internet connection.
		this.setMessage(R.string.cannot_connect_internet)

		// Then setup the button to open the internet settings when clicked on, and make it visible.
		activity.binding.button.setText(R.string.open_network_settings)
		activity.binding.button.setOnClickListener {

			// Open the WiFi settings.
			activity.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))

			// Also, close this application when the button clicked.
			// (Like closing the door on its way out).
			activity.finish()
		}

		// Hide the progress bar.
		this._progressBarVisible.value = false

		// Set the button to invisible.
		this._buttonVisible.value = true

		// Since technically everything (which is nothing) has been loaded, set the variable as so
		activity.loaded = true
	}
}