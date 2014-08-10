package com.kritikalerror.findmepmt;

import com.ks.googleplaceapidemo.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * 
 * @author Michael Hii
 * @date   7/1/2014
 */
public class LauncherActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_launch);
		
		// Set the spinner
		Spinner spinner = (Spinner) findViewById(R.id.spinner1);
		// Create an ArrayAdapter using the string array and a default spinner layout
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
		        R.array.provider, android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new SpinnerCheck());

		// Set the button
		final ImageButton activityButton = (ImageButton) findViewById(R.id.find_start);

		activityButton.setOnClickListener(
			new View.OnClickListener()
			{
				public void onClick(View aView)
				{
					Spinner spinner = (Spinner) findViewById(R.id.spinner1);
					Intent toAnotherActivity = new Intent(aView.getContext(), MainActivity.class);
					toAnotherActivity.putExtra("provider", String.valueOf(spinner.getSelectedItem()));
					toAnotherActivity.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
					startActivity(toAnotherActivity);
				}
			}
		);       
	}
	
	public class SpinnerCheck implements OnItemSelectedListener {
	    public void onItemSelected(AdapterView<?> parent, View view, 
	            int pos, long id) {
	        // An item was selected. You can retrieve the selected item using
	        // parent.getItemAtPosition(pos)
	    	//Toast.makeText(getApplicationContext(), parent.getItemAtPosition(pos).toString(),
			//		Toast.LENGTH_LONG).show();
	    }

	    public void onNothingSelected(AdapterView<?> parent) {
	        // Another interface callback
	    }
	}
}
