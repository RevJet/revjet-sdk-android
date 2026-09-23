package com.revjet.sdk

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout

/** Hosts a tag's web view, which only initializes properly once it is attached to a window. */
class TestHostActivity : Activity() {
    lateinit var container: FrameLayout
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        container = FrameLayout(this)
        setContentView(container)
    }
}
