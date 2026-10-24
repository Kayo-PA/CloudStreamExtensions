package com.kayo.helper

import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

 val searchPageUrls =
    ConcurrentHashMap<
            String,
            ConcurrentHashMap<Int, String>
            >()

private val searchFilters = mapOf(
    "4k" to "4",
    "1080p" to "2",
    "movies" to "3",
    "mega" to "7"
)

fun getSearchUrl(mainUrl:String,query: String): String {
    val cleanedQuery = query.trim()

    if (cleanedQuery.equals("latest", ignoreCase = true)) {
        return "$mainUrl/torrents/browse/all/"
    }

    val parts = cleanedQuery.split(",", limit = 2)
    val filter = parts[0].trim().lowercase()
    val searchTerm = parts.getOrNull(1)?.trim()
    val category = searchFilters[filter]

    return if (category != null && !searchTerm.isNullOrBlank()) {
        "$mainUrl/torrents/search/$category/${encodeSearchQuery(searchTerm)}"
    } else {
        "$mainUrl/torrents/search/all/${encodeSearchQuery(cleanedQuery)}"
    }
}

private fun encodeSearchQuery(query: String): String =
    URLEncoder.encode(query, "UTF-8").replace("+", "%20")