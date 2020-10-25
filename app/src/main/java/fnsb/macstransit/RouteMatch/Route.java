package fnsb.macstransit.RouteMatch;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import fnsb.macstransit.Activities.MapsActivity;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.5.
 * @since Beta 3.
 */
public class Route {

	/**
	 * The name of the route.
	 * Note: This cannot contain whitespace characters (ie spaces, tabs, or new lines),
	 * as its used in a url.
	 */
	public String routeName;

	/**
	 * The color of the route.
	 * This is optional, as there is a high chance that the parentRoute does not have one.
	 * <p>
	 * This is an int instead of a Color object as android stores its colors as an integer.
	 */
	public int color;

	/**
	 * The array of stops for this route.
	 * This may be empty / null if the route has not been initialized, and the stops haven't been loaded.
	 */
	public Stop[] stops;

	/**
	 * The array of LatLng coordinates that will be used to create the polyline (if enabled).
	 */
	public LatLng[] polyLineCoordinates;

	/**
	 * TODO Documentation
	 */
	public boolean enabled = false;

	/**
	 * The polyline that corresponds to this route.
	 */
	private Polyline polyline;

	/**
	 * Constructor for the route. The name of the route is the only thing that is required.
	 * Be sure that the provided route name does <b>NOT</b> contain any whitespace characters!
	 *
	 * @param routeName The name of the route. Be sure this does <b>NOT</b>
	 *                  contain any whitespace characters!
	 * @throws RouteException Thrown if the route name contains white space characters.
	 */
	public Route(@NotNull String routeName) throws RouteException {

		Pattern whitespace = Pattern.compile("\\s");

		if (whitespace.matcher(routeName).find()) {
			throw new RouteException("Route name cannot contain white space!");
		} else {
			this.routeName = routeName;
		}
	}

	/**
	 * Constructor for the route. The name of the route is the only thing that is required.
	 * Be sure that the provided route name does <b>NOT</b> contain any whitespace characters!
	 *
	 * @param routeName The name of the route. e sure this does <b>NOT</b>
	 *                  contain any whitespace characters!
	 * @param color     The route's color. This is optional,
	 *                  and of the color is non-existent simply use the
	 *                  {@code Route(String routeName)} constructor.
	 * @throws RouteException Thrown if the route name contains white space characters.
	 */
	public Route(String routeName, int color) throws RouteException {
		this(routeName);
		this.color = color;
	}

	/**
	 * TODO Documentation
	 * @param masterSchedule
	 * @return
	 */
	@NotNull
	public static Route[] generateRoutes(JSONArray masterSchedule) {
		// Create an array to store all the generated routes.
		Collection<Route> routes = new ArrayList<>(0);

		// Iterate through the data array to begin parsing the childRoutes
		int count = masterSchedule.length();
		for (int index = 0; index < count; index++) {
			// Get the current progress for parsing the routes
			Log.d("generateRoutes", String.format("Parsing route %d/%d", index + 1, count));

			// Try to get the route data from the array.
			// If there's an issue parsing the data, simply go to the next iteration of the loop (continue).
			JSONObject routeData;
			try {
				routeData = masterSchedule.getJSONObject(index);
			} catch (JSONException e) {
				Log.w("generateRoutes", "Issue retrieving the route data");
				continue;
			}

			// Try to create the route using the route data obtained above.
			// If there was a route exception thrown, simply go to the next iteration of the loop (continue).
			try {
				Route route = Route.generateRoute(routeData);
				routes.add(route);
			} catch (RouteException e) {
				Log.w("generateRoutes", "Issue creating route from route data");
				// Since this is the end of the loop, there isn't a need for a continue statement.
			}
		}

		// Return the parentRoute array list as a new array of childRoutes. Yes, they are different.
		Route[] returnRoutes = new Route[routes.size()];
		routes.toArray(returnRoutes);
		return returnRoutes;
	}

	/**
	 * TODO Documentation
	 *
	 * @param jsonObject
	 * @return
	 * @throws RouteException
	 */
	@NotNull
	private static Route generateRoute(@NotNull JSONObject jsonObject) throws RouteException {
		String name;

		try {
			// First, parse the name.
			name = jsonObject.getString("routeId");
		} catch (JSONException e) {
			throw new RouteException("Unable to get route name from JSON");
		}

		Route route;

		try {
			// Now try to parse the color.
			String colorName = jsonObject.getString("routeColor");
			int color = android.graphics.Color.parseColor(colorName);

			route = new Route(name, color);

		} catch (JSONException e) {
			// TODO
			route = new Route(name);
		}

		return route;
	}

	/**
	 * Enabled a route by route name,
	 * and returns a new array of enabled childRoutes including the previously enabled childRoutes.
	 *
	 * @param routeName The new route to enable (by route name).
	 * @param oldRoutes The array of old routes that were previously enabled.
	 *                  If there were no previously enabled routes then this must be an array of size 0.
	 * @return The array of routes that are now being tracked.
	 */
	@Deprecated
	public static Route[] enableRoutes(String routeName, Route[] oldRoutes) { // FIXME
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

				// TODO Show markers

				// If there are any preexisting buses in the route, show them.
				try {
					for (Bus bus : MapsActivity.buses) {
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
	@NotNull
	@Deprecated
	public static Route[] disableRoute(String routeName, Route[] oldRoutes) { // FIXME
		Log.d("disableRoute", "Disabling route: " + routeName);

		// Convert all the old childRoutes to an array list of childRoutes.
		ArrayList<Route> routes = new ArrayList<>(Arrays.asList(oldRoutes));

		// Iterate through the currently enabled childRoutes.
		for (Route route : routes) {

			// If the parentRoute name of the current parentRoute matches that of the parentRoute to be disabled,
			// execute the following:
			if (route.routeName.equals(routeName)) {
				// TODO Hide markers
				// Remove the bus icons
				try {
					for (Bus bus : MapsActivity.buses) {
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
				Polyline polyline = route.polyline;
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
	 * TODO Documentation
	 *
	 * @param allRoutes
	 * @param favoritedRoutes
	 */
	public static void enableFavoriteRoutes(Route[] allRoutes, Route[] favoritedRoutes) {

		// Make sure there are routes to iterate over.
		if (allRoutes == null) {
			return;
		}

		// Iterate through all the routes that will be used in the activity.
		for (Route allRoute : allRoutes) {

			// Iterate though the favorite routes
			for (Route favoritedRoute : favoritedRoutes) {

				// If the route name matches the favorited route name, enable it.
				if (allRoute.routeName.equals(favoritedRoute.routeName)) {
					allRoute.enabled = true;
					break;
				}
			}
		}

	}

	/**
	 * TODO Documentation
	 */
	public Stop[] loadStops() {

		// Get all the stops for the route from the RouteMatch object.
		JSONObject allStopsObject = MapsActivity.routeMatch.getAllStops(this);

		// Get the data from all the stops and store it in a JSONArray.
		JSONArray data = RouteMatch.parseData(allStopsObject);

		// Create a large array that will store all the potential stops.
		// This array will be trimmed down to fit only the valid stops when we are ready to return.
		int count = data.length(),
		validStops = 0;
		Stop[] potentialStops = new Stop[count];

		// Iterate through the data in the JSONArray.
		for (int index = 0; index < count; index++) {

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
			if (Stop.isDuplicate(stop, potentialStops)) {
				continue;
			}


			potentialStops[validStops] = stop;
			validStops++;
		}

		// Return the array list that contains all the stops as a new Stop array.
		Stop[] returnArray = new Stop[validStops];
		System.arraycopy(potentialStops, 0, returnArray, 0, validStops);
		return returnArray;
	}

	/**
	 * TODO Documentation
	 */
	public void loadPolyLineCoordinates() throws JSONException {
		// Make sure the RouteMatch object exists.
		if (MapsActivity.routeMatch == null) {
			return;
		}

		// Get the land route json object from the RouteMatch server.
		JSONObject landRouteObject = MapsActivity.routeMatch.getLandRoute(this);

		// Get the land route data array from the land route object.
		JSONArray landRouteData = RouteMatch.parseData(landRouteObject);

		// Get the land route points object from the land route data array.
		JSONObject landRoutePoints = landRouteData.getJSONObject(0);

		// Get the land route points array from the land route points object.
		JSONArray landRoutePointsArray = landRoutePoints.getJSONArray("points");

		// Get the number of points in the array.
		int count = landRoutePointsArray.length();

		// Create a new LatLng array to store all the coordinates.
		LatLng[] coordinates = new LatLng[count];

		// Initialize the array of coordinates by iterating through the land route points array.
		for (int i = 0; i < count; i++) {
			// Get the land route point object from the land route points array.
			JSONObject landRoutePoint = landRoutePointsArray.getJSONObject(i);

			// Get the latitude and longitude from the land route point.
			double latitude = landRoutePoint.getDouble("latitude"),
					longitude = landRoutePoint.getDouble("longitude");

			// Create a new LatLng object using the latitude and longitude.
			LatLng latLng = new LatLng(latitude, longitude);

			// Add the newly created LatLng object to the LatLng array.
			coordinates[i] = latLng;
		}

		// Set the polyline coordinates array to the finished LatLng array.
		this.polyLineCoordinates = coordinates;
	}

	/**
	 * Returns the polyline that corresponds to the route.
	 *
	 * @return The polyline that corresponds to the route.
	 */
	public Polyline getPolyline() {
		return this.polyline;
	}

	/**
	 * Creates the polyline that correspond to this route's path by utilizing the polyLineCoordinates.
	 *
	 * @param map The map to add the polyline to.
	 */
	public void createPolyline(@NotNull com.google.android.gms.maps.GoogleMap map) {
		Log.v("createPolyline", "Creating route polyline");
		// Add the polyline based off the polyline coordinates within the parentRoute.
		PolylineOptions options = new PolylineOptions().add(this.polyLineCoordinates);

		// Make sure its not clickable.
		options.clickable(false);

		// Set the color of the polylines based on the parentRoute color.
		options.color(this.color);

		// Make sure the polyline starts out invisible.
		options.visible(false);

		// Add the polyline to the map, and return it.
		this.polyline = map.addPolyline(options);
	}

	/**
	 * TODO Documentation
	 */
	public static class RouteException extends Exception {

		/**
		 * TODO Documentation
		 *
		 * @param message
		 */
		public RouteException(String message) {
			super(message);
		}

		/**
		 * TODO Documentation
		 *
		 * @param message
		 * @param cause
		 */
		public RouteException(String message, Throwable cause) {
			super(message, cause);
		}

		/**
		 * TODO Documentation
		 *
		 * @param cause
		 */
		public RouteException(Throwable cause) {
			super(cause);
		}

	}
}
