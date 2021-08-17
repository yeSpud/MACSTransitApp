package fnsb.macstransit.activities.splashactivity.splashscreenrunnables

import android.util.Log
import fnsb.macstransit.activities.MapsActivity
import fnsb.macstransit.activities.splashactivity.SplashActivity

/**
 * Created by Spud on 8/16/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
class DownloadBusRoutes(private val activity: SplashActivity) : SplashListener {

	override fun splashRunnableFinished() { // Comments
		this.activity.mapBusProgress++
		Log.v("downloadBusRoutes", "Map progress remaining: ${this.activity.mapBusProgress}")
		if (this.activity.mapBusProgress == 0) {
			this.activity.downloadBusStops()
		}

		// Update progress. FIXME There is an issue with this getting called one last time from MapBusStops!
		//this.activity.setProgressBar(progress + step + MapsActivity.allRoutes!!.size + this.activity.mapBusProgress)
	}

	companion object {

		/**
		 * Documentation
		 */
		private val step: Double = SplashActivity.LOAD_BUS_ROUTES.toDouble() / MapsActivity.allRoutes!!.size

		/**
		 * Documentation
		 */
		private const val progress: Double =
				(SplashActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS + SplashActivity.PARSE_MASTER_SCHEDULE + SplashActivity.DOWNLOAD_BUS_ROUTES).toDouble()

	}

}