package com.pranav.java.ide;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.*;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.appbar.AppBarLayout;
import android.app.*;
import android.os.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.webkit.*;
import android.animation.*;
import android.view.animation.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.text.*;
import org.json.*;
import android.widget.HorizontalScrollView;
import com.google.android.material.textfield.*;
import android.app.Activity;
import android.content.SharedPreferences;
import io.github.rosemoe.sora.langs.java.*;
import io.github.rosemoe.sora.*;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.radiobutton.*;
import androidx.appcompat.widget.AppCompatEditText;
import com.google.android.material.textview.MaterialTextView;


public class SettingActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private MaterialTextView versionTxt;
	private HorizontalScrollView hscroll3;
	private MaterialTextView classpathTxt;
	private TextInputLayout textinputlayout1;
	private RadioGroup javaGroup;
	private MaterialRadioButton java3;
	private MaterialRadioButton java4;
	private MaterialRadioButton java5;
	private MaterialRadioButton java6;
	private MaterialRadioButton java7;
	private MaterialRadioButton java8;
	private AppCompatEditText classpath;
	
	private SharedPreferences settings;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.setting);
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		_app_bar = findViewById(R.id._app_bar);
		_coordinator = findViewById(R.id._coordinator);
		_toolbar = findViewById(R.id._toolbar);
		setSupportActionBar(_toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		_toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _v) {
				onBackPressed();
			}
		});
		versionTxt = findViewById(R.id.versionTxt);
		hscroll3 = findViewById(R.id.hscroll3);
		classpathTxt = findViewById(R.id.classpathTxt);
		textinputlayout1 = findViewById(R.id.textinputlayout1);
		javaGroup = findViewById(R.id.javaGroup);
		java3 = findViewById(R.id.java3);
		java4 = findViewById(R.id.java4);
		java5 = findViewById(R.id.java5);
		java6 = findViewById(R.id.java6);
		java7 = findViewById(R.id.java7);
		java8 = findViewById(R.id.java8);
		classpath = findViewById(R.id.classpath);
		settings = getSharedPreferences("compiler_settings", Activity.MODE_PRIVATE);
	}
	
	private void initializeLogic() {
		if (settings.contains("javaVersion")) {
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
				
				case "1.7":
				    java7.setChecked(true);
				    break;
				
				case "1.8":
				    java8.setChecked(true);
				    break;
				
				default:
				    java7.setChecked(true);
				    break;
			}
			classpath.setText(settings.getString("classpath", ""));
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		//Set a default value to local variable version to avoid npe
		double version = 7;
		if (java3.isChecked()) version = 1.3;
		else if (java4.isChecked()) version = 1.4;
		else if (java5.isChecked()) version = 1.5;
		else if (java6.isChecked()) version = 1.6;
		else if (java7.isChecked()) version = 1.7;
		else if (java8.isChecked()) version = 1.8;
		settings.edit().putString("javaVersion", String.valueOf(version)).commit();
		settings.edit().putString("classpath", classpath.getText().toString()).commit();
	}
	

}
