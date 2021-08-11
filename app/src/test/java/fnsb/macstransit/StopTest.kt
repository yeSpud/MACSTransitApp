package fnsb.macstransit

import fnsb.macstransit.activities.MapsActivity
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.routematch.RouteMatch
import fnsb.macstransit.routematch.SharedStop
import fnsb.macstransit.routematch.Stop
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import java.util.*

/**
 * Created by Spud on 10/26/20 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Release 1.2.
 */
class StopTest {

	@Test
	fun StopCreationTest() {

		val blueStopJson: JSONObject = try {
			 Helper.getJSON(Helper.BLUE_STOPS)
		} catch (e: JSONException) {
			e.printStackTrace()
			Assert.fail()
			return
		}

		val blueStopJsonArray: JSONArray = RouteMatch.parseData(blueStopJson)
		val blueRoute = Route("Blue")
		var startTime: Long = System.nanoTime()
		var blueStops: Array<Stop> = Stop.generateStops(blueStopJsonArray, blueRoute)
		var endTime: Long = System.nanoTime()
		Helper.printTime(startTime, endTime)

		// Before duplication checking this should be 232 in length
		Assert.assertNotNull(blueStops)
		Assert.assertEquals(233, blueStops.size.toLong())
		startTime = System.nanoTime()
		blueStops = Stop.validateGeneratedStops(blueStops)
		endTime = System.nanoTime()
		Helper.printTime(startTime, endTime)

		// After duplication checking
		Assert.assertNotNull(blueStops)
		Assert.assertEquals(66, blueStops.size.toLong())
	}

	/*
	@Test
	fun isDuplicateCheck() {
			// TODO
	}
	 */

	@Test
	fun averageStops() {
		val files = arrayOf(Helper.BLUE_STOPS, Helper.BROWN_STOPS, Helper.GOLD_STOPS, Helper.GREEN_STOPS,
		                    Helper.PURPLE_STOPS, Helper.RED_STOPS, Helper.YELLOW_STOPS)
		var count = 0
		for (file in files) {
			try {
				val jsonObject: JSONObject = Helper.getJSON(file)
				val jsonArray: JSONArray = RouteMatch.parseData(jsonObject)
				val pStops: Array<Stop> = Stop.generateStops(jsonArray, Route("Foo"))
				val vStops: Array<Stop> = Stop.validateGeneratedStops(pStops)
				count += vStops.size
			} catch (e: Exception) {
				e.printStackTrace()
				Assert.fail()
			}
		}
		println(count / files.size)
	}

	@Test
	/*
	 * This test should test the loading of multiple stops, apply a duplication check, apply shared stops,
	 * and finally remove overlapping shared stops and regular stops.
	 */
	fun stopGauntlet() {
		try {			// Start by getting the json files to use for testing.
			// TODO Add Gray, Orange lines.
			val loadedFiles = 6
			val files = arrayOf(Helper.BLUE_STOPS, Helper.BROWN_STOPS, Helper.GREEN_STOPS, Helper.PURPLE_STOPS,
			                    Helper.RED_STOPS, Helper.YELLOW_STOPS)
			val routes = arrayOf(Route("Blue"), Route("Brown"), Route("Green"),
			                     Route("Purple"), Route("Red"), Route("Yellow"))


			// Check the stops that will have duplicates.
			val stopsWithDuplicates = ArrayList<Array<Stop>>(loadedFiles)

			// Start by loading the stops from each file.
			for (i in 0 until loadedFiles) {
				val stopJsonFile: java.io.File = files[i]
				val jsonObject: JSONObject = Helper.getJSON(stopJsonFile)
				val dataArray: JSONArray = RouteMatch.parseData(jsonObject)

				// Also iterate though the raw data and print each stops lat long
				/*
				System.out.println(routes[i].routeName + " stops:");
				for (int j = 0; j < dataArray.length(); j++) {
					JSONObject stop = dataArray.getJSONObject(j);
					double lat = stop.getDouble("latitude");
					double lon = stop.getDouble("longitude");
					System.out.println(String.format("%f, %f", lat, lon));
				}
				 */
				val stops: Array<Stop> = Stop.generateStops(dataArray, routes[i])
				stopsWithDuplicates.add(stops)
			}

			// Now iterate though each stop that has duplicates and verify the number of stops.
			// This number should be large as we haven not removed the duplicate stops at this point.
			val validDuplicateStopCounts = intArrayOf(233, 24, 144, 78, 176, 145)
			for (i in 0 until loadedFiles) {
				val stops: Array<Stop> = stopsWithDuplicates[i]
				println("Number of stops for ${stops[0].route.routeName} (with potential duplicates): ${stops.size}\n")
				Assert.assertEquals(validDuplicateStopCounts[i], stops.size)
			}


			// Now test the removal of duplicate stops.
			val validateStopCounts = intArrayOf(66, 24, 104, 39, 58, 56)
			for (i in 0 until loadedFiles) {
				val stops: Array<Stop> = stopsWithDuplicates[i]
				val vStops: Array<Stop> = Stop.validateGeneratedStops(stops)
				println("Number of stops for ${vStops[0].route.routeName}: ${vStops.size}\n")
				Assert.assertEquals(validateStopCounts[i], vStops.size)
				routes[i].stops = vStops
			}


			// Temporarily set all routes to not null in order to bypass a null check.
			MapsActivity.allRoutes = routes

			// Now test the creation of shared stops.
			for (routeIndex in 0 until loadedFiles) {

				// Get a first comparison route.
				val route = routes[routeIndex]

				// Iterate through all the stops in our first comparison route.
				for (stop in route.stops) {

					// Make sure our stop is not already in our shared stop.
					val sharedStops = route.sharedStops
					if (sharedStops.isNotEmpty()) {
						var found = false

						// Iterate though the shared stops in the route.
						for (ssCheck in sharedStops) {

							// If the route was found, continue.
							if (ssCheck.equals(stop)) {
								found = true
								break
							}
						}
						if (found) {
							continue
						}
					}

					// Get an array of shared routes.
					val sharedRoutes = SharedStop.getSharedRoutes(route, routeIndex, stop)

					// If the shared routes array has more than one entry, create a new shared stop object.
					if (sharedRoutes.size > 1) {
						val sharedStop = SharedStop(stop.circleOptions.center, stop.name, sharedRoutes)

						// Iterate though all the routes in the shared route, and add our newly created shared stop.
						for (sharedRoute in sharedRoutes) {
							sharedRoute.addSharedStop(sharedStop)
						}
					}
				}
			}

			// Test the number of shared stops.
			val sharedStopsCount = intArrayOf(14, 3, 10, 10, 12, 17)
			for (i in 0 until loadedFiles) {
				val route: Route = routes[i]
				println("${route.routeName} route stops: ${route.stops.size}\n")
				val sharedStops: Array<SharedStop> = route.sharedStops
				println("${route.routeName} route shared stops: ${sharedStops.size}\n")
				Assert.assertEquals(sharedStopsCount[i], sharedStops.size)
			}

			// Reset all routes.
			MapsActivity.allRoutes = null


			// Test removal of stops that have shared stops.
			val finalStops = ArrayList<Array<Stop>>(loadedFiles)
			for (route in routes) {
				val stops: Array<Stop> = SharedStop.removeStopsWithSharedStops(route.stops, route.sharedStops)
				println("Going from ${route.stops.size} stops to ${stops.size} stops for route ${route.routeName}\n")
				finalStops.add(stops)
			}
			val finalStopCount = intArrayOf(66 - 14, 24 - 3, 104 - 10, 39 - 10, 58 - 12, 56 - 17)
			for (i in 0 until loadedFiles) {
				val stops: Array<Stop> = finalStops[i]
				Assert.assertEquals(finalStopCount[i], stops.size)
			}
		} catch (e: JSONException) {

			// If anything goes wrong, print and then fail.
			e.printStackTrace()
			Assert.fail()
		} catch (e: RuntimeException) {
			e.printStackTrace()
			Assert.fail()
		}
	}
}