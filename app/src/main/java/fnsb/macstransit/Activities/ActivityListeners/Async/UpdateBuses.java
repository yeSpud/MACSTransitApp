package fnsb.macstransit.Activities.ActivityListeners.Async;


import com.google.android.gms.maps.GoogleMap;

import fnsb.macstransit.RouteMatch.Bus;
import fnsb.macstransit.RouteMatch.Route;

/**
 * Created by Spud on 2019-11-23 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Beta 8
 */
public class UpdateBuses extends android.os.AsyncTask<Route, Void, Route[]> {

	/**
	 * TODO Documentation
	 */
	private GoogleMap map;

	/**
	 * TODO Documentation
	 *
	 * @param map
	 */
	public UpdateBuses(GoogleMap map) {
		this.map = map;
	}

	/**
	 * TODO Documentation
	 *
	 * @param routes
	 * @return
	 */
	@Override
	protected Route[] doInBackground(Route... routes) {
		for (Route route : routes) {
			if (this.isCancelled()) {
				break;
			}
			try {
				Bus[] oldBuses = route.buses;

				Bus[] newBuses = Bus.getBuses(route);
				if (oldBuses.length != newBuses.length) {
					route.buses = newBuses;
				} else {
					for (int index = 0; index < oldBuses.length; index++) {
						Bus oldBus = oldBuses[index];
						for (Bus newBus : newBuses) {
							if (newBus.busID.equals(oldBus.busID)) {
								oldBus.color = newBus.color;
								oldBus.latitude = newBus.latitude;
								oldBus.longitude = newBus.longitude;
							}
						}
						oldBuses[index] = oldBus;
					}
				}
			} catch (org.json.JSONException e) {
				e.printStackTrace();
			}
		}
		return routes;
	}


	/**
	 * TODO Documentation
	 *
	 * @param result
	 */
	@Override
	protected void onPostExecute(Route[] result) {
		Bus.drawBuses(result, this.map);
	}
}
