package fnsb.macstransit.Threads;

import android.util.Log;

import com.android.volley.Request;
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
public class SplashActivityLock implements RequestQueue.RequestEventListener {

	/**
	 * TODO Documentation
	 */
	public static final Object LOCK = new Object();

	@Override
	public void onRequestEvent(Request<?> request, int event) {
		if (event == RequestQueue.RequestEvent.REQUEST_FINISHED) {
			synchronized (SplashActivityLock.LOCK) {
				Log.d("SplashActivityLock", "Notifying lock");
				SplashActivityLock.LOCK.notifyAll();
			}
		}
	}

	/**
	 * TODO Documentation
	 */
	public void waitForLock() {
		MapsActivity.routeMatch.networkQueue.addRequestEventListener(this);
		synchronized (SplashActivityLock.LOCK) {
			try {
				Log.d("SplashActivityLock", "Waiting for lock...");
				SplashActivityLock.LOCK.wait();
				MapsActivity.routeMatch.networkQueue.removeRequestEventListener(this);
			} catch (InterruptedException e) {
				Log.e("SplashActivityLock", "Interrupted!", e);
			}
		}
	}
}
