package com.hiddify.ang.downloader

import com.v2ray.ang.util.Utils

interface HTTPDownloader {
    fun download(urlStr:String,timeout: Int=10000): Utils.Response ;
}
