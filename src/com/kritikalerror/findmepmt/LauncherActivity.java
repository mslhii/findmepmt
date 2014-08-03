package com.kritikalerror.findmepmt;

import com.ks.googleplaceapidemo.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

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

		final Button activityButton = (Button) findViewById(R.id.find_start);

		activityButton.setOnClickListener(
			new View.OnClickListener()
			{
				public void onClick(View aView)
				{
					Intent toAnotherActivity = new Intent(aView.getContext(), MainActivity.class);
					startActivity(toAnotherActivity);
				}
			}
		);       
	}
}
