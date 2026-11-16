package com.kayo

import com.google.gson.Gson
import com.kayo.helper.FindPerformerResponse
import com.kayo.helper.FindPerformersResponse
import com.kayo.helper.FindSceneResponse
import com.kayo.helper.FindScenesResponse
import com.kayo.helper.PerformerDetails
import com.kayo.helper.SceneFull
import com.kayo.helper.SceneItem
import com.kayo.helper.findPerformer
import com.kayo.helper.findSceneById
import com.kayo.helper.getAllScenes
import com.kayo.helper.getFavAtScenes
import com.kayo.helper.getJavAtScenes
import com.kayo.helper.getMostViews
import com.kayo.helper.getPerformerById
import com.kayo.helper.getPerformerScenes
import com.kayo.helper.getRanAtScenes
import com.kayo.helper.getUpdatedAtScenes
import com.kayo.helper.updateViewCount
import com.lagradost.cloudstream3.Actor
import com.lagradost.cloudstream3.ActorData
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchQuality
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SearchResponseList
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.VPNStatus
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.addDate
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newSearchResponseList
import com.lagradost.cloudstream3.newSubtitleFile
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class Stash : MainAPI() {

    override var mainUrl = "http://192.168.1.6:30198"
    override var name = "Stash"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val supportedTypes = setOf(TvType.NSFW)

    // Put your existing Stash API key here. Do not commit a real key to a public repository.
    private val apiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOiJrYXlvIiwic3ViIjoiQVBJS2V5IiwiaWF0IjoxNzY0MDcwNjcwfQ.LkVoGtPjLOLiPNcR44WVwI7V8k105VNIWikxFWilRPg"

    private val gson = Gson()
    private val okHttp = OkHttpClient()
    private val pageSize = 40

    override val mainPage = mainPageOf(
        "latest" to "Latest",
        "most_views" to "Most Views",
        "random" to "Random",
        "updated_at" to "Updated At",
        "favourite" to "Favourite",
        "jav" to "Jav",
        "performers" to "Performers"
    )

    // -------------------------------------------------------------------------
    // Shared URL and response helpers
    // -------------------------------------------------------------------------

    private fun addApiKey(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.contains("apikey=")) return url

        val separator = if ('?' in url) "&" else "?"
        return "$url${separator}apikey=$apiKey"
    }

    private fun sceneQuality(height: Int?): SearchQuality {
        return when (height ?: 0) {
            in 2160..5000 -> SearchQuality.FourK
            in 1440..2159 -> SearchQuality.HQ
            in 1080..1439 -> SearchQuality.HD
            in 720..1079 -> SearchQuality.WebRip
            in 480..719 -> SearchQuality.DVD
            in 360..479 -> SearchQuality.SD
            else -> SearchQuality.SD
        }
    }

    private fun sceneSearchItem(scene: SceneItem): SearchResponse? {
        val sceneId = scene.id ?: return null
        val studio = scene.studio?.name?.takeIf { it.isNotBlank() }
        val actors = scene.performers
            ?.mapNotNull { it.name?.takeIf(String::isNotBlank) }
            ?.joinToString(", ")
            .orEmpty()

        val titleParts = buildList {
            studio?.let { add("[$it]") }
            actors.takeIf { it.isNotBlank() }?.let(::add)
            add(scene.title?.takeIf { it.isNotBlank() } ?: "Untitled")
        }

        val bestFile = scene.files?.maxByOrNull { it.height ?: 0 }

        return newMovieSearchResponse(
            titleParts.joinToString(" - "),
            sceneId,
            TvType.NSFW,
            fix = false
        ) {
            posterUrl = addApiKey(scene.paths?.screenshot)
            quality = sceneQuality(bestFile?.height)
        }
    }

    /**
     * One shared mapper is used by both the Performers homepage and `p=...` search.
     * Performer cards open the same route, which load() recognises as a performer page.
     */
    private fun performerSearchItem(performer: PerformerDetails): SearchResponse? {
        val performerId = performer.id ?: return null
        val performerName = performer.name?.takeIf { it.isNotBlank() } ?: "Unknown"
        val performerUrl = "${mainUrl.trimEnd('/')}/performers/$performerId/scenes?sortby=date"

        return newTvSeriesSearchResponse(
            performerName,
            performerUrl,
            TvType.NSFW,
            fix = false
        ) {
            posterUrl = addApiKey(performer.image_path)
            episodes = performer.scene_count
        }
    }

    private data class SearchPage(
        val items: List<SearchResponse>,
        val hasNext: Boolean
    )

    /** Shared request + DTO parsing + mapping for homepage and p= performer search. */
    private fun getPerformerSearchPage(
        page: Int,
        query: String,
        sort: String
    ): SearchPage? {
        val response = stashGraphQL(findPerformer(page, query, sort))
        val result = gson.fromJson(response, FindPerformersResponse::class.java)
            ?.data
            ?.findPerformers
            ?: return null

        val items = result.performers.orEmpty().mapNotNull(::performerSearchItem)
        val totalCount = result.count ?: 0
        return SearchPage(items, hasNext = page * pageSize < totalCount)
    }

    // -------------------------------------------------------------------------
    // Homepage
    // -------------------------------------------------------------------------

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        // The Performers row uses exactly the same search implementation as p=... .
        if (request.name.equals("Performers", ignoreCase = true)) {
            val result = getPerformerSearchPage(
                page = page,
                query = "",
                sort = "random"
            ) ?: return newHomePageResponse(request, emptyList(), false)

            return newHomePageResponse(request, result.items, result.hasNext)
        }

        val requestBody = when (request.name) {
            "Latest" -> getAllScenes(page)
            "Updated At" -> getUpdatedAtScenes(page)
            "Favourite" -> getFavAtScenes(page)
            "Random" -> getRanAtScenes(page)
            "Jav" -> getJavAtScenes(page)
            "Most Views" -> getMostViews(page)
            else -> getAllScenes(page)
        }

        val response = stashGraphQL(requestBody)
        val result = gson.fromJson(response, FindScenesResponse::class.java)
            ?.data
            ?.findScenes
            ?: return newHomePageResponse(request, emptyList(), false)

        val items = result.scenes.orEmpty().mapNotNull(::sceneSearchItem)
        val totalCount = result.count ?: 0
        val hasNext = page * pageSize < totalCount

        return newHomePageResponse(request, items, hasNext)
    }

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    override suspend fun search(query: String, page: Int): SearchResponseList? {
        if (query.startsWith("p=", ignoreCase = true)) {
            val performerQuery = query.substringAfter('=', "").trim()
            if (performerQuery.isBlank()) return null

            // Same shared code path as the Performers homepage, but alphabetically sorted.
            val result = getPerformerSearchPage(
                page = page,
                query = performerQuery,
                sort = "name"
            ) ?: return null

            return newSearchResponseList(result.items, result.hasNext)
        }
        val newQuery = if(query=="latest"){""}else(query)
        val response = stashGraphQL(getAllScenes(page, newQuery))
        val result = gson.fromJson(response, FindScenesResponse::class.java)
            ?.data
            ?.findScenes
            ?: return null

        val items = result.scenes.orEmpty().mapNotNull(::sceneSearchItem)
        val totalCount = result.count ?: 0
        val hasNext = page * pageSize < totalCount

        return newSearchResponseList(items, hasNext)
    }

    // -------------------------------------------------------------------------
    // Load router: scene detail or performer profile
    // -------------------------------------------------------------------------

    override suspend fun load(url: String): LoadResponse? {
        val performerId = Regex(
            """/performers/(\d+)/scenes(?:[/?#]|$)"""
        ).find(url)?.groupValues?.getOrNull(1)

        if (performerId != null) {
            return loadPerformer(performerId, url)
        }

        val sceneId = url.substringBefore('?')
            .trimEnd('/')
            .substringAfterLast('/')
            .toIntOrNull()
            ?: url.toIntOrNull()
            ?: return null

        return loadScene(sceneId, url)
    }

    private suspend fun loadScene(sceneId: Int, url: String): LoadResponse? {
        val response = stashGraphQL(findSceneById(sceneId))
        val scene = gson.fromJson(response, FindSceneResponse::class.java)
            ?.data
            ?.findScene
            ?: return null

        val studioName = scene.studio?.name?.takeIf { it.isNotBlank() }
        val actorNames = scene.performers
            ?.mapNotNull { it.name?.takeIf(String::isNotBlank) }
            ?.joinToString(", ")
            .orEmpty()

        val title = buildList {
            studioName?.let { add("[$it]") }
            actorNames.takeIf { it.isNotBlank() }?.let(::add)
            add(scene.title?.takeIf { it.isNotBlank() } ?: "Untitled")
        }.joinToString(" - ")

        val actors = scene.performers.orEmpty().map { performer ->
            ActorData(
                Actor(
                    performer.name ?: "Unknown",
                    addApiKey(performer.image_path)
                )
            )
        }

        val bestFile = scene.files?.maxByOrNull { it.height ?: 0 }
        val poster = addApiKey(scene.paths?.screenshot)
        val preview = addApiKey(scene.paths?.preview)

        return newMovieLoadResponse(
            title,
            url,
            TvType.NSFW,
            sceneId.toString()
        ) {
            posterUrl = poster
            plot = scene.details
            tags = scene.tags?.mapNotNull { it.name }
            this.actors = actors
            duration = bestFile?.duration?.div(60.0)?.toInt()
            year = scene.date?.take(4)?.toIntOrNull()

            preview?.let { addTrailer(it, addRaw = true) }
        }
    }

    // -------------------------------------------------------------------------
    // Performer profile: POST performer details, then POST its scene list
    // -------------------------------------------------------------------------

    private suspend fun loadPerformer(
        performerId: String,
        url: String
    ): LoadResponse? {
        // First POST: complete performer profile.
        val performerResponse = stashGraphQL(getPerformerById(performerId))
        val performer = gson.fromJson(
            performerResponse,
            FindPerformerResponse::class.java
        )?.data?.findPerformer ?: return null

        // Second POST: first page of scenes belonging to this performer.
        val firstScenesResponse = stashGraphQL(
            getPerformerScenes(page = 1, performerId = performerId)
        )
        val firstResult = gson.fromJson(
            firstScenesResponse,
            FindScenesResponse::class.java
        )?.data?.findScenes ?: return null

        val totalCount = firstResult.count ?: 0
        val allScenes = firstResult.scenes.orEmpty().toMutableList()
        val seenIds = allScenes.mapNotNull { it.id }.toMutableSet()

        // Load additional pages when the performer has more than 40 scenes.
        var page = 2
        while (allScenes.size < totalCount) {
            val pageResponse = stashGraphQL(
                getPerformerScenes(page = page, performerId = performerId)
            )
            val pageResult = gson.fromJson(
                pageResponse,
                FindScenesResponse::class.java
            )?.data?.findScenes ?: break

            val nextScenes = pageResult.scenes.orEmpty()
            if (nextScenes.isEmpty()) break

            val newScenes = nextScenes.filter { scene ->
                scene.id == null || seenIds.add(scene.id)
            }
            if (newScenes.isEmpty()) break

            allScenes.addAll(newScenes)
            page++
        }

        val scenes = allScenes.distinctBy { it.id }

        // Every episode sends only the scene ID to loadLinks().
        val episodes = scenes.mapIndexedNotNull { index, scene ->
            val sceneId = scene.id ?: return@mapIndexedNotNull null
            val bestFile = scene.files?.maxByOrNull { it.height ?: 0 }

            val episodeDescription = buildList {
                scene.details
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::add)

                scene.studio?.name
                    ?.takeIf { it.isNotBlank() }
                    ?.let { add("Studio: $it") }

                scene.director
                    ?.takeIf { it.isNotBlank() }
                    ?.let { add("Director: $it") }

                val cast = scene.performers
                    ?.mapNotNull { it.name?.takeIf(String::isNotBlank) }
                    ?.takeIf { it.isNotEmpty() }
                cast?.let { add("Performers: ${it.joinToString(", ")}") }

                val videoInfo = buildList {
                    bestFile?.height?.let { add("Resolution: ${it}p") }
                    bestFile?.video_codec?.let { add("Video: $it") }
                    bestFile?.audio_codec?.let { add("Audio: $it") }
                    bestFile?.size?.let { size ->
                        add("File size: ${size / (1024.0 * 1024.0 * 1024.0)} GB")
                    }
                }
                if (videoInfo.isNotEmpty()) add(videoInfo.joinToString(" • "))

                scene.tags
                    ?.mapNotNull { it.name?.takeIf(String::isNotBlank) }
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { add("Tags: ${it.joinToString(", ")}") }
            }.joinToString("\n\n")

            newEpisode(sceneId) {
                name = scene.title?.takeIf { it.isNotBlank() } ?: "Untitled"
                season = 1
                episode = index + 1
                posterUrl = addApiKey(scene.paths?.screenshot)
                description = episodeDescription.takeIf { it.isNotBlank() }
                addDate(scene.date)
                runTime = bestFile?.duration?.toInt()
            }
        }

        val performerPoster = addApiKey(performer.image_path)

        val profileDescription = buildList {
            performer.details
                ?.takeIf { it.isNotBlank() }
                ?.let(::add)

            performer.gender?.let { add("Gender: $it") }
            performer.birthdate?.let { add("Birthdate: $it") }
            performer.country?.let { add("Country: $it") }
            performer.ethnicity?.let { add("Ethnicity: $it") }
            performer.height_cm?.let { add("Height: $it cm") }
            performer.measurements?.let { add("Measurements: $it") }
            performer.eye_color?.let { add("Eye color: $it") }
            performer.hair_color?.let { add("Hair color: $it") }
            performer.weight?.let { add("Weight: $it") }
            performer.career_start?.let { add("Career start: $it") }
            performer.career_end?.let { add("Career end: $it") }
            performer.death_date?.let { add("Death date: $it") }
            performer.tattoos?.takeIf { it.isNotBlank() }?.let { add("Tattoos: $it") }
            performer.piercings?.takeIf { it.isNotBlank() }?.let { add("Piercings: $it") }
            performer.fake_tits?.let { add("Breast type: $it") }
            performer.scene_count?.let { add("Scenes: $it") }
            performer.image_count?.let { add("Images: $it") }
            performer.gallery_count?.let { add("Galleries: $it") }
            performer.group_count?.let { add("Groups: $it") }
            performer.performer_count?.let { add("Associated performers: $it") }
            performer.rating100?.let { add("Rating: $it/100") }
            if (performer.favorite == true) add("Favourite: Yes")

            performer.alias_list
                ?.takeIf { it.isNotEmpty() }
                ?.let { add("Aliases: ${it.joinToString(", ")}") }

            performer.urls
                ?.takeIf { it.isNotEmpty() }
                ?.let { add("External profiles:\n${it.joinToString("\n")}") }
        }.joinToString("\n")

        val firstScreenshot = scenes.firstNotNullOfOrNull {
            addApiKey(it.paths?.screenshot)
        }
        val firstPreview = scenes.firstNotNullOfOrNull {
            addApiKey(it.paths?.preview)
        }

        return newTvSeriesLoadResponse(
            performer.name ?: "Unknown",
            url,
            TvType.NSFW,
            episodes
        ) {
            posterUrl = performerPoster
            backgroundPosterUrl = firstScreenshot
            plot = profileDescription.takeIf { it.isNotBlank() }
            tags = performer.tags?.mapNotNull { it.name }
            actors = listOfNotNull(
                performer.name?.let { ActorData(Actor(it, performerPoster)) }
            )

            // Preview is a server preview endpoint. CloudStream can display raw trailers,
            // but playback still depends on this endpoint returning a playable video.
            firstPreview?.let { addTrailer(it, addRaw = true) }
        }
    }

    // -------------------------------------------------------------------------
    // Link loading
    // -------------------------------------------------------------------------

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val sceneId = data.substringBefore('?')
            .trimEnd('/')
            .substringAfterLast('/')
            .toIntOrNull()
            ?: data.toIntOrNull()
            ?: return false

        runCatching { stashGraphQL(updateViewCount(sceneId)) }

        val response = stashGraphQL(findSceneById(sceneId))
        val scene = gson.fromJson(response, FindSceneResponse::class.java)
            ?.data
            ?.findScene
            ?: return false

        val captionUrl = scene.paths?.caption
        if (!captionUrl.isNullOrBlank()) {
            scene.captions.orEmpty().forEach { caption ->
                val baseUrl = addApiKey(captionUrl) ?: return@forEach
                val separator = if ('?' in baseUrl) "&" else "?"
                val subtitleUrl = buildString {
                    append(baseUrl)
                    append(separator)
                    append("lang=${caption.language_code}")
                    append("&type=${caption.caption_type}")
                }
                subtitleCallback(newSubtitleFile(caption.language_code, subtitleUrl))
            }
        }

        val bestHeight = scene.files?.maxOfOrNull { it.height ?: 0 } ?: 0
        var foundLink = false

        // Stash-generated stream URLs.
        scene.sceneStreams.orEmpty().forEach { stream ->
            val streamUrl = addApiKey(stream.url) ?: return@forEach
            val available = runCatching {
                val request = Request.Builder()
                    .url(streamUrl)
                    .get()
                    .header("Range", "bytes=0-0")
                    .build()

                okHttp.newCall(request).execute().use { it.isSuccessful }
            }.getOrDefault(false)

            if (available) {
                callback(
                    newExtractorLink(
                        source = name,
                        name = stream.label ?: "Stash stream",
                        url = streamUrl,
                        type = if (streamUrl.contains(".m3u8", ignoreCase = true)) {
                            ExtractorLinkType.M3U8
                        } else {
                            ExtractorLinkType.VIDEO
                        }
                    ) {
                        quality = bestHeight
                    }
                )
                foundLink = true
            }
        }

        // Fallback to the standard Stash scene stream path when sceneStreams is empty
        // or none of the generated stream URLs responded successfully.
        if (!foundLink) {
            val fallbackStream = addApiKey(scene.paths?.stream)
            if (!fallbackStream.isNullOrBlank()) {
                val available = runCatching {
                    val request = Request.Builder()
                        .url(fallbackStream)
                        .get()
                        .header("Range", "bytes=0-0")
                        .build()
                    okHttp.newCall(request).execute().use { it.isSuccessful }
                }.getOrDefault(false)

                if (available) {
                    callback(
                        newExtractorLink(
                            source = name,
                            name = "Stash stream",
                            url = fallbackStream,
                            type = if (fallbackStream.contains(".m3u8", ignoreCase = true)) {
                                ExtractorLinkType.M3U8
                            } else {
                                ExtractorLinkType.VIDEO
                            }
                        ) {
                            quality = bestHeight
                        }
                    )
                    foundLink = true
                }
            }
        }

        // External scene URLs, including explicitly tagged direct video/HLS links.
        scene.urls.orEmpty().forEach { externalUrl ->
            when {
                externalUrl.startsWith("m3u8-") -> {
                    callback(
                        newExtractorLink(
                            source = name,
                            name = "Direct HLS",
                            url = externalUrl.removePrefix("m3u8-"),
                            type = ExtractorLinkType.M3U8
                        ) {
                            quality = bestHeight
                        }
                    )
                    foundLink = true
                }

                externalUrl.startsWith("video-") -> {
                    callback(
                        newExtractorLink(
                            source = name,
                            name = "Direct Video",
                            url = externalUrl.removePrefix("video-"),
                            type = ExtractorLinkType.VIDEO
                        ) {
                            quality = bestHeight
                        }
                    )
                    foundLink = true
                }

                externalUrl.startsWith("http://", ignoreCase = true) ||
                        externalUrl.startsWith("https://", ignoreCase = true) -> {
                    loadExtractor(
                        externalUrl,
                        referer = mainUrl,
                        subtitleCallback = subtitleCallback,
                        callback = { link ->
                            foundLink = true
                            callback(link)
                        }
                    )
                }
            }
        }

        return foundLink
    }

    // -------------------------------------------------------------------------
    // GraphQL POST
    // -------------------------------------------------------------------------

    private fun stashGraphQL(bodyJson: String): String {
        val request = Request.Builder()
            .url("${mainUrl.trimEnd('/')}/graphql")
            .addHeader("ApiKey", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        return okHttp.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Stash GraphQL request failed: HTTP ${response.code}: $body")
            }
            body
        }
    }
}
