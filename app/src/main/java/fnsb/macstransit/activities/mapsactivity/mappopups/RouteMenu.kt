package fnsb.macstransit.activities.mapsactivity.mappopups

import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.children
import com.orhanobut.dialogplus.DialogPlus
import fnsb.macstransit.R
import fnsb.macstransit.activities.LoadedRoutes
import fnsb.macstransit.activities.mapsactivity.MapsActivity
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.settings.CurrentSettings
import fnsb.macstransit.settings.V2
import org.json.JSONException

class RouteMenu(private val mapsActivity: MapsActivity): BaseAdapter() {

	private var setup = false

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

		val settings = CurrentSettings.settingsImplementation as V2
		for ((name, route) in LoadedRoutes.routes) {
			val selectableRoute = SelectableRoute(mapsActivity)
			selectableRoute.routeName.text = name
			selectableRoute.routeName.backgroundTintList = ColorStateList.valueOf(route.color)

			selectableRoute.selected.isChecked = route.enabled

			// Clicking the route name also checks the box
			selectableRoute.routeName.setOnClickListener {
				selectableRoute.selected.isChecked = !selectableRoute.selected.isChecked
			}

			selectableRoute.favoriteRoute.isChecked = settings.favoriteRouteNames.contains(name)

			routesContainer.addView(selectableRoute)
		}

		closeButton.setOnClickListener {
			mapsActivity.routeMenu.dismiss()
		}

		setup = true

		return view
	}

	companion object {

		fun onDismissListener(mapsActivity: MapsActivity, dialog: DialogPlus) {
			val routesContainer: LinearLayout = dialog.findViewById(R.id.routes) as LinearLayout

			val settings = CurrentSettings.settingsImplementation as V2

			val favoritedRoutes: MutableList<Route> = mutableListOf()
			for (selectableRoute in routesContainer.children) {
				if (selectableRoute !is SelectableRoute) {
					Log.w("onDismissListener", "Entry in routes container isn't selectable route")
					return
				}

				LoadedRoutes.routes[selectableRoute.routeName.text]?.enabled = selectableRoute.selected.isChecked

				if (selectableRoute.favoriteRoute.isChecked) {
					val route = LoadedRoutes.routes[selectableRoute.routeName.text]
					favoritedRoutes.add(route!!)
				}
			}

			val settingsJson = try {
				settings.formatSettingsToJsonString(
					settings.traffic,
					settings.darktheme, settings.polylines,
					settings.streetView, settings.maptype,
					*favoritedRoutes.toTypedArray()
				)
			} catch (e: JSONException) {
				Log.e("RouteMenu", "Json error when writing settings", e)
				return
			}
			settings.writeSettingsToFile(settingsJson.toString(), mapsActivity)

			// Reload the settings
			settings.parseSettings(settingsJson)

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
	}
}