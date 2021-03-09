package fnsb.macstransit.RouteMatch;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import org.jetbrains.annotations.NotNull;

import fnsb.macstransit.Activities.MapsActivity;

/**
 * Created by Spud on 2019-11-01 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.0
 * @since Beta 7.
 */
public class SharedStop extends MarkedObject {

	/**
	 * TODO Documentation
	 */
	public Route[] routes;

	/**
	 * TODO Documentation
	 */
	public CircleOptions[] circleOptions;

	/**
	 * TODO Documentation
	 */
	public Circle[] circles;

	/**
	 * TODO Documentation
	 */
	public LatLng location;

	public SharedStop(LatLng latLng, String stopName, Route @NotNull [] routes) {
		super(stopName);

		this.location = latLng;
		this.routes = routes;

		this.circleOptions = new CircleOptions[routes.length];
		this.circles = new Circle[routes.length];

		for (int i = 0; i < routes.length; i++) {
			this.circleOptions[i] = new CircleOptions().center(this.location);

			Route route = routes[i];
			if (route.color != 0) {
				this.circleOptions[i].fillColor(route.color);
				this.circleOptions[i].strokeColor(route.color);
			}
		}

		this.setCircleSizes(12.0d);
	}

	/**
	 * TODO Documentation
	 * @param route
	 * @param routeIndex
	 * @param stop
	 * @return
	 */
	public static @NotNull Route[] getSharedRoutes(Route route, int routeIndex, Stop stop) {

		if (MapsActivity.allRoutes == null) {
			return new Route[]{route};
		}

		// Create an array of potential routes that could share a same stop
		// (the stop that we are iterating over).
		// Set the array size to that of all the routes minus the current index as to make
		// it decrease every iteration.
		Route[] potentialRoutes = new Route[MapsActivity.allRoutes.length - routeIndex];

		// Add the current route to the potential routes, and update the potential route index.
		potentialRoutes[0] = route;
		int potentialRouteIndex = 1;

		for (int route2Index = routeIndex + 1; route2Index < MapsActivity.allRoutes.length; route2Index++) {
			Route route2 = MapsActivity.allRoutes[route2Index];
			if (route == route2) {
				continue;
			}
			for (Stop stop2 : route2.stops) {
				try {
					if (Stop.stopMatches(stop, stop2)) {
						potentialRoutes[potentialRouteIndex] = route2;
						potentialRouteIndex++;
					}
				} catch (NullPointerException e) {
					Log.e("mapSharedStops", "Stop doesn't have a circle!", e);
				}
			}
		}

		Route[] actualRoutes = new Route[potentialRouteIndex];
		System.arraycopy(potentialRoutes, 0, actualRoutes, 0, potentialRouteIndex);
		return actualRoutes;
	}

	/**
	 * TODO Documentation
	 * @param stops
	 * @param sharedStops
	 * @return
	 */
	public static @NotNull Stop[] recreateStops(Stop[] stops, SharedStop[] sharedStops) { // TODO Check me

		if (stops == null || sharedStops == null) {
			Log.w("recreateStops", "Arguments are null!");
			return stops;
		}

		Stop[] potentialStops = new Stop[stops.length];
		int finalIndex = 0;
		for (Stop stop : stops) {

			boolean noMatch = true;
			for (SharedStop sharedStop : sharedStops) {
				if (SharedStop.areEqual(sharedStop, stop)) {
					noMatch = false;
					break;
				}
			}

			if (noMatch) {
				try {
					potentialStops[finalIndex] = stop;
					finalIndex++;
				} catch (ArrayIndexOutOfBoundsException e) {
					Log.w("recreateStops", String.format("Failed to add stop: %s for route %s",
							stop.name, stop.route.routeName));
					Log.e("recreateStops", "Final stops array is too small!", e);
				}
			}
		}

		if (finalIndex != (stops.length - sharedStops.length)) {
			Log.w("recreateStops", String.format("Final index differs from standard number! (%d vs %d)",
					stops.length - sharedStops.length, finalIndex));
		}

		final Stop[] finalStops = new Stop[finalIndex];
		System.arraycopy(potentialStops, 0, finalStops, 0, finalIndex);
		return finalStops;
	}

	/**
	 * TODO Documentation
	 * @param sharedStop
	 * @param stop
	 * @return
	 */
	public static boolean areEqual(@NotNull SharedStop sharedStop, @NotNull Stop stop) {
		boolean nameMatch = sharedStop.name.equals(stop.name),
		latitudeMatch = sharedStop.circleOptions[0].getCenter().latitude == stop.circleOptions.getCenter().latitude,
		longitudeMatch = sharedStop.circleOptions[0].getCenter().longitude == stop.circleOptions.getCenter().longitude;

		return nameMatch && latitudeMatch && longitudeMatch;
	}

	/**
	 * TODO Documentation
	 * @param map
	 */
	public void showSharedStop(GoogleMap map) {
		// TODO
		for (int i = 0; i < circles.length; i++) {
			Circle circle = this.circles[i];
			if (circle == null) {
				// TODO
				this.circles[i] = SharedStop.createSharedStopCircle(map, this.circleOptions[i], this, i == 0);
			} else {
				circle.setClickable(i == 0);
				circle.setVisible(true);
			}
		}
	}

	/**
	 * TODO Documentation
	 */
	public void hideStop() {
		for (Route route : this.routes) {
			if (route.enabled) {
				return;
			}
		}

		for (Circle circle : this.circles) {
			if (circle != null) {
				circle.setClickable(false);
				circle.setVisible(false);
			}
		}
	}

	/**
	 * TODO Documentation
	 * @param size
	 */
	public void setCircleSizes(double size) {

		Log.d("setCircleSizes",  "Setting initial size to: " + (size * (1.0d/(1))));
		for (int i = 0; i < routes.length; i++) {
			this.circleOptions[i].radius(size * (1.0d/(i+1)));
			if (this.circles[i] != null) {
				this.circles[i].setRadius(size * (1.0d / (i + 1)));
			}
		}
	}

	/**
	 * TODO Documentation
	 * @param map
	 * @param options
	 * @param sharedStop
	 * @param clickable
	 * @return
	 */
	private static @NotNull Circle createSharedStopCircle(@NotNull GoogleMap map, CircleOptions options, SharedStop sharedStop, boolean clickable) {
		Circle circle = map.addCircle(options);
		circle.setTag(sharedStop);
		circle.setClickable(clickable);
		circle.setVisible(true);
		return circle;
	}
}
