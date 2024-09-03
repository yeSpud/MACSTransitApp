package fnsb.macstransit.activities.mapsactivity.mappopups

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import fnsb.macstransit.R

class StopEntry : LinearLayout {

	val routeName: TextView?
	val stopTime: TextView?

	constructor(context: Context): this(context, null)
	constructor(context: Context, attributeSet: AttributeSet?): this(context, attributeSet, 0)
	constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context,
	                                                                                      attributeSet,
	                                                                                      defStyleAttr) {
		inflate(context, R.layout.stop_entry, this)
		routeName = findViewById(R.id.route_name)
		stopTime = findViewById(R.id.stop_name)
	}
}