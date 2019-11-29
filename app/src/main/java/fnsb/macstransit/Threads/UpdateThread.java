package fnsb.macstransit.Threads;

import android.util.Log;

import fnsb.macstransit.Activities.MapsActivity;

/**
 * Created by Spud on 2019-10-13 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.3
 * @since Beta 3
 */
public class UpdateThread {

	/**
	 * Create a boolean that will be used to determine if the update thread should be running or not.
	 */
	public boolean run = false;

	/**
	 * How quickly the thread should loop after its completed.
	 * Keep in mind that the smaller this number is the quicker it loops,
	 * and thus the more frequently its pulls data from the routematch server,
	 * and thus the more data it will consume.
	 * <p>
	 * This number is stored as a long, as it is the time in <i>milliseconds</i>,
	 * with the default being 4000 (4 seconds).
	 */
	private long updateFrequency = 4000;

	/**
	 * The MapsActivity (Main activity).
	 */
	private MapsActivity activity;

	/**
	 * Constructor for the UpdateThread.
	 *
	 * @param activity The MapsActivity (this should be the main activity).
	 */
	public UpdateThread(MapsActivity activity) {
		this.activity = activity;
	}

	/**
	 * Constructor for the UpdateThread.
	 *
	 * @param activity        The MapsActivity (this should be the main activity).
	 * @param updateFrequency How frequency (in milliseconds) the thread should loop.
	 *                        If this is omitted, it will default to 4000 milliseconds (4 seconds).
	 */
	public UpdateThread(MapsActivity activity, long updateFrequency) {
		this(activity);
		this.updateFrequency = updateFrequency;
	}

	/**
	 * This is the thread that repeatedly queries the routematch server for data on the buses, routes, and stops.
	 * It loops with the frequency defined by the {@code updateFrequency} variable (default of 4000 milliseconds, or 4 seconds).
	 *
	 * @return The thread. Note that this dies not run the thread, that has to be called separately.
	 */
	public Thread thread() {
		return new Thread(() -> {
			// For debugging purposes, let the poor developer know when the thread has started.
			Log.w("Update thread", "Starting up...");

			// Loop continuously while the run variable is true, and the thread hasn't been interrupted.
			while (this.run && !Thread.interrupted()) {

				// Make a copy of the selected routes array to run iterations on (to avoid Concurrent Modification Exceptions).
				fnsb.macstransit.RouteMatch.Route[] routes = this.activity.selectedRoutes;

				/*
				 * If there are no selected routes,
				 * loop quickly (every quarter second) rather than the set frequency.
				 * If there are selected routes (route length will be greater than 0),
				 * update the bus positions on the map (and do so on the UI thread).
				 * Then, sleep for the given update frequency.
				 */
				if (routes.length > 0) {
					this.activity.runOnUiThread(() -> new fnsb.macstransit.Activities
							.ActivityListeners.Async.UpdateBuses(this.activity.map).execute(routes));

					// Sleep for the given update frequency
					try {
						Thread.sleep(this.updateFrequency);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Thread.yield();
				} else {
					// Quick sleep since there are no routes to track.
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Thread.yield();
				}

				// Notify the developer that the thread is now starting over.
				Log.d("Update thread", "Looping...");
			}

			// Notify the developer that the thread has exited the while loop and will now stop.
			Log.w("Update thread", "Shutting down...");
		});
	}
}
