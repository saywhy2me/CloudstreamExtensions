package com.example.plex

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.net.URLEncoder

/**
 * Plays media straight from the user's own Plex Media Server.
 *
 * Nothing is scraped from the public internet: every request goes to the
 * server address the user typed into this extension's settings, authenticated
 * with their personal X-Plex-Token. See PlexSettingsFragment for where those
 * two values are entered and stored.
 */
class PlexProvider : MainAPI() {
    override var name = "Plex"

    // We build fully-qualified URLs ourselves, so mainUrl is only a sensible
    // default/label. It is refreshed from settings whenever we have one.
    override var mainUrl = server() ?: "http://127.0.0.1:32400"

    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "en"
    override val hasMainPage = true

    // A single home entry; getMainPage returns one row per Plex library inside it.
    override val mainPage = mainPageOf("home" to "Library")

    companion object {
        const val KEY_SERVER = "plex_server_url"
        const val KEY_TOKEN = "plex_token"

        /** Saved server address, e.g. "http://10.0.0.50:32400", or null if unset. */
        fun server(): String? =
            CloudStreamApp.getKey<String>(KEY_SERVER)?.trim()?.trimEnd('/')?.ifBlank { null }

        /** Saved X-Plex-Token, or null if unset. */
        fun token(): String? =
            CloudStreamApp.getKey<String>(KEY_TOKEN)?.trim()?.ifBlank { null }
    }

    // ---- small helpers -----------------------------------------------------

    private fun headers(tok: String) = mapOf(
        "Accept" to "application/json",
        "X-Plex-Token" to tok
    )

    /** Turn a server-relative path into an absolute, token-carrying URL.
     *  The token goes in the query string so the image loader / player
     *  (which don't send our Accept/token headers) still authenticate. */
    private fun abs(path: String, srv: String, tok: String): String {
        val sep = if (path.contains("?")) "&" else "?"
        return "$srv$path${sep}X-Plex-Token=$tok"
    }

    private fun resToQuality(res: String?): Int = when (res?.lowercase()) {
        "4k", "2160" -> Qualities.P2160.value
        "1080" -> Qualities.P1080.value
        "720" -> Qualities.P720.value
        "480", "sd" -> Qualities.P480.value
        else -> Qualities.Unknown.value
    }

    /** GET a Plex JSON endpoint and return its MediaContainer, or null. */
    private suspend fun fetch(path: String): PlexMediaContainer? {
        val srv = server() ?: return null
        val tok = token() ?: return null
        val res = app.get("$srv$path", headers = headers(tok))
        return tryParseJson<PlexContainerRoot>(res.text)?.mediaContainer
    }

    private fun PlexMetadata.toSearchResponse(srv: String, tok: String): SearchResponse? {
        val rk = ratingKey ?: return null
        val url = "/library/metadata/$rk"
        val poster = thumb?.let { abs(it, srv, tok) }
        return when (type) {
            "show" -> newTvSeriesSearchResponse(title ?: "Untitled", url, TvType.TvSeries, fix = false) {
                this.posterUrl = poster
            }
            // movies (and anything else we surface) are treated as a single playable item
            else -> newMovieSearchResponse(title ?: "Untitled", url, TvType.Movie, fix = false) {
                this.posterUrl = poster
            }
        }
    }

    // ---- MainAPI overrides --------------------------------------------------

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val srv = server() ?: throw ErrorLoadingException(
            "Open this extension's settings (gear icon) and enter your Plex server URL and token."
        )
        val tok = token() ?: throw ErrorLoadingException(
            "Open this extension's settings (gear icon) and enter your Plex token."
        )

        val sections = fetch("/library/sections")?.directory.orEmpty()
            .filter { it.type == "movie" || it.type == "show" }
        if (sections.isEmpty()) throw ErrorLoadingException(
            "No Movie or TV libraries were returned. Double-check the server URL and token."
        )

        val lists = sections.mapNotNull { dir ->
            val key = dir.key ?: return@mapNotNull null
            // Recently added first, capped so the home screen loads quickly.
            val items = fetch(
                "/library/sections/$key/all?sort=addedAt:desc&X-Plex-Container-Start=0&X-Plex-Container-Size=50"
            )?.metadata.orEmpty().mapNotNull { it.toSearchResponse(srv, tok) }
            if (items.isEmpty()) null else HomePageList(dir.title ?: "Library", items)
        }
        return newHomePageResponse(lists, hasNext = false)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val srv = server() ?: return emptyList()
        val tok = token() ?: return emptyList()
        val encoded = URLEncoder.encode(query, "UTF-8")
        return fetch("/search?query=$encoded")?.metadata.orEmpty()
            .filter { it.type == "movie" || it.type == "show" }
            .mapNotNull { it.toSearchResponse(srv, tok) }
    }

    override suspend fun load(url: String): LoadResponse? {
        val srv = server() ?: throw ErrorLoadingException("Set your Plex server in settings.")
        val tok = token() ?: throw ErrorLoadingException("Set your Plex token in settings.")

        val meta = fetch(url)?.metadata?.firstOrNull() ?: return null
        val rk = url.substringAfterLast("/")
        val poster = meta.thumb?.let { abs(it, srv, tok) }
        val background = meta.art?.let { abs(it, srv, tok) }

        return if (meta.type == "show") {
            // allLeaves returns every episode of the show in one request.
            val episodes = fetch("/library/metadata/$rk/allLeaves")?.metadata.orEmpty()
                .mapNotNull { ep ->
                    val erk = ep.ratingKey ?: return@mapNotNull null
                    newEpisode(
                        url = erk, // becomes `data` passed to loadLinks
                        initializer = {
                            this.name = ep.title
                            this.season = ep.parentIndex
                            this.episode = ep.index
                            this.posterUrl = ep.thumb?.let { abs(it, srv, tok) }
                            this.description = ep.summary
                        },
                        fix = false
                    )
                }
            newTvSeriesLoadResponse(meta.title ?: "Untitled", url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.backgroundPosterUrl = background
                this.plot = meta.summary
                this.year = meta.year
            }
        } else {
            newMovieLoadResponse(meta.title ?: "Untitled", url, TvType.Movie, dataUrl = rk) {
                this.posterUrl = poster
                this.backgroundPosterUrl = background
                this.plot = meta.summary
                this.year = meta.year
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val srv = server() ?: return false
        val tok = token() ?: return false

        // `data` is a Plex ratingKey (movie or episode).
        val meta = fetch("/library/metadata/$data")?.metadata?.firstOrNull() ?: return false
        val media = meta.media?.firstOrNull() ?: return false
        val partKey = media.part?.firstOrNull()?.key ?: return false

        callback(
            newExtractorLink(
                source = name,
                name = "$name Direct Play",
                url = abs(partKey, srv, tok),
                type = ExtractorLinkType.VIDEO
            ) {
                this.quality = resToQuality(media.videoResolution)
            }
        )
        return true
    }
}

// ---- Plex JSON models (only the fields we use) ------------------------------

@JsonIgnoreProperties(ignoreUnknown = true)
data class PlexContainerRoot(
    @JsonProperty("MediaContainer") val mediaContainer: PlexMediaContainer?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PlexMediaContainer(
    @JsonProperty("Directory") val directory: List<PlexDirectory>? = null,
    @JsonProperty("Metadata") val metadata: List<PlexMetadata>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PlexDirectory(
    val key: String? = null,
    val title: String? = null,
    val type: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PlexMetadata(
    val ratingKey: String? = null,
    val key: String? = null,
    val type: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val year: Int? = null,
    val thumb: String? = null,
    val art: String? = null,
    val index: Int? = null,
    val parentIndex: Int? = null,
    val grandparentTitle: String? = null,
    @JsonProperty("Media") val media: List<PlexMediaInfo>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PlexMediaInfo(
    val videoResolution: String? = null,
    @JsonProperty("Part") val part: List<PlexPart>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PlexPart(
    val key: String? = null,
    val file: String? = null,
    val container: String? = null
)
