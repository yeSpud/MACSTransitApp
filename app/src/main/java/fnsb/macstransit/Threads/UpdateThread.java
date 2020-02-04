package fnsb.macstransit.Threads;

import android.util.Log;

import fnsb.macstransit.RouteMatch.Route;

/**
 * Created by Spud on 2019-10-13 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 3.0
 * @since Beta 3.
 */
public class UpdateThread {

	/**
	 * Create a boolean that will be used to determine if the update thread should be running or not.
	 */
	public boolean run = false;

	/**
	 * The route that this update thread belongs to.
	 */
	public Route route;

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
	 * Constructor for the UpdateThread.
	 *
	 * @param route The route that this update thread belongs to and will update after a given frequency.
	 */
	public UpdateThread(Route route) {
		this.route = route;
	}

	/**
	 * Constructor for the UpdateThread.
	 *
	 * @param route           The route that this update thread belongs to and will update after a given frequency.
	 * @param updateFrequency How frequency (in milliseconds) the thread should loop.
	 *                        If this is omitted, it will default to 4000 milliseconds (4 seconds).
	 */
	@SuppressWarnings("unused")
	public UpdateThread(Route route, long updateFrequency) {
		this(route);
		this.updateFrequency = updateFrequency;
	}

	/**
	 * This is the thread that repeatedly queries the routematch server for data on the buses,
	 * childRoutes, and stops.
	 * It loops with the frequency defined by the {@code updateFrequency} variable
	 * (default of 4000 milliseconds, or 4 seconds).
	 *
	 * @return The thread. Note that this dies not run the thread, that has to be called separately.
	 */
	public Thread thread() {
		return new Thread(() -> {
			// For debugging purposes, let the poor developer know when the thread has started.
			Log.w("Update thread", "Starting up...");

			// Loop continuously while the run variable is true, and the thread hasn't been interrupted.
			while (this.run && !Thread.interrupted()) {

				// Make sure the route isn't null before continuing
				if (this.route != null) {

					// Update the buses for the route.
					Log.d("Update thread", "Updating bus positions for route " + this.route.routeName);
					this.route.asyncBusUpdater = new fnsb.macstransit.Activities.ActivityListeners.Async.UpdateBuses(this.route);
					this.route.asyncBusUpdater.execute();
				} else {
					Log.w("Update thread", "Route is null!");
				}

				// Sleep for the given update frequency
				try {
					Thread.sleep(this.updateFrequency);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Thread.yield();

				// Notify the developer that the thread is now starting over.
				Log.d("Update thread", "Looping...");

			}

			// Notify the developer that the thread has exited the while loop and will now stop.
			Log.w("Update thread", "Shutting down...");

			this.route.asyncBusUpdater.cancel(true);
		});
	}
}