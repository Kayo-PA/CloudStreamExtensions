
package com.kayo

import com.kayo.helper.getSearchUrl
import com.kayo.helper.searchPageUrls
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class XXXClub : MainAPI() {
    override var mainUrl = "https://xxxclub.to"
    override var name = "XXXClub"
    override val hasMainPage = true
    override var lang = "jp"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "trending" to "Trending",
        "$mainUrl/torrents/topten/" to "Top 10",
        "$mainUrl/torrents/top100/" to "Top 100",
        "$mainUrl/torrents/catalog/" to "Catalog",
        "$mainUrl/torrents/browse/2/" to "1080p",
        "$mainUrl/torrents/browse/4/" to "4k",
        "$mainUrl/torrents/browse/3/" to "Movie",
        "$mainUrl/torrents/browse/7/" to "megaPack",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val result: List<SearchResponse> = when (request.name) {
            "Trending" -> {
                val document = app.get(mainUrl).document

                document.select("div.feature-display div.slide-content a.feature-link")
                    .mapNotNull { it.toMainPageResult(request.name) }
                    .distinctBy { it.url }

            }
            "Top 10" ->{

                val document = app.get(request.data).document
                document.select("div.top-10-flex a.file-card")
                    .mapNotNull { it.toMainPageResult(request.name) }
                    .distinctBy { it.url }

            }
            "Top 100"->{
                val document = app.get(request.data).document
                document.select("div.divtableinside ul > li:has(a[href^=/torrents/details/])")
                    .mapNotNull { it.toMainPageResult(request.name) }
                    .distinctBy { it.url }
            }
            "Catalog" ->{
                val document = app.get(request.data).document
                document.select("div.cataloginside span")
                    .mapNotNull { it.toMainPageResult(request.name) }
                    .distinctBy { it.url }
            }

            else -> {
                val document = app.get(request.data).document
                document.select("ul > li > span.toral")
                    .mapNotNull { it.toMainPageResult(request.name) }
                    .distinctBy { it.url }
            }
        }



        return newHomePageResponse(
            list = listOf(
                HomePageList(
                    name = request.name,
                    list = result,
                    isHorizontalImages = true
                )
            ),
            hasNext = false
        )
    }

    private fun Element.toMainPageResult(name: String): SearchResponse? {
        var title: String
        var url: String
        var image: String?
        var quality: SearchQuality?
        var year: Int?

        when (name) {
            "Trending" -> {
                title = selectFirst(".feature-title")?.text().orEmpty()
                url = attr("href")
                image = selectFirst("img.feature-img")?.attr("src")
            }

            "Top 10" -> {
                    title = selectFirst("h4.title")?.text().orEmpty()
                    url = attr("href")
                    image = selectFirst("img.poster")?.attr("src")
            }

            "Top 100" -> {
                val link = selectFirst("a[href^=/torrents/details/]")

                title = link?.text().orEmpty()
                url = link?.attr("href").orEmpty()
                image = selectFirst("img.floaterimg")?.attr("src")
            }

            "Catalog" -> {
                val link = selectFirst("a[href^=/torrents/details/]")

                title = link?.text().orEmpty()
                url = link?.attr("href").orEmpty()
                image = link?.selectFirst("img.catalogimg")?.attr("src")
            }

            else -> {
                val link = selectFirst("a[href^=/torrents/details/]")

                title = link?.text().orEmpty()
                url = link?.attr("href").orEmpty()
                image = selectFirst("img.floaterimg")?.attr("src")
            }
        }

        title = title.trim()
        if (title.isBlank() || url.isBlank()) return null

        url = fixUrl(url)
        image = image?.takeIf { it.isNotBlank() }?.let(::fixUrl)

        quality = getQualityFromString(title)

        year = Regex("""\b(19\d{2}|20\d{2})\b""")
            .find(title)
            ?.value
            ?.toIntOrNull()

        return newMovieSearchResponse(title, url, TvType.Movie) {
            posterUrl = image
            this.quality = quality
            this.year = year
        }
    }

    override suspend fun search(
        query: String,
        page: Int
    ): SearchResponseList {
        val key = query.trim()

        val urls = searchPageUrls.getOrPut(key) {
            java.util.concurrent.ConcurrentHashMap()
        }

        if (page == 1) {
            urls.clear()
            urls[1] = getSearchUrl(mainUrl,key)
        }

        val url = urls[page]
            ?: return newSearchResponseList(emptyList(), hasNext = false)

        val document = app.get(url).document

        val results = document
            .select("ul > li > span.toral")
            .mapNotNull { it.toMainPageResult("Search") }
            .distinctBy { it.url }

        val nextPageUrl = document
            .selectFirst("a[title='Next Page']")
            ?.attr("href")
            ?.takeIf { it.isNotBlank() }
            ?.let(::fixUrl)

        if (nextPageUrl != null) {
            urls[page + 1] = nextPageUrl
        } else {
            urls.remove(page + 1)
        }

        return newSearchResponseList(
            results,
            hasNext = nextPageUrl != null
        )
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst(".detailsdiv h1")
            ?.text()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val poster = document.selectFirst(".detailsimg img")
            ?.let { fixUrlNull(it.absUrl("src").ifBlank { it.attr("src") }) }

        val detailRows = document.select(".detailsdescr li")

        fun detailValue(label: String): String? {
            val row = detailRows.firstOrNull {
                it.selectFirst("span")
                    ?.text()
                    ?.trim()
                    .equals(label, ignoreCase = true)
            }

            return row?.select("span")
                ?.lastOrNull()
                ?.text()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }

        val category = detailValue("Category")
        val size = detailValue("Size")
        val collection = detailValue("Collection")

        val peersRow = detailRows.firstOrNull {
            it.selectFirst("span")
                ?.text()
                ?.trim()
                .equals("Peers", ignoreCase = true)
        }

        val seeders = peersRow?.selectFirst(".see")?.text()?.trim()
        val leechers = peersRow?.selectFirst(".lee")?.text()?.trim()

        val descriptionHtml = document.selectFirst(".description")
            ?.html()
            .orEmpty()

        val durationText = Regex(
            """Duration[^:]*:\s*([^<\r\n]+)""",
            RegexOption.IGNORE_CASE
        ).find(descriptionHtml)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()

        val durationMinutes = durationText?.let { text ->
            val match = Regex(
                """(?:(\d+)\s*h)?\s*(?:(\d+)\s*min)?\s*(?:(\d+)\s*s)?""",
                RegexOption.IGNORE_CASE
            ).matchEntire(text) ?: return@let null

            val totalSeconds =
                (match.groupValues[1].toIntOrNull() ?: 0) * 3600 +
                        (match.groupValues[2].toIntOrNull() ?: 0) * 60 +
                        (match.groupValues[3].toIntOrNull() ?: 0)

            if (totalSeconds > 0) (totalSeconds + 30) / 60 else null
        }

        val year = Regex("""\b(19\d{2}|20\d{2})\b""")
            .find(title)
            ?.value
            ?.toIntOrNull()

        val magnetLink = document
            .selectFirst("a[title='Magnet Link']")
            ?.attr("href")
            ?.takeIf { it.startsWith("magnet:") }

        val screenshots = document.select(".description img[src]")
            .mapNotNull { img ->
                val imageUrl = img.absUrl("src")
                    .ifBlank { img.attr("src") }

                fixUrlNull(imageUrl)
                    ?.takeIf { it.isNotBlank() }
            }
            .distinct()

        val episodes = screenshots.mapIndexed { index, imageUrl ->
            newEpisode(magnetLink ?: imageUrl) {
                name = "Screenshot ${index + 1}"
                season = 1
                episode = index + 1
                posterUrl = imageUrl
            }
        }

        val plot = buildList {
            category?.let { add("Category: $it") }
            size?.let { add("Size: $it") }

            if (seeders != null || leechers != null) {
                add("Seeders: ${seeders ?: "Unknown"}")
                add("Leechers: ${leechers ?: "Unknown"}")
            }

            collection?.let { add("Collection: $it") }
            durationText?.let { add("Duration: $it") }
        }.joinToString("\n").takeIf { it.isNotBlank() }

        return newTvSeriesLoadResponse(
            title,
            url,
            TvType.NSFW,
            episodes
        ) {
            posterUrl = poster
            this.year = year
            this.duration = durationMinutes
            this.plot = plot
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val magnetLink = data.trim()

        if (!magnetLink.startsWith("magnet:?", ignoreCase = true)) {
            return false
        }

        callback(
            newExtractorLink(
                source = this.name,
                name = "Torrent Magnet",
                url = magnetLink,
                type = ExtractorLinkType.MAGNET
            )
        )

        return true
    }
}