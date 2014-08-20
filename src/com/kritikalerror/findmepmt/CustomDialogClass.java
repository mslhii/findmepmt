package com.kritikalerror.findmepmt;

import com.kritikalerror.findmepmt.R;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

/**
 * Custom dialog class to display About Us
 * @author Michael Hii
 * @date 8/19/2014
 */
public class CustomDialogClass extends Dialog implements android.view.View.OnClickListener {

	public Activity currentActivity;
	public Dialog customDialog;
	public Button okButton;

	public CustomDialogClass(Activity inputActivity) {
		super(inputActivity);
		// TODO Auto-generated constructor stub
		this.currentActivity = inputActivity;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.custom_dialog);
		okButton = (Button) findViewById(R.id.ok_button);
		okButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		dismiss();
	}
}
