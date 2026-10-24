package com.kayo

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class XXXClubPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(XXXClub())
    }
}