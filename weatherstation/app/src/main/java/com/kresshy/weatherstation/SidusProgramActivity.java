package com.kresshy.weatherstation;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SidusProgramActivity extends ActionBarActivity {
	
	private final String TAG = "Program_Activity";
	private LinearLayout rootLinearLayout;

	private OnClickListener onClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			String rowNumber = ((TextView) ((LinearLayout) v.getParent()).getChildAt(0)).getText().toString();
			int index = 0;

			switch (v.getId()) {
			case R.id.time:
				index = 1;
				break;
			case R.id.servo1:
				index = 2;
				break;
			case R.id.servo2:
				index = 3;
				break;
			case R.id.servo3:
				index = 4;
				break;
			default:
				break;
			}

			Log.i(TAG, "RowNumber: " + rowNumber + "id: " + index);
			Toast.makeText(getApplicationContext(), "RowNumber: " + rowNumber + " id: " + index, Toast.LENGTH_LONG).show();
		}
	};

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_program);

		// scale dip to pixels
		final float scale = getApplicationContext().getResources().getDisplayMetrics().density;

		// display properties
		Display display = getWindowManager().getDefaultDisplay();

		// build the UI from XML elements
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// get the height and width of the display depending on API_LEVEL
		int width;
		int height;

		if (android.os.Build.VERSION.SDK_INT >= 13) {
			Point dimensions = new Point();
			display.getSize(dimensions);

			width = dimensions.x;
			height = dimensions.y;
		} else {
			width = display.getWidth();
			height = display.getHeight();
		}

		Log.i(TAG, "Screen dimensions: " + width + "x" + height + " dpi: " + scale);

		// find the linear layout on the UI
		rootLinearLayout = (LinearLayout) findViewById(R.id.programacitivity_linearlayout);
		rootLinearLayout.setBackgroundColor(Color.argb((int) 25, 0, 0, 0));

		// send button to upload program to timer
		Button sendButton = new Button(this);
		sendButton.setText("Send");

		// send Button layout parameters
		LinearLayout.LayoutParams sendButtonParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		sendButtonParams.setMargins(0, (int) (20 * scale), 0, (int) (20 * scale));
		sendButton.setLayoutParams(sendButtonParams);

		// add send button to the rootLinearLayout
		rootLinearLayout.addView(sendButton, sendButtonParams);

		// children TextViews layout parameters
		LinearLayout.LayoutParams textViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		textViewParams.setMargins((int) (1 * scale), (int) (1 * scale), (int) (1 * scale), (int) (1 * scale));

		// inflated LinearLayout layout parameters
		LinearLayout.LayoutParams lLayoutViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		lLayoutViewParams.setMargins((int) (width * 0.015 * scale), (int) (2.5 * scale), 0, (int) (2.5 * scale));

		LinearLayout tableHeaderLayout = (LinearLayout) inflater.inflate(R.layout.program_row, null);

		for (int i = 0; i < tableHeaderLayout.getChildCount(); i++) {
			TextView tView = (TextView) tableHeaderLayout.getChildAt(i);
			tView.setTextSize((int) (14));

			switch (i) {
			case 0:
				tView.setWidth((int) ((width * 0.09)));
				tView.setText("#");
				tView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
				break;
			case 1:
				tView.setWidth((int) ((width * 0.29)));
				tView.setText("Time");
				tView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
				break;
			case 2:
				tView.setWidth((int) ((width * 0.19)));
				tView.setText("Servo_1");
				tView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
				break;
			case 3:
				tView.setWidth((int) ((width * 0.19)));
				tView.setText("Servo_2");
				tView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
				break;
			case 4:
				tView.setWidth((int) ((width * 0.19)));
				tView.setText("Servo_3");
				tView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
				break;
			default:
				break;
			}

			tView.setPadding((int) (5 * scale), (int) (1 * scale), (int) (5 * scale), (int) (1 * scale));
			tView.setLayoutParams(textViewParams);
			tView.setBackgroundColor(Color.argb(255, 179, 213, 230));
		}

		rootLinearLayout.addView(tableHeaderLayout, lLayoutViewParams);

		// scrollview for the UI elements
		ScrollView scrollView = new ScrollView(getApplicationContext());
		rootLinearLayout.addView(scrollView);

		// inner scrollable container linear layout
		LinearLayout linearLayout = new LinearLayout(getApplicationContext());
		// linearLayout.setGravity(Gravity.CENTER_HORIZONTAL);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		scrollView.addView(linearLayout);

		for (int i = 0; i < 10; i++) {

			// inflate the linear layout
			LinearLayout lLayout = (LinearLayout) inflater.inflate(R.layout.program_row, null);

			// set the row number
			TextView view = (TextView) lLayout.getChildAt(0);
			view.setText("" + i);

			// set the children views parameters
			for (int j = 0; j < lLayout.getChildCount(); j++) {

				TextView tView = (TextView) lLayout.getChildAt(j);
				tView.setTextSize((int) (14 * scale));

				switch (j) {
				case 0:
					tView.setWidth((int) ((width * 0.09)));
					tView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
					break;
				case 1:
					tView.setWidth((int) ((width * 0.29)));
					tView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
					break;
				case 2:
					tView.setWidth((int) ((width * 0.19)));
					tView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
					break;
				case 3:
					tView.setWidth((int) ((width * 0.19)));
					tView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
					break;
				case 4:
					tView.setWidth((int) ((width * 0.19)));
					tView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
					break;
				default:
					break;
				}

				tView.setPadding((int) (5 * scale), (int) (1 * scale), (int) (5 * scale), (int) (1 * scale));
				tView.setLayoutParams(textViewParams);
				tView.setBackgroundColor(Color.argb(25, 0, 0, 0));
				tView.setOnClickListener(onClickListener);
			}

			// add the inflated layout to the LinearLayout on the UI
			linearLayout.addView(lLayout, lLayoutViewParams);
		}

		// set the UI changes
		setContentView(rootLinearLayout);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.program, menu);

		return true;
	}

}
