package com.kayo

import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.security.MessageDigest

open class Xtremestream : ExtractorApi() {
    override var name = "Xtremestream"
    override var mainUrl = "https://pervl2.xtremestream.xyz"
    override val requiresReferer = true

    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36"

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        try {
            resolve(url, referer, callback)
        } catch (e: Throwable) {
        }
    }

    private suspend fun resolve(url: String, referer: String?, callback: (ExtractorLink) -> Unit) {
        val uri = URI(url)
        val origin = "${uri.scheme}://${uri.host}"

        val page = app.get(
            url,
            referer = referer ?: "https://tube.perverzija.com/",
            headers = mapOf("User-Agent" to userAgent)
        ).text
        val boot = Regex("""XSFTK\.boot\(\s*["']([^"']+)["']\s*,\s*["']([^"']+)["']""").find(page)
        if (boot == null) {
            return
        }
        val videoId = boot.groupValues[1]
        val token = boot.groupValues[2]

        fun api(op: String) = "$origin/player/proto_api.php?data=$videoId&op=$op&vt=${
            URLEncoder.encode(
                token,
                "UTF-8"
            )
        }"

        val apiHeaders = mapOf("User-Agent" to userAgent, "Referer" to url, "X-XS-P" to "1")

        val key = app.get(api("key"), headers = apiHeaders).okhttpResponse.body.bytes()
        val raw = app.get(api("manifest"), headers = apiHeaders).okhttpResponse.body.bytes()

        val mask = MessageDigest.getInstance("SHA-256").digest("M|$videoId|$token".toByteArray())
        val manifest =
            JSONObject(String(ByteArray(raw.size) { (raw[it].toInt() xor mask[it % mask.size].toInt()).toByte() }))

        val cdn = manifest.opt("cdn")
        val hosts: List<String> = when (cdn) {
            is JSONObject -> cdn.keys().asSequence().sortedBy { it.toIntOrNull() ?: 0 }
                .map { cdn.getString(it) }.toList()

            is JSONArray -> (0 until cdn.length()).map { cdn.getString(it) }
            else -> emptyList()
        }
        if (hosts.isEmpty()) {
            return
        }
        val keyUri = LocalResourceServer.register(key, "application/octet-stream", "key")
        val headers = mapOf(
            "User-Agent" to userAgent,
            "Referer" to "$origin/",
            "Origin" to origin,
            "Accept" to "*/*"
        )

        manifest.keys().asSequence().mapNotNull { it.toIntOrNull() }.sorted().forEach { q ->
            val entry = manifest.optJSONObject(q.toString()) ?: return@forEach
            val durs = entry.optJSONArray("durs") ?: return@forEach
            if (durs.length() == 0) return@forEach

            val playlist = buildString {
                append("#EXTM3U\n#EXT-X-VERSION:3\n")
                append(
                    "#EXT-X-TARGETDURATION:${
                        entry.optInt(
                            "td",
                            10
                        )
                    }\n#EXT-X-MEDIA-SEQUENCE:0\n#EXT-X-PLAYLIST-TYPE:VOD\n"
                )
                append("#EXT-X-KEY:METHOD=AES-128,URI=\"$keyUri\"\n")
                for (i in 0 until durs.length()) {
                    append("#EXTINF:${durs.get(i)},\nhttps://${hosts[i % hosts.size]}/cdn/down/$videoId/${q}_enc/$q$i.html\n")
                }
                append("#EXT-X-ENDLIST\n")
            }
            val playlistUri = LocalResourceServer.register(
                playlist.toByteArray(),
                "application/vnd.apple.mpegurl",
                "m3u8"
            )
            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = "${this.name} ${q}p",
                    url = playlistUri,
                    type = ExtractorLinkType.M3U8
                ) {
                    this.referer = "$origin/"
                    this.quality = q
                    this.headers = headers
                })
        }
    }

}
