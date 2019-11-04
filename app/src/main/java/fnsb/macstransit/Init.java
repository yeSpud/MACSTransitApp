package fnsb.macstransit;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by Spud on 2019-11-04 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 */
public class Init extends AppCompatActivity {

	@Override
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.init);
	}

	@Override
	protected void onResume() {
		super.onResume();
		this.findViewById(R.id.button).setOnClickListener((a) -> {
			this.startActivity(new Intent(this, MapsActivity.class));
			this.finish();
		});


	}

}
