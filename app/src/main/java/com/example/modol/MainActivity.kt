package com.example.modol

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.view.animation.AnimationUtils
import com.example.modol.databinding.ActivityMainBinding
//private val ActivityMainBinding.btn: Any


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCustom.setOnClickListener {
            val intent = Intent(this, com.example.modol.CustomActivity::class.java)
            intent.putExtra("lantai", 1)
            startActivity(intent)
        }

        binding.btnModul1.setOnClickListener {
            val intent = Intent(this, Modul1Activity::class.java)
            intent.putExtra("lantai", 2)
            startActivity(intent)
        }

        val title = findViewById<TextView>(R.id.tvTitle)
        val animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        title.startAnimation(animation)

    }
}
