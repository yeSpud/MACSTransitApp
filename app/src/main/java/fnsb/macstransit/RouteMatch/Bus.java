package fnsb.macstransit.RouteMatch;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONException;
import org.json.JSONObject;

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
	 * Fallback constant in the event that we need to return an empty bus array.
	 */
	public static final Bus[] EMPTY_BUSES = new Bus[0];

	/**
	 * Creates a new bus object. While the name, route,
	 * and location of the bus are required for initialization,
	 * the bus' speed and heading can be added after the fact.
	 *
	 * @param vehicleId The ID of the bus.
	 * @param route     The route object the bus belongs to. This cannot be null.
	 * @param latitude  The latitude of the bus.
	 * @param longitude The longitude of the bus.
	 */
	public Bus(String vehicleId, Route route, double latitude, double longitude) throws Route.RouteException {
		super(String.format("Bus %s", vehicleId));

		// Make sure the provided route is not null.
		if (route == null) {
			throw new Route.RouteException("Route cannot be null!");
		}

		// Since the route is not null, set the bus route and color.
		this.route = route;
		this.color = route.color;

		// Set the position of the bus.
		this.latitude = latitude;
		this.longitude = longitude;
	}

	/**
	 * Creates an array of bus objects using the information in the provided json array.
	 * The size of the return array is the size of the json array.
	 *
	 * @param vehiclesJson The json array containing the bus information.
	 * @return An array of buses created from the information in the json array.
	 * @throws Route.RouteException Thrown if there are no routes to track
	 *                              (either MapsActivity.allRoutes is null or is 0 in length).
	 */
	@NonNull
	public static Bus[] getBuses(org.json.JSONArray vehiclesJson) throws Route.RouteException {

		// Check if the json array is null. If it is return an empty bus array.
		if (vehiclesJson == null) {
			Log.w("getBuses", "Vehicles json is null!");
			return EMPTY_BUSES;
		}

		// Create an array to store all the buses that are in the json array.
		Bus[] buses = new Bus[vehiclesJson.length()];

		// Loop through the json array and get the json object corresponding to the bus.
		for (int i = 0; i < vehiclesJson.length(); i++) {

			// Try to get the json object corresponding to the bus. If unsuccessful then log it,
			// and continue the loop without executing any of the lower code.
			JSONObject busObject;
			try {
				busObject = vehiclesJson.getJSONObject(i);
			} catch (JSONException e) {
				Log.e("getBuses", "Could not get individual bus object from loop", e);
				continue;
			}

			// Try to create a new bus object using the content in the json object.
			Bus bus;
			try {
				bus = Bus.createNewBus(busObject);
			} catch (JSONException | NullPointerException e) {
				Log.e("getBuses", "Could not create new bus object from json", e);
				continue;
			}

			// Add the bus to the buses array.
			Log.d("getBuses",
					String.format("Adding bus %s belonging to the %s route to the bus array", bus.name,
							bus.route.routeName));
			buses[i] = bus;
		}

		// Return the bus array.
		return buses;
	}

	/**
	 * Creates a new bus object from the information in the provided json object.
	 *
	 * @param busObject The json object containing the information to create the bus. This cannot be null.
	 * @return The newly created bus object.
	 * @throws JSONException        Thrown if there was an issue parsing the json.
	 * @throws Route.RouteException Thrown if there are no routes to track
	 *                              (either MapsActivity.allRoutes is null or is 0 in length).
	 * @throws NullPointerException Thrown if the provided json is null.
	 */
	@org.jetbrains.annotations.Contract("null -> fail")
	@NonNull
	public static Bus createNewBus(JSONObject busObject) throws JSONException, Route.RouteException,
			NullPointerException {

		// Make sure the bus object is not null.
		if (busObject == null) {
			throw new NullPointerException("Bus json object cannot be null!");
		}

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
		double longitude = busObject.getDouble("longitude");

		// Try to get the heading of the bus. This value isn't necessary, but is nice to have.
		String heading = busObject.optString("headingName", "");

		// Try to get the current speed of the bus. This value isn't necessary, but is nice to have.
		int speed = busObject.optInt("speed", 0);

		// Create a new bus object using the determined information.
		Bus bus = new Bus(vehicleId, route, latitude, longitude);
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
	@UiThread
	public static void removeOldBuses(@NonNull Bus[] oldBuses, Bus[] newBuses) {

		// Iterate through the oldBuses
		for (Bus oldBus : oldBuses) {

			// Check if the new buses match the old bus.
			// If it doesn't, then remove it from the map.
			boolean notFound = Bus.isBusNotInArray(oldBus, newBuses);

			// If the bus was not found, remove the marker.
			if (notFound) {
				if (oldBus.marker != null) {
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
	}

	/**
	 * Compares two bus arrays and updates the positions, heading,
	 * and speed of the buses with matching IDs.
	 *
	 * @param oldBuses The original buses.
	 * @param newBuses The new buses retrieved from the server.
	 * @return An array of buses which have been updated.
	 */
	@NonNull
	@UiThread
	public static Bus[] updateCurrentBuses(Bus[] oldBuses, @NonNull Bus[] newBuses) {

		// Create an array with the maximum size of the size of our new buses.
		// We will resize the array later,
		// this is just to make sure we can fit at most all the new buses into the potential array.
		Bus[] potentialBuses = new Bus[newBuses.length];
		int busSize = 0;

		// Iterate through the new buses.
		for (Bus newBus : newBuses) {

			// Compare the new bus to the oldBuses.
			// If they match, then add it to the potential bus array and update its position.
			for (Bus oldBus : oldBuses) {
				if (newBus.name.equals(oldBus.name)) {

					// Update the buses position, heading, and speed.
					Marker marker = oldBus.marker;
					if (marker != null) {
						marker.setPosition(new LatLng(newBus.latitude, newBus.longitude));
						oldBus.marker = marker;
						oldBus.heading = newBus.heading;
						oldBus.speed = newBus.speed;

						// Add the bus to the potential bus array.
						potentialBuses[busSize] = oldBus;
						busSize++;
					} else {
						Log.w("updateCurrentBuses", String.format("Marker is null for updated bus %s", oldBus.name));
					}
				}
			}
		}

		// Down size the array to its actual size and return it.
		Bus[] buses = new Bus[busSize];
		System.arraycopy(potentialBuses, 0, buses, 0, busSize);
		return buses;
	}

	/**
	 * Compares two bus arrays and determines which buses are new.
	 *
	 * @param oldBuses The original buses.
	 * @param newBuses The new buses retrieved from the server.
	 * @return An array of all the new buses.
	 */
	@NonNull
	@UiThread
	public static Bus[] addNewBuses(Bus[] oldBuses, @NonNull Bus[] newBuses) {

		// Create an array with the maximum size of the size of our new buses.
		// We will resize the array later,
		// this is just to make sure we can fit at most all the new buses into the potential array.
		Bus[] potentialBuses = new Bus[newBuses.length];
		int busSize = 0;

		// Iterate through the new buses.
		for (Bus newBus : newBuses) {

			// Compare the new bus to the oldBuses.
			// If they don't match, then it has not been added to the map yet,
			// so add it to the array and map.
			boolean notFound = Bus.isBusNotInArray(newBus, oldBuses);

			if (notFound) {
				Log.d("addNewBuses", String.format("Adding new bus to map: %s", newBus.name));

				// Create the bus marker.
				Marker busMarker = newBus.addMarker(MapsActivity.map,
						new LatLng(newBus.latitude, newBus.longitude), newBus.color);

				// Determine whether or not to show the bus marker.
				busMarker.setVisible(newBus.route.enabled);

				// Set the bus marker.
				newBus.marker = busMarker;

				// Add the bus to the bus array.
				potentialBuses[busSize] = newBus;
				busSize++;
			}
		}

		// Down size the array to its actual size and return it.
		Bus[] buses = new Bus[busSize];
		System.arraycopy(potentialBuses, 0, buses, 0, busSize);
		return buses;
	}

	/**
	 * Searches a given bus array for a given bus, and returns if it was not found.
	 *
	 * @param bus   The bus to search for.
	 * @param buses The bus array to search for the given bus.
	 * @return Whether the bus was NOT found.
	 */
	public static boolean isBusNotInArray(Bus bus, @NonNull Bus[] buses) {

		// Iterate though each bus.
		for (Bus iteratorBus : buses) {

			// Check of the bus we are searching for matches our current bus.
			Log.v("noBusMatch", String.format("Comparing %s to %s", bus.name, iteratorBus.name));
			if (bus.name.equals(iteratorBus.name)) {
				Log.d("noBusMatch", "Vehicle IDs match");

				// Check if the routes for the bus also match. If they do, return false (found).
				if (bus.route.routeName.equals(iteratorBus.route.routeName)) {
					Log.d("noBusMatch", "Objects match!");
					return false;
				}
			}
		}

		// Since the bus was not found in our array, return true.
		Log.d("noBusMatch", "No objects match");
		return true;
	}
}