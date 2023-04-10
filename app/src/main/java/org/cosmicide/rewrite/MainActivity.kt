package org.cosmicide.rewrite

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import org.cosmicide.rewrite.databinding.ActivityMainBinding
import org.cosmicide.rewrite.fragment.ProjectFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (supportFragmentManager.findFragmentByTag("ProjectFragment") == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProjectFragment(), "ProjectFragment")
                .commit()
        }
    }
}