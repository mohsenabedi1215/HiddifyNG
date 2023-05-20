//package com.hiddify.ang.downloader
//
//
//
//
//import android.util.Log
//import com.google.api.Authentication
//import com.v2ray.ang.AppConfig
//import com.v2ray.ang.BuildConfig
//import com.v2ray.ang.util.Utils
//
//import java.net.URL
//import khttp.get
//import khttp.structures.authorization.Authorization
//import khttp.structures.authorization.BasicAuthorization
//
//class KHttpDownloader:HTTPDownloader {
//
//    override fun download(urlStr:String,timeout: Int): Utils.Response {
//
//        val url = URL(urlStr)
//        val r=get(urlStr, allowRedirects = true, timeout = timeout.toDouble(),
//            auth = BasicAuthorization(url.userInfo?.split(":")?.get(0)?:"",url.userInfo?.split(":")?.get(1)?:""),
//            headers = mapOf(
//            Pair("User-Agent", "HiddifyNG/${BuildConfig.VERSION_NAME}")
//        ))
//
//
//        val headersMap = r.headers
//            .mapValues { (key, values) -> listOf(values)}
//        return Utils.Response(headersMap, r.text, urlStr, r.content)
//    }
//
//}