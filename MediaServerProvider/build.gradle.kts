dependencies {
    implementation("com.google.android.material:material:1.12.0")
}

// Use an integer for version numbers
version = 1

cloudstream {
    description = "Stream from your own Plex Media Server. Set your server URL and token in the extension settings."
    authors = listOf("Jordan")

    /**
     * Status int as one of the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta-only
     **/
    status = 1

    // Your Plex library holds whatever you put in it, so advertise the broad types.
    tvTypes = listOf("Movie", "TvSeries")

    requiresResources = true
    language = "en"

    iconUrl = "https://raw.githubusercontent.com/plexinc/plex-media-player/master/resources/images/icon.png"
}

android {
    // Must match the Kotlin package in this module so BuildConfig/R resolve.
    namespace = "com.example.plex"

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}
