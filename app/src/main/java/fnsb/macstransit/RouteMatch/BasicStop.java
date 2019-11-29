package fnsb.macstransit.RouteMatch;

/**
 * Created by Spud on 2019-11-03 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.1
 * @since Beta 7
 */
public class BasicStop extends MarkedObject {

	/**
	 * The latitude and longitude (coordinates) of the stop.
	 */
	public double latitude, longitude;

	/**
	 * The route that this stop corresponds to.
	 */
	public Route route;

	/**
	 * The ID of the stop. This is usually the stop name.
	 */
	public String stopID;

	/**
	 * Constructor for the BasicStop object. All that is required is the stopID, latitude, longitude,
	 * and route.
	 *
	 * @param stopID    The ID of the stop. This is typically the name of the Stop.
	 * @param latitude  The latitude of the stop.
	 * @param longitude The longitude of the stop.
	 * @param route     The route this stop corresponds to.
	 */
	public BasicStop(String stopID, double latitude, double longitude, Route route) {
		// Set the stop ID, coordinates, and the corresponding route.
		this.stopID = stopID;
		this.latitude = latitude;
		this.longitude = longitude;
		this.route = route;
	}

}
