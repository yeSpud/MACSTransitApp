package fnsb.macstransit.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import fnsb.macstransit.R;

/**
 * Created by Spud on 2019-11-24 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.0
 * @since Beta 8.
 */
public class SettingsActivity extends AppCompatActivity {


	/**
	 * Variable used to track how many changes are to be applied when writing new settings.
	 * This is also used to determine whether or not the apply button should be enabled or disabled.
	 */
	private int changedSum = 0;

	/**
	 * TODO Documentation
	 *
	 * @param savedInstanceState
	 */
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// TODO
		this.setContentView(R.layout.settings);

		// TODO Load in and parse settings

		// TODO Update traffic view setting

		// TODO Update dark theme default setting

		// TODO Update polyline routes setting

		// TODO Update default map type setting

		// TODO Update current selected favorite routes setting

		// TODO Update current unselected favorite routes setting

		// Setup the cancel button
		this.findViewById(R.id.cancel).setOnClickListener((v) -> this.finish());

		// TODO Setup apply button action

	}

	/**
	 * Writes a given string to the settings file.
	 * <p>
	 * Derived from:
	 * https://developer.android.com/training/data-storage/app-specific#internal-store-stream
	 *
	 * @param string  The string to write to the file.
	 * @param context The context of the application (the activity that this is being called from).
	 */
	@Deprecated
	private static void writeToFile(String string, Context context) {
		/* FIXME
		try (java.io.FileOutputStream fos = context.openFileOutput(SettingsActivity.FILENAME, Context.MODE_PRIVATE)) {
			fos.write(string.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		 */
	}

	/**
	 * Writes all the settings to the file by determining which checkboxes are checked and unchecked.
	 *
	 * @param checkBoxes The checkboxes that dictate the current settings.
	 */
	private void writeSettings(CheckBox... checkBoxes) {
		// First, create the string based off the check boxes, and whether or no they are checked
		StringBuilder builder = new StringBuilder();

		// Then iterate through each checkbox, and determine the String name,
		// and whether or not the box has been checked.
		for (CheckBox box : checkBoxes) {
			Object tag = box.getTag();
			if (tag != null) {
				if (tag instanceof String) {
					builder.append(String.format("%s:%s\n", tag, Boolean.toString(box.isChecked())));
				}
			}
		}

		// Then, write that string to the settings file.
		SettingsActivity.writeToFile(builder.toString(), this);
	}

	/**
	 * TODO Documentation
	 */
	private void writeSettings() {
		// TODO
	}

	/**
	 * Creates and shows the settings popup dialog.
	 */
	@Deprecated
	public void showSettingsPopup() {

		Context context = this;

		// Find and inflate the settings view.
		android.view.View dialogView = android.view.LayoutInflater.from(context)
				.inflate(R.layout.settings, this.findViewById(R.id.content), false);

		// Setup the apply button.
		final Button applyButton = dialogView.findViewById(R.id.apply);

		// Create the checkboxes in the settings popup menu.
		/* FIXME
		final CheckBox trafficBox = this.createCheckbox(dialogView, R.id.traffic,
				SettingsActivity.ENABLE_TRAFFIC_VIEW, applyButton, SettingsActivity.TRAFFIC_KEY),
				nightBox = this.createCheckbox(dialogView, R.id.night_mode,
						SettingsActivity.DEFAULT_NIGHT_MODE, applyButton,
						SettingsActivity.NIGHT_MODE_KEY), polyBox = this.createCheckbox(dialogView,
				R.id.polylines, SettingsActivity.SHOW_POLYLINES, applyButton,
				SettingsActivity.POLYLINES_KEY), VRBox = this.createCheckbox(dialogView, R.id.VR,
				SettingsActivity.ENABLE_VR_OPTIONS, applyButton, SettingsActivity.VR_KEY);
		 */

		// Create the dialog via the alert dialog builder.
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setView(dialogView);
		AlertDialog alertDialog = builder.create();

		// Setup the cancel button.
		dialogView.findViewById(R.id.cancel).setOnClickListener((click) -> alertDialog.cancel());

		// Setup the apply button click listener.
		applyButton.setOnClickListener((click) -> {
			// Write the settings to the settings file.
			//this.writeSettings(trafficBox, nightBox, polyBox, VRBox); FIXME

			// Inform the user that a restart is required in order for changes to take effect.
			Toast.makeText(context, R.string.restart_required, Toast.LENGTH_LONG)
					.show();

			// Close the alert dialog.
			alertDialog.cancel();
		});

		// Show the dialog.
		alertDialog.show();
	}

	/**
	 * Creates the checkboxes as that will be used in the settings popup window.
	 *
	 * @param view    The settings popup window view.
	 * @param id      THe ID of the Checkbox within the View.
	 * @param checked Whether or not the box is to be checked by default.
	 * @param button  The apply button
	 *                (as the apply button needs to be passed as an argument for the checkbox's listener).
	 * @param tag     The text of the checkbox (which will be applied as the checkbox's tag)
	 * @return The newly created checkbox object.
	 */
	private CheckBox createCheckbox(android.view.View view, int id, boolean checked, Button button,
	                                String tag) {

		// Find the checkbox within the view.
		CheckBox checkBox = view.findViewById(id);

		// Set the checkbox to be checked based on the checked value.
		checkBox.setChecked(checked);

		// Add an onCheckChanged listener to update the apply button.
		checkBox.setOnCheckedChangeListener((a, checkedValue) -> this
				.changeApplyButton(checkedValue, checked, button));

		// Set the tag of the checkbox.
		checkBox.setTag(tag);

		// Return the newly created checkbox.
		return checkBox;
	}

	/**
	 * Sets the apply button to be enabled or disabled depending on the changedSum value.
	 *
	 * @param defaultValue The default value of the boolean.
	 * @param newValue     The current value of the boolean.
	 * @param button       The apply button.
	 */
	private void changeApplyButton(boolean defaultValue, boolean newValue, Button button) {
		// If the default current value is different than the default value,
		// increase the change sum by 1.
		// If the two values are the same, decrease the change sum by 1.
		this.changedSum = (newValue != defaultValue) ? this.changedSum + 1 : this.changedSum - 1;
		Log.d("showSettingsPopup", "Updated changedSum to " + this.changedSum);
		button.setEnabled(this.changedSum > 0);
	}
}
