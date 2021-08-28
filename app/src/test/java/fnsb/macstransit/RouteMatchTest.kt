package fnsb.macstransit

import fnsb.macstransit.Helper.getText
import fnsb.macstransit.routematch.RouteMatch.Companion.parseData
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert

/**
 * Created by Spud on 6/25/20 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Release 1.2.
 */
class RouteMatchTest {

	/* TODO Move me to android tests (because context is now required)!
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
	 */

	@org.junit.Test
	fun parseDataTest() {

		// Load some test data
		Assert.assertTrue(Helper.MASTERROUTE_JSON.exists())
		Assert.assertTrue(Helper.MASTERROUTE_JSON.canRead())
		var data: String = getText(Helper.MASTERROUTE_JSON)
		Assert.assertNotNull(data)
		var testData: JSONObject = try {
			JSONObject(data)
		} catch (e: JSONException) {
			Assert.fail()
			return
		}

		// Run tests
		var array = parseData(testData)
		Assert.assertNotNull(array)
		Assert.assertNotEquals(0, array.length().toLong())
		Assert.assertEquals(8, array.length().toLong())

		// Load in empty test data
		Assert.assertTrue(Helper.ALL_VEHICLES_EMPTY_JSON.exists())
		Assert.assertTrue(Helper.ALL_VEHICLES_EMPTY_JSON.canRead())
		data = getText(Helper.ALL_VEHICLES_EMPTY_JSON)
		Assert.assertNotNull(data)
		testData = try {
			JSONObject(data)
		} catch (e: JSONException) {
			Assert.fail()
			return
		}

		// Run even more tests
		array = parseData(testData)
		Assert.assertNotNull(array)
		Assert.assertEquals(0, array.length().toLong())

		// Load in empty test data
		Assert.assertTrue(Helper.ALL_VEHICLES_JSON.exists())
		Assert.assertTrue(Helper.ALL_VEHICLES_JSON.canRead())
		data = getText(Helper.ALL_VEHICLES_JSON)
		Assert.assertNotNull(data)
		testData = try {
			JSONObject(data)
		} catch (e: JSONException) {
			Assert.fail()
			return
		}

		// Run even MORE tests
		array = parseData(testData)
		Assert.assertNotNull(array)
		Assert.assertEquals(3, array.length().toLong())
	}
}