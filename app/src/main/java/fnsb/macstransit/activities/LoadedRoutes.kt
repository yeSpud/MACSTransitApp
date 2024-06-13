package fnsb.macstransit.activities

import fnsb.macstransit.routematch.Route

object LoadedRoutes {

	/**
	 * All of the routes that can be tracked by the app. This will be determined by the master schedule.
	 */
	val routes: HashMap<String, Route> = HashMap() // todo Preload this from default_routes.json?
}