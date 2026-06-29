package com.devicespooflab.hooks

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnRandomize = findViewById<Button>(R.id.btnRandomize)
        btnRandomize.setOnClickListener {
            generateAndSaveRandomParams()
        }
    }

    private fun generateAndSaveRandomParams() {
        // Write to MODE_PRIVATE first (works in the module's own process), then
        // force the resulting file world-readable so XSharedPreferences on the hook
        // side can read it. MODE_WORLD_READABLE is blocked in the module process
        // even when LSPosed is active, because LSPosed only intercepts getSharedPreferences
        // in the target app's process, not in the module's own process.
        val prefs = getSharedPreferences("rsh_params", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val randomizer = Randomizer()
        val params = randomizer.generateAll()

        for ((key, value) in params) {
            editor.putString(key, value)
        }

        if (editor.commit()) {
            // Make the prefs file world-readable so XSharedPreferences can read it.
            val prefsFile = File(filesDir.parent, "shared_prefs/rsh_params.xml")
            val readable = prefsFile.setReadable(true, false)
            if (!readable) {
                Toast.makeText(this, "Saved, but failed to set world-readable permission.", Toast.LENGTH_SHORT).show()
                return
            }
            Toast.makeText(this, "Generated ${params.size} random spoof parameters!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to save parameters.", Toast.LENGTH_SHORT).show()
        }
    }
}
