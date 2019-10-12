package spud;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {
		// Get data from the URL, it shoudl parse the JSON
		// https://fnsb.routematch.com/feed/departures/byStop/141%20-%20Aurora%20Dr%20%40%20Bridgewater%20Dr

		// First, try parsing it directly
		//JSONObject json = readJsonFromUrl("https://fnsb.routematch.com/feed/departures/byStop/141%20-%20Aurora%20Dr%20%40%20Bridgewater%20Dr");
		// System.out.println(json.toString());

		// Now try passing just a bus color
		//JSONObject blue = readJsonFromUrl("https://fnsb.routematch.com/feed/vehicle/byRoutes/Red");

		RouteMatch routeMatch = new RouteMatch("fnsb", "https://fnsb.routematch.com/feed/vehicle/byRoutes/", new String[]{"Blue"});

		JSONObject blue = routeMatch.getRoute("Blue");
		System.out.println(blue.toString());

		// Not try getting a specific field from that data
		JSONArray blueArray = blue.getJSONArray("data");
		System.out.println(blueArray.toString());

		for (int i = 0; i < blueArray.length(); i++) {
			JSONObject object = blueArray.getJSONObject(i);
			double[] latlong = getLatLong(object);
			System.out.println(String.format("%f, %f", latlong[0], latlong[1]));

		}
	}

	public static double[] getLatLong(JSONObject jsonObject) {
		System.out.println(jsonObject.toString());
		Object lat = jsonObject.get("latitude");
		System.out.println(lat.toString());
		Object longi = jsonObject.get("longitude");
		System.out.println(longi);

		return new double[]{
				Double.parseDouble(lat.toString()), Double.parseDouble(longi.toString())
		};
	}
}
