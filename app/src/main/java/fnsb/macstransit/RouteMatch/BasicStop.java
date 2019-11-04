package fnsb.macstransit.RouteMatch;

/**
 * Created by Spud on 2019-11-03 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 */
public class BasicStop {

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
	 * TODO Documentation
	 *
	 * @param stopID
	 * @param latitude
	 * @param longitude
	 * @param route
	 */
	public BasicStop(String stopID, double latitude, double longitude, Route route) {
		// Set the stop ID, coordinates, and the corresponding route.
		this.stopID = stopID;
		this.latitude = latitude;
		this.longitude = longitude;
		this.route = route;
	}

}
