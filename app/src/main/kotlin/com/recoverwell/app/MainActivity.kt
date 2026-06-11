package com.recoverwell.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.recoverwell.core.Placeholder

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = Placeholder.APP_NAME
        setContentView(tv)
    }
}
