package com.pranav.java.ide

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class DebugActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val intent: Intent? = getIntent()
		val errorMessage = intent?.getStringExtra("error")!!

		AlertDialog.Builder(this)
				.setTitle("An error occurred")
				.setMessage(errorMessage)
				.setPositiveButton("Quit", {_, _ -> finish()})
				.create()
				.show()
	}
}
