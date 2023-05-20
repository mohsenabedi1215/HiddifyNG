//package com.hiddify.ang.downloader
//
//import android.util.Log
//import com.v2ray.ang.AppConfig
//import com.v2ray.ang.BuildConfig
//import com.v2ray.ang.util.Utils
//import org.http4k.client.*
//import org.http4k.core.HttpHandler
//import org.http4k.core.Method
//import org.http4k.core.Request
//import java.net.URL
//
//class HTTP4KDownloader:HTTPDownloader {
//
//    override fun download(urlStr:String,timeout: Int): Utils.Response {
//        val clients= mutableListOf(
//            ApacheClient(),
//            OkHttp(),
//            Java8HttpClient(),
////            Apache4Client(),
//            Fuel()
//        )
//        var error:java.lang.Exception?=null;
//        for (client in clients){
//            try{
//                Log.d(AppConfig.ANG_PACKAGE,"Downloading using "+client.javaClass.name)
//                return _download(urlStr,client)
//            }catch (e:java.lang.Exception){
//                Log.e(AppConfig.ANG_PACKAGE,client.javaClass.name+" Error "+e.toString())
//                error=e
//            }
//        }
//        throw error!!
//    }
//    fun _download(urlStr: String, client: HttpHandler): Utils.Response {
//
//        val url = URL(urlStr)
//        val req = Request(Method.GET, urlStr)
//            .header("User-Agent", "HiddifyNG/${BuildConfig.VERSION_NAME}")
//            .header("Connection", "close")
//
//        url.userInfo?.let {
//            req.header("Authorization", "Basic ${Utils.encode(Utils.urlDecode(it))}")
//        }
//        val call = client(req)
//        val headersMap = call.headers
//            .groupBy { it.first.lowercase() }
//            .mapValues { (_, values) -> values.map { it.second } } as Map<String, List<String>>
//        return Utils.Response(headersMap, call.bodyString(), urlStr, call.body.payload.array())
//    }
//
//}