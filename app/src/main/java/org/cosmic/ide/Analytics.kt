package org.cosmic.ide

import java.net.URL
import org.cosmic.ide.activity.BaseActivity

object Analytics {

    private val PROJECT_ID = "8a8923dd-c722-43b7-b4bf-26483877ea58"
    private val url = "https://app.piratepx.com/ship?"

    @JvmStatic
    fun onAppStarted() {
        URL(url + "p=${ PROJECT_ID }&i=App").openStream()
    }

    @JvmStatic
    fun onActivity(activity: BaseActivity) {
        URL(url + "p=${ PROJECT_ID }&i=${ activity::class.simpleName }").openStream()
    }
}