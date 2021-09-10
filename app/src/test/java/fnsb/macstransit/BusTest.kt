package fnsb.macstransit

import com.google.android.gms.maps.model.LatLng
import fnsb.macstransit.routematch.Bus
import fnsb.macstransit.routematch.RouteMatch
import fnsb.macstransit.routematch.Route
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import java.lang.NullPointerException
import kotlin.collections.HashMap

/**
 * Created by Spud on 6/27/20 for the project: MACS Transit.
 *
 *
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Release 1.2.
 */
class BusTest {

	private val routes: HashMap<String, Route> = HashMap(3)

	@Test
	fun busesTest() {

		// Test the individual buses.
		// First test bad arguments,
		val empty: Array<Bus> = emptyArray()
		Assert.assertArrayEquals(empty, Bus.getBuses(JSONArray(), this.routes))

		// Now test valid buses.
		try {
			Assert.assertArrayEquals(empty, Bus.getBuses(RouteMatch.parseData(Helper.
			getJSON(Helper.ALL_VEHICLES_EMPTY_JSON)), this.routes))
			val buses: Array<Bus> = Bus.getBuses(RouteMatch.parseData(Helper.
			getJSON(Helper.ALL_VEHICLES_JSON)), this.routes)
			Assert.assertEquals(3, buses.size.toLong())

			// Test the individual buses.
			val ids: Array<String> = arrayOf("Bus 142", "Bus 131", "Bus 71")
			val lat: DoubleArray = doubleArrayOf(64.85543060302734, 64.81417083740234, 64.84135437011719)
			val lon: DoubleArray = doubleArrayOf(-147.7141876220703, -147.61318969726562, -147.71914672851562)
			for (i in buses.indices) {
				val bus: Bus = buses[i]
				Assert.assertEquals(ids[i], bus.name)
				Assert.assertSame(this.routes[bus.routeName], bus.route)
				Assert.assertEquals(lat[i], bus.location.latitude, 0.0)
				Assert.assertEquals(lon[i], bus.location.longitude, 0.0)
			}
		} catch (e: JSONException) {
			e.printStackTrace()
			Assert.fail()
		} catch (e: NullPointerException) {
			e.printStackTrace()
			Assert.fail()
		}
	}

	@Test
	fun createNewBusTest() {

		// Test with valid arguments.
		val ids: Array<String> = arrayOf("Bus 142", "Bus 131", "Bus 71")
		val lat: DoubleArray = doubleArrayOf(64.85543060302734, 64.81417083740234, 64.84135437011719)
		val lon: DoubleArray = doubleArrayOf(-147.7141876220703, -147.61318969726562, -147.71914672851562)
		try {
			val busArray: JSONArray = RouteMatch.parseData(Helper.getJSON(Helper.ALL_VEHICLES_JSON))
			for (i in 0 until busArray.length()) {

				val busObject: JSONObject = busArray.getJSONObject(i)


				val name: String = busObject.getString("vehicleId")
				val location = LatLng(busObject.getDouble("latitude"), busObject.getDouble("longitude"))
				val route = Route(busObject.getString("masterRouteId"))
				val heading: String = busObject.optString("headingName", "")
				val speed: Int = busObject.optInt("speed", 0)
				val bus = Bus(name, location, route, heading = heading, speed = speed)

				Assert.assertEquals(ids[i], bus.name)
				// Assert.assertSame(MapsActivity.allRoutes[i], bus.route) FIXME
				Assert.assertEquals(lat[i], bus.location.latitude, 0.0)
				Assert.assertEquals(lon[i], bus.location.longitude, 0.0)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			Assert.fail()
		}
	}

	@Test
	fun noBusMatchTest() {

		// Create various dummy test routes.
		val greenRoute = Route("Green")
		val blueRoute = Route("Blue")
		val otherGreen = Route("Green")

		val location = LatLng(0.0,0.0)

		// Create various dummy buses.
		val bus0 = Bus("0", location, greenRoute)
		val bus1 = Bus("1", location, greenRoute)
		val bus2 = Bus("2", location, greenRoute)
		val bus3 = Bus("3", location, greenRoute)
		val bus2Blue = Bus("2", location, blueRoute)
		val bus0OG = Bus("0", location, otherGreen)

		// Load most of the dummy buses into a test array.
		val testBusArray = arrayOf(bus0, bus1, bus2)

		// Test the noBusMatch method.
		Assert.assertFalse(bus0.isBusNotInArray(testBusArray))
		Assert.assertFalse(bus1.isBusNotInArray(testBusArray))
		Assert.assertFalse(bus2.isBusNotInArray(testBusArray))
		Assert.assertTrue(bus3.isBusNotInArray(testBusArray))
		Assert.assertTrue(bus2Blue.isBusNotInArray(testBusArray))
		Assert.assertFalse(bus0OG.isBusNotInArray(testBusArray))
	}

	init {
		this.routes["Brown"] = Route("Brown")
		this.routes["Green"] = Route("Green")
		this.routes["Red"] = Route("Red")
	}
}