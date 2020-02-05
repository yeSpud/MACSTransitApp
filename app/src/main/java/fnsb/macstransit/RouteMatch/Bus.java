package fnsb.macstransit.RouteMatch;

import android.util.Log;

import java.util.ArrayList;

import fnsb.macstransit.Activities.MapsActivity;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.0
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
	 * Compares two bus arrays and removes any buses that were in the old bus array that are not in the new bus array.
	 *
	 * @param oldBuses The origional buses.
	 * @param newBuses The new buses retrieved from the server.
	 */
	public static void removeOldBuses(Bus[] oldBuses, Bus[] newBuses) {
		// Iterate through the oldBuses
		for (Bus oldBus : oldBuses) {

			// Check if the new buses match the old bus.
			// If it doesnt, then remove it from the map.
			boolean found = false;
			for (Bus newBus : newBuses) {
				if (oldBus.busID.equals(newBus.busID)) {
					found = true;
					break;
				}
			}

			if (!found) {
				Log.d("removeOldBuses", String.format("Removing bus %s from map", oldBus.busID));
				try {
					oldBus.getMarker().remove();
				} catch (NullPointerException NPE) {
					Log.w("removeOldBuses", "Makrer already null!");
				}
				oldBus.setMarker(null);
			}

		}
	}

	/**
	 * Compares two bus arrays and updates the positions, heading,
	 * and speed of the buses whos IDs match.
	 *
	 * @param oldBuses The origional buses.
	 * @param newBuses The new buses retrieved from the server.
	 * @return An array of buses which have been updated.
	 */
	public static Bus[] updateCurrentBuses(Bus[] oldBuses, Bus[] newBuses) {

		// Create an arraylift for storing all the matching buses
		ArrayList<Bus> buses = new ArrayList<>();

		// Iterate through the new buses
		for (Bus newBus : newBuses) {

			// Compare the new bus to the oldBuses.
			// If they match, then add it to the arraylist and update its position.
			for (Bus oldBus : oldBuses) {
				Log.d("updateCurrentBuses",
						String.format("Comparing bus %s to bus %s", newBus.busID, oldBus.busID));

				if (newBus.busID.equals(oldBus.busID)) {
					Log.d("updateCurrentBuses", "Found matching bus " + newBus.busID);

					// Update the buses position, heading, and speed
					com.google.android.gms.maps.model.Marker marker = oldBus.getMarker();
					if (marker != null) {
						marker.setPosition(new com.google.android.gms.maps.model.LatLng(newBus.latitude,
								newBus.longitude));
						oldBus.setMarker(marker);
						oldBus.heading = newBus.heading;
						oldBus.speed = newBus.speed;
						buses.add(oldBus);
					} else {
						Log.w("updateCurrentBuses", "Marker is null for updated bus "
								+ oldBus.busID);
					}
				}
			}
		}


		return buses.toArray(new Bus[0]);
	}

	/**
	 * Compares two bus arrays and determines which bueses are new.
	 *
	 * @param oldBuses The origional buses.
	 * @param newBuses The new buses retrieved from the server.
	 * @return An array of all the new buses.
	 */
	public static Bus[] addNewBuses(Bus[] oldBuses, Bus[] newBuses) {

		// Create an arraylist for storing all the new buses.
		ArrayList<Bus> buses = new ArrayList<>();

		// Iterate through the new buses
		for (Bus newBus : newBuses) {

			// Compare the new bus to the oldBuses.
			// If they dont match, then it has not been added to the map yet,
			// so add it to the array and map.
			boolean found = false;
			for (Bus oldBus : oldBuses) {
				Log.d("addNewBuses",
						String.format("Comparing bus %s to bus %s", newBus.busID, oldBus.busID));
				if (newBus.busID.equals(oldBus.busID)) {
					Log.d("addNewBuses", "Found matching bus " + newBus.busID);
					found = true;
					break;
				}
			}

			if (!found) {
				Log.d("addNewBuses", "Adding new bus to map: " + newBus.busID);
				newBus.setMarker(newBus.addMarker(MapsActivity.map, newBus.latitude, newBus.longitude,
						newBus.color, "Bus " + newBus.busID));
				buses.add(newBus);
			}
		}
		return buses.toArray(new Bus[0]);
	}
}