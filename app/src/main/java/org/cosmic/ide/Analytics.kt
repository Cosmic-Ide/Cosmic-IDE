package org.cosmic.ide

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import java.net.URL
import org.cosmic.ide.activity.BaseActivity

object Analytics {

    private val PROJECT_ID = "8a8923dd-c722-43b7-b4bf-26483877ea58"
    private val url = "https://app.piratepx.com/ship?"

    @JvmStatic
    fun onAppStarted() {
        if (!isNetworkAvailable(App.context)) return
        URL(url + "p=${ PROJECT_ID }&i=App").openStream()
    }

    @JvmStatic
    fun onActivity(activity: BaseActivity) {
        if (!isNetworkAvailable(activity)) return
        URL(url + "p=${ PROJECT_ID }&i=${ activity::class.simpleName }").openStream()
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        return true
                    }
                }
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                return true
            }
        }
        return false
    }
}
