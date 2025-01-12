package com.example.handtalk

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.handtalk.classification.ClassifierActivity
import com.example.handtalk.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main)

        handleUI()
    }

    private fun handleUI(){
        binding.start.setOnClickListener {
            startActivity(Intent(this,ClassifierActivity::class.java).apply {

            })
        }

        AppSharedPreferences.init(this)

        if(AppSharedPreferences.getUserMode()){
            binding.devMode.isChecked = true
        }

        binding.help.setOnClickListener {
            val browserIntent =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://ru.wikipedia.org/wiki/Русский_жестовый_язык"))
            startActivity(browserIntent)
        }

        binding.devMode.setOnCheckedChangeListener { compoundButton, isChecked ->
            if(isChecked){
                AppSharedPreferences.setUserMode(true)
            }else{
                AppSharedPreferences.setUserMode(false)
            }
        }
    }
}