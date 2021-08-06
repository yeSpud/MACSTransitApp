package fnsb.macstransit.Threads;

import android.util.Log;

import com.android.volley.RequestQueue;

import fnsb.macstransit.Activities.MapsActivity;

/**
 * Created by Spud on 5/18/21 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.2.6.
 */
@Deprecated
public class SplashActivityLock implements RequestQueue.RequestEventListener {

	/**
	 * Object used for locking.
	 */
	public static final Object LOCK = new Object();

	@Override
	public void onRequestEvent(com.android.volley.Request<?> request, int event) {

		// Check if the event was a finish event.
		if (event == RequestQueue.RequestEvent.REQUEST_FINISHED) {
			synchronized (SplashActivityLock.LOCK) {

				// Notify the lock object (freeing all processes waiting on the lock).
				Log.d("SplashActivityLock", "Notifying lock");
				SplashActivityLock.LOCK.notify();
			}
		}
	}

	/**
	 * Waits for the network event to be finished.
	 */
	public void waitForLock() {

		// Add this as the listener for network events.
		MapsActivity.routeMatch.networkQueue.addRequestEventListener(this);

		synchronized (SplashActivityLock.LOCK) {
			try {

				// Wait for the lock to be notified.
				Log.d("SplashActivityLock", "Waiting for lock...");
				SplashActivityLock.LOCK.wait();

				// Remove this from the network events.
				MapsActivity.routeMatch.networkQueue.removeRequestEventListener(this);
			} catch (InterruptedException e) {
				Log.e("SplashActivityLock", "Interrupted!", e);
			}
		}
	}
}
