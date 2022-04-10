package com.pranav.java.ide;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.radiobutton.MaterialRadioButton;

public class SettingActivity extends AppCompatActivity {
	
	private MaterialRadioButton java3;
	private MaterialRadioButton java4;
	private MaterialRadioButton java5;
	private MaterialRadioButton java6;
	private MaterialRadioButton java8;
	
	private MaterialRadioButton dexer_d8;
	
	private AppCompatEditText classpath;
	
	private SharedPreferences settings;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting);
		
		final Toolbar toolbar = findViewById(R.id.toolbar);
	
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		toolbar.setNavigationOnClickListener(v -> onBackPressed());
		
		java3 = findViewById(R.id.java3);
		java4 = findViewById(R.id.java4);
		java5 = findViewById(R.id.java5);
		java6 = findViewById(R.id.java6);
		java8 = findViewById(R.id.java8);
		
		final MaterialRadioButton java7 = findViewById(R.id.java7);
		final MaterialRadioButton dexer_dx = findViewById(R.id.dexer_dx);
		dexer_d8 = findViewById(R.id.dexer_d8);
		classpath = findViewById(R.id.classpath);
		settings = getSharedPreferences("compiler_settings", MODE_PRIVATE);

		switch (settings.getString("javaVersion", "1.7")) {
				
			case "1.3":
			    java3.setChecked(true);
				  break;
				
			case "1.4":
			    java4.setChecked(true);
			    break;
				
			case "1.5":
			    java5.setChecked(true);
			    break;
				
			case "1.6":
			    java6.setChecked(true);
			    break;
				
			case "1.8":
			    java8.setChecked(true);
			    break;
				
			case "1.7":
			default:
			    java7.setChecked(true);
			    break;
		}
		
    switch (settings.getString("dexer", "dx")) {
		  case "d8":
		    dexer_d8.setChecked(true);
		    break;
		  
		  case "dx":
		  default:
		    dexer_dx.setChecked(true);
		    break;
		}
		classpath.setText(settings.getString("classpath", ""));
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		//Set a default value to local variable version to avoid npe
		double version = 1.7;
		if (java3.isChecked()) version = 1.3;
		else if (java4.isChecked()) version = 1.4;
		else if (java5.isChecked()) version = 5.0;
		else if (java6.isChecked()) version = 6.0;
		else if (java8.isChecked()) version = 8.0;
		else if (java9.isChecked()) version = 9.0;
		else if (java10.isChecked()) version = 10.0;
		else if (java11.isChecked()) version = 11.0;
		else if (java12.isChecked()) version = 12.0;
		else if (java13.isChecked()) version = 13.0;
		else if (java14.isChecked()) version = 14.0;
		else if (java15.isChecked()) version = 15.0;
		settings.edit().putString("javaVersion", String.valueOf(version)).apply();
		String dexer = "dx";
		if (dexer_d8.isChecked()) dexer = "d8";
		settings.edit().putString("dexer", dexer).commit();
		settings.edit().putString("classpath", classpath.getText().toString()).apply();
	}
}
