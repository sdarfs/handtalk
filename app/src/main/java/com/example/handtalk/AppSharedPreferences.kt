package com.example.handtalk

import android.content.Context
import android.content.SharedPreferences

object AppSharedPreferences {

    private const val PREFERENCE_NAME = "user_preference"
    private const val IS_DEV = "isDev"
    private const val ACCURACY = "accuracy"
    private lateinit var preference : SharedPreferences

    fun init(context : Context){
        preference = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    fun getUserMode() : Boolean{
        return preference.getBoolean(IS_DEV,false)
    }

    fun setUserMode(devMode : Boolean){
        val editor = preference.edit()
        editor.putBoolean(IS_DEV,devMode)
        editor.apply()
    }

    fun getAccuracy() : Int{
        return preference.getInt(ACCURACY,40)
    }

    fun setAccuracy(accuracy : Int){
        val editor = preference.edit()
        editor.putInt(ACCURACY,accuracy)
        editor.apply()
    }

}