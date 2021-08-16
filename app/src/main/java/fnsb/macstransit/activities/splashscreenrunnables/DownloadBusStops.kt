package fnsb.macstransit.activities.splashscreenrunnables

import fnsb.macstransit.activities.MapsActivity
import fnsb.macstransit.activities.SplashActivity

/**
 * Created by Spud on 8/16/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
class DownloadBusStops(private val activity: SplashActivity) : SplashListener {

	/**
	 * Documentation
	 */
	private val step: Double =
			SplashActivity.LOAD_BUS_STOPS.toDouble() / MapsActivity.allRoutes!!.size

	/**
	 * Documentation
	 */
	private val progress: Double =
			(SplashActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS + SplashActivity.PARSE_MASTER_SCHEDULE + SplashActivity.DOWNLOAD_BUS_ROUTES + SplashActivity.LOAD_BUS_ROUTES + SplashActivity.DOWNLOAD_BUS_STOPS).toDouble()

	override fun splashRunnableFinished() { // Comments

		this.activity.mapStopProgress++
		android.util.Log.v("splashRunnableFinished", "Stop progress remaining: ${this.activity.mapStopProgress}")
		if (this.activity.mapStopProgress == 0) {
			this.activity.cleanupThread().start()
		}

		// Update progress.
		this.activity.setProgressBar(
				progress + step + MapsActivity.allRoutes!!.size + this.activity.mapStopProgress)
	}

}