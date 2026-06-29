package com.devicespooflab.hooks

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnRandomize = findViewById<Button>(R.id.btnRandomize)
        btnRandomize.setOnClickListener {
            generateAndSaveRandomParams()
        }
    }

    @Suppress("DEPRECATION")
    private fun generateAndSaveRandomParams() {
        // According to Xposed design, SharedPreferences should be created world readable so module can read it.
        // On newer Androids MODE_WORLD_READABLE is deprecated and might throw exception, but LSPosed handles this.
        val prefs = try {
            getSharedPreferences("rsh_params", Context.MODE_WORLD_READABLE)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Failed to save parameters: world-readable storage unavailable.", Toast.LENGTH_SHORT).show()
            return
        }

        val editor = prefs.edit()
        val randomizer = Randomizer()
        val params = randomizer.generateAll()

        for ((key, value) in params) {
            editor.putString(key, value)
        }

        if (editor.commit()) {
            Toast.makeText(this, "Generated 45 random spoof parameters!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to save parameters.", Toast.LENGTH_SHORT).show()
        }
    }
}
