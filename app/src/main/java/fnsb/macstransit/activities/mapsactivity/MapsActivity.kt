package fnsb.macstransit.activities.mapsactivity

import android.graphics.Color
import androidx.core.graphics.Insets
import android.os.Bundle
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.R
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.SupportMapFragment
import com.orhanobut.dialogplus.DialogPlus
import fnsb.macstransit.activities.LoadedRoutes
import fnsb.macstransit.activities.SettingsActivity
import fnsb.macstransit.activities.mapsactivity.mappopups.FarePopupWindow
import fnsb.macstransit.activities.mapsactivity.mappopups.RouteMenu
import fnsb.macstransit.databinding.ActivityMapsBinding
import fnsb.macstransit.routematch.Bus
import fnsb.macstransit.routematch.SharedStop
import fnsb.macstransit.routematch.Stop
import fnsb.macstransit.settings.CurrentSettings
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException

class MapsActivity: FragmentActivity() {

	/**
	 * The view model for the maps activity.
	 * This is usually where all the large functions and additional properties are.
	 */
	lateinit var viewModel: MapsViewModel

	/**
	 * Create a variable to store our fare popup window instance.
	 */
	lateinit var farePopupWindow: FarePopupWindow

	lateinit var routeMenu: DialogPlus

	lateinit var binding: ActivityMapsBinding

	var bars: Insets? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		Log.v("onCreate", "onCreate has been called!")

		// https://stackoverflow.com/questions/76960994/enableedgetoedge-navigation-system-bar-is-not-fully-transparent
		enableEdgeToEdge(
			statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
			navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT) // light causes internally enforce the navigation bar to be fully transparent
		)
		super.onCreate(savedInstanceState)

		// Setup view model.
		viewModel = ViewModelProvider(this)[MapsViewModel::class.java]

		// Setup binding.
		binding = ActivityMapsBinding.inflate(layoutInflater)
		binding.viewmodel = viewModel
		binding.lifecycleOwner = this
		binding.activity = this

		// Set the activity view to the map activity layout.
		setContentView(binding.root)

		// Load in the current settings.
		try {
			CurrentSettings.loadSettings(this)
		} catch (e: JSONException) {

			// If there was an exception loading the settings simply log it and return.
			Log.e("onCreate", "Exception when loading settings", e)
			return
		}

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val supportFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

		// Launch the maps coroutine (which sets up the Google Map object).
		lifecycleScope.launch {
			val setupJob = launch(Dispatchers.Main, CoroutineStart.LAZY) { viewModel.setupMap(supportFragment, this@MapsActivity) }
			Log.v("onCreate", "Setting up map")
			setupJob.start()
			setupJob.join()
			Log.v("onCreate", "Map finished")
			repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.runUpdater() }
		}

		// Setup the fares popup window.
		Log.v("onCreate", "Setting up fare window")
		farePopupWindow = FarePopupWindow(this)

		ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
			val bars = insets.getInsets(
				WindowInsetsCompat.Type.systemBars()
						or WindowInsetsCompat.Type.displayCutout()
			)

			binding.buttons.setPadding(bars.left,bars.top, bars.right, bars.bottom)
			this.bars = bars

			WindowInsetsCompat.CONSUMED
		}
	}

	override fun onDestroy() {
		Log.v("onDestroy", "onDestroy called!")
		super.onDestroy()

		// Launch the following as a cleanup job (that way we can essentially multi-thread the onDestroy process)
		lifecycleScope.launch(Dispatchers.Main, start= CoroutineStart.UNDISPATCHED) {
			Log.i("onDestroy", "Beginning onDestroy cleanup coroutine...")

			// Iterate though each route to get access to its shared stops and regular stops.
			for (route: Route in LoadedRoutes.routes.values) {

				Log.d("onDestroy", "Removing stop circles")
				for (stop: Stop in route.stops.values) {
					stop.close()
				}

				Log.d("onDestroy", "Removing shared stop circles")
				for (sharedStop: SharedStop in route.sharedStops.values) {
					sharedStop.close()
				}

				// Remove route polylines.
				Log.d("onDestroy", "Removing route polyline")
				route.removePolyline()
			}

			Log.i("onDestroy", "Finished onDestroy cleanup coroutine")
		}

		Log.d("onDestroy", "Removing bus markers")
		for (bus: Bus in viewModel.buses) {
			bus.close()
		}

		// Stop the update thread.
		if (viewModel.updater != null) {
			viewModel.updater!!.run = false
			viewModel.updater = null
		}

		// Be sure to clear the map.
		if (viewModel.map != null) {
			viewModel.map!!.clear()
			viewModel.map = null
		}

		Log.v("onDestroy", "Finished onDestroy")
	}

	override fun onResume() {
		Log.v("onResume", "onResume has been called!")
		super.onResume()

		// Update the map's dynamic settings.
		viewModel.updateMapSettings()

		// Resume the coroutine
		if (viewModel.updater != null) {
			viewModel.runUpdater()
		}
	}

	override fun onPause() {
		Log.v("onPause", "onPause has been called!")
		super.onPause()

		// Stop the coroutine
		if (viewModel.updater != null) {
			viewModel.updater!!.run = false
		}
	}

	fun showRoutesMenu() {

		// Constantly recreating the dialog window is wasteful,
		// but it fixes a bug where after the menu was dismissed its height would be crushed back down to minimums
		routeMenu = DialogPlus.newDialog(this)
			.setAdapter(RouteMenu(this))
			.setContentBackgroundResource(R.color.colorPrimaryDark)
			.setExpanded(false)
			.setGravity(Gravity.CENTER)
			.setContentWidth(ViewGroup.LayoutParams.MATCH_PARENT)
			.setContentHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
			.setOnDismissListener { dialog -> RouteMenu.onDismissListener(this, dialog) }
			.create()

		routeMenu.show()
	}

	fun launchSettingsActivity() {

		// Create the intent to launch the settings activity.
		val settingsIntent = android.content.Intent(this, SettingsActivity::class.java)

		// Start the settings activity.
		startActivity(settingsIntent)
	}

	companion object {

		/**
		 * Used to determine if the MapsActivity has been run before in the app's lifecycle.
		 * This will be set to true coming out of SplashActivity
		 */
		var firstRun: Boolean = true
	}
}
