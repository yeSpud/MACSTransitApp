package fnsb.macstransit.RouteMatch;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

import fnsb.macstransit.Activities.MapsActivity;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.1.
 * @since Beta 3.
 */
public class Bus extends MarkedObject {

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
	 * The bus's corresponding route.
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
	 *
	 * @param vehicleId The ID of the bus.
	 * @param route The route object the bus belongs to. This cannot be null.
	 * @param latitude
	 * @param longitude
	 */
	public Bus(String vehicleId, Route route, double latitude, double longitude) throws Route.RouteException {
		super(vehicleId);

		if (route == null) {
			throw new Route.RouteException("Route cannot be null!");
		}


		this.route = route;
		this.color = route.color;

		this.latitude = latitude;
		this.longitude = longitude;
	}

	/**
	 * TODO Documentation
	 * TODO Comments
	 *
	 * @param vehiclesJson
	 * @return
	 * @throws Route.RouteException
	 */
	@NotNull
	public static Bus[] getBuses(@NotNull JSONArray vehiclesJson) throws Route.RouteException { // TODO Unit test

		// Create an array to store all the buses that are in the json array.
		Bus[] buses = new Bus[vehiclesJson.length()];

		// Loop through the json array and get the json object corresponding to the bus.
		for (int i = 0; i < vehiclesJson.length(); i++) {

			// Try to get the json object corresponding to the bus. If unsuccessful,
			// continue on the loop without executing any of the lower checks.
			JSONObject busObject;
			try {
				busObject = vehiclesJson.getJSONObject(i);
			} catch (JSONException e) {
				Log.e("getBuses", "Could not get individual bus object from loop", e);
				continue;
			}

			Bus bus;
			try {
				bus = Bus.createNewBus(busObject);
			} catch (JSONException e) {
				Log.e("getBuses", "Could not create new bus object from json", e);
				continue;
			}

			// Add the bus to the buses array
			Log.d("getBuses",
					String.format("Adding bus %s belonging to the %s route to the bus array", bus.name,
							bus.route.routeName));
			buses[i] = bus;
		}

		// Return the bus array as an array of buses.
		return buses;
	}

	/**
	 * TODO Documentation
	 * @param busObject
	 * @throws JSONException
	 * @throws Route.RouteException
	 * @return
	 */
	@NotNull
	public static Bus createNewBus(@NotNull JSONObject busObject) throws JSONException, Route.RouteException { // TODO Unit test

		// Get the vehicle ID of the bus.
		String vehicleId = busObject.getString("vehicleId");

		// Ge the route name of the bus.
		// This will be used to determine the route object of the bus from our array of all routes.
		String routeName = busObject.getString("masterRouteId");

		// Since we have the route name we need to now find the actual route that belongs to it.
		// First make sure all the routes have been loaded before continuing.
		if (MapsActivity.allRoutes == null || MapsActivity.allRoutes.length == 0) {
			throw new Route.RouteException("There are no loaded routes!");
		}

		// Now iterate through all the routes.
		Route route = null;
		for (Route r : MapsActivity.allRoutes) {

			// If the route name matches that of our bus route, then that's our route object.
			if (r.routeName.equals(routeName)) {
				route = r;
				break;
			}
		}

		// Get the the latitude of the bus.
		double latitude = busObject.getDouble("latitude");

		// Get the longitude of the bus.
		double longitud = busObject.getDouble("longitude");

		// Try to get the heading of the bus. This value isn't necessary, but is nice to have.
		String heading = busObject.optString("headingName", "");

		// Try to get the current speed of the bus. This value isn't necessary, but is nice to have.
		int speed = busObject.optInt("speed", 0);

		// Create a new bus object using the determined information.
		Bus bus = new Bus(vehicleId, route, latitude, longitud);
		bus.heading = heading;
		bus.speed = speed;

		// Return our newly created bus.
		return bus;
	}

	/**
	 * Compares two bus arrays and removes any buses that were in the old bus array that are not in the new bus array.
	 *
	 * @param oldBuses The original buses.
	 * @param newBuses The new buses retrieved from the server.
	 */
	public static void removeOldBuses(@NotNull Bus[] oldBuses, Bus[] newBuses) {
		// Iterate through the oldBuses
		for (Bus oldBus : oldBuses) {

			// Check if the new buses match the old bus.
			// If it doesn't, then remove it from the map.
			boolean notFound = Bus.noBusMatch(oldBus, newBuses);

			if (notFound) {
				Log.d("removeOldBuses", String.format("Removing bus %s from map", oldBus.name));
				try {
					oldBus.marker.remove();
				} catch (NullPointerException e) {
					Log.w("removeOldBuses", "Marker already null!");
				}
				oldBus.marker = null;
			}

		}
	}

	/**
	 * Compares two bus arrays and updates the positions, heading,
	 * and speed of the buses with matching IDs.
	 *
	 * @param oldBuses The original buses.
	 * @param newBuses The new buses retrieved from the server.
	 * @return An array of buses which have been updated.
	 */
	@NotNull
	public static Bus[] updateCurrentBuses(Bus[] oldBuses, @NotNull Bus[] newBuses) {
		// Create an array list for storing all the matching buses
		ArrayList<Bus> buses = new ArrayList<>(0);

		// Iterate through the new buses
		for (Bus newBus : newBuses) {

			// Compare the new bus to the oldBuses.
			// If they match, then add it to the array list and update its position.
			for (Bus oldBus : oldBuses) {

				if (newBus.name.equals(oldBus.name)) {

					// Update the buses position, heading, and speed
					Marker marker = oldBus.marker;
					if (marker != null) {
						marker.setPosition(new LatLng(newBus.latitude, newBus.longitude));
						oldBus.marker = marker;
						oldBus.heading = newBus.heading;
						oldBus.speed = newBus.speed;
						buses.add(oldBus);
					} else {
						Log.w("updateCurrentBuses", String.format("Marker is null for updated bus %s", oldBus.name));
					}
				}
			}
		}

		// Turn the array list into an array, and return it.
		Bus[] returnBuses = new Bus[buses.size()];
		returnBuses = buses.toArray(returnBuses);
		return returnBuses;
	}

	/**
	 * Compares two bus arrays and determines which buses are new.
	 *
	 * @param oldBuses The original buses.
	 * @param newBuses The new buses retrieved from the server.
	 * @return An array of all the new buses.
	 */
	@NotNull
	public static Bus[] addNewBuses(Bus[] oldBuses, @NotNull Bus[] newBuses) {
		// Create an array list for storing all the new buses.
		ArrayList<Bus> buses = new ArrayList<>(0);

		// Iterate through the new buses
		for (Bus newBus : newBuses) {

			// Compare the new bus to the oldBuses.
			// If they don't match, then it has not been added to the map yet,
			// so add it to the array and map.
			boolean notFound = Bus.noBusMatch(newBus, oldBuses);

			if (notFound) {
				Log.d("addNewBuses", "Adding new bus to map: " + newBus.name);

				// Create the bus marker
				Marker busMarker = newBus.addMarker(MapsActivity.map, new LatLng(newBus.latitude, newBus.longitude),
						newBus.color, "Bus " + newBus.name);

				// Determine whether or not to show the bus marker.
				busMarker.setVisible(newBus.route.enabled);

				newBus.marker = busMarker;
				buses.add(newBus);
			}
		}

		// Turn the array list into an array, and return it.
		Bus[] returnBuses = new Bus[buses.size()];
		returnBuses = buses.toArray(returnBuses);
		return returnBuses;
	}

	/**
	 * TODO Documentation
	 *
	 * @param busObject
	 * @param busObjects
	 * @return
	 */
	public static boolean noBusMatch(Bus busObject, @NotNull Bus[] busObjects) {
		for (Bus bus : busObjects) {
			Log.d("noBusMatch", String.format("Comparing %s to %s", busObject.name, bus.name));

			if (busObject.name.equals(bus.name)) {
				Log.d("noBusMatch", "Vehicle IDs match");
				if (busObject.route.routeName.equals(bus.route.routeName)) {
					Log.d("noBusMatch", "Objects match!");
					return false;
				}
			}
		}

		Log.d("noBusMatch", "No objects match");
		return true;
	}
}