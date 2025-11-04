package fnsb.macstransit.activities.mapsactivity.mappopups

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import fnsb.macstransit.R
import fnsb.macstransit.activities.LoadedRoutes
import fnsb.macstransit.activities.mapsactivity.MapsActivity

class RouteMenu(private val context: MapsActivity): BaseAdapter() {

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
		val layoutInflater: LayoutInflater = LayoutInflater.from(context)
		val view = convertView ?: layoutInflater.inflate(R.layout.route_menu, parent,
			false)
		val routesContainer: LinearLayout = view.findViewById(R.id.routes)

		for ((name, route) in LoadedRoutes.routes) {
			val selectableRoute = SelectableRoute(context)
			selectableRoute.routeName.text = name
			selectableRoute.routeName.backgroundTintList = ColorStateList.valueOf(route.color)
			selectableRoute.selected.isChecked = route.enabled

			routesContainer.addView(selectableRoute)
		}

		// view.set

		return view
	}

}