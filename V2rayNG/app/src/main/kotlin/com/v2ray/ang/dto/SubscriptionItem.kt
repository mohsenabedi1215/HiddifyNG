package com.v2ray.ang.dto

data class SubscriptionItem(
    var remarks: String = "",
    var url: String = "",
    var enabled: Boolean = true,

    var addedTime: Long = System.currentTimeMillis(),
    var lastUpdated: Long = -1,
    var autoUpdate: Boolean = false,
    var updateInterval: Int? = null,

    var used: Long = -1,
    var total: Long = -1,
    var expire: Long = -1,//in ms
    var home_link: String = "",
    var support_link: String = "",
    var dns: String? = null
) {


    fun needUpdate(): Boolean {
        if (!url.startsWith("http")) return false
        if (updateInterval==null ||  updateInterval!! < 0) return false
        var hours = (System.currentTimeMillis() - lastUpdated) / (1000 * 60 * 60)
        return (hours >= updateInterval!!)


    }
}
