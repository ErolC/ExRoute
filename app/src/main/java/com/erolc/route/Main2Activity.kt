package com.erolc.route

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.erolc.exroute.Extra
import com.erolc.exroute.Route

@Route
class Main2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
    }
}
