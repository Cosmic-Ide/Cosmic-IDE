package com.pranav.java.ide

import android.content.SharedPreferences
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.Toolbar

import com.google.android.material.radiobutton.MaterialRadioButton

class SettingActivity : AppCompatActivity() {

	private var java3: MaterialRadioButton
	private var java4: MaterialRadioButton
	private var java5: MaterialRadioButton
	private var java6: MaterialRadioButton
	private var java8: MaterialRadioButton
	private var java9: MaterialRadioButton
	private var java10: MaterialRadioButton
	private var java11: MaterialRadioButton
	private var java12: MaterialRadioButton
	private var java13: MaterialRadioButton
	private var java14: MaterialRadioButton
	private var java15: MaterialRadioButton
	private var java16: MaterialRadioButton
	private var java17: MaterialRadioButton

	private lateinit var classpath: AppCompatEditText

	private lateinit var settings: SharedPreferences

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_setting)

		val toolbar: Toolbar = findViewById(R.id.toolbar)

    setSupportActionBar(toolbar)
    getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)
    getSupportActionBar()?.setHomeButtonEnabled(true)
    toolbar.setNavigationOnClickListener { v ->
      onBackPressed()
		}

		java3 = findViewById(R.id.java3)
		java4 = findViewById(R.id.java4)
		java5 = findViewById(R.id.java5)
		java6 = findViewById(R.id.java6)
		java8 = findViewById(R.id.java8)
		java9 = findViewById(R.id.java9)
		java10 = findViewById(R.id.java10)
		java11 = findViewById(R.id.java11)
		java12 = findViewById(R.id.java12)
		java13 = findViewById(R.id.java13)
		java14 = findViewById(R.id.java14)
		java15 = findViewById(R.id.java15)
		java16 = findViewById(R.id.java16)
		java17 = findViewById(R.id.java17)
		
		classpath = findViewById(R.id.classpath)

		val java7 = findViewById(R.id.java7)
		settings = getSharedPreferences("compiler_settings", MODE_PRIVATE)

		when (settings.getString("javaVersion", "7.0")) {

			"1.3" -> java3.setChecked(true)

			"1.4" -> java4.setChecked(true)

			"5.0" -> java5.setChecked(true)

			"6.0" -> java6.setChecked(true)

			"8.0" -> java8.setChecked(true)

			"9.0" -> java9.setChecked(true)

			"10.0" -> java10.setChecked(true)

			"11.0" -> java11.setChecked(true)

			"12.0" -> java12.setChecked(true)

			"13.0" -> java13.setChecked(true)

			"14.0" -> java14.setChecked(true)

			"15.0" -> java15.setChecked(true)

			"16.0" -> java16.setChecked(true)

			"17.0" -> java17.setChecked(true)

			else -> java7.setChecked(true)
		}

		classpath.setText(settings.getString("classpath", ""))
	}

	override fun onDestroy() {
	  super.onDestroy()
		var version = 1.7

		if (java3.isChecked()) version = 1.3
		else if (java4.isChecked()) version = 1.4
		else if (java5.isChecked()) version = 5.0
		else if (java6.isChecked()) version = 6.0
		else if (java8.isChecked()) version = 8.0
		else if (java9.isChecked()) version = 9.0
		else if (java10.isChecked()) version = 10.0
		else if (java11.isChecked()) version = 11.0
		else if (java12.isChecked()) version = 12.0
		else if (java13.isChecked()) version = 13.0
		else if (java14.isChecked()) version = 14.0
		else if (java15.isChecked()) version = 15.0
		else if (java16.isChecked()) version = 16.0
		else if (java17.isChecked()) version = 17.0

		settings.edit().putString("javaVersion", String.valueOf(version)).apply()

		settings.edit().putString("classpath", classpath.getText().toString()).apply()
	}
}
