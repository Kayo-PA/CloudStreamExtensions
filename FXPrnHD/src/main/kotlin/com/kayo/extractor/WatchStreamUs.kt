package com.kayo.extractor

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.newExtractorLink

open class WatchStreamUs(
    override val name: String = "WatchStreamUs",
    override val mainUrl: String = "https://cdn.watchstreamus.com",
    override val requiresReferer: Boolean = false
) : ExtractorApi() {
    val MainUrl2 =  "https://player.watchstreamus.com"
    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        var selectedUrl:String = mainUrl
        runCatching {
            if (url.contains(MainUrl2)){
                selectedUrl = MainUrl2
            }

            val resp = app.get(url).document

            val url = resp.select("body.embed-page div.player-shell").attr("data-src")

            callback(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = selectedUrl+url,
                    type = ExtractorLinkType.M3U8
                ) {
                    this.referer = "$mainUrl/"
                }
            )

        }.getOrNull()
    }

}