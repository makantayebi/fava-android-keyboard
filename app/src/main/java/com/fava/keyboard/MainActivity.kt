package com.fava.keyboard

import android.app.Activity
import android.os.Bundle
import android.view.Window
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Not part of the IME itself — just an entry point so the app has
 * something to tap in the launcher, with instructions, and a field to
 * try the keyboard in before switching your default.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE) // default title bar otherwise
        // overlaps the content below it instead of reserving its own space
        super.onCreate(savedInstanceState)
        window.setDecorFitsSystemWindows(true) // targetSdk 35's edge-to-edge
        // default would otherwise let content draw under the status bar too

        val dp = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * dp).toInt(), (24 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())
        }

        val versionName = packageManager.getPackageInfo(packageName, 0).versionName

        root.addView(TextView(this).apply {
            text = "Fava Keyboard v$versionName"
            textSize = 22f
        })

        root.addView(TextView(this).apply {
            text = "\n1. Settings → System → Languages & input → On-screen keyboard → Manage keyboards → enable Fava\n" +
                "2. Tap the text field below, then switch to Fava from the keyboard-switcher icon\n\n" +
                "Try it:"
            textSize = 16f
        })

        root.addView(EditText(this).apply {
            hint = "Type here to test"
            textSize = 20f
        })

        setContentView(root)
    }
}
