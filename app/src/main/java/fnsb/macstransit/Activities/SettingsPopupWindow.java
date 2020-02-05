package fnsb.macstransit.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import fnsb.macstransit.R;

/**
 * Created by Spud on 2019-11-24 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.2
 * @since Beta 8.
 */
@SuppressWarnings("WeakerAccess")
public class SettingsPopupWindow extends AlertDialog {

	/**
	 * Various string constants used by this class,
	 * such as the key names for various settings, or the file name for the settings file.
	 */
	private static final String TRAFFIC_KEY = "Enable Traffic View",
			NIGHT_MODE_KEY = "Enable Dark Theme", POLYLINES_KEY = "Show Polylines",
			VR_KEY = "Show VR Options", FILENAME = "settings.txt";

	/**
	 * Booleans used by the application to determine what settings should be enabled during initialization.
	 */
	public static boolean ENABLE_TRAFFIC_VIEW, DEFAULT_NIGHT_MODE, SHOW_POLYLINES, ENABLE_VR_OPTIONS;

	/**
	 * Variable used to track how many changes are to be applied when writing new settings.
	 * This is also used to determine whether or not the apply button should be enabled or disabled.
	 */
	private int changedSum = 0;

	/**
	 * Constructor for the Settings popup window.
	 *
	 * @param context The context this class is being called from (the activity).
	 */
	public SettingsPopupWindow(Context context) {
		super(context);
	}

	/**
	 * Loads the settings from the settings file,
	 * and stores them in the static global variables for this class.
	 *
	 * @param context The context of the application (the activity that this is being called from).
	 */
	public static void loadSettings(Context context) {
		// First get the settings file
		File file = new File(context.getFilesDir(), SettingsPopupWindow.FILENAME);
		Log.d("loadSettings", "Supposed file location: " + file.getAbsolutePath());

		// Then be sure to check if the file exists
		Log.d("loadSettings", "Checking of settings file exists...");
		if (file.exists()) {
			// Get the file content
			Log.d("loadSettings", "Reading content of settings");
			String[] content = SettingsPopupWindow.readFile(context);

			if (content != null) {
				// Parse the settings into the static global variables above.
				for (String string : content) {
					String[] line = string.split(":");
					// Check what the first line is (to see if its an important key).
					switch (line[0]) {
						case SettingsPopupWindow.TRAFFIC_KEY:
							Log.d("loadSettings", "Updating traffic view setting");
							SettingsPopupWindow.ENABLE_TRAFFIC_VIEW = Boolean.parseBoolean(line[1]);
							break;
						case SettingsPopupWindow.NIGHT_MODE_KEY:
							Log.d("loadSettings", "Updating dark mode setting");
							SettingsPopupWindow.DEFAULT_NIGHT_MODE = Boolean.parseBoolean(line[1]);
							break;
						case SettingsPopupWindow.POLYLINES_KEY:
							Log.d("loadSettings", "Updating polyline setting");
							SettingsPopupWindow.SHOW_POLYLINES = Boolean.parseBoolean(line[1]);
							break;
						case SettingsPopupWindow.VR_KEY:
							Log.d("loadSettings", "Updating VR setting");
							SettingsPopupWindow.ENABLE_VR_OPTIONS = Boolean.parseBoolean(line[1]);
							break;
						default:
							Log.w("loadSettings", "Line unaccounted for!\n" + string);
							break;
					}
				}
			} else {
				// Since we were unable to load the content of the file, recreate it and rerun.
				Log.w("loadSettings", "Unable to parse content!");
				SettingsPopupWindow.createSettingsFile(context);
				SettingsPopupWindow.loadSettings(context);
			}
		} else {
			// Since the file doesn't exist, create a new one and return
			Log.w("loadSettings", "File does not exist! Creating new one...");
			SettingsPopupWindow.createSettingsFile(context);
			SettingsPopupWindow.loadSettings(context);
		}
	}

	/**
	 * Creates a new settings file with default values.
	 *
	 * @param context The context of the application (the activity that this is being called from).
	 */
	private static void createSettingsFile(Context context) {
		Log.d("createSettingsFile", "Creating new settings file");

		// Create the string to write to the file for the first time.
		String outputString = SettingsPopupWindow.TRAFFIC_KEY + ":true\n"
				+ SettingsPopupWindow.NIGHT_MODE_KEY + ":false\n"
				+ SettingsPopupWindow.POLYLINES_KEY + ":false\n"
				+ SettingsPopupWindow.VR_KEY + ":false";

		// Write that string to the settings file
		SettingsPopupWindow.writeToFile(outputString, context);
	}

	/**
	 * Reads the content of the settings file, and splits on the new line.
	 * <p>
	 * Derived from:
	 * https://developer.android.com/training/data-storage/app-specific#internal-access-stream
	 *
	 * @param context The context of the application (the activity that this is being called from).
	 * @return The content of the settings file as a string array. Each new line is a new string.
	 */
	private static String[] readFile(Context context) {
		// Try to create a file input stream in order to read the data from the file.
		java.io.FileInputStream fis = null;
		try {
			fis = context.openFileInput(SettingsPopupWindow.FILENAME);
		} catch (java.io.FileNotFoundException e) {
			e.printStackTrace();
		}

		// If the file input stream was created successfully, execute the following:
		if (fis != null) {
			StringBuilder stringBuilder = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(fis,
					java.nio.charset.StandardCharsets.UTF_8))) {
				String line = reader.readLine();
				while (line != null) {
					stringBuilder.append(line).append("\n");
					line = reader.readLine();
				}
			} catch (IOException e) {
				// Error occurred when opening raw file for reading.
				e.printStackTrace();
			}
			String contents = stringBuilder.toString();
			return contents.split("\n");
		} else {
			// If the file input stream was unable to be created, return null (for now).
			return null;
		}
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
	private static void writeToFile(String string, Context context) {
		try (java.io.FileOutputStream fos = context.openFileOutput(SettingsPopupWindow.FILENAME, Context.MODE_PRIVATE)) {
			fos.write(string.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		SettingsPopupWindow.writeToFile(builder.toString(), this.getContext());
	}

	/**
	 * Creates and shows the settings popup dialog.
	 */
	public void showSettingsPopup() {

		Context context = this.getContext();

		// Find and inflate the settings view.
		android.view.View dialogView = android.view.LayoutInflater.from(context)
				.inflate(R.layout.settings_popup, this.findViewById(R.id.content), false);

		// Setup the apply button.
		final Button applyButton = dialogView.findViewById(R.id.apply);

		// Create the checkboxes in the settings popup menu.
		final CheckBox trafficBox = this.createCheckbox(dialogView, R.id.traffic,
				SettingsPopupWindow.ENABLE_TRAFFIC_VIEW, applyButton, SettingsPopupWindow.TRAFFIC_KEY),
				nightBox = this.createCheckbox(dialogView, R.id.nightMode,
						SettingsPopupWindow.DEFAULT_NIGHT_MODE, applyButton,
						SettingsPopupWindow.NIGHT_MODE_KEY), polyBox = this.createCheckbox(dialogView,
				R.id.polylines, SettingsPopupWindow.SHOW_POLYLINES, applyButton,
				SettingsPopupWindow.POLYLINES_KEY), VRBox = this.createCheckbox(dialogView, R.id.VR,
				SettingsPopupWindow.ENABLE_VR_OPTIONS, applyButton, SettingsPopupWindow.VR_KEY);

		// Create the dialog via the alert dialog builder.
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setView(dialogView);
		AlertDialog alertDialog = builder.create();

		// Setup the cancel button.
		dialogView.findViewById(R.id.cancel).setOnClickListener((click) -> alertDialog.cancel());

		// Setup the apply button click listener.
		applyButton.setOnClickListener((click) -> {
			// Write the settings to the settings file.
			this.writeSettings(trafficBox, nightBox, polyBox, VRBox);

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
