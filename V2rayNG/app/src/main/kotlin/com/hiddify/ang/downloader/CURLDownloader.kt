//package com.hiddify.ang.downloader
//
//
//
//
//import android.util.Log
//import com.v2ray.ang.AppConfig
//import com.v2ray.ang.BuildConfig
//import com.v2ray.ang.util.Utils
//
//import org.symphonyoss.symphony.jcurl.JCurl
//import java.net.URL
//import java.nio.charset.Charset
//
//class CURLDownloader:HTTPDownloader {
//
//    override fun download(urlStr:String,timeout: Int): Utils.Response {
//        val url = URL(urlStr)
//        val jcurl = JCurl.builder()
//            .method(JCurl.HttpMethod.GET)
//            .header("User-Agent", "HiddifyNG/${BuildConfig.VERSION_NAME}")
//        url.userInfo?.let {
//            jcurl.header("Authorization", "Basic ${Utils.encode(Utils.urlDecode(it))}")
//        }
//        val curl=jcurl.build();
//        val connection = curl.connect(urlStr);
//        val contentBytes = connection.inputStream.use {
//            it.readBytes()
//        }
//        val content=String(contentBytes, try{
//            Charset.forName(connection.contentEncoding)}catch (e:Exception){Charsets.UTF_8})
//        val response = curl.processResponse(connection);
//        return Utils.Response(response.headers, content, urlStr, contentBytes)
//
//
//    }
//
//}