package fnsb.macstransit.RouteMatch;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.4
 * @since Beta 3.
 */
public class Bus extends MarkedObject {

	/**
	 * The ID of the bus. While typically this is a number, on the rare occasion it can also be a name.
	 * As such, it should just be stored as a string.
	 * If this needs to be a number try parsing it from the string.
	 */
	public String busID;

	/**
	 * The latitude and longitude of the bus. Essentially making up its respective coordinates.
	 * This is stored as a double as latitude and longitude values are floating points numbers.
	 */
	public double latitude, longitude;

	/**
	 * The current bus's color.
	 * This is more representative of the parentRoute its on (ie what is its parentRoute color),
	 * and thus is optional.
	 * <p>
	 * This is an int instead of a Color object because for whatever reason android stores its colors as ints.
	 */
	public int color;

	/**
	 * The bus's corresponding parentRoute.
	 */
	public Route route;

	/**
	 * String used to store the buses current heading.
	 * This is optional, and may just be an empty string as a result.
	 */
	public String heading = "";

	/**
	 * Variables to store the current bus speed in mph.
	 */
	public int speed;

	/**
	 * Construction for the bus.
	 * Only the bus's ID and its corresponding parentRoute are required.
	 *
	 * @param busID The ID belonging to the bus.
	 * @param route The bus's parentRoute.
	 */
	public Bus(String busID, Route route) {
		this.busID = busID;
		this.route = route;
	}

	/**
	 * Gets the buses from the provided parentRoute.
	 *
	 * @param route The parentRoute to get the buses from.
	 * @return The array of buses that coorespond to the provided parentRoute.
	 * @throws Thrown if there is an exception when parsing the JSON corresponding to the bus.
	 */
	public static Bus[] getBuses(Route route) throws org.json.JSONException {
		// Get the bus array for the provided rotue.
		org.json.JSONArray busArray = RouteMatch.parseData(fnsb.macstransit.Activities.
				MapsActivity.routeMatch.getBuses(route));

		// Create an arraylist to store the buses.
		ArrayList<Bus> buses = new ArrayList<>();

		// Get the count of buses based off the length of the JSONArray.
		int count = busArray.length();

		// Iterate through the bus array and execute the following:
		for (int i = 0; i < count; i++) {
			Log.d("getBuses", String.format("Parsing bus %d/%d", i + 1, count));

			// Get the JSONObject corresonding to the bus.
			org.json.JSONObject object = busArray.getJSONObject(i);

			// Create the bus by getting the bus ID from the JSONObject,
			// as well as the provided parentRoute.
			Bus bus = new Bus(object.getString("vehicleId"), route);

			// Get the lattitude, longitude, heading, speed, and current capacity from the JSONObject.
			bus.latitude = object.getDouble("latitude");
			bus.longitude = object.getDouble("longitude");
			bus.heading = object.getString("headingName");
			bus.speed = object.getInt("speed");

			// Set the bus color to that of the parentRoute.
			bus.color = route.color;

			// Add the bus to the bus array.
			buses.add(bus);
			Log.d("getBuses", "Adding bus to array");
		}
		Log.d("getBuses", "Returning array of size " + buses.size());

		// Return the bus array as an array of buses.
		return buses.toArray(new Bus[0]);
	}

	/**
	 * TODO Documentaiton
	 *
	 * @param oldBuses
	 * @param newBuses
	 */
	public static void removeOldBuses(Bus[] oldBuses, Bus[] newBuses) {
		// TODO
	}

	/**
	 * TODO Documentaiton
	 *
	 * @param oldBuses
	 * @param newBuses
	 * @return
	 */
	public static Bus[] updateCurrentBuses(Bus[] oldBuses, Bus[] newBuses) {
		// TODO
		return new Bus[0];
	}

	/**
	 * TODO Documentation
	 *
	 * @param oldBuses
	 * @param newBuses
	 * @return
	 */
	public static Bus[] addNewBuses(Bus[] oldBuses, Bus[] newBuses) {
		// TODO
		return new Bus[0];
	}
}
