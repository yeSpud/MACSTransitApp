package fnsb.macstransit.RouteMatch;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import fnsb.macstransit.Activities.ActivityListeners.Async.UpdateBuses;
import fnsb.macstransit.Threads.UpdateThread;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.4
 * @since Beta 3.
 */
public class Route {

	/**
	 * The name of the parentRoute.
	 * Note: This cannot contain whitespace characters (ie spaces, tabs, or new lines),
	 * as its used in a url.
	 */
	public String routeName;

	/**
	 * The color of the parentRoute.
	 * This is optional, as there is a high chance that the parentRoute does not have one.
	 * <p>
	 * This is an int instead of a Color object because for whatever reason android stores its colors as ints.
	 */
	public int color;

	/**
	 * The array of stops for this parentRoute.
	 * This may be empty / null if the parentRoute has not been initialized,
	 * and the stops haven't been loaded.
	 */
	public Stop[] stops;

	/**
	 * The array of buses that belong to this route.
	 */
	public Bus[] buses = new Bus[0];

	/**
	 * The asynchronous method that updates the buses for this route.
	 */
	public UpdateBuses asyncBusUpdater = new UpdateBuses(this);

	/**
	 * The array of LatLng coordinates that will be used to create the polyline (if enabled).
	 * This should be initialized with an array of length 0.
	 */
	public LatLng[] polyLineCoordinates = new LatLng[0];

	/**
	 * The network thread that updates this given route.
	 */
	public UpdateThread updateThread;

	/**
	 * The polyline that corresponds to this parentRoute. This may be null if not enabled.
	 */
	private Polyline polyline;

	/**
	 * Constructor for the parentRoute. The name of the parentRoute is the only thing that is required.
	 * Be sure that the provided parentRoute name does <b>NOT</b> contain any whitespace characters!
	 *
	 * @param routeName The name of the parentRoute. Be sure this does <b>NOT</b>
	 *                  contain any whitespace characters!
	 * @throws Exception Thrown if the parentRoute name contains white space characters.
	 */
	public Route(String routeName) throws Exception {
		if (routeName.contains(" ") || routeName.contains("\n") || routeName.contains("\t")) {
			throw new Exception("Route name cannot contain white space!");
		} else {
			this.routeName = routeName;
			this.updateThread = new UpdateThread(this);
		}
	}

	/**
	 * Constructor for the parentRoute. The name of the parentRoute is the only thing that is required.
	 * Be sure that the provided parentRoute name does <b>NOT</b> contain any whitespace characters!
	 *
	 * @param routeName The name of the parentRoute. e sure this does <b>NOT</b>
	 *                  contain any whitespace characters!
	 * @param color     The parentRoute's color. This is optional,
	 *                  and of the color is non-existent simply use the
	 *                  {@code Route(String routeName)} constructor.
	 * @throws Exception Thrown if the parentRoute name contains white space characters.
	 */
	public Route(String routeName, int color) throws Exception {
		this(routeName);
		this.color = color;
	}

	/**
	 * Dynamically generates the childRoutes that are used by parsing the master schedule.
	 * This may return an empty parentRoute array if there was an issue parsing the data,
	 * or if there were no childRoutes to parse based off the master schedule.
	 *
	 * @param masterSchedule The master schedule JSONObject from the RouteMatch server.
	 * @return An array of childRoutes that <b><i>can be</i></b> tracked.
	 */
	public static Route[] generateRoutes(JSONObject masterSchedule) {

		// Create an array to store all the generated childRoutes. This will be returned in the end.
		ArrayList<Route> routes = new ArrayList<>();

		// Get the data from the master schedule, and store it in a JSONArray.
		JSONArray data = RouteMatch.parseData(masterSchedule);

		// Iterate through the data array to begin parsing the childRoutes
		int count = data.length();
		for (int index = 0; index < count; index++) {
			try {

				// Get the current progress for parsing the childRoutes
				Log.d("generateRoutes", String.format("Parsing parentRoute %d/%d", index + 1,
						count));

				// Get the routeData that we are currently parsing as its own JSONObject variable.
				org.json.JSONObject routeData = data.getJSONObject(index);

				// First, parse the name.
				String name = routeData.getString("shortName");

				// Now try to parse the color.
				try {
					int color = android.graphics.Color.parseColor(routeData.getString("routeColor"));
					routes.add(new Route(name, color));
				} catch (IllegalArgumentException | JSONException colorError) {
					Log.w("generateRoutes", "Unable to determine parentRoute color");
					// Just return the parentRoute with the name
					routes.add(new Route(name));
				}

			} catch (Exception e) {
				// If there was an exception, simply print the stacktrace, and break from the for loop.
				e.printStackTrace();
				break;
			}
		}

		// Return the parentRoute array list as a new array of childRoutes. Yes, they are different.
		return routes.toArray(new Route[0]);
	}

	/**
	 * Enabled a parentRoute by parentRoute name,
	 * and returns a new array of enabled childRoutes including the previously enabled childRoutes.
	 *
	 * @param routeName The new parentRoute to enable (by parentRoute name).
	 * @param oldRoutes The array of old childRoutes that were previously enabled.
	 *                  If there were no previously enabled childRoutes then this must be an array of size 0.
	 * @return The array of childRoutes that are now being tracked.
	 */
	public static Route[] enableRoutes(String routeName, Route[] oldRoutes) {
		Log.d("enableRoutes", "Enabling route: " + routeName);

		// Make a copy of the oldRoutes array, but have it be one sizer bigger.
		Route[] routes = Arrays.copyOf(oldRoutes, oldRoutes.length + 1);

		// If the parentRoute is to be enabled, iterate through all the allRoutes that are able to be tracked.
		for (Route route : fnsb.macstransit.Activities.MapsActivity.allRoutes) {

			// If the parentRoute that is able to be tracked is equal to that of the parentRoute
			// entered as an argument, add that parentRoute to the selected allRoutes array.
			if (route.routeName.equals(routeName)) {
				Log.d("enableRoutes", "Found matching route!");

				routes[oldRoutes.length] = route;

				// Enable the routes update thread.
				route.updateThread.run = true;
				route.updateThread.thread().start();

				// If there are any preexisting buses in the route, show them.
				try {
					for (Bus bus : route.buses) {
						Marker marker = bus.getMarker();
						if (marker != null) {
							marker.setVisible(true);
							bus.setMarker(marker);
						}
					}
				} catch (NullPointerException warn) {
					Log.w("enableRoutes", "There were no preexisting buses!");
				}

				// Since we only add one parentRoute at a time (as there is only one routeName argument),
				// break as soon as its added.
				break;
			}
		}

		// Return the newly enabled childRoutes as an array.
		return routes;
	}

	/**
	 * Disables a parentRoute by name, and removes all elements of that parentRoute from the map
	 * (except stops).
	 *
	 * @param routeName The parentRoute the be disabled by name.
	 * @param oldRoutes The array of old, though currently enabled childRoutes.
	 * @return The array of childRoutes still to be tracked,
	 * with the omission of the parentRoute that was to be removed.
	 * If there are no more childRoutes that are to be enabled, then an array of size 0 will be returned.
	 */
	public static Route[] disableRoute(String routeName, Route[] oldRoutes) {
		Log.d("disableRoute", "Disabling route: " + routeName);

		// Convert all the old childRoutes to an array list of childRoutes.
		ArrayList<Route> routes = new ArrayList<>(Arrays.asList(oldRoutes));

		// Iterate through the currently enabled childRoutes.
		for (Route route : routes) {

			// If the parentRoute name of the current parentRoute matches that of the parentRoute to be disabled,
			// execute the following:
			if (route.routeName.equals(routeName)) {

				// Disable the update thread for the route.
				route.updateThread.run = false;
				route.asyncBusUpdater.cancel(true);

				// Remove the bus icons
				try {
					for (Bus bus : route.buses) {
						Marker marker = bus.getMarker();
						if (marker != null) {
							marker.setVisible(false);
							bus.setMarker(marker);
						}
					}
				} catch (NullPointerException warn) {
					Log.w("disableRoute", "There weren't any buses to disable");
				}

				// Remove the polyline from the map as well as the parentRoute.
				Polyline polyline = route.getPolyline();
				if (polyline != null) {
					polyline.remove();
					route.polyline = null;
				}

				// Finally remove the parentRoute from the array of enabled childRoutes,
				// and break since we are only removing one parentRoute per call.
				routes.remove(route);
				break;
			}
		}

		return routes.toArray(new Route[0]);
	}

	/**
	 * Loads the stops from the provided url.
	 * If there was an error parsing the JSON data for the stop object,
	 * then this will return what stops it had parsed successfully up until that point.
	 * As a result, the Stop array returned may be smaller than expected, or have a length of 0.
	 *
	 * @param routeMatch The parentRoute match instance (for pulling from the RouteMatch server).
	 * @return The array of stops that were loaded.
	 */
	public Stop[] loadStops(RouteMatch routeMatch) {

		// Create an array that will store the parsed stop objects
		ArrayList<Stop> returnArray = new ArrayList<>();

		// Get the data from all the stops and store it in a JSONArray.
		JSONArray data = RouteMatch.parseData(routeMatch.getAllStops(this));

		// Iterate through the data in the JSONArray
		int count = data.length();
		for (int index = 0; index < count; index++) {

			// Output the current index for debugging reasons
			Log.d("loadStops", String.format("Parsing stop %d/%d", index + 1, count));

			Stop stop;
			try {
				// Using the data from the stop array, try to create a new stop object.
				stop = new Stop(data.getJSONObject(index), this);
			} catch (JSONException e) {
				// If a JSONException was thrown, that means that there was an issue parsing the stop data.
				// Just print a warning message, and then break from the for loop.
				// This will cause the function to return whatever was in the return array.
				Log.w("loadStops", "An issue occurred while attempting to parse the JSON data");
				break;
			}

			// Iterate through the return array and check if the created stop is a duplicate.
			boolean found = false;
			for (Stop s : returnArray) {
				// If the stop is a duplicate (matches latitude, longitude, and parentRoute),
				// set the found boolean to true, abd break form the for loop
				if (stop.latitude == s.latitude && s.longitude == s.longitude && stop.parentRoute.equals(s.parentRoute)) {
					found = true;
					break;
				}
			}

			// If the stop was never found, add it to the return array.
			if (!found) {
				Log.d("loadStops", "Adding stop: " + stop.stopID + " to the array");
				returnArray.add(stop);
			}
		}

		// Return the array list that contains all the stops as a new Stop array.
		return returnArray.toArray(new Stop[0]);
	}

	/**
	 * Declares and initializes an array of LatLng coordinates to be used by the childRoutes polyline.
	 *
	 * @param routeMatch The RouteMatch object.
	 * @return The initialized array of LatLng coordinates.
	 */
	public LatLng[] loadPolyLineCoordinates(RouteMatch routeMatch) {

		// Get the JSONArray of points for this parentRoute from the RouteMatch server.
		JSONArray points = null;
		try {
			points = RouteMatch.parseData(routeMatch.getLandRoute(this)).getJSONObject(0)
					.getJSONArray("points");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		// If the points are not null execute the following:
		if (points != null) {
			// Get the number of points in the array.
			int count = points.length();

			// Create a new array of LatLng coordinates with the size being that of the number
			// of points in the array.
			LatLng[] coordinates = new LatLng[count];

			// Initialize the array fo coordinates by iterating through the JSONArray.
			for (int index = 0; index < count; index++) {
				Log.d("loadPolyLineCoordinates", String.format("Parsing coordinate %d/%d",
						index + 1, count));
				try {
					// Get the JSONObject at the current index of the array.
					JSONObject object = points.getJSONObject(index);

					// Set the LatLng object to the latitude and longitude data from the JSONObject.
					LatLng latLng = new LatLng(object.getDouble("latitude"),
							object.getDouble("longitude"));
					coordinates[index] = latLng;
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			return coordinates;
		} else {
			// Since the points were null, just return a LatLng array of size 0.
			return new LatLng[0];
		}
	}

	/**
	 * Returns the polyline that corresponds to the parentRoute.
	 *
	 * @return The polyline that corresponds to the parentRoute.
	 */
	public Polyline getPolyline() {
		return this.polyline;
	}

	/**
	 * Creates the polyline that correspond to this route's path by utilizing the polyLineCoordinates.
	 *
	 * @param map The map to add the polyline to.
	 */
	public void createPolyline(com.google.android.gms.maps.GoogleMap map) {
		// Add the polyline based off the polyline coordinates within the parentRoute.
		PolylineOptions options = new PolylineOptions().add(this.polyLineCoordinates);

		// Make sure its not clickable.
		options.clickable(false);

		// Set the color of the polylines based on the parentRoute color.
		options.color(this.color);

		// Make sure the polyline is visible.
		options.visible(true);

		// Add the polyline to the map, and return it.
		this.polyline = map.addPolyline(options);
	}
}
