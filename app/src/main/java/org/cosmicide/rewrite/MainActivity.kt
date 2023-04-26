package org.cosmicide.rewrite

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.cosmicide.rewrite.databinding.ActivityMainBinding
import org.cosmicide.rewrite.fragment.ProjectFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (supportFragmentManager.findFragmentByTag("ProjectFragment") == null) {
            supportFragmentManager.beginTransaction().apply {
                add(binding.fragmentContainer.id, ProjectFragment(), "ProjectFragment")
                setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            }.commit()
        }
    }
}

