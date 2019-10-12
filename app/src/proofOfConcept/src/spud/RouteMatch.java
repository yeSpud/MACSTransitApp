package spud;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;

public class RouteMatch {

	/**
	 * Name variable
	 */
	String name;

	/**
	 * URL location
	 *
	 * https://fnsb.routematch.com/feed/vehicle/byRoutes/
	 */
	String url;

	/**
	 * Bus line array
	 */
	String[] lines;

	public RouteMatch(String name, String url, String[] lines) {
		this.name = name;
		this.url = url;
		this.lines = lines;
	}

	public JSONObject getRoute(String route) {

		try {
			return this.readJsonFromUrl(this.url + route); //JSONObject blue = readJsonFromUrl("https://fnsb.routematch.com/feed/vehicle/byRoutes/Red");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public JSONObject[] getAllRoutes() {
		JSONObject[] jsonObjects = new JSONObject[this.lines.length]; //:)
		for (int index = 0; index < jsonObjects.length; index++) {
			jsonObjects[index] = this.getRoute(this.lines[index]);
		}

		return jsonObjects; // :)
	}

	private JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = this.readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	private String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}
}
