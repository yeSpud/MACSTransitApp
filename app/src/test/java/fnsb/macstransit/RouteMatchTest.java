package fnsb.macstransit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;

import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.RouteMatch;
import fnsb.macstransit.Settings.CurrentSettings;

import static org.junit.Assert.*;

/**
 * Created by Spud on 6/25/20 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0
 * @since Release 1.2.
 */
public class RouteMatchTest {

	@Test
	public void routeMatchTest() {

		try {
			new RouteMatch("https://fnsb.routematch.com/feed/");
			new RouteMatch("http://fnsb.routematch.com/feed/");
		} catch (MalformedURLException e) {
			fail();
		}

		assertThrows(MalformedURLException.class, () -> new RouteMatch("https://fnsb.routematch.com/feed"));
		assertThrows(MalformedURLException.class, () -> new RouteMatch("http://fnsb.routematch.com/feed"));
		assertThrows(MalformedURLException.class, () -> new RouteMatch("ssh://fnsb.routematch.com/feed/"));
		assertThrows(MalformedURLException.class, () -> new RouteMatch("ssh://fnsb.routematch.com/feed"));
	}

	@Test
	public void parseDataTest() {
		// Load some test data
		assertTrue(testFile.exists());
		assertTrue(testFile.canRead());
		String data = CurrentSettings.readFile(testFile);
		assertNotNull(data);
		JSONObject testData;
		try {
			testData = new JSONObject(data);
		} catch (JSONException e) {
			fail();
			return;
		}

		// Run tests
		JSONArray array = RouteMatch.parseData(testData);
		assertNotNull(array);
		assertNotEquals(0, array.length());
		assertEquals(8, array.length());

		// Load in empty test data
		assertTrue(testFile.exists());
		assertTrue(testFile.canRead());
		data = CurrentSettings.readFile(testFile);
		assertNotNull(data);
		try {
			testData = new JSONObject(data);
		} catch (JSONException e) {
			fail();
			return;
		}

		// Run even more tests
		array = RouteMatch.parseData(testData);
		assertNotNull(array);
		assertEquals(0, array.length());

		// Load in empty test data
		assertTrue(testFile.exists());
		assertTrue(testFile.canRead());
		data = CurrentSettings.readFile(testFile);
		assertNotNull(data);
		try {
			testData = new JSONObject(data);
		} catch (JSONException e) {
			fail();
			return;
		}

		// Run even MORE tests
		array = RouteMatch.parseData(testData);
		assertNotNull(array);
		assertEquals(3, array.length());


	}

}
