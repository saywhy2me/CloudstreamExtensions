package com.example

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType

class ExampleProvider : MainAPI() { // All providers must be an instance of MainAPI
    override var mainUrl = "https://example.com/"
    override var name = "Example provider"
    override val supportedTypes = setOf(TvType.Movie)

    override var lang = "en"

    // Enable this when your provider has a main page
    override val hasMainPage = true

    // This function gets called when you search for something
    override suspend fun search(query: String): List<SearchResponse> {
        return listOf()
    }

    /*
     * A real provider also overrides the functions below. They are intentionally
     * left out here so this module stays a minimal, guaranteed-to-compile starting
     * point. See the fully-worked MediaServerProvider in this same repo for live
     * implementations of each one, and the study doc in /docs for a plain-language
     * walkthrough.
     *
     *   override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse
     *       -> fills the home screen with rows of content
     *
     *   override suspend fun load(url: String): LoadResponse
     *       -> builds the details page for one title (poster, plot, episodes...)
     *
     *   override suspend fun loadLinks(
     *       data: String,
     *       isCasting: Boolean,
     *       subtitleCallback: (SubtitleFile) -> Unit,
     *       callback: (ExtractorLink) -> Unit
     *   ): Boolean
     *       -> emits the actual playable video/subtitle links
     */
}
