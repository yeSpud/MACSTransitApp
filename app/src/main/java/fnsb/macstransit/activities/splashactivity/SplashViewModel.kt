package fnsb.macstransit.activities.splashactivity

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.math.roundToInt

/**
 * Created by Spud on 8/16/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
class SplashViewModel(application: Application) : AndroidViewModel(application) {

	/**
	 * Documentation
	 */
	private val _currentProgress: MutableLiveData<Int> = MutableLiveData()

	/**
	 * TODO Documentation
	 */
	val currentProgress: LiveData<Int>
		get() = _currentProgress

	/**
	 * Documentation
	 */
	private val _progressBarVisible: MutableLiveData<Boolean> = MutableLiveData()

	/**
	 * Documentation
	 */
	private val _textviewText: MutableLiveData<String> = MutableLiveData()

	/**
	 * Documentation
	 */
	val textviewText: LiveData<String>
		get() = _textviewText

	/**
	 * Documentation
	 */
	val progressBarVisible: LiveData<Boolean>
		get() = _progressBarVisible

	/**
	 * Update the progress bar to the current progress.
	 *
	 * @param progress The current progress out of SplashActivity.maxProgress.
	 */
	@MainThread
	fun setProgressBar(progress: Double) {
		Log.v("setProgressBar", "Provided progress: $progress")

		// Convert the progress to be an int out of 100.
		var p: Int = (progress / SplashActivity.MAX_PROGRESS * 100).roundToInt()

		// Validate that that the progress is between 0 and 100.
		p = if (p > 100) 100 else kotlin.math.max(p, 0)

		this._currentProgress.value = p
	}

	/**
	 * Documentation
	 */
	@MainThread
	fun showProgressBar() {
		this._progressBarVisible.value = true
	}

	/**
	 * Documentation
	 */
	@MainThread
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

		// Comments
		this._textviewText.value = this.getApplication<Application>().getString(resID)
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
}