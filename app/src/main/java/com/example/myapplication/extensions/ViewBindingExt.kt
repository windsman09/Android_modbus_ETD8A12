package com.example.myapplication.extensions

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

fun <T : ViewBinding> AppCompatActivity.viewBinding(
    bindingInflater: (LayoutInflater) -> T
) = lazy { bindingInflater(layoutInflater) }
