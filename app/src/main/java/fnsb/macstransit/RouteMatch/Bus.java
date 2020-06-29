package fnsb.macstransit.RouteMatch;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

import fnsb.macstransit.Activities.MapsActivity;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.1
 * @since Beta 3.
 */
public class Bus extends MarkedObject {

	/**
	 * The ID of the bus. While typically this is a number, on the rare occasion it can also be a name.
	 * As such, it should just be stored as a string.
	 * If this needs to be a number try parsing it from the string.
	 */
	public String vehicleId;

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
	public String heading;

	/**
	 * Variables to store the current bus speed in mph.
	 */
	public int speed;

	/**
	 * TODO Documentation
	 * @param vehicleId
	 * @param route
	 * @param latitude
	 * @param longitude
	 */
	public Bus(String vehicleId, Route route, double latitude, double longitude) {
		this.vehicleId = vehicleId;
		this.route = route;
		this.color = route.color;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	/**
	 * TODO Documentation
	 * TODO Comments
	 * @param vehiclesJson
	 * @return
	 * @throws Route.RouteException
	 */
	public static Bus[] getBuses(JSONArray vehiclesJson) throws Route.RouteException {
		// Make sure all the routes have been loaded before continuing.
		if (MapsActivity.allRoutes.length == 0) {
			throw new Route.RouteException("There are no loaded routes!");
		}

		// Create an array to store all the buses that are in the json array.
		Bus[] buses = new Bus[vehiclesJson.length()];

		// Loop throgh the json array and get the json object corresponding to the bus.
		for (int i = 0; i < vehiclesJson.length(); i++) {

			// Try to get the json object corresonding to the bus. If unsuccessful,
			// continue on the loop without executing any of the lower checks.
			JSONObject busObject = null;
			try {
				busObject = vehiclesJson.getJSONObject(i);
			} catch (JSONException e) {
				Log.e("getBuses", "Could not get individual bus object from loop", e);
				continue;
			}

			// Try to get the necessary value (the vehicle id) for creating a new bus object.
			// If unsuccessful continue on the loop without executing any of the lower checks.
			String vehicleId = null;
			try {
				vehicleId = busObject.getString("vehicleId");
			} catch (JSONException e) {
				Log.e("getBuses", "Could not get bus id from bus json object", e);
				continue;
			}

			// Try to get the necessary value (the route name) for creating a new bus object.
			// If unsuccessful continue on the loop without executing any of the lower checks.
			String routeName = null;
			try {
				routeName = busObject.getString("masterRouteId");
			} catch (JSONException e) {
				Log.e("getBuses", "Could not get route name from bus json object", e);
				continue;
			}

			// Try to get the necessary value (the latitude) for creating a new bus object.
			// If unsuccessful continue on the loop without executing any of the lower checks.
			double latitude = 0.0d;
			try {
				latitude = busObject.getDouble("latitude");
			} catch (JSONException e) {
				Log.e("getBuses", "Could not get latitude from bus json object", e);
				continue;
			}

			// Try to get the necessary value (the longitude) for creating a new bus object.
			// If unsuccessful continue on the loop without executing any of the lower checks.
			double longitud = 0.0d;
			try {
				longitud = busObject.getDouble("longitude");
			} catch (JSONException e) {
				Log.e("getBuses", "Could not get longitude from bus json object", e);
				continue;
			}

			// Try to get any extra values (the heading) for creating a new bus object.
			// These valeus arent necessary, but are nice to have.
			String heading = "";
			try {
				heading = busObject.getString("headingName");
			} catch (JSONException e) {
				Log.w("getBuses", "Could not get bus heading from json object");
			}

			// Try to get any extra values (the speed) for creating a new bus object.
			// These valeus arent necessary, but are nice to have.
			int speed = 0;
			try {
				speed = busObject.getInt("speed");
			} catch (JSONException e) {
				Log.w("getBuses", "Could not get bus speed from json object");
			}

			// Iterate through all the routes.
			for (Route route : MapsActivity.allRoutes) {

				// If the route name maches the bus route name create a new bus belonging to that route,
				// and add it to the bus array.
				if (route.routeName.equals(routeName)) {
					Bus bus = new Bus(vehicleId, route, latitude, longitud);
					bus.heading = heading;
					bus.speed = speed;

					// Add the bus to the buses array
					Log.d("getBuses", String.format(Locale.US,
							"Adding bus %s belonging to the %s route to the bus array",
							bus.vehicleId, route.routeName));
					buses[i] = bus;
				}
			}
		}

		// Return the bus array as an array of buses.
		return buses;
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
				if (oldBus.vehicleId.equals(newBus.vehicleId)) {
					found = true;
					break;
				}
			}

			if (!found) {
				Log.d("removeOldBuses", String.format("Removing bus %s from map", oldBus.vehicleId));
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
						String.format("Comparing bus %s to bus %s", newBus.vehicleId, oldBus.vehicleId));

				if (newBus.vehicleId.equals(oldBus.vehicleId)) {
					Log.d("updateCurrentBuses", "Found matching bus " + newBus.vehicleId);

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
								+ oldBus.vehicleId);
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
						String.format("Comparing bus %s to bus %s", newBus.vehicleId, oldBus.vehicleId));
				if (newBus.vehicleId.equals(oldBus.vehicleId)) {
					Log.d("addNewBuses", "Found matching bus " + newBus.vehicleId);
					found = true;
					break;
				}
			}

			if (!found) {
				Log.d("addNewBuses", "Adding new bus to map: " + newBus.vehicleId);
				newBus.setMarker(newBus.addMarker(MapsActivity.map, newBus.latitude, newBus.longitude,
						newBus.color, "Bus " + newBus.vehicleId));
				buses.add(newBus);
			}
		}
		return buses.toArray(new Bus[0]);
	}
}