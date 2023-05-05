package org.cosmicide.rewrite

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentTransaction
import org.cosmicide.rewrite.databinding.ActivityMainBinding
import org.cosmicide.rewrite.fragment.ProjectFragment

class MainActivity : AppCompatActivity() {

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        splashScreen.setSplashScreenTheme(R.style.Theme_CosmicIDERewrite)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (supportFragmentManager.findFragmentByTag("ProjectFragment") == null) {
            supportFragmentManager.beginTransaction().apply {
                add(binding.fragmentContainer.id, ProjectFragment(), "ProjectFragment")
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            }.commit()
        }
    }
}

