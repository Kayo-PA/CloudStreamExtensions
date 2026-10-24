package com.kayo.extractor

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.newExtractorLink

open class WatchStreamUs2(
    override val name: String = "WatchStreamUs",
    override val mainUrl: String = "https://player.watchstreamus.com",
    override val requiresReferer: Boolean = false
) : WatchStreamUs()

open class WatchStreamUs(
    override val name: String = "WatchStreamUs",
    override val mainUrl: String = "https://cdn.watchstreamus.com",
    override val requiresReferer: Boolean = false
) : ExtractorApi() {
    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        runCatching {

            val resp = app.get(url).document

            val url = resp.select("body.embed-page div.player-shell").attr("data-src")

            callback(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = mainUrl+url,
                    type = ExtractorLinkType.M3U8
                ) {
                    this.referer = "$mainUrl/"
                }
            )

        }.getOrNull()
    }

}