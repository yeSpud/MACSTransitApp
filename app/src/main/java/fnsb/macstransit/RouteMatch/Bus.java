package fnsb.macstransit.RouteMatch;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import fnsb.macstransit.Activities.ActivityListeners.Helpers;
import fnsb.macstransit.Activities.MapsActivity;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.2
 * @since Beta 3
 */
public class Bus extends MarkedObject {

	/**
	 * The ID of the bus. While typically this is a number, on the rare occasion it can also be a name.
	 * As such, it should just be stored as a string. If this needs to be a number try parsing it from the string.
	 */
	public String busID;

	/**
	 * The latitude and longitude of the bus. Essentially making up its respective coordinates.
	 * This is stored as a double as latitude and longitude values are floating points numbers.
	 */
	public double latitude, longitude;

	/**
	 * The current heading of the bus (Think cardinal directions).
	 */
	public Heading heading;

	/**
	 * The current bus's color. This is more representative of the route its on (ie what is its route color),
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
	 * Construction for the bus.
	 * Only the bus's ID and its corresponding route are required.
	 *
	 * @param busID The ID belonging to the bus.
	 * @param route The bus's route.
	 */
	public Bus(String busID, Route route) {
		this.busID = busID;
		this.route = route;
	}

	/**
	 * TODO Documentation
	 *
	 * @param route
	 * @return
	 */
	public static Bus[] getBuses(Route route) throws JSONException {

		JSONArray busArray = RouteMatch.parseData(MapsActivity.routeMatch.getBuses(route));

		ArrayList<Bus> buses = new ArrayList<>();
		int count = busArray.length();
		for (int i = 0; i < count; i++) {

			Log.d("getBuses", String.format("Parsing bus %d/%d", i + 1, count));
			JSONObject object = busArray.getJSONObject(i);
			Bus bus = new Bus(object.getString("vehicleId"), route);
			bus.latitude = object.getDouble("latitude");
			bus.longitude = object.getDouble("longitude");
			bus.color = route.color;
			try {
				// The heading is stored in the data as headingName
				bus.heading = Heading.valueOf(object.getString("headingName"));
			} catch (IllegalArgumentException e) {
				bus.heading = Heading.NORTH;
			}
			buses.add(bus);
			Log.d("getBuses", "Adding bus to array");
		}

		Log.d("getBuses", "Returning array of size " + buses.size());
		return buses.toArray(new Bus[0]);
	}

	/**
	 * TODO Documentation
	 *
	 * @param routes
	 * @param map
	 */
	public static void drawBuses(Route[] routes, GoogleMap map) {
		for (Route route : routes) {
			Bus[] buses = route.buses;
			if (buses != null) {
				for (Bus bus : buses) {
					com.google.android.gms.maps.model.Marker marker = bus.getMarker();
					if (marker != null) {
						// Just update the position
						marker.setPosition(new LatLng(bus.latitude, bus.longitude));

					} else {
						marker = fnsb.macstransit.Activities.ActivityListeners.Helpers.addMarker(map,
								bus.latitude, bus.longitude, bus.color, "Bus " + bus.busID, bus);
					}

					marker.setVisible(true);
					bus.setMarker(marker);
				}
			}
		}
	}
}
