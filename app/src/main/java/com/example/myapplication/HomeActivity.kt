package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityHomeBinding
import android.widget.TextView
import android.view.animation.AnimationUtils
class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnModul1.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("lantai", 1)
            startActivity(intent)
        }

        binding.btnModul2.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("lantai", 2)
            startActivity(intent)
        }

        val title = findViewById<TextView>(R.id.tvTitle)
        val animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        title.startAnimation(animation)

    }
}
