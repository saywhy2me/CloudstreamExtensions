package com.example.plex

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lagradost.cloudstream3.CloudStreamApp

/**
 * Bottom-sheet settings screen for the Plex provider.
 *
 * The user types their server address and personal token here; both are saved
 * with CloudStreamApp.setKey so PlexProvider can read them on every request.
 * Layout/views are looked up by name because a plugin's resources are not part
 * of the host app's generated R class.
 */
class PlexSettingsFragment(private val plugin: PlexPlugin) : BottomSheetDialogFragment() {

    @SuppressLint("DiscouragedApi")
    private fun <T : View> View.findByName(name: String): T? {
        val id = plugin.resources?.getIdentifier(name, "id", BuildConfig.LIBRARY_PACKAGE_NAME)
        return findViewById(id ?: return null)
    }

    @SuppressLint("DiscouragedApi")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutId = plugin.resources?.getIdentifier(
            "fragment_plex_settings", "layout", BuildConfig.LIBRARY_PACKAGE_NAME
        ) ?: return null
        return inflater.inflate(plugin.resources?.getLayout(layoutId), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val serverField: EditText? = view.findByName("plexServerUrl")
        val tokenField: EditText? = view.findByName("plexToken")
        val saveButton: Button? = view.findByName("plexSave")

        // Pre-fill with whatever is already saved.
        serverField?.setText(CloudStreamApp.getKey<String>(PlexProvider.KEY_SERVER) ?: "")
        tokenField?.setText(CloudStreamApp.getKey<String>(PlexProvider.KEY_TOKEN) ?: "")

        saveButton?.setOnClickListener {
            val server = serverField?.text?.toString()?.trim()?.trimEnd('/').orEmpty()
            val token = tokenField?.text?.toString()?.trim().orEmpty()

            if (server.isBlank() || token.isBlank()) {
                Toast.makeText(view.context, "Both server URL and token are required", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (!server.startsWith("http://") && !server.startsWith("https://")) {
                Toast.makeText(view.context, "Server URL must start with http:// or https://", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            CloudStreamApp.setKey(PlexProvider.KEY_SERVER, server)
            CloudStreamApp.setKey(PlexProvider.KEY_TOKEN, token)
            Toast.makeText(view.context, "Plex settings saved", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }
}
