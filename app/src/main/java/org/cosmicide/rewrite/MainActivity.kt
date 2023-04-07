package org.cosmicide.rewrite

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import org.cosmicide.rewrite.databinding.ActivityMainBinding
import org.cosmicide.rewrite.fragment.EditorFragment
import org.cosmicide.rewrite.fragment.ProjectFragment
import org.cosmicide.rewrite.util.ProjectHandler

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction().add(R.id.container, ProjectFragment()).addToBackStack("ProjectFragment").commit()

    }
}