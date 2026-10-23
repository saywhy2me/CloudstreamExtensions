package com.example.plex

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class PlexPlugin : Plugin() {
    private var activity: AppCompatActivity? = null

    override fun load(context: Context) {
        activity = context as? AppCompatActivity

        // Register the provider so CloudStream shows it in the provider list.
        registerMainAPI(PlexProvider())

        // Gear icon next to the provider -> opens our settings sheet where the
        // user enters their server URL and token.
        openSettings = {
            val frag = PlexSettingsFragment(this)
            activity?.let {
                frag.show(it.supportFragmentManager, "PlexSettings")
            }
        }
    }
}
