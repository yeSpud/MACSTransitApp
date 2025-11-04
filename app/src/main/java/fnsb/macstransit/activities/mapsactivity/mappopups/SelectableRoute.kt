package fnsb.macstransit.activities.mapsactivity.mappopups

import android.content.Context
import android.util.AttributeSet
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import fnsb.macstransit.R

class SelectableRoute : LinearLayout {

	val selected: CheckBox

	val broadcastingIcon: ImageView

	val routeName: TextView

	// val routeFrequency: TextView

	val favoriteRoute: CheckBox


	constructor(context: Context): this(context, null)
	constructor(context: Context, attributeSet: AttributeSet?): this(context, attributeSet, 0)
	constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context,
		attributeSet, defStyleAttr) {
		inflate(context, R.layout.selectable_route, this)

		selected = findViewById(R.id.selected)
		broadcastingIcon = findViewById(R.id.broadcasting_icon)
		routeName = findViewById(R.id.route_name)
		// routeFrequency = findViewById(R.id.route_frequency)
		favoriteRoute = findViewById(R.id.favorite_route)
	}

}