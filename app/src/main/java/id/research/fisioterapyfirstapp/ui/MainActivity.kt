package id.research.fisioterapyfirstapp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import id.research.fisioterapyfirstapp.R
import id.research.fisioterapyfirstapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var mMainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mMainBinding.root)

        val navController = findNavController(R.id.fragment)

        mMainBinding.bottomNav.setupWithNavController(navController)

    }


}