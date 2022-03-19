package com.pranav.java.ide

import androidx.appcompat.app.AppCompatActivity
import androidx.annotation.*
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import android.app.*
import android.os.*
import android.view.*
import android.view.View.*
import android.widget.*
import android.content.*
import android.content.res.*
import android.graphics.*
import android.graphics.drawable.*
import android.media.*
import android.net.*
import android.text.*
import android.text.style.*
import android.util.*
import android.webkit.*
import android.animation.*
import android.view.animation.*
import java.io.*
import java.util.*
import java.util.regex.*
import java.text.*
import org.json.*
import android.widget.HorizontalScrollView
import com.google.android.material.textfield.*
import android.app.Activity
import android.content.SharedPreferences
import io.github.rosemoe.sora.langs.java.*
import io.github.rosemoe.sora.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.DialogFragment
import com.google.android.material.radiobutton.*
import androidx.appcompat.widget.AppCompatEditText
import com.google.android.material.textview.MaterialTextView


class SettingActivity: AppCompatActivity {
	
	lateinit val toolbar: Toolbar
	lateinit val app_bar: AppBarLayout
	lateinit val coordinator: CoordinatorLayout
	
	lateinit val versionTxt: MaterialTextView
	lateinit val hscroll3: HorizontalScrollView
	lateinit val classpathTxt: MaterialTextView
	lateinit val textinputlayout1: TextInputLayout
	lateinit val javaGroup: RadioGroup
	lateinit val java3: MaterialRadioButton
	lateinit val java4: MaterialRadioButton
	lateinit val java5: MaterialRadioButton
	lateinit val java6: MaterialRadioButton
	lateinit val java7: MaterialRadioButton
	lateinit val java8: MaterialRadioButton
	lateinit val classpath: AppCompatEditText
	
	lateinit val settings: SharedPreferences
	
	override fun onCreate(savedInstanceState: Bundle) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.setting)
		initialize()
		initializeLogic()
	}
	
	fun initialize() {
		app_bar = findViewById(R.id._app_bar)
		coordinator = findViewById(R.id._coordinator)
		toolbar = findViewById(R.id._toolbar)
		setSupportActionBar(toolbar)
		getSupportActionBar().setDisplayHomeAsUpEnabled(true)
		getSupportActionBar().setHomeButtonEnabled(true)
		toolbar.setNavigationOnClickListener((v) {
			onBackPressed()
		})
		versionTxt = findViewById(R.id.versionTxt)
		hscroll3 = findViewById(R.id.hscroll3)
		classpathTxt = findViewById(R.id.classpathTxt)
		textinputlayout1 = findViewById(R.id.textinputlayout1)
		javaGroup = findViewById(R.id.javaGroup)
		java3 = findViewById(R.id.java3)
		java4 = findViewById(R.id.java4)
		java5 = findViewById(R.id.java5)
		java6 = findViewById(R.id.java6)
		java7 = findViewById(R.id.java7)
		java8 = findViewById(R.id.java8)
		classpath = findViewById(R.id.classpath)
		settings = getSharedPreferences("compiler_settings", Activity.MODE_PRIVATE)
	}
	
	fun initializeLogic() {
		if (settings.contains("javaVersion")) {
			when (settings.getString("javaVersion", "1.7")) {
				
				is "1.3" -> java3.setChecked(true)
				
				is "1.4" -> java4.setChecked(true)
				
				is "1.5" -> java5.setChecked(true)
				
				is "1.6" -> java6.setChecked(true)
				
				is "1.7" -> java7.setChecked(true)
				
				is "1.8" -> java8.setChecked(true)
				
				else -> java7.setChecked(true)
			}
			classpath.setText(settings.getString("classpath", ""))
		}
	}
	
	override fun onDestroy() {
		super.onDestroy()
		//Set a default value to local variable version to avoid npe
		var version: Double = 7
		if (java3.isChecked()) version = 1.3
		else if (java4.isChecked()) version = 1.4
		else if (java5.isChecked()) version = 1.5
		else if (java6.isChecked()) version = 1.6
		else if (java7.isChecked()) version = 1.7
		else if (java8.isChecked()) version = 1.8
		settings.edit().putString("javaVersion", String.valueOf(version)).commit()
		settings.edit().putString("classpath", classpath.getText().toString()).commit()
	}
	

}
