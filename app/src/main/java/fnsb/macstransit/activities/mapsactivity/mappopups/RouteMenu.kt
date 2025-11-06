package fnsb.macstransit.activities.mapsactivity.mappopups

import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.LinearLayout
import fnsb.macstransit.R
import fnsb.macstransit.activities.LoadedRoutes
import fnsb.macstransit.activities.mapsactivity.MapsActivity
import fnsb.macstransit.settings.CurrentSettings
import fnsb.macstransit.settings.V2

class RouteMenu(private val mapsActivity: MapsActivity): BaseAdapter() {

	private var setup = false

	private val settings = CurrentSettings.settingsImplementation as V2

	override fun getCount(): Int {
		return 1
	}

	override fun getItem(position: Int): Any {
		return position // Todo properly implement me?
	}

	override fun getItemId(position: Int): Long {
		return position.toLong() // Todo properly implement me?
	}

	override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
		val layoutInflater: LayoutInflater = LayoutInflater.from(mapsActivity)
		val view = convertView ?: layoutInflater.inflate(R.layout.route_menu, parent,
			false)
		val routesContainer: LinearLayout = view.findViewById(R.id.routes)
		val closeButton: Button = view.findViewById(R.id.close)

		if (setup) {
			return view
		}
		Log.d("RouteMenu", "Setting up menu for first time")

		for ((name, route) in LoadedRoutes.routes) {
			val selectableRoute = SelectableRoute(mapsActivity)
			selectableRoute.routeName.text = name
			selectableRoute.routeName.backgroundTintList = ColorStateList.valueOf(route.color)

			selectableRoute.selected.isChecked = route.enabled
			selectableRoute.selected.setOnCheckedChangeListener { _, isChecked ->
				LoadedRoutes.routes[name]!!.enabled = isChecked

				// todo: Only redraw on menu close

				// Try to (re)draw the buses onto the map.
				// Because we are iterating a static variable that is modified on a different thread
				// there is a possibility of a concurrent modification.
				try {
					mapsActivity.viewModel.drawBuses()
				} catch (e: ConcurrentModificationException) {
					Log.e("onOptionsItemSelected",
						"Unable to redraw all buses due to concurrent modification", e)
				}

				// (Re) draw the stops onto the map.
				mapsActivity.viewModel.drawStops()

				// (Re) draw the routes onto the map (if enabled).
				if (settings.polylines) {
					mapsActivity.viewModel.drawRoutes()
				}
			}

			selectableRoute.favoriteRoute.isChecked = settings.favoriteRouteNames.contains(name)
			selectableRoute.favoriteRoute.setOnCheckedChangeListener { _, isChecked ->
				// TODO
			}

			routesContainer.addView(selectableRoute)
		}

		closeButton.setOnClickListener {
			mapsActivity.routeMenu.dismiss()
		}

		setup = true

		return view
	}

}